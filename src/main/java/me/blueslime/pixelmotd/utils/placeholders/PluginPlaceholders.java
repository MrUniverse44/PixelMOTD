package me.blueslime.pixelmotd.utils.placeholders;

import me.blueslime.pixelmotd.Configuration;
import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.initialization.bungeecord.BungeeMOTD;
import me.blueslime.pixelmotd.initialization.velocity.VelocityMOTD;
import me.blueslime.pixelmotd.servers.platform.BungeeServerHandler;
import me.blueslime.pixelmotd.servers.Server;
import me.blueslime.pixelmotd.servers.platform.VelocityServerHandler;
import me.blueslime.pixelmotd.status.StatusChecker;
import me.blueslime.slimelib.file.configuration.ConfigurationHandler;
import me.blueslime.slimelib.file.configuration.TextDecoration;
import me.blueslime.pixelmotd.utils.OnlineList;
import me.blueslime.pixelmotd.utils.internal.storage.PluginStorage;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PluginPlaceholders {

    private final PluginStorage<String, List<String>> serversMap = PluginStorage.initAsConcurrentHash();
    private final PluginStorage<String, OnlineList> onlineMap = PluginStorage.initAsConcurrentHash();
    private final Map<String, CachedValue> onlineCache = new ConcurrentHashMap<>();
    private final Map<String, CachedValue> formatCache = new ConcurrentHashMap<>();
    private final static Pattern PLAYER_PATTERN = Pattern.compile("%player_(\\d)+%");
    private final boolean IS_VELOCITY_PLATFORM;
    private final boolean IS_BUNGEE_PLATFORM;
    private final PixelMOTD<?> plugin;
    private String prefix;
    private final int max;

    public PluginPlaceholders(PixelMOTD<?> plugin) {
        this.IS_VELOCITY_PLATFORM = plugin.getServerHandler() instanceof VelocityServerHandler;
        this.IS_BUNGEE_PLATFORM = plugin.getServerHandler() instanceof BungeeServerHandler;

        this.plugin = plugin;
        this.max    = plugin.getPlayerHandler().getMaxPlayers();


        load();
    }

    public void update() {
        load();
    }

    private void load() {
        serversMap.clear();
        onlineMap.clear();

        ConfigurationHandler settings = plugin.getConfiguration(Configuration.SETTINGS);

        String path = "settings.online-variables";

        prefix = settings.getString(path + ".prefix", "custom_online");

        if (settings.getStatus(path + ".enabled", false)) {
            for (String key : settings.getContent(path, false)) {
                if (!key.equalsIgnoreCase("prefix") && !key.equalsIgnoreCase("enabled")) {

                    OnlineList mode = OnlineList.fromText(
                            key,
                            settings.getString(path + "." + key + ".mode", "")
                    );

                    List<String> values = settings.getStringList(path + "." + key + ".values");

                    serversMap.set(key, values);
                    onlineMap.set(key, mode);
                }
            }
        }
    }

    public String replace(String message, int online, int max, String username) {

        return replaceServers(
                message.replace("%online%", "" + plugin.getPlayerHandler().getPlayersSize())
                .replace("%max%","" + this.max)
                .replace("%fake_online%", "" + online)
                .replace("%plugin_author%", "MrUniverse44")
                .replace("%whitelist_author%", getWhitelistAuthor())
                .replace("%user%", username)
                .replace("%fake_max%", "" + max)
                .replace("[box]", "▇")
        );
    }

    private String replaceServers(String message) {
        if (message.contains("%" + prefix + "_")) {
            if (onlineMap.size() != 0) {

                List<Server> serverList = plugin.getServerHandler().getServers();

                for (String key : onlineMap.getKeys()) {
                    int online;

                    if (onlineCache.containsKey(key)) {
                        CachedValue cachedValue = onlineCache.get(key);
                        if (!cachedValue.isExpired()) {
                            online = cachedValue.asIntValue();
                        } else {
                            online = switch (onlineMap.get(key)) {
                                case NAME -> getOnlineByNames(serverList, serversMap.get(key));
                                case CONTAINS -> getOnlineByContains(serverList, serversMap.get(key));
                            };
                            onlineCache.put(
                                key,
                                new CachedValue(
                                    online,
                                    plugin.getSettings().getLong("settings.online-variables.cache-expire-ms", 4000)
                                )
                            );
                        }
                    } else {
                        online = switch (onlineMap.get(key)) {
                            case NAME -> getOnlineByNames(serverList, serversMap.get(key));
                            case CONTAINS -> getOnlineByContains(serverList, serversMap.get(key));
                        };
                        onlineCache.put(
                            key,
                            new CachedValue(
                                online,
                                plugin.getSettings().getLong("settings.online-variables.cache-expire-ms", 4000)
                            )
                        );
                    }

                    message = message.replace("%" + prefix + "_" + key + "%", "" + online);
                }
            }
        }

        if (message.contains("%online_") || message.contains("%status_")) {

            StatusChecker checker = null;

            if (IS_VELOCITY_PLATFORM) {
                checker = VelocityMOTD.getInstance().getChecker();
            }

            if (IS_BUNGEE_PLATFORM) {
                checker = BungeeMOTD.getInstance().getChecker();
            }

            for (Server server : plugin.getServerHandler().getServers()) {
                message = message.replace("%online_" + server.getName() + "%", server.getOnline() + "");

                if (checker != null) {
                    message = message.replace(
                            "%status_" + server.getName() + "%",
                            checker.getServerStatus(server.getName())
                    );
                }
            }
        }
        return replaceEvents(message);
    }

    private String replaceEvents(String message) {
        ConfigurationHandler events = plugin.getConfiguration(Configuration.EVENTS);

        if (events.getBoolean("enabled", false)) {
            Pattern eventPattern = Pattern.compile("%event_([a-zA-Z0-9]+)_(name|timezone|timeleft|left)%", Pattern.CASE_INSENSITIVE);
            Matcher eventMatcher = eventPattern.matcher(message);
            StringBuilder resultMessage = new StringBuilder();

            while (eventMatcher.find()) {
                String eventName = eventMatcher.group(1);
                String placeholder = eventMatcher.group(2).toLowerCase(Locale.ENGLISH);

                String path = "events." + eventName + ".";
                String eventNameValue = events.getString(path + "name", "Example Event 001");
                String timeZoneValue = events.getString(path + "time-zone", "12/21/24 23:59:00");
                Date date = getSpecifiedEvent(events, eventName);

                String replacement = eventMatcher.group(0);

                if ("name".equals(placeholder)) {
                    replacement = eventNameValue;
                } else if ("timezone".equals(placeholder)) {
                    replacement = timeZoneValue;
                } else if (("timeleft".equals(placeholder) || "left".equals(placeholder)) && date != null) {
                    long difference = date.getTime() - new Date().getTime();
                    if (difference >= 0L) {
                        String formatName = events.getString(path + "display-format", "digital");
                        replacement = formatEventTime(eventName, formatName, events, difference);
                    } else {
                        replacement = events.getString(TextDecoration.LEGACY, path + "end-message", "&cThe event finished.");
                    }
                }
                eventMatcher.appendReplacement(resultMessage, Matcher.quoteReplacement(replacement));
            }
            eventMatcher.appendTail(resultMessage);
            return resultMessage.toString();
        }
        return message;
    }

    private String formatEventTime(String eventName, String formatName, ConfigurationHandler events, long time) {
        String cacheKey = eventName + ":" + formatName;
        CachedValue cachedValue = formatCache.get(cacheKey);

        if (cachedValue != null && !cachedValue.isExpired()) {
            return cachedValue.getValue();
        }

        Map<String, Long> timeValues = new HashMap<>();
        long secondsTotal = time / 1000;
        long minutesTotal = secondsTotal / 60;
        long hoursTotal = minutesTotal / 60;
        long daysTotal = hoursTotal / 24;
        long weeksTotal = daysTotal / 7;
        long monthsTotal = daysTotal / 30;

        timeValues.put("total_months", monthsTotal);
        timeValues.put("total_weeks", weeksTotal);
        timeValues.put("total_days", daysTotal);
        timeValues.put("total_hours", hoursTotal);
        timeValues.put("total_minutes", minutesTotal);
        timeValues.put("total_seconds", secondsTotal);

        timeValues.put("months", daysTotal / 30);
        timeValues.put("weeks", daysTotal % 30 / 7);
        timeValues.put("days", daysTotal % 7);
        timeValues.put("hours", hoursTotal % 24);
        timeValues.put("minutes", minutesTotal % 60);
        timeValues.put("seconds", secondsTotal % 60);

        Map<String, String> varMap = new HashMap<>();
        varMap.put("mm", "months");
        varMap.put("ww", "weeks");
        varMap.put("dd", "days");
        varMap.put("hh", "hours");
        varMap.put("MM", "minutes");
        varMap.put("ss", "seconds");

        List<String> formatKeys = events.getContent("formats." + formatName, false);
        String result = "";

        for (String key : formatKeys) {
            String conditionPath = "formats." + formatName + "." + key + ".condition";
            String resultPath = "formats." + formatName + "." + key + ".result";

            String condition = events.getString(conditionPath);
            String resultTemplate = events.getString(resultPath);

            if ("DEFAULT".equalsIgnoreCase(condition) || evaluateCondition(condition, timeValues)) {
                result = renderTemplate(resultTemplate, timeValues, varMap);
                break;
            }
        }

        long expirationTime = System.currentTimeMillis() + events.getLong("cache-expire-ms", 1000);
        formatCache.put(cacheKey, new CachedValue(result, expirationTime));

        return result;
    }

    private String renderTemplate(String template, Map<String, Long> values, Map<String, String> varMap) {
        Pattern pattern = Pattern.compile("\\{([a-zA-Z]+)(?::(\\d))?(?:\\?(.*?)\\|(.*?))?}");
        Matcher matcher = pattern.matcher(template);
        StringBuilder renderedString = new StringBuilder();

        while (matcher.find()) {
            String placeholderName = matcher.group(1);
            String zeroPadding = matcher.group(2);
            String singular = matcher.group(3);
            String plural = matcher.group(4);

            Long value = values.get(varMap.get(placeholderName));

            if (value == null) {
                plugin.getLogs().warn("Placeholder '" + placeholderName + "' has no mapped value. Check your configuration.");
                continue;
            }

            String replacement;
            if (singular != null && plural != null) {
                replacement = (value == 1) ? singular : plural;
            } else {
                replacement = String.valueOf(value);
            }

            if (zeroPadding != null) {
                try {
                    int padding = Integer.parseInt(zeroPadding);
                    if (padding > 0) {
                        replacement = String.format("%0" + padding + "d", value);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogs().error("Invalid number for zero-padding: " + zeroPadding + ". Check your format pattern.", e);
                }
            }

            matcher.appendReplacement(renderedString, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(renderedString);

        return renderedString.toString();
    }

    private boolean evaluateCondition(String condition, Map<String, Long> timeValues) {
        try {
            return new ExpressionEvaluator(condition, timeValues).evaluate();
        } catch (Exception e) {
            plugin.getLogs().error("Wrong formatted condition: '" + condition + "'. Error evaluating condition: " + condition, e);
            return false;
        }
    }

    private Date getSpecifiedEvent(ConfigurationHandler control, String event) {
        SimpleDateFormat format = new SimpleDateFormat(
                control.getString("pattern", "MM/dd/yy HH:mm:ss")
        );
        format.setTimeZone(
                TimeZone.getTimeZone(
                        control.getString("events." + event + ".time-zone")
                )
        );
        try {
            return format.parse(
                    control.getString("events." + event + ".date")
            );
        } catch (ParseException ignored) {
            return null;
        }
    }

    private String replaceSpecifiedPlayer(String message) {
        Matcher matcher = PLAYER_PATTERN.matcher(message);

        if (plugin.getPlayerHandler().getPlayersSize() >= 1) {
            List<String> players = new ArrayList<>(plugin.getPlayerHandler().getPlayersNames());
            while (matcher.find()) {

                int number = Integer.parseInt(matcher.group(1));

                if (players.size() >= number && number != 0) {
                    message = message.replace("%player_" + number + "%", players.get(number - 1));
                } else {
                    message = message.replace("%player_" + number + "%","%canNotFindX02_" + number + "%");
                }

            }
        } else {
            message = message.replace("%player_", "%canNotFindX02_");
        }
        return message;
    }

    public List<String> replaceHoverLine(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> array = new ArrayList<>();
        int showedPlayers = 0;
        for (String line : lines) {
            if (line.contains("<hasOnline>") || line.contains("<hasMoreOnline>")) {

                int size = plugin.getPlayerHandler().getPlayersSize();

                if (line.contains("<hasOnline>") && size >= 1) {
                    line = line.replace("<hasOnline>", "");

                    String replaceOnlineVariable = replaceSpecifiedPlayer(line);

                    if (!replaceOnlineVariable.contains("%canNotFindX02_")) {
                        array.add(replaceOnlineVariable);
                        showedPlayers++;
                    }

                    continue;
                }
                if (size > showedPlayers && showedPlayers != 0 && size >= 1) {

                    int fixedSize = size - showedPlayers;

                    line = line.replace("<hasMoreOnline>","")
                            .replace("%more_online%","" + fixedSize);

                    array.add(line);
                }
                continue;
            }
            array.add(line);
        }
        return array;
    }

    private String getWhitelistAuthor() {
        ConfigurationHandler whitelist = plugin.getConfiguration(Configuration.WHITELIST);

        return whitelist.getString("author", "Console");
    }


    private int getOnlineByNames(List<Server> serverList, List<String> values) {
        int count = 0;
        for (Server server : serverList) {
            if (values.contains(server.getName())) {
                count = count + server.getOnline();
            }
        }
        return count;
    }

    private int getOnlineByContains(List<Server> serverList, List<String> values) {
        int count = 0;
        for (Server server : serverList) {
            count = count + containServer(server,values);
        }
        return count;
    }

    private int containServer(Server server,List<String> values) {
        for (String value : values) {
            if (server.getName().contains(value)) {
                return server.getOnline();
            }
        }
        return 0;
    }
}
