package me.blueslime.pixelmotd.motd.platforms;

import me.blueslime.pixelmotd.color.renders.BungeeRenderer;
import me.blueslime.pixelmotd.motd.setup.MotdSetup;
import me.blueslime.slimelib.utils.ClassUtils;
import me.blueslime.pixelmotd.motd.CachedMoTD;
import me.blueslime.pixelmotd.motd.MoTDProtocol;
import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.motd.builder.PingBuilder;
import me.blueslime.pixelmotd.motd.builder.favicon.FaviconModule;
import me.blueslime.pixelmotd.motd.builder.hover.HoverModule;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeePing extends PingBuilder<Plugin, Favicon, ServerPing, ServerPing.PlayerInfo> {
    private static final boolean HAS_RGB_SUPPORT = ClassUtils.hasMethod(ChatColor.class, "of", String.class);

    public BungeePing(
            PixelMOTD<Plugin> plugin,
            FaviconModule<Plugin, Favicon> builder,
            HoverModule<ServerPing.PlayerInfo> hoverModule
    ) {
        super(plugin, builder, hoverModule);
    }

    @Override
    public void execute(ServerPing ping, MotdSetup setup) {
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
                    ping.setFavicon(
                            favicon
                    );
                }
            }
        }

        online = motd.getOnlineAmount(getPlugin());
        max    = motd.getMaxAmount(getPlugin(), online);

        if (motd.hasHover()) {
            if (motd.isHoverCached()) {
                if (motd.getHoverObject() == null) {
                    ServerPing.PlayerInfo[] array = getHoverModule().convert(
                            getHoverModule().generate(
                                    motd.getHover(),
                                    setup.getUser(),
                                    online,
                                    max
                            )
                    );

                    ping.getPlayers().setSample(
                            array
                    );
                    motd.setHoverObject(array);
                } else {
                    ping.getPlayers().setSample(
                        (ServerPing.PlayerInfo[]) motd.getHoverObject()
                    );
                }
            } else {
                ServerPing.PlayerInfo[] array = getHoverModule().convert(
                        getHoverModule().generate(
                                motd.getHover(),
                                setup.getUser(),
                                online,
                                max
                        )
                );

                ping.getPlayers().setSample(
                        array
                );
            }
        }

        MoTDProtocol protocol = MoTDProtocol.fromOther(
                motd.getModifier()
        ).setCode(setup.getCode());

        if (protocol != MoTDProtocol.DEFAULT) {
            if (protocol != MoTDProtocol.ALWAYS_NEGATIVE) {
                ping.getVersion().setProtocol(setup.getCode());
            } else {
                ping.getVersion().setProtocol(-1);
            }
        }

        ping.getVersion().setName(
                ChatColor.translateAlternateColorCodes(
                        '&',
                        getExtras().replace(
                                motd.getProtocolText(),
                                online,
                                max,
                                setup.getUser()
                        )
                )
        );

        TextComponent result = new TextComponent("");

        if (motd.hasHex() && HAS_RGB_SUPPORT) {

            line1 = motd.getLine1();
            line2 = motd.getLine2();

            completed = getExtras().replace(line1, online, max, setup.getUser()) + "\n" + getExtras().replace(line2, online, max, setup.getUser());

            if (isDebug()) {
                getLogs().debug("Using Universal Color by PixelMOTD for motd hex processor");
            }

            result = new TextComponent(
                BungeeRenderer.create(completed)
            );

        } else {

            line1 = ChatColor.translateAlternateColorCodes('&',motd.getLine1());
            line2 = ChatColor.translateAlternateColorCodes('&',motd.getLine2());

            completed = getExtras().replace(line1, online, max, setup.getUser()) + "\n" + getExtras().replace(line2, online, max, setup.getUser());

            if (completed.contains("<#") && completed.contains(">") && isDebug()) {
                getLogs().info("Are you trying to use gradients in a MotdType without support to gradients? :(, please remove <# or > from your motd lines");
                getLogs().info("to stop this spam, motd type and motd name causing this issue: " + motd);
            }

            result.addExtra(completed);

        }

        ping.setDescriptionComponent(result);
        ping.getPlayers().setOnline(online);
        ping.getPlayers().setMax(max);
    }
}