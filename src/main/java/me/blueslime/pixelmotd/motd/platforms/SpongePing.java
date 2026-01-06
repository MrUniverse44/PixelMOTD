package me.blueslime.pixelmotd.motd.platforms;

import me.blueslime.pixelmotd.color.renders.VelocitySpongeRenderer;
import me.blueslime.pixelmotd.motd.setup.MotdSetup;
import me.blueslime.pixelmotd.motd.CachedMoTD;
import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.motd.builder.PingBuilder;
import me.blueslime.pixelmotd.motd.builder.favicon.FaviconModule;
import me.blueslime.pixelmotd.motd.builder.hover.HoverModule;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.network.status.Favicon;
import org.spongepowered.api.profile.GameProfile;

import java.util.List;

public class SpongePing extends PingBuilder<Server, Favicon, ClientPingServerEvent, GameProfile> {
    public SpongePing(
            PixelMOTD<Server> plugin,
            FaviconModule<Server, Favicon> faviconModule,
            HoverModule<GameProfile> hoverModule
    ) {
        super(plugin, faviconModule, hoverModule);
    }

    @Override
    public void execute(ClientPingServerEvent event, MotdSetup setup) {
        ClientPingServerEvent.Response ping = event.response();

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
                Favicon favicon = getFavicon().getFavicon(
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
            if (ping.players().isPresent()) {
                ping.players().get().profiles().clear();

                if (motd.isHoverCached()) {
                    if (motd.getHoverObject() == null) {
                        List<GameProfile> array = getHoverModule().generate(
                                motd.getHover(),
                                setup.getUser(),
                                online,
                                max
                        );

                        ping.players().get().profiles().addAll(
                                array
                        );
                        motd.setHoverObject(array);
                    } else {
                        //noinspection unchecked
                        ping.players().get().profiles().addAll(
                            (List<GameProfile>) motd.getHoverObject()
                        );
                    }
                } else {
                    List<GameProfile> array = getHoverModule().generate(
                            motd.getHover(),
                            setup.getUser(),
                            online,
                            max
                    );

                    ping.players().get().profiles().addAll(
                            array
                    );
                }
            }
        }

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

            result = Component.text(
                    completed
            );
        }

        ping.setDescription(result);

        if (ping.players().isPresent()) {
            ping.players().get().setOnline(online);
            ping.players().get().setMax(max);
        }
    }
}
