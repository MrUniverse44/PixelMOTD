package me.blueslime.pixelmotd.utils.internal.players;

import me.blueslime.pixelmotd.players.PlayerHandler;
import me.blueslime.pixelmotd.utils.internal.players.injects.*;

import java.util.Locale;

public class PlayerModules {
    public static PlayerModule MULTIPLIER_MODULE = MultiplierModule.INSTANCE;
    public static PlayerModule DEFAULT_MODULE = DefaultModule.INSTANCE;
    public static PlayerModule REMOVE_MODULE = RemoverModule.INSTANCE;
    public static PlayerModule MIDDLE_MODULE = MiddleModule.INSTANCE;
    public static PlayerModule SPLIT_MODULE = SplitModule.INSTANCE;
    public static PlayerModule ADD_MODULE = AdderModule.INSTANCE;

    public static int execute(boolean isOnline, Object type, PlayerHandler players, int online, Object values) {
        String typeAsString = type.toString();
        if (typeAsString == null) {
            if (isOnline) {
                return online;
            }
            return players.getMaxPlayers();
        }
        switch (typeAsString.toUpperCase(Locale.ENGLISH)) {
            case "FIXED", "1" -> {
                return DEFAULT_MODULE.execute(online, values);
            }
            case "2", "ADD" -> {
                return ADD_MODULE.execute(online, values);
            }
            case "3", "REMOVE" -> {
                return REMOVE_MODULE.execute(online, values);
            }
            case "4", "MULTIPLY" -> {
                return MULTIPLIER_MODULE.execute(online, values);
            }
            case "5", "ONLINE_SPLIT" -> {
                return SPLIT_MODULE.execute(online, values);
            }
            case "6", "MIDDLE" -> {
                return MIDDLE_MODULE.execute(online, values);
            }
            case "7", "MIDDLE_ADD" -> {
                return online + MIDDLE_MODULE.execute(online, values);
            }
            case "8", "MIDDLE_REMOVE" -> {
                int minimum = online - MIDDLE_MODULE.execute(online, values);

                if (minimum < 1) {
                    minimum = 0;
                }

                return minimum;
            }
            case "9", "ONLINE" -> {
                return online;
            }
            default -> {
                if (isOnline) {
                    return online;
                }
                return players.getMaxPlayers();
            }
        }
    }

    public static int execute(Object type, int online, Object values) {
        return executeDirect(
            type,
            online,
            values
        );
    }

    public static int execute(Object type, int online, int motdOnline, Object values) {
        String typeAsString = type.toString();
        if (typeAsString == null) {
            return online;
        }
        switch (typeAsString.toUpperCase(Locale.ENGLISH)) {
            case "FIXED", "1" -> {
                return DEFAULT_MODULE.execute(online, values);
            }
            case "2", "ADD" -> {
                return ADD_MODULE.execute(online, values);
            }
            case "3", "REMOVE" -> {
                return REMOVE_MODULE.execute(online, values);
            }
            case "4", "MULTIPLY" -> {
                return MULTIPLIER_MODULE.execute(online, values);
            }
            case "5", "ONLINE_SPLIT" -> {
                return SPLIT_MODULE.execute(online, values);
            }
            case "6", "MIDDLE" -> {
                return MIDDLE_MODULE.execute(online, values);
            }
            case "7", "MIDDLE_ADD" -> {
                return online + MIDDLE_MODULE.execute(online, values);
            }
            case "8", "MIDDLE_REMOVE" -> {
                int minimum = online - MIDDLE_MODULE.execute(online, values);

                if (minimum < 1) {
                    minimum = 0;
                }

                return minimum;
            }
            case "9", "ONLINE" -> {
                return motdOnline;
            }
            default -> {
                return online;
            }
        }
    }

    public static int executeDirect(Object type, int online, Object values) {
        String typeAsString = type.toString();
        if (typeAsString == null) {
            return online;
        }
        switch (typeAsString.toUpperCase(Locale.ENGLISH)) {
            case "FIXED", "1" -> {
                return DEFAULT_MODULE.execute(online, values);
            }
            case "2", "ADD" -> {
                return ADD_MODULE.execute(online, values);
            }
            case "3", "REMOVE" -> {
                return REMOVE_MODULE.execute(online, values);
            }
            case "4", "MULTIPLY" -> {
                return MULTIPLIER_MODULE.execute(online, values);
            }
            case "5", "ONLINE_SPLIT" -> {
                return SPLIT_MODULE.execute(online, values);
            }
            case "6", "MIDDLE" -> {
                return MIDDLE_MODULE.execute(online, values);
            }
            case "7", "MIDDLE_ADD" -> {
                return online + MIDDLE_MODULE.execute(online, values);
            }
            case "8", "MIDDLE_REMOVE" -> {
                int minimum = online - MIDDLE_MODULE.execute(online, values);

                if (minimum < 1) {
                    minimum = 0;
                }

                return minimum;
            }
            default -> {
                return online;
            }
        }
    }

}
