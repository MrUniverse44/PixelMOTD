package me.blueslime.pixelmotd.status.types;

import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.Configuration;
import me.blueslime.pixelmotd.status.StatusChecker;
import me.blueslime.slimelib.file.configuration.ConfigurationHandler;
import me.blueslime.slimelib.file.configuration.TextDecoration;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * BungeeCord implementation of a server status checker that offloads socket checks
 * to an ExecutorService for better control and to avoid blocking proxy scheduler threads.
 */
public class BungeeServerStatusChecker implements StatusChecker {

    private final ConcurrentHashMap<String, Boolean> statusMap = new ConcurrentHashMap<>();
    private final List<ScheduledTask> pingTasks = new CopyOnWriteArrayList<>();
    private final PixelMOTD<Plugin> plugin;

    private volatile ConfigurationHandler control;
    private volatile ExecutorService executor;

    private volatile int pingTimeout = 500;

    private volatile String online;
    private volatile String offline;

    /**
     * Constructs the checker and loads configuration.
     *
     * @param plugin plugin instance (not null)
     */
    public BungeeServerStatusChecker(PixelMOTD<Plugin> plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        load();
    }

    /**
     * Loads configuration values and starts the checker if enabled.
     * Safe to call on reload.
     */
    private void load() {
        this.control = plugin.getConfigurationHandler(Configuration.SETTINGS);
        this.online = control.getString(TextDecoration.LEGACY, "settings.server-status.online", "&a&lONLINE");
        this.offline = control.getString(TextDecoration.LEGACY, "settings.server-status.offline", "&c&lOFFLINE");

        if (control.getStatus("settings.server-status.toggle")) {
            start();
        } else {
            stop();
        }
    }

    /**
     * Reloads configuration and restarts tasks if necessary.
     */
    public void update() {
        load();
    }

    /**
     * Starts scheduled tasks and prepares the executor.
     * Existing tasks and executor will be stopped first.
     */
    public synchronized void start() {
        stop();

        int pingOnline = control.getInt("settings.server-status.intervals.online", 10);
        int pingOffline = control.getInt("settings.server-status.intervals.offline", 10);
        this.pingTimeout = Math.max(0, control.getInt("settings.server-status.intervals.timeout", 500));

        if (pingOnline <= 0 && pingOffline <= 0) {
            return; // nothing to schedule
        }

        // executor threads can be configured via settings (optional)
        int defaultThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        int threads = control.getInt("settings.server-status.executor-threads", defaultThreads);
        threads = Math.max(1, threads);

        this.executor = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "PixelMOTD-BungeeStatusChecker");
            t.setDaemon(true);
            return t;
        });

        Plugin p = plugin.getPlugin();

        // If both intervals equal and non-zero -> single repeating task that scans all servers
        if (pingOnline == pingOffline) {
            //noinspection ConstantValue
            if (pingOnline == 0) {
                return;
            }
            ScheduledTask task = p.getProxy().getScheduler().schedule(
                    p,
                    () -> {
                        if (control.getStatus("settings.server-status.toggle")) {
                            refreshStatusMap(p.getProxy().getServers().values());
                        } else {
                            stop();
                        }
                    },
                    10L,
                    pingOnline,
                    TimeUnit.SECONDS
            );
            pingTasks.add(task);
        } else {
            // initial immediate refresh for responsiveness
            refreshStatusMap(p.getProxy().getServers().values());

            // online-only periodic refresh
            if (pingOnline > 0) {
                ScheduledTask onlineTask = p.getProxy().getScheduler().schedule(
                        p,
                        () -> {
                            if (control.getStatus("settings.server-status.toggle")) {
                                refreshStatusMap(getOnlineServers());
                            } else {
                                stop();
                            }
                        },
                        10L,
                        pingOnline,
                        TimeUnit.SECONDS
                );
                pingTasks.add(onlineTask);
            }

            // offline/full periodic refresh
            if (pingOffline > 0) {
                ScheduledTask offlineTask = p.getProxy().getScheduler().schedule(
                        p,
                        () -> {
                            if (control.getStatus("settings.server-status.toggle")) {
                                refreshStatusMap(getOfflineServers());
                            } else {
                                stop();
                            }
                        },
                        10L,
                        pingOffline,
                        TimeUnit.SECONDS
                );
                pingTasks.add(offlineTask);
            }
        }
    }

    /**
     * Returns servers that are currently considered online according to the internal status map.
     *
     * @return collection of ServerInfo marked online
     */
    public Collection<ServerInfo> getOnlineServers() {
        return statusMap.entrySet()
                .stream()
                .filter(e -> Boolean.TRUE.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .map(name -> plugin.getPlugin().getProxy().getServerInfo(name))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Returns servers that are currently considered offline according to the internal status map.
     *
     * @return collection of ServerInfo marked offline
     */
    public Collection<ServerInfo> getOfflineServers() {
        return statusMap.entrySet()
                .stream()
                .filter(e -> Boolean.FALSE.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .map(name -> plugin.getPlugin().getProxy().getServerInfo(name))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Refreshes status for a collection of servers. Servers that have players connected are
     * considered online immediately; servers with no players are checked asynchronously
     * by the ExecutorService to determine reachability.
     *
     * @param servers collection of ServerInfo to check
     */
    public void refreshStatusMap(Collection<ServerInfo> servers) {
        if (servers == null || servers.isEmpty()) {
            return;
        }

        for (final ServerInfo server : servers) {
            if (server.getPlayers().isEmpty()) {
                ExecutorService ex = this.executor;
                if (ex != null && !ex.isShutdown()) {
                    ex.submit(() -> {
                        @SuppressWarnings("deprecation") boolean reachable = isReachable((InetSocketAddress) server.getAddress());
                        setStatus(server, reachable);
                    });
                } else {
                    // fallback to marking offline when executor is not available
                    setStatus(server, false);
                }
            } else {
                setStatus(server, true);
            }
        }
    }

    /**
     * Attempts to open a socket to the provided address with the configured timeout.
     *
     * @param address InetSocketAddress to test
     * @return true if reachable within timeout; false otherwise
     */
    private boolean isReachable(InetSocketAddress address) {
        if (address == null) {
            return false;
        }
        try (Socket socket = new Socket()) {
            socket.connect(address, pingTimeout);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    /**
     * Set status for the server name in the internal map.
     *
     * @param server registered server
     * @param online online state
     */
    private void setStatus(ServerInfo server, boolean online) {
        if (server == null) {
            return;
        }
        statusMap.put(server.getName(), online);
    }

    /**
     * Returns the last known boolean status for a server, or null if unknown.
     *
     * @param server server info
     * @return Boolean status or null
     */
    public Boolean getStatus(ServerInfo server) {
        if (server == null) {
            return null;
        }
        return statusMap.get(server.getName());
    }

    /**
     * Stops all scheduled tasks and shuts down the executor service (if present).
     * Clears the internal status map.
     */
    public synchronized void stop() {
        // cancel scheduled tasks
        for (ScheduledTask task : pingTasks) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }
        pingTasks.clear();

        // shutdown executor
        if (executor != null) {
            try {
                executor.shutdownNow();
                //noinspection ResultOfMethodCallIgnored
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                executor = null;
            }
        }

        statusMap.clear();
    }

    /**
     * Returns an immutable snapshot of the internal status map.
     *
     * @return unmodifiable map of server name -> boolean
     */
    public Map<String, Boolean> getStatusMap() {
        return Collections.unmodifiableMap(new HashMap<>(statusMap));
    }

    /**
     * Returns the localized status string for a given server name (online/offline).
     *
     * @param server server name
     * @return online or offline display string
     */
    public String getServerStatus(String server) {
        return Boolean.TRUE.equals(statusMap.get(server)) ? online : offline;
    }
}
