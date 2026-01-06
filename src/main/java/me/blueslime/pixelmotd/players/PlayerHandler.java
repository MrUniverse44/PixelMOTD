package me.blueslime.pixelmotd.players;

import me.blueslime.pixelmotd.players.platform.BungeePlayerHandler;
import me.blueslime.pixelmotd.players.platform.BukkitPlayerHandler;
import me.blueslime.pixelmotd.players.platform.VelocityPlayerHandler;
import me.blueslime.slimelib.SlimePlatform;

import java.util.List;

public interface PlayerHandler {

    /**
     * Get the names of all players
     * @return List (String) Players Names
     */
    List<String> getPlayersNames();

    /**
     * Get the name of a specified player
     * @return String Player Name
     */
    default String getPlayerName(int id) {
        List<String> names = getPlayersNames();

        if (names.size() >= id) {
            return names.get(id - 1);
        }
        return null;
    }

    /**
     * Get the number of players online in the server
     * @return int Players Size
     */
    int getPlayersSize();

    /**
     * Get the number of max players in the server
     * @return int Max players size
     */
    int getMaxPlayers();

    static <T> PlayerHandler fromPlatform(SlimePlatform platform, T plugin) {
        return switch (platform) {
            case BUKKIT, PAPER, SPIGOT -> new BukkitPlayerHandler(plugin);
            case VELOCITY -> new VelocityPlayerHandler(plugin);
            default -> new BungeePlayerHandler(plugin);
        };
    }

}
