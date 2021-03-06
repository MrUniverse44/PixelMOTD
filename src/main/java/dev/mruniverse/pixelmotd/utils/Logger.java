package dev.mruniverse.pixelmotd.utils;

import dev.mruniverse.pixelmotd.PixelBungee;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Logger {
    /**
     * Colorize a string provided to method
     * for spigot
     *
     * @param message Message to transform.
     * @return transformed message with colors.
     */
    public static String spigotColor(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Colorize a string provided to method
     * for proxies.
     *
     * @param message Message to transform.
     * @return transformed message with colors.
     */
    public static String bungeeColor(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Send a error message to console.
     * @param message message to send.
     */
    public static void error(String message) {
        sendMessage("&f[&cERROR &7| &fPixel MOTD] " + message);
    }

    /**
     * Send a warn message to console.
     * @param message message to send.
     */
    public static void warn(String message) {
        sendMessage("&f[&eWARN &7| &fPixel MOTD] " + message);
    }

    /**
     * Send a debug message to console.
     * @param message message to send.
     */
    public static void debug(String message) {
        sendMessage("&f[&9DEBUG &7| &fPixel MOTD] " + message);
    }

    /**
     * Send a info message to console.
     * @param message message to send.
     */
    public static void info(String message) {
        sendMessage("&f[&bINFO &7| &fPixel MOTD] " + message);
    }

    /**
     * Sends a message to a Bukkit command sender.
     *
     * @param sender Bukkit CommandSender
     * @param message Message to send.
     */
    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(spigotColor(message));
    }

    /**
     * Sends a message to a Proxied Player.
     *
     * @param player Proxied Player
     * @param message Message to send.
     */
    public static void sendMessage(net.md_5.bungee.api.CommandSender player, String message) {
        player.sendMessage(new ComponentBuilder(bungeeColor(message)).create());
    }

    /**
     * Used to other methods and prevent this copy pasta
     * to those methods.
     *
     * @param message Provided message
     */
    private static void sendMessage(String message) {
        if (!LoaderUtils.isBungee) {
            Bukkit.getConsoleSender().sendMessage(spigotColor(message));
        }

        PixelBungee plugin = PixelBungee.getInstance();
        plugin.getProxy().getConsole().sendMessage(new ComponentBuilder(bungeeColor(message)).create());
    }
}