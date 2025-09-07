package me.blueslime.pixelmotd.motd.builder;

import me.blueslime.pixelmotd.Configuration;
import me.blueslime.pixelmotd.motd.setup.MotdSetup;
import me.blueslime.pixelmotd.utils.placeholders.ConditionEvaluator;
import me.blueslime.slimelib.file.configuration.ConfigurationHandler;
import me.blueslime.slimelib.logs.SlimeLogs;
import me.blueslime.pixelmotd.motd.CachedMotd;
import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.motd.builder.favicon.FaviconModule;
import me.blueslime.pixelmotd.motd.builder.hover.HoverModule;
import me.blueslime.pixelmotd.utils.placeholders.PluginPlaceholders;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("unused")
public abstract class PingBuilder<T, I, E, H> {

    private final List<CachedMotd> motdList = new ArrayList<>();
    private final FaviconModule<T, I> faviconModule;
    private final HoverModule<H> hoverModule;

    private boolean iconSystem = false;

    private final PixelMOTD<T> plugin;
    private final PluginPlaceholders pluginPlaceholders;

    public PingBuilder(PixelMOTD<T> plugin, FaviconModule<T, I> faviconModule, HoverModule<H> hoverModule) {
        this.faviconModule = faviconModule;
        this.hoverModule = hoverModule;
        this.plugin = plugin;
        this.pluginPlaceholders = new PluginPlaceholders(plugin);
    }

    public void update() {
        load();
        faviconModule.update();
    }

    private void load() {
        ConfigurationHandler settings = plugin.getSettings();

        if (settings != null) {
            iconSystem = settings.getBoolean("settings.icon-system", false);
        } else {
            iconSystem = true;
            plugin.getLogs().error("Can't load settings data");
        }

        motdList.clear();

        ConfigurationHandler motdsFile = plugin.getConfiguration(Configuration.MOTDS);

        if (motdsFile.contains("motds")) {
            for (String motdId : motdsFile.getContent("motds", false)) {
                motdList.add(new CachedMotd(motdsFile, "motds." + motdId + "."));
            }
        }

        // Sort MOTDs by priority from highest to lowest.
        motdList.sort(Comparator.comparingInt(CachedMotd::getPriority).reversed());
    }

    public CachedMotd fetchMotd(int protocol, String domain, boolean userIsBlacklisted) {
        // Collect all variables needed for condition evaluation.
        Map<String, Object> variables = new HashMap<>();
        variables.put("client_protocol", protocol);
        variables.put("client_used_domain", domain != null ? domain.toLowerCase(Locale.ENGLISH) : "");
        variables.put("server_whitelist_status", plugin.getConfiguration(Configuration.WHITELIST).getStatus("enabled", false));
        variables.put("client_support_hex_colors", protocol >= 735);
        variables.put("client_is_blacklisted", userIsBlacklisted);
        ConfigurationHandler events = plugin.getConfiguration(Configuration.EVENTS);

        boolean eventsActive = false;

        for (String eventName : events.getContent("events", false)) {
            String path = "events." + eventName + ".";
            String eventNameValue = events.getString(path + "name", "Example Event 001");
            String timeZoneValue = events.getString(path + "time-zone", "12/21/24 23:59:00");
            Date eventDate = PluginPlaceholders.getSpecifiedEvent(events, eventName);
            if (eventDate != null) {
                long difference = eventDate.getTime() - System.currentTimeMillis();
                variables.put("server_event_" + eventName + "_is_active", difference >= 0L);
                if (difference >= 0L) {
                    eventsActive = true;
                }
            }
        }

        variables.put("server_events_running", eventsActive);

        for (CachedMotd motd : motdList) {
            if (evaluateConditions(motd, variables)) {
                return motd;
            }
        }

        // Return a random MOTD from the list if none of the conditions are met.
        // This acts as the default fallback.
        if (!motdList.isEmpty()) {
            return motdList.get(ThreadLocalRandom.current().nextInt(motdList.size()));
        }

        return null;
    }

    private boolean evaluateConditions(CachedMotd motd, Map<String, Object> variables) {
        if (motd.getConditionSet().isEmpty()) {
            return true;
        }

        for (String condition : motd.getConditionSet()) {
            ConditionEvaluator evaluator = new ConditionEvaluator(condition, variables);
            if (!evaluator.evaluate()) {
                return false;
            }
        }
        return true;
    }

    public SlimeLogs getLogs() {
        return plugin.getLogs();
    }

    public abstract void execute(E ping, MotdSetup setup);

    public PixelMOTD<T> getPlugin() {
        return plugin;
    }

    public FaviconModule<T, I> getFavicon() {
        return faviconModule;
    }

    public HoverModule<H> getHoverModule() {
        return hoverModule;
    }

    public boolean isIconSystem() {
        return iconSystem;
    }

    public boolean isDebug() {
        return plugin.getSettings().getBoolean("settings.debug-mode", false);
    }

    public PluginPlaceholders getExtras() {
        return pluginPlaceholders;
    }
}