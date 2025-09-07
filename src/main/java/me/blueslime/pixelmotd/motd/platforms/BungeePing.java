package me.blueslime.pixelmotd.motd.platforms;

import me.blueslime.pixelmotd.motd.setup.MotdSetup;
import me.blueslime.slimelib.colors.platforms.bungeecord.BungeeSlimeColor;
import me.blueslime.slimelib.utils.ClassUtils;
import me.blueslime.pixelmotd.motd.CachedMotd;
import me.blueslime.pixelmotd.motd.MotdProtocol;
import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.external.minedown.MineDown;
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
        CachedMotd motd = fetchMotd(setup.getCode(), setup.getDomain(), setup.isUserBlacklisted());

        if (motd == null) {
            getLogs().debug("The plugin don't detect motds to show with this next setup:");
            getLogs().debug("Domain: " + setup.getDomain());
            getLogs().debug("User: " + setup.getUser());
            getLogs().debug("Protocol: " + setup.getCode());
            getLogs().debug("User blacklist status: " + setup.isUserBlacklisted());
            return;
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

        MotdProtocol protocol = MotdProtocol.fromOther(
                motd.getModifier()
        ).setCode(setup.getCode());

        if (protocol != MotdProtocol.DEFAULT) {
            if (protocol != MotdProtocol.ALWAYS_NEGATIVE) {
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

        if (motd.hasHex()) {

            line1 = motd.getLine1();
            line2 = motd.getLine2();

            completed = getExtras().replace(line1, online, max, setup.getUser()) + "\n" + getExtras().replace(line2, online, max, setup.getUser());

            if (line1.contains("%(slimecolor") || line2.contains("%(slimecolor")) {

                if (isDebug()) {
                    getLogs().debug("Using SlimeColorAPI for the motd lines:" + completed);
                }

                result.addExtra(
                        new BungeeSlimeColor(completed, HAS_RGB_SUPPORT)
                                .build()
                );

            }  else {
                if (isDebug()) {
                    getLogs().debug("Using MineDown for the motd lines:" + completed);
                }
                result = new TextComponent(
                        new MineDown(
                                completed.replace('§', '&')).urlDetection(false).toComponent()
                );

            }

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