package me.blueslime.pixelmotd.motd.platforms;

import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import me.blueslime.pixelmotd.color.renders.VelocitySpongeRenderer;
import me.blueslime.pixelmotd.motd.setup.MotdSetup;
import me.blueslime.pixelmotd.motd.CachedMoTD;
import me.blueslime.pixelmotd.motd.MoTDProtocol;
import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.motd.builder.PingBuilder;
import me.blueslime.pixelmotd.motd.builder.favicon.FaviconModule;
import me.blueslime.pixelmotd.motd.builder.hover.HoverModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VelocityPing extends PingBuilder<ProxyServer, Favicon, ProxyPingEvent, ServerPing.SamplePlayer> {

    public VelocityPing(
            PixelMOTD<ProxyServer> plugin,
            FaviconModule<ProxyServer, Favicon> builder,
            HoverModule<ServerPing.SamplePlayer> hoverModule
    ) {
        super(plugin, builder, hoverModule);
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public void execute(ProxyPingEvent event, MotdSetup setup) {
        ServerPing.Builder ping = event.getPing().asBuilder();

        CachedMoTD motd = fetchMotd(setup.getCode(), setup.getDomain(), setup.isUserBlacklisted());

        if (motd == null) {
            getLogs().debug("The plugin don't detect motds to show with this next setup:");
            getLogs().debug("Domain: " + setup.getDomain());
            getLogs().debug("User: " + setup.getUser());
            getLogs().debug("Protocol: " + setup.getCode());
            getLogs().debug("User blacklist status: " + setup.isUserBlacklisted());
            return;
        }

        if (isDebug()) {
            getLogs().debug("Domain: " + setup.getDomain());
            getLogs().debug("User: " + setup.getUser());
            getLogs().debug("Protocol: " + setup.getCode());
            getLogs().debug("User blacklist status: " + setup.isUserBlacklisted());
        }

        String line1, line2, completed;

        int online, max;

        if (isIconSystem()) {
            if (motd.hasServerIcon()) {
                Favicon favicon = getFavicon().getFavicon(
                        motd.getServerIcon()
                );
                if (favicon != null) {
                    ping.favicon(favicon);
                }
            }
        }

        online = motd.getOnlineAmount(getPlugin());
        max    = motd.getMaxAmount(getPlugin(), online);

        if (motd.hasHover()) {
            if (motd.isHoverCached()) {
                if (motd.getHoverObject() == null) {
                    List<String> lines = motd.getConfiguration().getStringList("server-hover.value");

                    lines.replaceAll(
                            line -> line = legacy(line)
                    );

                    ServerPing.SamplePlayer[] array = getHoverModule().convert(
                            getHoverModule().generate(
                                    lines,
                                    setup.getUser(),
                                    online,
                                    max
                            )
                    );

                    ping.samplePlayers(
                        array
                    );
                    motd.setHoverObject(array);
                } else {
                    ping.samplePlayers(
                        (ServerPing.SamplePlayer[]) motd.getHoverObject()
                    );
                }
            } else {
                List<String> lines = motd.getConfiguration().getStringList("server-hover.value");

                lines.replaceAll(
                        line -> line = legacy(line)
                );

                ServerPing.SamplePlayer[] array = getHoverModule().convert(
                        getHoverModule().generate(
                                lines,
                                setup.getUser(),
                                online,
                                max
                        )
                );

                ping.samplePlayers(
                        array
                );
            }
        }

        MoTDProtocol protocol = MoTDProtocol.fromOther(
                motd.getModifier()
        );

        if (protocol != MoTDProtocol.ALWAYS_NEGATIVE) {
            protocol = protocol.setCode(setup.getCode());
        }

        int p1 = ping.getVersion().getProtocol();

        Component n1 = VelocitySpongeRenderer.create(
            getExtras().replace(
                motd.getProtocolText(),
                online,
                max,
                setup.getUser()
            )
        );

        if (protocol != MoTDProtocol.DEFAULT) {
            p1 = protocol.getCode();
        }

        ping.version(
                new ServerPing.Version(
                        p1,
                        legacy(n1)
                )
        );

        Component result;

        if (motd.hasHex()) {
            line1 = motd.getLine1();
            line2 = motd.getLine2();

            completed = getExtras().replace(
                    line1,
                    online,
                    max,
                    setup.getUser()
            ) + "\n" + getExtras().replace(
                    line2,
                    online,
                    max,
                    setup.getUser()
            );

            result = VelocitySpongeRenderer.create(completed);

        } else {
            line1 = legacy(
                    motd.getLine1()
            );
            line2 = legacy(
                    motd.getLine2()
            );

            completed = getExtras().replace(
                    line1,
                    online,
                    max,
                    setup.getUser()
            ) + "\n" + getExtras().replace(
                    line2,
                    online,
                    max,
                    setup.getUser()
            );

            result = color(completed);
        }

        ping.description(result);
        ping.onlinePlayers(online);
        ping.maximumPlayers(max);

        event.setPing(ping.build());
    }

    private TextComponent color(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }

    private @NotNull String legacy(String content) {
        Component color = VelocitySpongeRenderer.create(content);

        return LegacyComponentSerializer.legacySection().serialize(
                color
        );
    }

    private @NotNull String legacy(Component color) {
        return LegacyComponentSerializer.legacySection().serialize(
                color
        );
    }
}