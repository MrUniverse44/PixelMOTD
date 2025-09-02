package me.blueslime.pixelmotd.loader.listener;

import me.blueslime.pixelmotd.PixelMOTD;
import me.blueslime.pixelmotd.loader.listener.type.BukkitListenerLoader;
import me.blueslime.pixelmotd.loader.listener.type.BungeeListenerLoader;
import me.blueslime.pixelmotd.loader.listener.type.SpongeListenerLoader;
import me.blueslime.pixelmotd.loader.listener.type.VelocityListenerLoader;

public abstract class ListenerLoader {
    public static ListenerLoader initialize(PixelMOTD<?> plugin) {
        return switch (plugin.getServerType()) {
            case SPONGE -> new SpongeListenerLoader(plugin);
            case BUKKIT, SPIGOT, PAPER -> new BukkitListenerLoader(plugin);
            case VELOCITY -> new VelocityListenerLoader(plugin);
            default -> new BungeeListenerLoader(plugin);
        };
    }
}
