package me.blueslime.pixelmotd.motd.platforms;

import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedServerPing;
import me.blueslime.pixelmotd.color.renders.SpigotStringRenderer;
import me.blueslime.pixelmotd.motd.setup.MotdSetup;
import me.blueslime.pixelmotd.motd.CachedMoTD;
import me.blueslime.pixelmotd.motd.MoTDProtocol;
import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.motd.builder.PingBuilder;
import me.blueslime.pixelmotd.motd.builder.favicon.FaviconModule;
import me.blueslime.pixelmotd.motd.builder.hover.HoverModule;
import me.blueslime.pixelmotd.utils.PlaceholderParser;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ProtocolPing extends PingBuilder<JavaPlugin, WrappedServerPing.CompressedImage, PacketEvent, WrappedGameProfile> {

    private final boolean hasPAPI;

    private boolean motdFailed = false;

    public ProtocolPing(
            PixelMOTD<JavaPlugin> plugin,
            FaviconModule<JavaPlugin, WrappedServerPing.CompressedImage> builder,
            HoverModule<WrappedGameProfile> hoverModule
    ) {
        super(plugin, builder, hoverModule);
        hasPAPI = plugin.getPlugin().getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    @Override
    public void update() {
        motdFailed = false;
        super.update();
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

        CachedMoTD motd = fetchMotd(setup.getCode(), setup.getDomain(), setup.isUserBlacklisted());

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

        if (motd.hasHover() && !motdFailed) {
            try {
                if (motd.isHoverCached()) {
                    if (motd.getHoverObject() == null) {
                        List<WrappedGameProfile> array = getHoverModule().generate(
                                motd.getHover(),
                                setup.getUser(),
                                online,
                                max
                        );
                        if (array != null && !array.isEmpty()) {
                            ping.setPlayers(
                                    array
                            );
                            motd.setHoverObject(array);
                        }
                    } else {
                        //noinspection unchecked
                        List<WrappedGameProfile> array = (List<WrappedGameProfile>) motd.getHoverObject();
                        if (array != null && !array.isEmpty()) {
                            ping.setPlayers(
                                    array
                            );
                        }
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
            } catch (Exception e) {
                motdFailed = true;
                getLogs().error("Failed to load hover, this is a ProtocolLib Exception, please report it to their developers, if you reload it again the plugin will attempt again to create the hover", e);
            }
        }

        MoTDProtocol protocol = MoTDProtocol.fromOther(
                motd.getModifier()
        ).setCode(setup.getCode());

        if (protocol != MoTDProtocol.DEFAULT) {
            if (protocol != MoTDProtocol.ALWAYS_NEGATIVE) {
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

            completed = SpigotStringRenderer.create(completed);
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
