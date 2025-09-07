package me.blueslime.pixelmotd.listener.bungeecord.proxy;

import me.blueslime.pixelmotd.listener.bungeecord.BungeeListener;
import me.blueslime.pixelmotd.motd.setup.MotdSetup;
import me.blueslime.slimelib.file.configuration.ConfigurationHandler;
import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.utils.ping.Ping;
import me.blueslime.pixelmotd.listener.type.BungeePluginListener;
import me.blueslime.pixelmotd.motd.builder.PingBuilder;
import me.blueslime.pixelmotd.motd.builder.favicon.platforms.BungeeFavicon;
import me.blueslime.pixelmotd.motd.builder.hover.platforms.BungeeHover;
import me.blueslime.pixelmotd.motd.platforms.BungeePing;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.event.EventHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class ProxyPingListener extends BungeePluginListener implements Ping {
    private final BungeePing builder;

    private String unknown;

    public ProxyPingListener(PixelMOTD<?> plugin) {
        super(plugin, BungeeListener.PROXY_PING);
        register();

        builder = new BungeePing(
                getBasePlugin(),
                new BungeeFavicon(
                        getBasePlugin()
                ),
                new BungeeHover(
                        getBasePlugin()
                )
        );

        reload();
    }

    @Override
    public void reload() {
        final ConfigurationHandler control = getSettings();

        if (control != null) {
            this.unknown = control.getString("settings.unknown-player", "unknown#1");
        } else {
            this.unknown = "unknown#1";
        }

        builder.update();
    }

    @EventHandler
    public void on(ProxyPingEvent event) {
        final ServerPing ping = event.getResponse();

        if (ping == null || event instanceof Cancellable && ((Cancellable) event).isCancelled()) {
            return;
        }

        final PendingConnection connection = event.getConnection();

        final SocketAddress address = connection.getSocketAddress();

        final int protocol = connection.getVersion();

        final String userName = getPlayerDatabase().getPlayer(
                address.toString(), unknown
        );

        InetSocketAddress virtualHost = event.getConnection().getVirtualHost();
        String playerDomain = virtualHost.getHostString();

        MotdSetup setup =  new MotdSetup(
            getBlacklist().getStringList("players.by-name").contains(userName),
            playerDomain,
            userName,
            protocol
        );

        builder.execute(ping, setup);
    }


    @Override
    public PingBuilder<?, ?, ?, ?> getPingBuilder() {
        return builder;
    }
}
