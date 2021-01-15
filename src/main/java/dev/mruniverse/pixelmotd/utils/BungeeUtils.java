package dev.mruniverse.pixelmotd.utils;

import dev.mruniverse.pixelmotd.enums.*;
import dev.mruniverse.pixelmotd.files.BungeeControl;
import dev.mruniverse.pixelmotd.init.BungeePixel;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.File;
import java.text.ParseException;
import java.util.*;

public class BungeeUtils {
    public static List<String> getPlayers(WhitelistMembers mode, String serverName) {
        if(mode.equals(WhitelistMembers.NAMEs)) {
            if(BungeeControl.getControl(Files.MODULES).get(Extras.getServerPath(Whitelist.PLAYERS_NAME,serverName)) != null)
                if(BungeeControl.getControl(Files.MODULES).get("modules.server-whitelist.servers." + serverName + " .players-name") != null) {
                    return BungeeControl.getControl(Files.MODULES).getStringList("modules.server-whitelist.servers." + serverName + " .players-name");
                }
            return new ArrayList<>();
        }
        if(BungeeControl.getControl(Files.MODULES).get("modules.server-whitelist.servers." + serverName + " .players-uuid") != null) {
            return BungeeControl.getControl(Files.MODULES).getStringList("modules.server-whitelist.servers." + serverName + " .players-uuid");
        }
        return new ArrayList<>();
    }
    public static List<String> getPlayers(BlacklistMembers mode, String serverName) {
        if(mode.equals(BlacklistMembers.NAMEs)) {
            if(BungeeControl.getControl(Files.MODULES).get(Extras.getServerPath(Blacklist.PLAYERS_NAME,serverName)) != null)
                if(BungeeControl.getControl(Files.MODULES).get("modules.server-blacklist.servers." + serverName + " .players-name") != null) {
                    return BungeeControl.getControl(Files.MODULES).getStringList("modules.server-blacklist.servers." + serverName + " .players-name");
                }
            return new ArrayList<>();
        }
        if(BungeeControl.getControl(Files.MODULES).get("modules.server-blacklist.servers." + serverName + " .players-uuid") != null) {
            return BungeeControl.getControl(Files.MODULES).getStringList("modules.server-blacklist.servers." + serverName + " .players-uuid");
        }
        return new ArrayList<>();
    }
    public static ServerPing.PlayerInfo[] getHover(MotdType motdType, String motdName,int online,int max) {
        int ids = 0;
        ServerPing.PlayerInfo[] hoverToShow = new ServerPing.PlayerInfo[0];
        if(motdType.equals(MotdType.NORMAL_MOTD)) {
            if(BungeeControl.getControl(Files.NORMAL_MOTD).getBoolean("normal." + motdName + ".otherSettings.customHover.toggle")) {
                for(String line : BungeeControl.getControl(Files.NORMAL_MOTD).getStringList("normal." + motdName + ".otherSettings.customHover.hover")) {
                    try {
                        hoverToShow = addHoverLine(hoverToShow, new ServerPing.PlayerInfo(applyColor(BungeeControl.getServers(line.replace("&","§").replace("%plugin_version%", BungeePixel.getInstance().getDescription().getVersion()).replace("%online%", online + "").replace("%max%", max + "").replace("%whitelist_author%", BungeeControl.getWhitelistAuthor()))), String.valueOf(ids)));
                    } catch (ParseException e) {
                        reportHoverError();
                        if(BungeeControl.isDetailed()) {
                            BungeePixel.sendConsole("&a[Pixel MOTD] [Detailed Error] Information: ");
                            //if(e.getCause().toString() != null) {
                            //    bungeePixelMOTD.sendConsole("&a[Pixel MOTD] Cause: " + e.getCause().toString());
                            //}
                            if(e.getMessage() != null) {
                                BungeePixel.sendConsole("&a[Pixel MOTD] Message: " + e.getMessage());
                            }
                            if(e.getLocalizedMessage() != null) {
                                BungeePixel.sendConsole("&a[Pixel MOTD] LocalizedMessage: " + e.getLocalizedMessage());
                            }
                            if(e.getStackTrace() != null) {
                                BungeePixel.sendConsole("&a[Pixel MOTD] StackTrace: ");
                                for(StackTraceElement str : e.getStackTrace()) {
                                    BungeePixel.sendConsole("&a[Pixel MOTD] (" + str.getLineNumber() + ") " + str.toString());
                                }
                            }
                            if(Arrays.toString(e.getSuppressed()) != null) {
                                BungeePixel.sendConsole("&a[Pixel MOTD] Suppressed: " + Arrays.toString(e.getSuppressed()));
                            }
                            BungeePixel.sendConsole("&a[Pixel MOTD] ErrorOffset: " + e.getErrorOffset());
                            BungeePixel.sendConsole("&a[Pixel MOTD] Class: " + e.getClass().getName() +".class");
                            BungeePixel.sendConsole("&a[Pixel MOTD] Plugin version:" + BungeePixel.getInstance().getDescription().getVersion());
                            BungeePixel.sendConsole("&a[Pixel MOTD] --------------- [Detailed Error]");
                        }
                    }
                    ids++;
                }
                return hoverToShow;
            }
            hoverToShow = addHoverLine(hoverToShow, new ServerPing.PlayerInfo("", ""));
            return hoverToShow;
        }
        if(BungeeControl.getControl(Files.WHITELIST_MOTD).getBoolean("whitelist." + motdName + ".otherSettings.customHover.toggle")) {
            for(String line : BungeeControl.getControl(Files.WHITELIST_MOTD).getStringList("whitelist." + motdName + ".otherSettings.customHover.hover")) {
                try {
                    hoverToShow = addHoverLine(hoverToShow, new ServerPing.PlayerInfo(applyColor(BungeeControl.getServers(line.replace("&","§").replace("%plugin_version%", BungeePixel.getInstance().getDescription().getVersion()).replace("%online%", online + "").replace("%max%", max + "").replace("%whitelist_author%", BungeeControl.getWhitelistAuthor()))), String.valueOf(ids)));
                } catch (ParseException e) {
                    reportHoverError();
                    if(BungeeControl.isDetailed()) {
                        BungeePixel.sendConsole("&a[Pixel MOTD] [Detailed Error] Information: ");
                        //if(e.getCause().toString() != null) {
                        //    bungeePixelMOTD.sendConsole("&a[Pixel MOTD] Cause: " + e.getCause().toString());
                        //}
                        if(e.getMessage() != null) {
                            BungeePixel.sendConsole("&a[Pixel MOTD] Message: " + e.getMessage());
                        }
                        if(e.getLocalizedMessage() != null) {
                            BungeePixel.sendConsole("&a[Pixel MOTD] LocalizedMessage: " + e.getLocalizedMessage());
                        }
                        if(e.getStackTrace() != null) {
                            BungeePixel.sendConsole("&a[Pixel MOTD] StackTrace: ");
                            for(StackTraceElement str : e.getStackTrace()) {
                                BungeePixel.sendConsole("&a[Pixel MOTD] (" + str.getLineNumber() + ") " + str.toString());
                            }
                        }
                        if(Arrays.toString(e.getSuppressed()) != null) {
                            BungeePixel.sendConsole("&a[Pixel MOTD] Suppressed: " + Arrays.toString(e.getSuppressed()));
                        }
                        BungeePixel.sendConsole("&a[Pixel MOTD] ErrorOffset: " + e.getErrorOffset());
                        BungeePixel.sendConsole("&a[Pixel MOTD] Class: " + e.getClass().getName() +".class");
                        BungeePixel.sendConsole("&a[Pixel MOTD] Plugin version:" + BungeePixel.getInstance().getDescription().getVersion());
                        BungeePixel.sendConsole("&a[Pixel MOTD] --------------- [Detailed Error]");
                    }
                }
                ids++;
            }
            return hoverToShow;
        }
        hoverToShow = addHoverLine(hoverToShow, new ServerPing.PlayerInfo("", ""));
        return hoverToShow;
    }
    private static void reportHoverError() {
        BungeePixel.sendConsole("Can't generate motd Hover, please verify if your hover is correctly created!");
    }
    private static void reportProtocolError() {
        BungeePixel.sendConsole("Can't generate motd Protocol, please verify if your protocol is correctly created!");
    }
    public static File getIcons(MotdType motdType,String motdName) {
        File iconFolder = BungeePixel.getFiles().getFile(Icons.FOLDER);
        if(motdType.equals(MotdType.NORMAL_MOTD)) {
            iconFolder = new File(BungeePixel.getFiles().getFile(Icons.FOLDER), "Normal-" + motdName);
        }
        if(motdType.equals(MotdType.WHITELIST_MOTD)) {
            iconFolder = new File(BungeePixel.getFiles().getFile(Icons.FOLDER), "Whitelist-" + motdName);
        }
        if(!iconFolder.exists()) BungeePixel.getFiles().loadFolder(iconFolder,"&fIcon Folder: &b" + motdName);
        return iconFolder;
    }
    public static boolean getPlayersStatus(MotdType motdType,String motdName) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            return BungeeControl.getControl(Files.NORMAL_MOTD).getBoolean("normal." + motdName + ".otherSettings.customMaxPlayers.toggle");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            return BungeeControl.getControl(Files.WHITELIST_MOTD).getBoolean("whitelist." + motdName + ".otherSettings.customMaxPlayers.toggle");
        }
        return BungeeControl.getControl(Files.TIMER_MOTD).getBoolean("timers." + motdName + ".otherSettings.customMaxPlayers.toggle");
    }
    public static boolean getProtocolStatus(MotdType motdType,String motdName) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            return BungeeControl.getControl(Files.NORMAL_MOTD).getBoolean("normal." + motdName + ".otherSettings.customProtocol.toggle");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            return BungeeControl.getControl(Files.WHITELIST_MOTD).getBoolean("whitelist." + motdName + ".otherSettings.customProtocol.toggle");
        }
        return BungeeControl.getControl(Files.TIMER_MOTD).getBoolean("timers." + motdName + ".otherSettings.customProtocol.toggle");
    }
    public static ValueMode getPlayersMode(MotdType motdType, String motdName) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            if(BungeeControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customMaxPlayers.mode").equalsIgnoreCase("CUSTOM-VALUES")) {
                return ValueMode.CUSTOM;
            }
            if(BungeeControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customMaxPlayers.mode").equalsIgnoreCase("ADD")) {
                return ValueMode.ADD;
            }
            if(BungeeControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customMaxPlayers.mode").equalsIgnoreCase("HALF-ADD")) {
                return ValueMode.HALF_ADD;
            }
            if(BungeeControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customMaxPlayers.mode").equalsIgnoreCase("HALF")) {
                return ValueMode.HALF;
            }
            return ValueMode.EQUAL;
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            if(BungeeControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customMaxPlayers.mode").equalsIgnoreCase("CUSTOM-VALUES")) {
                return ValueMode.CUSTOM;
            }
            if(BungeeControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customMaxPlayers.mode").equalsIgnoreCase("ADD")) {
                return ValueMode.ADD;
            }
            if(BungeeControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customMaxPlayers.mode").equalsIgnoreCase("HALF-ADD")) {
                return ValueMode.HALF_ADD;
            }
            if(BungeeControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customMaxPlayers.mode").equalsIgnoreCase("HALF")) {
                return ValueMode.HALF;
            }
            return ValueMode.EQUAL;
        }
        if(BungeeControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customMaxPlayers.mode").equalsIgnoreCase("CUSTOM-VALUES")) {
            return ValueMode.CUSTOM;
        }
        if(BungeeControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customMaxPlayers.mode").equalsIgnoreCase("ADD")) {
            return ValueMode.ADD;
        }
        if(BungeeControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customMaxPlayers.mode").equalsIgnoreCase("HALF-ADD")) {
            return ValueMode.HALF_ADD;
        }
        if(BungeeControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customMaxPlayers.mode").equalsIgnoreCase("HALF")) {
            return ValueMode.HALF;
        }
        return ValueMode.EQUAL;
    }
    public static String getServerIcon() { return "                                                                   "; }
    public static String getLine1(MotdType motdType,String motdName, ShowType showType) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            if(showType.equals(ShowType.FIRST)) {
                return BungeeControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".line1");
            }
            return BungeeControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customHexMotd.line1");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            if(showType.equals(ShowType.FIRST)) {
                return BungeeControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".line1");
            }
            return BungeeControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customHexMotd.line1");
        }
        if(showType.equals(ShowType.FIRST)) {
            return BungeeControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".line1");
        }
        return BungeeControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customHexMotd.line1");
    }
    public static String getLine2(MotdType motdType, String motdName, ShowType showType) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            if(showType.equals(ShowType.FIRST)) {
                return BungeeControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".line2");
            }
            return BungeeControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customHexMotd.line2");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            if(showType.equals(ShowType.FIRST)) {
                return BungeeControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".line2");
            }
            return BungeeControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customHexMotd.line2");
        }
        if(showType.equals(ShowType.FIRST)) {
            return BungeeControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".line2");
        }
        return BungeeControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customHexMotd.line2");
    }
    //getHoverStatus
    public static boolean getHoverStatus(MotdType motdType,String motdName) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            return BungeeControl.getControl(Files.NORMAL_MOTD).getBoolean("normal." + motdName + ".otherSettings.customHover.toggle");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            return BungeeControl.getControl(Files.WHITELIST_MOTD).getBoolean("whitelist." + motdName + ".otherSettings.customHover.toggle");
        }
        return BungeeControl.getControl(Files.TIMER_MOTD).getBoolean("timers." + motdName + ".otherSettings.customHover.toggle");
    }
    public static boolean getProtocolVersion(MotdType motdType,String motdName) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            return BungeeControl.getControl(Files.NORMAL_MOTD).getBoolean("normal." + motdName + ".otherSettings.customProtocol.changeProtocolVersion");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            return BungeeControl.getControl(Files.WHITELIST_MOTD).getBoolean("whitelist." + motdName + ".otherSettings.customProtocol.changeProtocolVersion");
        }
        return BungeeControl.getControl(Files.TIMER_MOTD).getBoolean("timers." + motdName + ".otherSettings.customProtocol.changeProtocolVersion");
    }
    public static String getProtocolMessage(MotdType motdType,String motdName) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            return BungeeControl.getControl(Files.NORMAL_MOTD).getString("normal." + motdName + ".otherSettings.customProtocol.protocol");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            return BungeeControl.getControl(Files.WHITELIST_MOTD).getString("whitelist." + motdName + ".otherSettings.customProtocol.protocol");
        }
        return BungeeControl.getControl(Files.TIMER_MOTD).getString("timers." + motdName + ".otherSettings.customProtocol.protocol");
    }
    public static String replaceVariables(String msg,int online,int max) {
        try {
            msg = BungeeControl.getServers(msg).replace("%online%",online + "")
                    .replace("%max%",max + "")
                    .replace("%plugin_author%","MrUniverse44")
                    .replace("%whitelist_author%", BungeeControl.getWhitelistAuthor())
                    .replace("%plugin_version%", BungeePixel.getInstance().getDescription().getVersion());
        } catch (ParseException e) {
            reportProtocolError();
            if(BungeeControl.isDetailed()) {
                BungeePixel.sendConsole("&a[Pixel MOTD] [Detailed Error] Information: ");
                //if(e.getCause().toString() != null) {
                //    bungeePixelMOTD.sendConsole("&a[Pixel MOTD] Cause: " + e.getCause().toString());
                //}
                if(e.getMessage() != null) {
                    BungeePixel.sendConsole("&a[Pixel MOTD] Message: " + e.getMessage());
                }
                if(e.getLocalizedMessage() != null) {
                    BungeePixel.sendConsole("&a[Pixel MOTD] LocalizedMessage: " + e.getLocalizedMessage());
                }
                if(e.getStackTrace() != null) {
                    BungeePixel.sendConsole("&a[Pixel MOTD] StackTrace: ");
                    for(StackTraceElement line : e.getStackTrace()) {
                        BungeePixel.sendConsole("&a[Pixel MOTD] (" + line.getLineNumber() + ") " + line.toString());
                    }
                }
                if(Arrays.toString(e.getSuppressed()) != null) {
                    BungeePixel.sendConsole("&a[Pixel MOTD] Suppressed: " + Arrays.toString(e.getSuppressed()));
                }
                BungeePixel.sendConsole("&a[Pixel MOTD] ErrorOffset: " + e.getErrorOffset());
                BungeePixel.sendConsole("&a[Pixel MOTD] Class: " + e.getClass().getName() +".class");
                BungeePixel.sendConsole("&a[Pixel MOTD] Plugin version:" + BungeePixel.getInstance().getDescription().getVersion());
                BungeePixel.sendConsole("&a[Pixel MOTD] --------------- [Detailed Error]");
            }
        }
        return msg;
    }
    public static int getPlayersValue(MotdType motdType,String motdName) {
        List<Integer> values = new ArrayList<>();
        if(motdType.equals(MotdType.NORMAL_MOTD)) {
            values = BungeeControl.getControl(Files.NORMAL_MOTD).getIntList("normal." + motdName + ".otherSettings.customMaxPlayers.values");
        }
        if(motdType.equals(MotdType.WHITELIST_MOTD)) {
            values = BungeeControl.getControl(Files.WHITELIST_MOTD).getIntList("whitelist." + motdName + ".otherSettings.customMaxPlayers.values");
        }
        if(motdType.equals(MotdType.TIMER_MOTD)) {
            values = BungeeControl.getControl(Files.TIMER_MOTD).getIntList("timers." + motdName + ".otherSettings.customMaxPlayers.values");
        }
        return values.get(new Random().nextInt(values.size()));
    }
    private static ServerPing.PlayerInfo[] addHoverLine(ServerPing.PlayerInfo[] player, ServerPing.PlayerInfo info) {
        ServerPing.PlayerInfo[] hoverText = new ServerPing.PlayerInfo[player.length + 1];
        for(int id = 0; id < player.length; id++) {
            hoverText[id] = player[id];
        }
        hoverText[player.length] = info;
        return hoverText;
    }
    public static boolean getHexMotdStatus(MotdType motdType,String motdName) {
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            return BungeeControl.getControl(Files.NORMAL_MOTD).getBoolean("normal." + motdName + ".otherSettings.customHexMotd.toggle");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            return BungeeControl.getControl(Files.WHITELIST_MOTD).getBoolean("whitelist." + motdName + ".otherSettings.customHexMotd.toggle");
        }
        return BungeeControl.getControl(Files.TIMER_MOTD).getBoolean("timers." + motdName + ".otherSettings.customHexMotd.toggle");
    }
    public static String getPlayer() {
        return BungeeControl.getControl(Files.SETTINGS).getString("settings.defaultUnknownUserName");
    }
    public static void sendColored(CommandSender sender, String message) {
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
    }
    public static String getPermissionMessage(String permission) {
        try {
            if (BungeeControl.getControl(Files.EDITABLE).getString("messages.no-perms").contains("<permission>")) {
                return Objects.requireNonNull(BungeeControl.getControl(Files.EDITABLE).getString("messages.no-perms")).replace("<permission>", permission);
            }
        } catch (Throwable throwable) {
            reportMistake();
        }
        return BungeeControl.getControl(Files.EDITABLE).getString("messages.no-perms");
    }
    public static void sendColored(ProxiedPlayer player, String message) {
        player.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
    }
    private static void reportMistake() {
        BungeePixel.sendConsole("&e[Pixel MOTD] &fThe plugin found an issue, fixing internal issue.");
    }
    public static boolean getIconStatus(MotdType motdType,String motdName,boolean customFile) {
        if(!customFile) {
            if (motdType.equals(MotdType.NORMAL_MOTD)) {
                return BungeeControl.getControl(Files.NORMAL_MOTD).getBoolean("normal." + motdName + ".otherSettings.customIcon.toggle");
            }
            if (motdType.equals(MotdType.WHITELIST_MOTD)) {
                return BungeeControl.getControl(Files.WHITELIST_MOTD).getBoolean("whitelist." + motdName + ".otherSettings.customIcon.toggle");
            }
            return BungeeControl.getControl(Files.TIMER_MOTD).getBoolean("timers." + motdName + ".otherSettings.customIcon.toggle");
        }
        if (motdType.equals(MotdType.NORMAL_MOTD)) {
            return BungeeControl.getControl(Files.NORMAL_MOTD).getBoolean("normal." + motdName + ".otherSettings.customIcon.customFile");
        }
        if (motdType.equals(MotdType.WHITELIST_MOTD)) {
            return BungeeControl.getControl(Files.WHITELIST_MOTD).getBoolean("whitelist." + motdName + ".otherSettings.customIcon.customFile");
        }
        return BungeeControl.getControl(Files.TIMER_MOTD).getBoolean("timers." + motdName + ".otherSettings.customIcon.customFile");
    }
    public static String applyColor(String message) {
        if(BungeePixel.getHex().getStatus()) {
            return nowCentered(BungeePixel.getHex().applyColor(message));
        }
        return nowCentered(ChatColor.translateAlternateColorCodes('&',message));
    }
    public static String applyColor(String message,ShowType showType) {
        if(showType.equals(ShowType.SECOND)) {
            return nowCentered(BungeePixel.getHex().applyColor(ChatColor.translateAlternateColorCodes('&',message)));
        }

        return nowCentered(ChatColor.translateAlternateColorCodes('&',message));
    }
    private static String nowCentered(String msg) {
        if(msg.contains("<centerText>")) {
            msg = msg.replace("<centerText>","");
            msg = CenterMotd.centerMotd(msg);
        }
        return msg;
    }
}