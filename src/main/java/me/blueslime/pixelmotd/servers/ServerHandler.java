package me.blueslime.pixelmotd.servers;

import me.blueslime.pixelmotd.servers.platform.BukkitServerHandler;
import me.blueslime.pixelmotd.servers.platform.BungeeServerHandler;
import me.blueslime.pixelmotd.servers.platform.VelocityServerHandler;
import me.blueslime.slimelib.SlimePlatform;

import java.util.List;

public interface ServerHandler {

    /**
     * Get servers or worlds in the network
     * @return List (Server) Servers or Worlds Names
     */
    List<Server> getServers();

    /**
     * Get the name of a specified server
     * @return Integer
     */
    default int getServerUsers(String serverName) {
        for (Server server : getServers()) {
            if (server.name().equalsIgnoreCase(serverName)) {
                return server.online();
            }
        }
        return 0;
    }

    /**
     * Get the amount of servers in the network
     * @return int Servers Size
     */
    int getSize();

    static <T> ServerHandler fromPlatform(SlimePlatform platform, T plugin) {
        return switch (platform) {
            case BUKKIT, PAPER, SPIGOT -> new BukkitServerHandler(plugin);
            case VELOCITY -> new VelocityServerHandler(plugin);
            default -> new BungeeServerHandler(plugin);
        };
    }
}
