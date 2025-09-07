package me.blueslime.pixelmotd.listener.bukkit.server;

import me.blueslime.pixelmotd.listener.bukkit.BukkitListener;
import me.blueslime.pixelmotd.motd.setup.MotdSetup;
import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.utils.ping.Ping;
import me.blueslime.pixelmotd.listener.type.BukkitPluginListener;
import me.blueslime.pixelmotd.motd.builder.PingBuilder;
import me.blueslime.pixelmotd.motd.builder.favicon.platforms.BukkitFavicon;
import me.blueslime.pixelmotd.motd.builder.hover.platforms.BukkitHover;
import me.blueslime.pixelmotd.motd.platforms.BukkitPing;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.net.InetAddress;

public class ServerListPingListener extends BukkitPluginListener implements Listener, Ping {
    private final BukkitPing builder;

    private String unknown;

    public ServerListPingListener(PixelMOTD<?> plugin) {
        super(plugin, BukkitListener.SERVER_LIST_PING);
        register();

        builder = new BukkitPing(
                getBasePlugin(),
                new BukkitFavicon(
                        getBasePlugin()
                ),
                new BukkitHover(
                        getBasePlugin()
                )
        );

        reload();
    }

    @Override
    public void reload() {
        this.unknown = getSettings().getString("settings.unknown-player", "unknown#1");

        builder.update();
    }

    @EventHandler
    public void on(ServerListPingEvent event) {
        final InetAddress address = event.getAddress();

        final String user = getPlayerDatabase().getPlayer(address.getHostAddress(), unknown);

        MotdSetup setup = new MotdSetup(
            getBlacklist().getStringList("players.by-name").contains(user),
            "",
            user,
            -1
        );

        builder.execute(event, setup);
    }

    @Override
    public PingBuilder<?, ?, ?, ?> getPingBuilder() {
        return builder;
    }
}
