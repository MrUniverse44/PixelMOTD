package me.blueslime.pixelmotd.motd.platforms;

import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedServerPing;
import me.blueslime.pixelmotd.motd.setup.MotdSetup;
import me.blueslime.pixelmotd.utils.color.BungeeHexUtil;
import me.blueslime.slimelib.colors.platforms.StringSlimeColor;
import me.blueslime.pixelmotd.motd.CachedMotd;
import me.blueslime.pixelmotd.motd.MotdProtocol;
import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.external.iridiumcolorapi.IridiumColorAPI;
import me.blueslime.pixelmotd.motd.builder.PingBuilder;
import me.blueslime.pixelmotd.motd.builder.favicon.FaviconModule;
import me.blueslime.pixelmotd.motd.builder.hover.HoverModule;
import me.blueslime.pixelmotd.utils.PlaceholderParser;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ProtocolPing extends PingBuilder<JavaPlugin, WrappedServerPing.CompressedImage, PacketEvent, WrappedGameProfile> {

    private final boolean hasPAPI;

    public ProtocolPing(
            PixelMOTD<JavaPlugin> plugin,
            FaviconModule<JavaPlugin, WrappedServerPing.CompressedImage> builder,
            HoverModule<WrappedGameProfile> hoverModule
    ) {
        super(plugin, builder, hoverModule);
        hasPAPI = plugin.getPlugin().getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    @Override
    public void execute(PacketEvent event, MotdSetup setup) {
        int index = 0;
        WrappedServerPing ping = event.getPacket().getServerPings().read(index);

        if (ping == null) {
            if (isDebug()) {
                getLogs().debug("The plugin is receiving a null ping from ProtocolLib, please report it to ProtocolLib, this issue is not caused by PixelMOTD");
            }
            return;
        }

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
                if (isDebug()) {
                    getLogs().info("Icon applied");
                }
                WrappedServerPing.CompressedImage favicon = getFavicon().getFavicon(
                        motd.getServerIcon()
                );

                if (favicon != null) {
                    ping.setFavicon(favicon);
                }
            }
        }

        online = motd.getOnlineAmount(getPlugin());
        max    = motd.getMaxAmount(getPlugin(), online);

        if (motd.hasHover()) {
            if (motd.isHoverCached()) {
                if (motd.getHoverObject() == null) {
                    List<WrappedGameProfile> array = getHoverModule().generate(
                            motd.getHover(),
                            setup.getUser(),
                            online,
                            max
                    );
                    ping.setPlayers(
                            array
                    );
                    motd.setHoverObject(array);
                } else {
                    //noinspection unchecked
                    ping.setPlayers(
                        (List<WrappedGameProfile>) motd.getHoverObject()
                    );
                }
            } else {
                List<WrappedGameProfile> array = getHoverModule().generate(
                        motd.getHover(),
                        setup.getUser(),
                        online,
                        max
                );
                ping.setPlayers(
                        array
                );
            }
        }

        MotdProtocol protocol = MotdProtocol.fromOther(
                motd.getModifier()
        ).setCode(setup.getCode());

        if (protocol != MotdProtocol.DEFAULT) {
            if (protocol != MotdProtocol.ALWAYS_NEGATIVE) {
                ping.setVersionProtocol(setup.getCode());
            } else {
                ping.setVersionProtocol(-1);
            }
        }

        ping.setVersionName(
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

        if (!motd.hasHex()) {
            line1 = motd.getLine1();
            line2 = motd.getLine2();

            if (hasPAPI) {
                line1 = PlaceholderParser.parse(getPlugin().getLogs(), setup.getUser(), line1);
                line2 = PlaceholderParser.parse(getPlugin().getLogs(), setup.getUser(), line2);
            }

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

            completed = ChatColor.translateAlternateColorCodes('&', completed);

        } else {
            line1 = motd.getLine1();
            line2 = motd.getLine2();

            if (hasPAPI) {
                line1 = PlaceholderParser.parse(getPlugin().getLogs(), setup.getUser(), line1);
                line2 = PlaceholderParser.parse(getPlugin().getLogs(), setup.getUser(), line2);
            }

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

            if (completed.contains("<GRADIENT") || completed.contains("<RAINBOW") || completed.contains("<SOLID:")) {

                completed = IridiumColorAPI.process(completed);

            } else {
                completed = new StringSlimeColor(
                        completed,
                        true
                ).build();

                completed = BungeeHexUtil.convert(completed);
            }

            completed = ChatColor.translateAlternateColorCodes('&', completed);
        }

        ping.setMotD(
                WrappedChatComponent.fromLegacyText(completed)
        );


        ping.setPlayersOnline(online);
        ping.setPlayersMaximum(max);

        event.getPacket().getServerPings().write(index, ping);
    }
}
