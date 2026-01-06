package me.blueslime.pixelmotd.listener.bukkit.player;

import me.blueslime.pixelmotd.color.renders.SpigotStringRenderer;
import me.blueslime.pixelmotd.listener.bukkit.BukkitListener;
import me.blueslime.slimelib.file.configuration.ConfigurationHandler;
import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.listener.type.BukkitPluginListener;
import me.blueslime.pixelmotd.utils.ListType;
import me.blueslime.pixelmotd.utils.ListUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;

public class PlayerLoginListener extends BukkitPluginListener implements Listener {
    public PlayerLoginListener(PixelMOTD<?> plugin) {
        super(plugin, BukkitListener.PLAYER_LOGIN);
        register();
    }

    @EventHandler
    public void on(PlayerLoginEvent event) {
        if (event == null) {
            return;
        }

        final Player connection = event.getPlayer();

        final InetSocketAddress socketAddress = connection.getAddress();

        if (socketAddress != null) {
            InetAddress address = socketAddress.getAddress();

            getPlayerDatabase().add(
                    address.getHostAddress(),
                    connection.getName()
            );
        }

        final String username = connection.getName();

        final UUID uuid = connection.getUniqueId();

        ConfigurationHandler whitelist = getWhitelist();
        ConfigurationHandler blacklist = getBlacklist();

        if (getWhitelist().getBoolean("enabled", false)) {
            if (!checkPlayer(ListType.WHITELIST, "global", username) && !checkUUID(ListType.WHITELIST, "global", uuid)) {
                String reason = ListUtil.ListToString(whitelist.getStringList("kick-message.global"));

                event.disallow(
                        PlayerLoginEvent.Result.KICK_WHITELIST,
                        colorize(
                                replace(
                                        reason,
                                        true,
                                        "global",
                                        username,
                                        uuid.toString()
                                )
                        )
                );
                return;
            }
        }

        if (getBlacklist().getBoolean("enabled", false)) {
            if (checkPlayer(ListType.BLACKLIST, "global", username) || checkUUID(ListType.BLACKLIST, "global", uuid)) {
                String reason = ListUtil.ListToString(blacklist.getStringList("kick-message.global"));

                event.disallow(
                        PlayerLoginEvent.Result.KICK_WHITELIST,
                        colorize(
                                replace(
                                        reason,
                                        false,
                                        "global",
                                        username,
                                        uuid.toString()
                                )
                        )
                );
            }
        }
    }

    public String colorize(String message) {
        return SpigotStringRenderer.create(
            message
        );
    }
}
