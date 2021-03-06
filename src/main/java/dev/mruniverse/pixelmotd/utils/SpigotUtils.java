package dev.mruniverse.pixelmotd.utils;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import dev.mruniverse.pixelmotd.enums.*;
import dev.mruniverse.pixelmotd.files.SpigotControl;
import dev.mruniverse.pixelmotd.PixelSpigot;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.text.ParseException;
import java.util.*;

import static dev.mruniverse.pixelmotd.utils.Logger.*;

public class SpigotUtils {
    public static List<String> getPlayers(WhitelistMembers mode, String worldName) {
        if(mode.equals(WhitelistMembers.NAMEs)) {
            if(SpigotControl.getControl(Files.MODULES).get(Extras.getWorldPath(Whitelist.PLAYERS_NAME,worldName)) != null)
                if(SpigotControl.getControl(Files.MODULES).get("modules.world-whitelist.worlds." + worldName + " .players-name") != null) {
                    return SpigotControl.getControl(Files.MODULES).getStringList("modules.world-whitelist.worlds." + worldName + " .players-name");
                }
            return new ArrayList<>();
        }
        if(SpigotControl.getControl(Files.MODULES).get("modules.world-whitelist.worlds." + worldName + " .players-uuid") != null) {
            return SpigotControl.getControl(Files.MODULES).getStringList("modules.world-whitelist.worlds." + worldName + " .players-uuid");
        }
        return new ArrayList<>();
    }
    public static List<String> getPlayers(BlacklistMembers mode, String worldName) {
        if(mode.equals(BlacklistMembers.NAMEs)) {
            if(SpigotControl.getControl(Files.MODULES).get(Extras.getWorldPath(Blacklist.PLAYERS_NAME,worldName)) != null)
                if(SpigotControl.getControl(Files.MODULES).get("modules.world-blacklist.worlds." + worldName + " .players-name") != null) {
                    return SpigotControl.getControl(Files.MODULES).getStringList("modules.world-blacklist.worlds." + worldName + " .players-name");
                }
            return new ArrayList<>();
        }
        if(SpigotControl.getControl(Files.MODULES).get("modules.world-blacklist.worlds." + worldName + " .players-uuid") != null) {
            return SpigotControl.getControl(Files.MODULES).getStringList("modules.world-blacklist.worlds." + worldName + " .players-uuid");
        }
        return new ArrayList<>();
    }
    public static List<WrappedGameProfile> getHover(MotdType motdType, String motdName, int online, int max) {
        List<WrappedGameProfile> result = new ArrayList<>();

        if(motdType.equals(MotdType.NORMAL_MOTD)) {
            for(String line : SpigotControl.getControl(Files.NORMAL_MOTD).getStringList("normal." + motdName + ".otherSettings.customHover.hover")) {
                result.add(new WrappedGameProfile(UUID.fromString("0-0-0-0-0"), replaceVariables(line,online,max)));
            }
            return result;
        }
        if(motdType.equals(MotdType.WHITELIST_MOTD)) {
            for(String line : SpigotControl.getControl(Files.WHITELIST_MOTD).getStringList("whitelist." + motdName + ".otherSettings.customHover.hover")) {
                result.add(new WrappedGameProfile(UUID.fromString("0-0-0-0-0"), replaceVariables(line,online,max)));
            }
            return result;
        }
        for(String line : SpigotControl.getControl(Files.TIMER_MOTD).getStringList("timers." + motdName + ".otherSettings.customHover.hover")) {
            result.add(new WrappedGameProfile(UUID.fromString("0-0-0-0-0"), replaceVariables(line,online,max)));
        }
        return result;
    }
    private static void reportProtocolError() {
        warn("Can't generate motd Protocol, please verify if your protocol is correctly created!");
    }
    public static File getIcons(MotdType motdType,String motdName) {
        File iconFolder = PixelSpigot.getFiles().getFile(Icons.FOLDER);
        if(motdType.equals(MotdType.NORMAL_MOTD)) {
            iconFolder = new File(PixelSpigot.getFiles().getFile(Icons.FOLDER), "Normal-" + motdName);
        }
        if(motdType.equals(MotdType.WHITELIST_MOTD)) {
            iconFolder = new File(PixelSpigot.getFiles().getFile(Icons.FOLDER), "Whitelist-" + motdName);
        }
        if(!iconFolder.exists()) PixelSpigot.getFiles().loadFolder(iconFolder,"&fIcon Folder: &b" + motdName);
        return iconFolder;
    }
    public static boolean getPlayersStatus(MotdType motdType,String motdName) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            return SpigotControl.getControl(Files.NORMAL_MOTD).getBoolean("normal." + motdName + ".otherSettings.customMaxPlayers.toggle");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            return SpigotControl.getControl(Files.WHITELIST_MOTD).getBoolean("whitelist." + motdName + ".otherSettings.customMaxPlayers.toggle");
        }
        return SpigotControl.getControl(Files.TIMER_MOTD).getBoolean("timers." + motdName + ".otherSettings.customMaxPlayers.toggle");
    }
    public static boolean getOnlineStatus(MotdType motdType,String motdName) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            return SpigotControl.getControl(Files.NORMAL_MOTD).getBoolean("normal." + motdName + ".otherSettings.customOnlinePlayers.toggle");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            return SpigotControl.getControl(Files.WHITELIST_MOTD).getBoolean("whitelist." + motdName + ".otherSettings.customOnlinePlayers.toggle");
        }
        return SpigotControl.getControl(Files.TIMER_MOTD).getBoolean("timers." + motdName + ".otherSettings.customOnlinePlayers.toggle");
    }
    public static boolean getProtocolStatus(MotdType motdType,String motdName) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            return SpigotControl.getControl(Files.NORMAL_MOTD).getBoolean("normal." + motdName + ".otherSettings.customProtocol.toggle");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            return SpigotControl.getControl(Files.WHITELIST_MOTD).getBoolean("whitelist." + motdName + ".otherSettings.customProtocol.toggle");
        }
        return SpigotControl.getControl(Files.TIMER_MOTD).getBoolean("timers." + motdName + ".otherSettings.customProtocol.toggle");
    }
    public static ValueMode getPlayersMode(MotdType motdType, String motdName) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            if(Objects.requireNonNull(SpigotControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customMaxPlayers.mode")).equalsIgnoreCase("CUSTOM-VALUES")) {
                return ValueMode.CUSTOM;
            }
            if(Objects.requireNonNull(SpigotControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customMaxPlayers.mode")).equalsIgnoreCase("ADD")) {
                return ValueMode.ADD;
            }
            if(Objects.requireNonNull(SpigotControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customMaxPlayers.mode")).equalsIgnoreCase("HALF-ADD")) {
                return ValueMode.HALF_ADD;
            }
            if(Objects.requireNonNull(SpigotControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customMaxPlayers.mode")).equalsIgnoreCase("HALF")) {
                return ValueMode.HALF;
            }
            return ValueMode.EQUAL;
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            if(Objects.requireNonNull(SpigotControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customMaxPlayers.mode")).equalsIgnoreCase("CUSTOM-VALUES")) {
                return ValueMode.CUSTOM;
            }
            if(Objects.requireNonNull(SpigotControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customMaxPlayers.mode")).equalsIgnoreCase("ADD")) {
                return ValueMode.ADD;
            }
            if(Objects.requireNonNull(SpigotControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customMaxPlayers.mode")).equalsIgnoreCase("HALF-ADD")) {
                return ValueMode.HALF_ADD;
            }
            if(Objects.requireNonNull(SpigotControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customMaxPlayers.mode")).equalsIgnoreCase("HALF")) {
                return ValueMode.HALF;
            }
            return ValueMode.EQUAL;
        }
        if(Objects.requireNonNull(SpigotControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customMaxPlayers.mode")).equalsIgnoreCase("CUSTOM-VALUES")) {
            return ValueMode.CUSTOM;
        }
        if(Objects.requireNonNull(SpigotControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customMaxPlayers.mode")).equalsIgnoreCase("ADD")) {
            return ValueMode.ADD;
        }
        if(Objects.requireNonNull(SpigotControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customMaxPlayers.mode")).equalsIgnoreCase("HALF-ADD")) {
            return ValueMode.HALF_ADD;
        }
        if(Objects.requireNonNull(SpigotControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customMaxPlayers.mode")).equalsIgnoreCase("HALF")) {
            return ValueMode.HALF;
        }
        return ValueMode.EQUAL;
    }
    public static ValueMode getOnlineMode(MotdType motdType, String motdName) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            if(Objects.requireNonNull(SpigotControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customOnlinePlayers.mode")).equalsIgnoreCase("CUSTOM-VALUES")) {
                return ValueMode.CUSTOM;
            }
            if(Objects.requireNonNull(SpigotControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customOnlinePlayers.mode")).equalsIgnoreCase("ADD")) {
                return ValueMode.ADD;
            }
            if(Objects.requireNonNull(SpigotControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customOnlinePlayers.mode")).equalsIgnoreCase("HALF-ADD")) {
                return ValueMode.HALF_ADD;
            }
            if(Objects.requireNonNull(SpigotControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customOnlinePlayers.mode")).equalsIgnoreCase("HALF")) {
                return ValueMode.HALF;
            }
            return ValueMode.EQUAL;
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            if(Objects.requireNonNull(SpigotControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customOnlinePlayers.mode")).equalsIgnoreCase("CUSTOM-VALUES")) {
                return ValueMode.CUSTOM;
            }
            if(Objects.requireNonNull(SpigotControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customOnlinePlayers.mode")).equalsIgnoreCase("ADD")) {
                return ValueMode.ADD;
            }
            if(Objects.requireNonNull(SpigotControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customOnlinePlayers.mode")).equalsIgnoreCase("HALF-ADD")) {
                return ValueMode.HALF_ADD;
            }
            if(Objects.requireNonNull(SpigotControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customOnlinePlayers.mode")).equalsIgnoreCase("HALF")) {
                return ValueMode.HALF;
            }
            return ValueMode.EQUAL;
        }
        if(Objects.requireNonNull(SpigotControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customOnlinePlayers.mode")).equalsIgnoreCase("CUSTOM-VALUES")) {
            return ValueMode.CUSTOM;
        }
        if(Objects.requireNonNull(SpigotControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customOnlinePlayers.mode")).equalsIgnoreCase("ADD")) {
            return ValueMode.ADD;
        }
        if(Objects.requireNonNull(SpigotControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customOnlinePlayers.mode")).equalsIgnoreCase("HALF-ADD")) {
            return ValueMode.HALF_ADD;
        }
        if(Objects.requireNonNull(SpigotControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customOnlinePlayers.mode")).equalsIgnoreCase("HALF")) {
            return ValueMode.HALF;
        }
        return ValueMode.EQUAL;
    }
    public static String replaceProtocolVariables(String protocolMessage,int online,int max,String playerName) {
        if(protocolMessage.contains("%server_icon%")) protocolMessage = protocolMessage.replace("%server_icon%",getServerIcon());
        if(protocolMessage.contains("%max%")) protocolMessage = protocolMessage.replace("%max%","" + max);
        if(protocolMessage.contains("%online%")) protocolMessage = protocolMessage.replace("%online%","" + online);
        if(protocolMessage.contains("%player%")) protocolMessage = protocolMessage.replace("%player%",playerName);
        return protocolMessage;
    }
    public static String getServerIcon() { return "                                                                  "; }
    public static String getLine1(MotdType motdType,String motdName, ShowType showType) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            if(showType.equals(ShowType.FIRST)) {
                return SpigotControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".line1");
            }
            return SpigotControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customHexMotd.line1");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            if(showType.equals(ShowType.FIRST)) {
                return SpigotControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".line1");
            }
            return SpigotControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customHexMotd.line1");
        }
        if(showType.equals(ShowType.FIRST)) {
            return SpigotControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".line1");
        }
        return SpigotControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customHexMotd.line1");
    }
    public static String getLine2(MotdType motdType, String motdName, ShowType showType) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            if(showType.equals(ShowType.FIRST)) {
                return SpigotControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".line2");
            }
            return SpigotControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customHexMotd.line2");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            if(showType.equals(ShowType.FIRST)) {
                return SpigotControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".line2");
            }
            return SpigotControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customHexMotd.line2");
        }
        if(showType.equals(ShowType.FIRST)) {
            return SpigotControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".line2");
        }
        return SpigotControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customHexMotd.line2");
    }
    //getHoverStatus
    public static boolean getHoverStatus(MotdType motdType,String motdName) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            return SpigotControl.getControl(Files.NORMAL_MOTD).getBoolean("normal." + motdName + ".otherSettings.customHover.toggle");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            return SpigotControl.getControl(Files.WHITELIST_MOTD).getBoolean("whitelist." + motdName + ".otherSettings.customHover.toggle");
        }
        return SpigotControl.getControl(Files.TIMER_MOTD).getBoolean("timers." + motdName + ".otherSettings.customHover.toggle");
    }
    public static void sendColored(Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    public static String getPermissionMessage(String permission) {
        try {
            if (SpigotControl.getControl(Files.EDITABLE).getString("messages.no-perms").contains("<permission>")) {
                return Objects.requireNonNull(SpigotControl.getControl(Files.EDITABLE).getString("messages.no-perms")).replace("<permission>", permission);
            }
        } catch (Throwable throwable) {
            reportMistake();
        }
        return SpigotControl.getControl(Files.EDITABLE).getString("messages.no-perms");
    }
    private static void reportMistake() {
        info("&e[Pixel MOTD] &fThe plugin found an issue, fixing internal issue.");
    }
    public static void sendColored(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    public static boolean getProtocolVersion(MotdType motdType,String motdName) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            return SpigotControl.getControl(Files.NORMAL_MOTD).getBoolean("normal." + motdName + ".otherSettings.customProtocol.changeProtocolVersion");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            return SpigotControl.getControl(Files.WHITELIST_MOTD).getBoolean("whitelist." + motdName + ".otherSettings.customProtocol.changeProtocolVersion");
        }
        return SpigotControl.getControl(Files.TIMER_MOTD).getBoolean("timers." + motdName + ".otherSettings.customProtocol.changeProtocolVersion");
    }
    public static String getProtocolMessage(MotdType motdType,String motdName) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            return SpigotControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customProtocol.protocol");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            return SpigotControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customProtocol.protocol");
        }
        return SpigotControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customProtocol.protocol");
    }
    public static String replaceVariables(String msg,int online,int max) {
        try {
            msg = PixelSpigot.getHex().applyColor(SpigotControl.getWorlds(msg).replace("%online%",online + "")
                    .replace("%max%",max + "")
                    .replace("%plugin_author%","MrUniverse44")
                    .replace("%whitelist_author%", SpigotControl.getWhitelistAuthor())
                    .replace("%plugin_version%", PixelSpigot.getInstance().getDescription().getVersion()));
        } catch (ParseException e) {
            reportProtocolError();
            if(SpigotControl.isDetailed()) {
                error("Information:");
                if(e.getMessage() != null) {
                    error("Message: " + e.getMessage());
                }
                if(e.getLocalizedMessage() != null) {
                    error("&a[Pixel MOTD] LocalizedMessage: " + e.getLocalizedMessage());
                }
                if(e.getStackTrace() != null) {
                    error("&a[Pixel MOTD] StackTrace: ");
                    for(StackTraceElement line : e.getStackTrace()) {
                        error("&a[Pixel MOTD] (" + line.getLineNumber() + ") " + line.toString());
                    }
                }
                if(e.getSuppressed() != null) {
                    error("&a[Pixel MOTD] Suppressed: " + Arrays.toString(e.getSuppressed()));
                }
                error("&a[Pixel MOTD] ErrorOffset: " + e.getErrorOffset());
                if(e.getClass().getName() != null) {
                    error("&a[Pixel MOTD] Class: " + e.getClass().getName() + ".class");
                }
                error("&a[Pixel MOTD] Plugin version:" + PixelSpigot.getInstance().getDescription().getVersion());
                error("&a[Pixel MOTD] --------------- [Detailed Error]");
            }
        }
        if(msg.contains("<centerText>")) {
            msg = msg.replace("<centerText>","");
            msg = CenterMotd.centerMotd(msg);
        }
        return msg;
    }
    //customOnlinePlayers
    public static int getOnlineValue(MotdType motdType,String motdName) {
        List<Integer> values = new ArrayList<>();
        if(motdType.equals(MotdType.NORMAL_MOTD)) {
            values = SpigotControl.getControl(Files.NORMAL_MOTD).getIntegerList("normal." + motdName + ".otherSettings.customOnlinePlayers.values");
        }
        if(motdType.equals(MotdType.WHITELIST_MOTD)) {
            values = SpigotControl.getControl(Files.WHITELIST_MOTD).getIntegerList("whitelist." + motdName + ".otherSettings.customOnlinePlayers.values");
        }
        if(motdType.equals(MotdType.TIMER_MOTD)) {
            values = SpigotControl.getControl(Files.TIMER_MOTD).getIntegerList("timers." + motdName + ".otherSettings.customOnlinePlayers.values");
        }
        return values.get(new Random().nextInt(values.size()));
    }
    public static int getPlayersValue(MotdType motdType,String motdName) {
        List<Integer> values = new ArrayList<>();
        if(motdType.equals(MotdType.NORMAL_MOTD)) {
            values = SpigotControl.getControl(Files.NORMAL_MOTD).getIntegerList("normal." + motdName + ".otherSettings.customMaxPlayers.values");
        }
        if(motdType.equals(MotdType.WHITELIST_MOTD)) {
            values = SpigotControl.getControl(Files.WHITELIST_MOTD).getIntegerList("whitelist." + motdName + ".otherSettings.customMaxPlayers.values");
        }
        if(motdType.equals(MotdType.TIMER_MOTD)) {
            values = SpigotControl.getControl(Files.TIMER_MOTD).getIntegerList("timers." + motdName + ".otherSettings.customMaxPlayers.values");
        }
        return values.get(new Random().nextInt(values.size()));
    }
    public static List<Integer> getPlayersList(MotdType motdType,String motdName) {
        List<Integer> values = new ArrayList<>();
        if(motdType.equals(MotdType.NORMAL_MOTD)) {
            values = SpigotControl.getControl(Files.NORMAL_MOTD).getIntegerList("normal." + motdName + ".otherSettings.customMaxPlayers.values");
        }
        if(motdType.equals(MotdType.WHITELIST_MOTD)) {
            values = SpigotControl.getControl(Files.WHITELIST_MOTD).getIntegerList("whitelist." + motdName + ".otherSettings.customMaxPlayers.values");
        }
        if(motdType.equals(MotdType.TIMER_MOTD)) {
            values = SpigotControl.getControl(Files.TIMER_MOTD).getIntegerList("timers." + motdName + ".otherSettings.customMaxPlayers.values");
        }
        return values;
    }
    //private static ServerPing.PlayerInfo[] addHoverLine(ServerPing.PlayerInfo[] player, ServerPing.PlayerInfo info) {
    //    ServerPing.PlayerInfo[] hoverText = new ServerPing.PlayerInfo[player.length + 1];
    //    for(int id = 0; id < player.length; id++) {
    //        hoverText[id] = player[id];
    //    }
    //    hoverText[player.length] = info;
    //    return hoverText;
    //}
    public static boolean getHexMotdStatus(MotdType motdType,String motdName) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            return SpigotControl.getControl(Files.NORMAL_MOTD).getBoolean("normal." + motdName + ".otherSettings.customHexMotd.toggle");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            return SpigotControl.getControl(Files.WHITELIST_MOTD).getBoolean("whitelist." + motdName + ".otherSettings.customHexMotd.toggle");
        }
        return SpigotControl.getControl(Files.TIMER_MOTD).getBoolean("timers." + motdName + ".otherSettings.customHexMotd.toggle");
    }
    public static String getPlayer() {
        return SpigotControl.getControl(Files.SETTINGS).getString("settings.defaultUnknownUserName");
    }
    public static boolean getIconStatus(MotdType motdType,String motdName,boolean customFile) {
        if(!customFile) {
            if (motdType.equals(MotdType.NORMAL_MOTD)) {
                return SpigotControl.getControl(Files.NORMAL_MOTD).getBoolean("normal." + motdName + ".otherSettings.customIcon.toggle");
            }
            if (motdType.equals(MotdType.WHITELIST_MOTD)) {
                return SpigotControl.getControl(Files.WHITELIST_MOTD).getBoolean("whitelist." + motdName + ".otherSettings.customIcon.toggle");
            }
            return SpigotControl.getControl(Files.TIMER_MOTD).getBoolean("timers." + motdName + ".otherSettings.customIcon.toggle");
        }
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            return SpigotControl.getControl(Files.NORMAL_MOTD).getBoolean("normal." + motdName + ".otherSettings.customIcon.customFile");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            return SpigotControl.getControl(Files.WHITELIST_MOTD).getBoolean("whitelist." + motdName + ".otherSettings.customIcon.customFile");
        }
        return SpigotControl.getControl(Files.TIMER_MOTD).getBoolean("timers." + motdName + ".otherSettings.customIcon.customFile");
    }
    public static String applyColor(String message) {
        if(PixelSpigot.getHex().getStatus()) {
            return PixelSpigot.getHex().applyColor(message);
        }
        return ChatColor.translateAlternateColorCodes('&',message);
    }
    public static String applyColor(String message,ShowType showType) {
        if(showType.equals(ShowType.SECOND)) {
            return PixelSpigot.getHex().applyColor(ChatColor.translateAlternateColorCodes('&',message));
        }
        return ChatColor.translateAlternateColorCodes('&',message);
    }
}