package me.blueslime.pixelmotd.motd;

import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.utils.Verifier;
import me.blueslime.slimelib.file.configuration.ConfigurationHandler;
import me.blueslime.slimelib.file.configuration.TextDecoration;
import me.blueslime.pixelmotd.utils.internal.players.PlayerModules;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * Cached representation of a MoTD configuration entry.
 * The class reads necessary values from a ConfigurationHandler on construction
 * and exposes safe, convenient accessors.
 */
public class CachedMoTD {

    private final ConfigurationHandler configuration;
    private final MoTDProtocol specifiedProtocol;
    private final String protocol;
    private final Set<String> conditionSet;
    private final String pathKey;

    private Object hoverObject = null;

    /**
     * Construct a CachedMoTD reading values from the provided configuration.
     *
     * @param configuration non-null configuration handler
     * @param pathKey       non-null path prefix (should include trailing path separators if needed)
     */
    public CachedMoTD(ConfigurationHandler configuration, String pathKey) {
        this.configuration = Verifier.requireNonNull(configuration, "configuration");
        this.pathKey = Verifier.requireNonNull(pathKey, "pathKey");

        // Load and sanitize displayed protocol data
        MoTDProtocol protocolModifier = MoTDProtocol.fromObject(
                this.configuration.get(pathKey + "server-displayed-protocol.modifier", "1"),
                0
        );

        String protoText = Objects.toString(
                this.configuration.getString(pathKey + "server-displayed-protocol.text", "&fPlayers: &a%online%/1000"),
                ""
        );

        // Process special placeholder "<before-the-icon>" + "<default>" split logic
        if (protoText.contains("<before-the-icon>")) {
            // remove the marker first
            protoText = protoText.replace("<before-the-icon>", "");
            String[] split = protoText.split("<default>", -1);

            String icon = split.length >= 1 ? split[0] : "";
            String def = split.length >= 2 ? split[1] : "";

            int max = Math.max(0, this.configuration.getInt(pathKey + "server-displayed-protocol.space-length", 30));
            protoText = icon + " ".repeat(max) + def;
        }

        this.specifiedProtocol = protocolModifier;
        this.protocol = protoText;

        // Load conditions; defensive copy and expose as an unmodifiable set
        List<String> rawConditions = this.configuration.getStringList(pathKey + "display-conditions");
        Set<String> cond = rawConditions == null ? new HashSet<>() : new HashSet<>(rawConditions);
        this.conditionSet = Collections.unmodifiableSet(cond);
    }

    /**
     * Returns an unmodifiable view of the display condition keys.
     *
     * @return unmodifiable set with condition strings
     */
    public Set<String> getConditionSet() {
        return conditionSet;
    }

    /**
     * Returns the first MoTD line configured.
     *
     * @return line 1 or empty string if missing
     */
    public String getLine1() {
        return Verifier.toString(configuration.getString(pathKey + "lines.line-1", ""), "");
    }

    /**
     * Returns the second MoTD line configured.
     *
     * @return line 2 or empty string if missing
     */
    public String getLine2() {
        return Verifier.toString(configuration.getString(pathKey + "lines.line-2", ""), "");
    }

    /**
     * Returns the parsed MoTD Protocol modifier for this MoTD.
     *
     * @return MoTD Protocol instance (never null)
     */
    public MoTDProtocol getModifier() {
        return specifiedProtocol;
    }

    /**
     * Returns the processed protocol text for display (may contain placeholders).
     *
     * @return protocol text (maybe empty)
     */
    public String getProtocolText() {
        return protocol;
    }

    /**
     * Checks whether a hover section is enabled for this MoTD.
     *
     * @return true if hover is enabled (types: 1, CACHED, PROCESS)
     */
    public boolean hasHover() {
        Object type = configuration.get(pathKey + "server-hover.type", "0");
        String text = toTypeString(type);

        return switch (text) {
            case "1", "CACHED", "PROCESS" -> true;
            default -> false;
        };
    }

    /**
     * Checks whether a hover type is explicitly set to CACHE.
     *
     * @return true if a hover type equals CACHED
     */
    public boolean isHoverCached() {
        Object type = configuration.get(pathKey + "server-hover.type", "0");
        return "CACHED".equals(toTypeString(type));
    }

    /**
     * Returns the hover lines for this MoTD. Uses legacy decoration mode to fetch strings.
     *
     * @return list of hover strings (maybe empty)
     */
    public List<String> getHover() {
        return configuration.getStringList(TextDecoration.LEGACY, pathKey + "server-hover.value");
    }

    /**
     * Computes the current online amount using PlayerModules logic and the plugin's player handler.
     *
     * @param plugin plugin instance
     * @return online amount computed by PlayerModules
     */
    public int getOnlineAmount(PixelMOTD<?> plugin) {
        return PlayerModules.execute(
            configuration.get(pathKey + "server-players.online.type", 0),
            plugin.getPlayerHandler().getPlayersSize(),
            configuration.get(pathKey + "server-players.online.value", "10")
        );
    }

    /**
     * Computes the maximum player amount using PlayerModules logic.
     *
     * @param plugin plugin instance
     * @return computed max amount
     */
    @SuppressWarnings("unused")
    public int getMaxAmount(PixelMOTD<?> plugin) {
        return PlayerModules.execute(
            configuration.get(pathKey + "server-players.max.type", 0),
            plugin.getPlayerHandler().getPlayersSize(),
            configuration.get(pathKey + "server-players.max.value", "10")
        );
    }

    /**
     * Alternative overload that forwards different parameters to PlayerModules.
     *
     * @param plugin       plugin instance
     * @param onlineAmount online amount already computed
     * @return computed max amount
     */
    public int getMaxAmount(PixelMOTD<?> plugin, int onlineAmount) {
        return PlayerModules.execute(
            false,
            configuration.get(pathKey + "server-players.max.type", 1),
            plugin.getPlayerHandler(),
            onlineAmount,
            configuration.get(pathKey + "server-players.max.value", "1000;1001")
        );
    }

    /**
     * Returns the priority of this MoTD (lower is earlier), -1 indicates no priority.
     *
     * @return priority integer
     */
    public int getPriority() {
        return configuration.getInt(pathKey + "priority", -1);
    }

    /**
     * Whether modern hex color processing is enabled for this MoTD.
     *
     * @return true if hex processing is enabled
     */
    public boolean hasHex() {
        return configuration.getBoolean(pathKey + "process-modern-colors", false);
    }

    /**
     * Checks whether server icon should be loaded for this MoTD.
     *
     * @return true if icon support is enabled (types: 1, CACHED, DEFAULT_LOAD)
     */
    public boolean hasServerIcon() {
        Object value = configuration.get(pathKey + "server-icon.type", "0");
        String text = toTypeString(value);

        return switch (text) {
            case "1", "CACHED", "DEFAULT_LOAD" -> true;
            default -> false;
        };
    }

    /**
     * Returns the server icon filename or null. When the configured value contains multiple options,
     * one is chosen randomly.
     *
     * @return icon string or null
     */
    public String getServerIcon() {
        return generateRandomParameter(
                configuration.get(pathKey + "server-icon.values", "default-icon.png")
        );
    }

    /**
     * Returns underlying ConfigurationHandler (read-only operations performed externally).
     *
     * @return configuration handler
     */
    public ConfigurationHandler getConfiguration() {
        return configuration;
    }

    /**
     * Returns the hover object stored for this MoTD (maybe used by the plugin runtime).
     *
     * @return hover object or null
     */
    public Object getHoverObject() {
        return hoverObject;
    }

    /**
     * Stores an arbitrary hover-related object for runtime use.
     *
     * @param array any object (maybe null)
     */
    public void setHoverObject(Object array) {
        this.hoverObject = array;
    }

    private static String toTypeString(Object value) {
        return value == null ? "0" : value.toString().toUpperCase(Locale.ENGLISH);
    }

    /**
     * Generates a random entry from the provided configuration mapping.
     * Supports:
     * - String (single value or separated by ';' or newline)
     * - Collection (List, Set)
     * - Array (Object[])
     * - Returns null for numeric mappings.
     *
     * @param valueMapping mapping value from configuration
     * @return selected string or null when none available
     */
    private String generateRandomParameter(Object valueMapping) {
        if (valueMapping == null) {
            return null;
        }

        if (valueMapping instanceof Number) {
            return null;
        }

        List<String> choices = new ArrayList<>();

        if (valueMapping instanceof CharSequence seq) {
            String s = seq.toString().replace("\r", "").replace("\n", ";");
            if (!s.contains(";")) {
                String trimmed = s.trim();
                return trimmed.isEmpty() ? null : trimmed;
            }
            choices.addAll(
                Stream.of(s.split(";"))
                    .map(String::trim)
                    .filter(str -> !str.isEmpty())
                    .toList()
            );
        } else if (valueMapping instanceof Collection<?> coll) {
            for (Object o : coll) {
                if (o != null) {
                    String t = o.toString().trim();
                    if (!t.isEmpty()) choices.add(t);
                }
            }
        } else if (valueMapping.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(valueMapping);
            for (int i = 0; i < len; i++) {
                Object o = java.lang.reflect.Array.get(valueMapping, i);
                if (o != null) {
                    String t = o.toString().trim();
                    if (!t.isEmpty()) choices.add(t);
                }
            }
        } else {
            String single = valueMapping.toString().trim();
            return single.isEmpty() ? null : single;
        }

        if (choices.isEmpty()) {
            return null;
        }

        if (choices.size() == 1) {
            return choices.getFirst();
        }

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        String val = choices.get(rnd.nextInt(choices.size()));
        return val.isEmpty() ? null : val;
    }
}
