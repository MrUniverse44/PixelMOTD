package me.blueslime.pixelmotd.listener.velocity.proxy;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.server.ServerPing;
import me.blueslime.pixelmotd.listener.velocity.VelocityListener;
import me.blueslime.pixelmotd.motd.setup.MotdSetup;
import me.blueslime.slimelib.file.configuration.ConfigurationHandler;
import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.utils.ping.Ping;
import me.blueslime.pixelmotd.listener.type.VelocityPluginListener;
import me.blueslime.pixelmotd.motd.builder.PingBuilder;
import me.blueslime.pixelmotd.motd.builder.favicon.platforms.VelocityFavicon;
import me.blueslime.pixelmotd.motd.builder.hover.platforms.VelocityHover;
import me.blueslime.pixelmotd.motd.platforms.VelocityPing;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

public class ProxyPingListener extends VelocityPluginListener implements Ping {

    private final VelocityPing builder;

    private String unknown;

    public ProxyPingListener(PixelMOTD<?> plugin) {
        super(plugin, VelocityListener.PROXY_PING);
        register();

        builder = new VelocityPing(
                getBasePlugin(),
                new VelocityFavicon(
                        getBasePlugin()
                ),
                new VelocityHover(
                        getBasePlugin()
                )
        );

        reload();
    }

    @Override
    public void reload() {
        ConfigurationHandler settings = getSettings();

        this.unknown = settings.getString("settings.unknown-player", "unknown#1");

        ConfigurationHandler whitelist = getWhitelist();
        ConfigurationHandler blacklist = getBlacklist();

        builder.update();
    }

    @Subscribe(order = PostOrder.EARLY)
    public void on(ProxyPingEvent event) {
        final ServerPing ping = event.getPing();

        if (ping == null) {
            return;
        }

        final int protocol = ping.getVersion().getProtocol();

        final String user;

        InboundConnection connection = event.getConnection();

        InetSocketAddress socketAddress = connection.getRemoteAddress();

        String domain = "";

        Optional<InetSocketAddress> virtualOptional = connection.getVirtualHost();

        if (virtualOptional.isPresent()) {
            domain = virtualOptional.get().getHostName();
        }

        if (socketAddress != null) {
            InetAddress address = socketAddress.getAddress();

            user = getPlayerDatabase().getPlayer(address.getHostAddress(), unknown);
        } else {
            user = unknown;
        }

        MotdSetup setup = new MotdSetup(
            getBlacklist().getStringList("players.by-name").contains(user),
            domain,
            user,
            protocol
        );

        builder.execute(event, setup);
    }

    @Override
    public PingBuilder<?, ?, ?, ?> getPingBuilder() {
        return builder;
    }
}
