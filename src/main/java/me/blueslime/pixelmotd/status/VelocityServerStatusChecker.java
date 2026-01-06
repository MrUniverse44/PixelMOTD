package me.blueslime.pixelmotd.status;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import me.blueslime.slimelib.colors.platforms.velocity.DefaultSlimeColor;
import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.Configuration;
import me.blueslime.pixelmotd.initialization.velocity.VelocityMOTD;
import me.blueslime.slimelib.file.configuration.ConfigurationHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Velocity-based server status checker using an ExecutorService
 * to handle socket reachability checks off the scheduler thread.
 *
 * <p>
 * This design provides stronger control over concurrency, avoids blocking
 * Velocity scheduler threads and scales better for large networks.
 * </p>
 */
public class VelocityServerStatusChecker implements StatusChecker {

    private final PixelMOTD<ProxyServer> plugin;

    private final ConcurrentHashMap<String, Boolean> statusMap = new ConcurrentHashMap<>();
    private final List<ScheduledTask> schedulerTasks = new CopyOnWriteArrayList<>();

    private ExecutorService executor;

    private volatile ConfigurationHandler control;
    private volatile int pingTimeout = 500;

    private volatile String online;
    private volatile String offline;

    /**
     * Creates a new VelocityServerStatusChecker instance.
     *
     * @param plugin PixelMOTD plugin instance
     */
    public VelocityServerStatusChecker(PixelMOTD<ProxyServer> plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        load();
    }

    /**
     * Loads configuration values and initializes tasks when enabled.
     */
    private void load() {
        this.control = plugin.getConfigurationHandler(Configuration.SETTINGS);

        this.online = legacy(control.getString("settings.server-status.online", "&a&lONLINE"));
        this.offline = legacy(control.getString("settings.server-status.offline", "&c&lOFFLINE"));

        if (control.getStatus("settings.server-status.toggle")) {
            start();
        } else {
            stop();
        }
    }

    /**
     * Reloads configuration and restarts tasks if needed.
     */
    public void update() {
        load();
    }

    /**
     * Converts legacy colored strings into serialized legacy format.
     *
     * @param content raw content
     * @return legacy serialized string
     */
    private String legacy(String content) {
        Component component = new DefaultSlimeColor(content, true).build();
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    /**
     * Starts the scheduler tasks and executor service.
     * Existing resources are stopped before starting new ones.
     */
    public synchronized void start() {
        stop();

        int pingOnline = control.getInt("settings.server-status.intervals.online", 10);
        int pingOffline = control.getInt("settings.server-status.intervals.offline", 10);
        this.pingTimeout = Math.max(0, control.getInt("settings.server-status.intervals.timeout", 500));

        if (pingOnline <= 0 && pingOffline <= 0) {
            return;
        }

        // Executor for socket reachability checks
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        this.executor = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "PixelMOTD-StatusChecker");
            t.setDaemon(true);
            return t;
        });

        ProxyServer proxy = plugin.getPlugin();
        VelocityMOTD motd = VelocityMOTD.getInstance();

        // Offline / full scan task
        if (pingOffline > 0) {
            ScheduledTask task = proxy.getScheduler()
                    .buildTask(motd, () -> {
                        if (control.getStatus("settings.server-status.toggle")) {
                            refreshStatusMap(proxy.getAllServers());
                        } else {
                            stop();
                        }
                    })
                    .delay(pingOffline, TimeUnit.SECONDS)
                    .repeat(pingOffline, TimeUnit.SECONDS)
                    .schedule();

            schedulerTasks.add(task);
        }

        // Online-only refresh task
        if (pingOnline > 0) {
            ScheduledTask task = proxy.getScheduler()
                    .buildTask(motd, () -> {
                        if (control.getStatus("settings.server-status.toggle")) {
                            refreshStatusMap(getOnlineServers());
                        } else {
                            stop();
                        }
                    })
                    .delay(pingOnline, TimeUnit.SECONDS)
                    .repeat(pingOnline, TimeUnit.SECONDS)
                    .schedule();

            schedulerTasks.add(task);
        }

        // Initial immediate scan
        refreshStatusMap(proxy.getAllServers());
    }

    /**
     * Returns a collection of servers currently marked as online.
     *
     * @return collection of online RegisteredServer
     */
    public Collection<RegisteredServer> getOnlineServers() {
        ProxyServer proxy = plugin.getPlugin();

        return statusMap.entrySet()
                .stream()
                .filter(e -> Boolean.TRUE.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .map(proxy::getServer)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    /**
     * Refreshes the status map for the provided servers.
     * Servers with connected players are marked online immediately.
     * Others are checked asynchronously via ExecutorService.
     *
     * @param servers servers to check
     */
    public void refreshStatusMap(Collection<RegisteredServer> servers) {
        if (servers == null || servers.isEmpty()) {
            return;
        }

        for (RegisteredServer server : servers) {
            if (!server.getPlayersConnected().isEmpty()) {
                setStatus(server, true);
                continue;
            }

            executor.submit(() -> {
                boolean reachable = isReachable(server.getServerInfo().getAddress());
                setStatus(server, reachable);
            });
        }
    }

    /**
     * Attempts to open a socket connection to the server address.
     *
     * @param address server socket address
     * @return true if reachable within timeout
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
     * Updates the status map for the given server.
     *
     * @param server registered server
     * @param online online status
     */
    private void setStatus(RegisteredServer server, boolean online) {
        if (server == null || server.getServerInfo() == null) {
            return;
        }
        statusMap.put(server.getServerInfo().getName(), online);
    }

    /**
     * Returns the last known status of a server.
     *
     * @param server registered server
     * @return Boolean status or null if unknown
     */
    public Boolean getStatus(RegisteredServer server) {
        if (server == null || server.getServerInfo() == null) {
            return null;
        }
        return statusMap.get(server.getServerInfo().getName());
    }

    /**
     * Stops all scheduler tasks and shuts down the executor service.
     */
    public synchronized void stop() {
        for (ScheduledTask task : schedulerTasks) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }
        schedulerTasks.clear();

        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        statusMap.clear();
    }

    /**
     * Returns an immutable snapshot of the current status map.
     *
     * @return unmodifiable map of server statuses
     */
    public Map<String, Boolean> getStatusMap() {
        return Collections.unmodifiableMap(new HashMap<>(statusMap));
    }

    /**
     * Returns the localized status string for a server name.
     *
     * @param server server name
     * @return online or offline status string
     */
    public String getServerStatus(String server) {
        return Boolean.TRUE.equals(statusMap.get(server)) ? online : offline;
    }
}
