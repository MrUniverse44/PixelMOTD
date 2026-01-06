package me.blueslime.pixelmotd.motd.platforms;

import me.blueslime.pixelmotd.color.renders.SpigotStringRenderer;
import me.blueslime.pixelmotd.motd.CachedMoTD;
import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.motd.builder.hover.EmptyPlayerInfo;
import me.blueslime.pixelmotd.motd.builder.favicon.FaviconModule;
import me.blueslime.pixelmotd.motd.builder.PingBuilder;
import me.blueslime.pixelmotd.motd.builder.hover.HoverModule;
import me.blueslime.pixelmotd.motd.setup.MotdSetup;
import me.blueslime.pixelmotd.utils.PlaceholderParser;
import org.bukkit.ChatColor;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.CachedServerIcon;

public class BukkitPing extends PingBuilder<JavaPlugin, CachedServerIcon, ServerListPingEvent, EmptyPlayerInfo> {

    private final boolean hasPAPI;

    public BukkitPing(
            PixelMOTD<JavaPlugin> plugin,
            FaviconModule<JavaPlugin, CachedServerIcon> builder,
            HoverModule<EmptyPlayerInfo> hoverModule
    ) {
        super(plugin, builder, hoverModule);

        hasPAPI = plugin.getPlugin().getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    @Override
    public void execute(ServerListPingEvent ping, MotdSetup setup) {
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

        int max;

        if (isIconSystem()) {
            if (motd.hasServerIcon()) {
                CachedServerIcon favicon = getFavicon().getFavicon(
                        motd.getServerIcon()
                );

                if (favicon != null) {
                    ping.setServerIcon(
                            favicon
                    );
                }
            }
        }

        max = motd.getMaxAmount(getPlugin(), ping.getNumPlayers());

        if (!motd.hasHex()) {
            line1 = ChatColor.translateAlternateColorCodes('&', motd.getLine1());
            line2 = ChatColor.translateAlternateColorCodes('&', motd.getLine2());

            if (hasPAPI) {
                line1 = PlaceholderParser.parse(getPlugin().getLogs(), setup.getUser(), line1);
                line2 = PlaceholderParser.parse(getPlugin().getLogs(), setup.getUser(), line2);
            }

            completed = getExtras().replace(
                    line1,
                    ping.getNumPlayers(),
                    ping.getMaxPlayers(),
                    setup.getUser()
            ) + "\n" + getExtras().replace(
                    line2,
                    ping.getNumPlayers(),
                    ping.getMaxPlayers(),
                    setup.getUser()
            );

        } else {
            line1 = motd.getLine1();
            line2 = motd.getLine2();

            if (hasPAPI) {
                line1 = PlaceholderParser.parse(getPlugin().getLogs(), setup.getUser(), line1);
                line2 = PlaceholderParser.parse(getPlugin().getLogs(), setup.getUser(), line2);
            }

            completed = getExtras().replace(
                    line1,
                    ping.getNumPlayers(),
                    ping.getMaxPlayers(),
                    setup.getUser()
            ) + "\n" + getExtras().replace(
                    line2,
                    ping.getNumPlayers(),
                    ping.getMaxPlayers(),
                    setup.getUser()
            );

            if (isDebug()) {
                getLogs().debug("Using Universal Color by PixelMOTD for motd hex processor");
            }

            completed = SpigotStringRenderer.create(completed);
            completed = ChatColor.translateAlternateColorCodes('&', completed);
        }

        ping.setMotd(
                ChatColor.translateAlternateColorCodes('&', completed)
        );
        ping.setMaxPlayers(max);

    }
}

