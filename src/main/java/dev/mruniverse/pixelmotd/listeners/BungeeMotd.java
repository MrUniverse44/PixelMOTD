package dev.mruniverse.pixelmotd.listeners;

import dev.mruniverse.pixelmotd.PixelBungee;
import dev.mruniverse.pixelmotd.enums.*;
import dev.mruniverse.pixelmotd.listeners.bungeecord.MotdLoadEvent;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static dev.mruniverse.pixelmotd.enums.ShowType.FIRST;
import static dev.mruniverse.pixelmotd.utils.Logger.error;

@SuppressWarnings("UnstableApiUsage")
public class BungeeMotd implements Listener {
    private final PixelBungee plugin;

    public BungeeMotd(PixelBungee plugin) {
        this.plugin = plugin;
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPing(ProxyPingEvent e) {

        //*verify for cancelled motd to prevent errors
        if(e instanceof Cancellable && ((Cancellable) e).isCancelled()) return;
        if(e.getResponse() == null) return;
        //** load server ping.
        ServerPing.Protocol protocol;
        ServerPing.Players MotdPlayers;
        ServerPing.PlayerInfo[] motdHover;
        Favicon icon = null;

        //** load MotdType
        MotdType ShowMode;
        ShowType showType;
        boolean mHover;
        File iconFile;
        //* load strings & integers
        String line1,line2,motd,ShowMotd;
        int max,online;
        ServerPing response = e.getResponse();
        max = response.getPlayers().getMax();
        online = response.getPlayers().getOnline();
        PendingConnection connection = e.getConnection();


        //* generate the motd name & get the server whitelist status
        if(plugin.getBungeeControl().getControl(Files.EDITABLE).getBoolean("whitelist.toggle")) {
            ShowMotd = plugin.getBungeeControl().getMotd(true);
            ShowMode = MotdType.WHITELIST_MOTD;
        } else {
            ShowMotd = plugin.getBungeeControl().getMotd(false);
            ShowMode = MotdType.NORMAL_MOTD;
        }
        showType = ShowType.FIRST;
        //* Motd Version Setup
        if(connection != null) {
            if (e.getConnection().getVersion() >= 735) {
                if (plugin.getBungeeUtils().getHexMotdStatus(ShowMode, ShowMotd)) {
                    showType = ShowType.SECOND;
                }
            }
        }

        //* Database information recollect
        //e.getConnection().getVirtualHost().getAddress();

        //* Motd Hover Setup
        motdHover = plugin.getBungeeUtils().getHover(ShowMode,ShowMotd,online,max);
        mHover = plugin.getBungeeUtils().getHoverStatus(ShowMode,ShowMotd);
        iconFile = null;
        //* Custom Server Icon Setup
        if(plugin.getBungeeUtils().getIconStatus(ShowMode,ShowMotd,false)) {
            File[] icons;
            if(plugin.getBungeeUtils().getIconStatus(ShowMode,ShowMotd,true)) {
                icons = plugin.getBungeeUtils().getIcons(ShowMode,ShowMotd).listFiles();
            } else {
                if(ShowMode.equals(MotdType.NORMAL_MOTD)) {
                    icons = plugin.getFiles().getFile(Icons.NORMAL).listFiles();
                } else {
                    icons = plugin.getFiles().getFile(Icons.WHITELIST).listFiles();
                }
            }
            List<File> validIcons = new ArrayList<>();
            if (icons != null && icons.length != 0) {
                for (File image : icons) {
                    if (com.google.common.io.Files.getFileExtension(image.getPath()).equals("png")) {
                        validIcons.add(image);
                    }
                }
                if(validIcons.size() != 0) {
                    iconFile = validIcons.get(new Random().nextInt(validIcons.size()));
                    BufferedImage image = getImage(iconFile);
                    if(image != null) {
                        icon = Favicon.create(image);
                    } else {
                        icon = response.getFaviconObject();
                    }
                } else {
                    icon = response.getFaviconObject();
                }
            } else {
                icon = response.getFaviconObject();
            }
        }

        //* player setup
        if(plugin.getBungeeUtils().getPlayersStatus(ShowMode,ShowMotd)) {
            if(plugin.getBungeeUtils().getPlayersMode(ShowMode,ShowMotd).equals(ValueMode.ADD)) {
                max = online + 1;
            }
            if(plugin.getBungeeUtils().getPlayersMode(ShowMode,ShowMotd).equals(ValueMode.CUSTOM)) {
                max = plugin.getBungeeUtils().getPlayersValue(ShowMode,ShowMotd);
            }
            if(plugin.getBungeeUtils().getPlayersMode(ShowMode,ShowMotd).equals(ValueMode.HALF)) {
                if(online >= 2) {
                    max = online / 2;
                } else {
                    max = 0;
                }
            }
            if(plugin.getBungeeUtils().getPlayersMode(ShowMode,ShowMotd).equals(ValueMode.HALF_ADD)) {
                int add;
                if(online >= 2) {
                    add = online / 2;
                } else {
                    add = 0;
                }
                max = online + add;
            }
            if(plugin.getBungeeUtils().getPlayersMode(ShowMode,ShowMotd).equals(ValueMode.EQUAL)) {
                max = online;
            }
        }

        //*custom Protocol Setup
        if(plugin.getBungeeUtils().getProtocolStatus(ShowMode,ShowMotd)) {
            ServerPing.Protocol Received = response.getVersion();
            Received.setName(plugin.getBungeeUtils().applyColor(plugin.getBungeeUtils().replaceVariables(plugin.getBungeeUtils().getProtocolMessage(ShowMode,ShowMotd),online,max).replace("%server_icon%", plugin.getBungeeUtils().getServerIcon())));
            if(plugin.getBungeeUtils().getProtocolVersion(ShowMode,ShowMotd)) {
                Received.setProtocol(-1);
            }
            protocol = Received;
        } else {
            protocol = response.getVersion();
        }

        //* motd Lines Setup

        line1 = plugin.getBungeeUtils().getLine1(ShowMode,ShowMotd,showType);
        line2 = plugin.getBungeeUtils().getLine2(ShowMode,ShowMotd,showType);
        if(mHover) {
            MotdPlayers = new ServerPing.Players(max, online, motdHover);
        } else {
            MotdPlayers = new ServerPing.Players(max, online, response.getPlayers().getSample());
        }

        //* motd Lines to show - Setup
        String motdL;
        motdL = plugin.getBungeeUtils().applyColor(plugin.getBungeeUtils().replaceVariables(line1,online,max),showType) + "\n" + plugin.getBungeeUtils().applyColor(plugin.getBungeeUtils().replaceVariables(line2,online,max),showType);
        if(connection != null) {
            InetSocketAddress virtualHost = connection.getVirtualHost();
            if (virtualHost != null) {
                motd = replacePlayer(virtualHost.getAddress(), motdL);
            } else {
                motd = replacePlayer(null, motdL);
            }
        } else {
            motd = replacePlayer(null, motdL);
        }
        ServerPing result;
        if(showType.equals(FIRST)) {
            MotdLoadEvent event = new MotdLoadEvent(plugin, false, ShowMode, ShowMotd, line1, line2, motdL, protocol.getName(), iconFile, online, max);
            plugin.getProxy().getPluginManager().callEvent(event);
            result = new ServerPing(protocol, MotdPlayers, new TextComponent(motd), icon);
        } else {
            MotdLoadEvent event = new MotdLoadEvent(plugin, true, ShowMode, ShowMotd, line1, line2, motdL, protocol.getName(), iconFile, online, max);
            plugin.getProxy().getPluginManager().callEvent(event);
            result = new ServerPing(protocol, MotdPlayers, new TextComponent(TextComponent.fromLegacyText(motd)), icon);
        }
        e.setResponse(result);

    }
    private String replacePlayer(InetAddress address,String message) {
        if(message.contains("%player%")) {
            message = message.replace("%player%","Unknown");
        }
        if(message.contains("%address%")) {
            message = message.replace("%address%",address.getCanonicalHostName());
        }
        return message;
    }
    @SuppressWarnings("ConstantConditions")
    private BufferedImage getImage(File file) {
        try {
            return ImageIO.read(file);
        } catch(IOException exception) {
            reportBadImage(file.getPath());
            if(plugin.getBungeeControl().isDetailed()) {
                error("Information: ");
                if(exception.getCause().toString() != null) {
                    error("Cause: " + exception.getCause().toString());
                }
                if(exception.getMessage() != null) {
                    error("Message: " + exception.getMessage());
                }
                if(exception.getLocalizedMessage() != null) {
                    error("LocalizedMessage: " + exception.getLocalizedMessage());
                }
                if(exception.getStackTrace() != null) {
                    error("StackTrace: ");
                    for(StackTraceElement line : exception.getStackTrace()) {
                        error("(" + line.getLineNumber() + ") " + line.toString());
                    }
                }
                if(Arrays.toString(exception.getSuppressed()) != null) {
                    error("Suppressed: " + Arrays.toString(exception.getSuppressed()));
                }
                error("Class: " + exception.getClass().getName() +".class");
                error("Plugin version:" + PixelBungee.getInstance().getDescription().getVersion());
                error("---------------");
            }
            return null;
        }
    }
    private void reportBadImage(String filePath) {
        error("Can't read image: &b" + filePath + "&f. Please check image size: 64x64 or check if the image isn't corrupted.");
    }
}