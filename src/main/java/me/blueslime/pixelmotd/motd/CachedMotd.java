package me.blueslime.pixelmotd.motd;

import me.blueslime.slimelib.file.configuration.ConfigurationHandler;
import me.blueslime.slimelib.file.configuration.TextDecoration;
import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.utils.internal.players.PlayerModules;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CachedMotd {
    private final ConfigurationHandler configuration;
    private MotdProtocol specifiedProtocol;
    private String protocol;

    private final HashSet<String> conditionSet = new HashSet<>();
    private final String pathKey;

    private Object hoverObject = null;

    public CachedMotd(ConfigurationHandler configuration, String pathKey) {
        this.configuration = configuration;
        this.pathKey = pathKey;
        init(pathKey);
    }

    private void init(String path) {
        this.specifiedProtocol = MotdProtocol.fromObject(
                configuration.get(path + "server-displayed-protocol.modifier", "1"),
                0
        );
        this.protocol = configuration.getString(path + "server-displayed-protocol.text", "&fPlayers: &a%online%/1000");

        this.conditionSet.addAll(configuration.getStringList(path + "display-conditions"));

        if (protocol != null && protocol.contains("<before-the-icon>")) {
            this.protocol = protocol.replace("<before-the-icon>", "");

            String[] split = protocol.split("<default>");

            String icon;
            String def;

            if (split.length >= 2) {
                icon = split[0];
                def = split[1];
            } else if (split.length == 1) {
                icon = split[0];
                def = "";
            } else {
                icon = "";
                def = "";
            }

            int max = configuration.getInt(path + "server-displayed-protocol.space-length", 30);

            this.protocol = icon + " ".repeat(Math.max(0, max)) + def;
        }
    }

    public Set<String> getConditionSet() {
        return conditionSet;
    }

    public String getLine1() {
        return configuration.getString(pathKey + "lines.line-1", "");
    }

    public String getLine2() {
        return configuration.getString(pathKey + "lines.line-2", "");
    }

    public MotdProtocol getModifier() {
        return specifiedProtocol;
    }

    public String getProtocolText() {
        return protocol;
    }

    public boolean hasHover() {
        Object type = configuration.get(pathKey + "server-hover.type", "0");

        String text = type.toString();

        if (text == null) {
            return false;
        }

        switch (text.toUpperCase(Locale.ENGLISH)) {
            case "1", "CACHED", "PROCESS" -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public boolean isHoverCached() {
        Object type = configuration.get(pathKey + "server-hover.type", "0");

        String text = type.toString();

        if (text == null) {
            return false;
        }

        return text.toUpperCase(Locale.ENGLISH).equals("CACHED");
    }

    public List<String> getHover() {
        return configuration.getStringList(TextDecoration.LEGACY, pathKey + "server-hover.value");
    }

    public int getOnlineAmount(PixelMOTD<?> plugin) {
        return PlayerModules.execute(
            configuration.get(pathKey + "server-players.online.type", 0),
            plugin.getPlayerHandler().getPlayersSize(),
            configuration.get(pathKey + "server-players.online.value", "10")
        );
    }

    public int getMaxAmount(PixelMOTD<?> plugin) {
        return PlayerModules.execute(
            configuration.get(pathKey + "server-players.max.type", 0),
            plugin.getPlayerHandler().getPlayersSize(),
            configuration.get(pathKey + "server-players.max.value", "10")
        );
    }

    public int getMaxAmount(PixelMOTD<?> plugin, int onlineAmount) {
        return PlayerModules.execute(
            false,
            configuration.get(pathKey + "server-players.max.type", 1),
            plugin.getPlayerHandler(),
            onlineAmount,
            configuration.get(pathKey + "server-players.max.value", "1000;1001")
        );
    }

    public int getPriority() {
        return configuration.getInt(pathKey + "priority", -1);
    }

    public boolean hasHex() {
        return configuration.getBoolean(pathKey + "process-modern-colors", false);
    }

    public boolean hasServerIcon() {
        Object value = configuration.get(pathKey + "server-icon.type", "0");
        String text = value.toString();

        if (text == null) {
            return false;
        }

        switch (text.toUpperCase(Locale.ENGLISH)) {
            case "1", "CACHED", "DEFAULT_LOAD" -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public String getServerIcon() {
        return generateRandomParameter(
            configuration.get(
                pathKey + "server-icon.values",
                "default-icon.png"
            )
        );
    }

    public ConfigurationHandler getConfiguration() {
        return configuration;
    }

    private String generateRandomParameter(Object valueMapping) {
        if (valueMapping instanceof Integer) {
            return null;
        }
        List<String> valueList = new ArrayList<>();
        if (valueMapping instanceof String values) {
            values = values.replace("\n", ";");
            if (!values.contains(";")) {
                return values;
            }
            valueList.addAll(Arrays.asList(values.split(";")));
        }
        if (valueMapping instanceof Set<?> stringSet) {
            stringSet.forEach(
                val -> valueList.add(val.toString())
            );
        }
        if (valueMapping instanceof List<?> stringList) {
            stringList.forEach(
                val -> valueList.add(val.toString())
            );
        }

        if (valueList.isEmpty()) {
            return null;
        }

        if (valueList.size() == 1) {
            return valueList.getFirst();
        }

        Random random = ThreadLocalRandom.current();

        String val = valueList.get(
            random.nextInt(
                valueList.size()
            )
        );

        return !val.isEmpty() ? val : null;
    }

    public Object getHoverObject() {
        return hoverObject;
    }

    public void setHoverObject(Object array) {
        this.hoverObject = array;
    }
}
