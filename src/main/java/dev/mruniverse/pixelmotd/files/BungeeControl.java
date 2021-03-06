package dev.mruniverse.pixelmotd.files;

import dev.mruniverse.pixelmotd.enums.Files;
import dev.mruniverse.pixelmotd.enums.MotdType;
import dev.mruniverse.pixelmotd.enums.SaveMode;
import dev.mruniverse.pixelmotd.PixelBungee;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static dev.mruniverse.pixelmotd.utils.Logger.error;
import static dev.mruniverse.pixelmotd.utils.Logger.info;

public class BungeeControl {
    private final PixelBungee plugin;

    private Configuration pEditable,pTimer, pModules ,pSettings, pWhitelist, pNormal,pCommand;

    public BungeeControl(PixelBungee plugin) {
        this.plugin = plugin;
    }

    private File newFile(String file) {
        return new File(plugin.getDataFolder(), file + ".yml");
    }

    private File getFile(Files fileToGet) {

        switch (fileToGet) {
            case NORMAL_MOTD:
                return newFile("normal-motd");
            case COMMAND:
                return newFile("command");
            case WHITELIST_MOTD:
                return newFile("whitelist-motd");
            case EDITABLE:
                return newFile("edit");
            case TIMER_MOTD:
                return newFile("timer-motd");
            case MODULES:
                return newFile("modules");
            default:
                return newFile("settings");
        }

//        if(fileToGet.equals(Files.NORMAL_MOTD)) {
//            return new File(PixelBungee.getInstance().getDataFolder(), "normal-motd.yml");
//        }
//        if(fileToGet.equals(Files.COMMAND)) {
//            return new File(PixelBungee.getInstance().getDataFolder(), "command.yml");
//        }
//        if(fileToGet.equals(Files.WHITELIST_MOTD)) {
//            return new File(PixelBungee.getInstance().getDataFolder(), "whitelist-motd.yml");
//        }
//        if(fileToGet.equals(Files.EDITABLE)) {
//            return new File(PixelBungee.getInstance().getDataFolder(), "edit.yml");
//        }
//        if(fileToGet.equals(Files.TIMER_MOTD)) {
//            return new File(PixelBungee.getInstance().getDataFolder(), "timer-motd.yml");
//        }
//        if(fileToGet.equals(Files.MODULES)) {
//            return new File(PixelBungee.getInstance().getDataFolder(), "modules.yml");
//        }
//        return new File(PixelBungee.getInstance().getDataFolder(), "settings.yml");
    }
    public boolean callMotds(MotdType motdType) {
        try {
            if (motdType.equals(MotdType.NORMAL_MOTD)) {
                return getControl(Files.NORMAL_MOTD).get("normal") == null;
            }
            if (motdType.equals(MotdType.WHITELIST_MOTD)) {
                return getControl(Files.WHITELIST_MOTD).get("whitelist") == null;
            }
            return getControl(Files.TIMER_MOTD).get("timers") == null;
        } catch(Throwable throwable) {
            return true;
        }
    }
    public String getServers(String msg) throws ParseException {
        if(msg.contains("%online_")) {
            for (ServerInfo svs : PixelBungee.getInstance().getProxy().getServers().values()) {
                msg = msg.replace("%online_" + svs.getName() + "%", svs.getPlayers().size() + "");
            }
        }
        return replaceEventInfo(msg);
    }

    public Date getEventDate(String eventName) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone(getControl(Files.SETTINGS).getString("events." + eventName + ".TimeZone")));
        return format.parse(getControl(Files.SETTINGS).getString("events." + eventName + ".eventDate"));
    }

    public String replaceEventInfo(String motdLineOrHoverLine) throws ParseException {
        if(motdLineOrHoverLine.contains("%event_")) {
            Date CurrentDate;
            CurrentDate = new Date();
            for(String event : getControl(Files.SETTINGS).getSection("events").getKeys()) {
                String TimeLeft = "<Invalid format-Type>";
                long difference = getEventDate(event).getTime() - CurrentDate.getTime();
                if(difference >= 0L) {
                    if(getControl(Files.SETTINGS).getString("events." + event + ".format-Type").equalsIgnoreCase("FIRST")) {
                        TimeLeft = convertToFinalResult(difference,"FIRST");
                    } else if(getControl(Files.SETTINGS).getString("events." + event + ".format-Type").equalsIgnoreCase("SECOND")) {
                        TimeLeft = convertToFinalResult(difference, "SECOND");
                    } else if(getControl(Files.SETTINGS).getString("events." + event + ".format-Type").equalsIgnoreCase("THIRD")) {
                        TimeLeft = convertToFinalResult(difference,"THIRD");
                    }
                } else {
                    TimeLeft = ChatColor.translateAlternateColorCodes('&',getControl(Files.SETTINGS).getString("events." + event + ".endMessage"));
                }
                motdLineOrHoverLine = motdLineOrHoverLine.replace("%event_" + event +  "_name%",getControl(Files.SETTINGS).getString("events." + event + ".eventName"))
                        .replace("%event_" + event + "_TimeZone%",getControl(Files.SETTINGS).getString("events." + event + ".TimeZone"))
                        .replace("%event_" + event + "_TimeLeft%",TimeLeft);
            }
        }
        return motdLineOrHoverLine;
    }
    public String convertToFinalResult(long time,String formatType) {
        StringJoiner joiner = new StringJoiner(" ");
        if (formatType.equalsIgnoreCase("SECOND")) {
            long seconds = time / 1000;
            int unitValue = Math.toIntExact(seconds / TimeUnit.DAYS.toSeconds(7));
            if (unitValue > 0) {
                seconds %= TimeUnit.DAYS.toSeconds(7);
                joiner.add(unitValue + ":");
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.DAYS.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.DAYS.toSeconds(1);
                joiner.add(unitValue + ":");
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.HOURS.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.HOURS.toSeconds(1);
                joiner.add(unitValue + ":");
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.MINUTES.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.MINUTES.toSeconds(1);
                joiner.add(unitValue + ":");
            }
            if (seconds > 0 || joiner.length() == 0) {
                joiner.add(seconds + "");
            }

        } else if(formatType.equalsIgnoreCase("FIRST")) {
            long seconds = time / 1000;
            String unit;
            int unitValue = Math.toIntExact(seconds / TimeUnit.DAYS.toSeconds(7));
            if (unitValue > 0) {
                seconds %= TimeUnit.DAYS.toSeconds(7);
                if (unitValue == 1) {
                    unit = getControl(Files.SETTINGS).getString("timings.week");
                } else {
                    unit = getControl(Files.SETTINGS).getString("timings.weeks");
                }
                joiner.add(unitValue + " " + unit);
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.DAYS.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.DAYS.toSeconds(1);
                if (unitValue == 1) {
                    unit = getControl(Files.SETTINGS).getString("timings.day");
                } else {
                    unit = getControl(Files.SETTINGS).getString("timings.days");
                }
                joiner.add(unitValue + " " + unit);
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.HOURS.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.HOURS.toSeconds(1);
                if (unitValue == 1) {
                    unit = getControl(Files.SETTINGS).getString("timings.hour");
                } else {
                    unit = getControl(Files.SETTINGS).getString("timings.hours");
                }

                joiner.add(unitValue + " " + unit);
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.MINUTES.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.MINUTES.toSeconds(1);
                if (unitValue == 1) {
                    unit = getControl(Files.SETTINGS).getString("timings.minute");
                } else {
                    unit = getControl(Files.SETTINGS).getString("timings.minutes");
                }

                joiner.add(unitValue + " " + unit);
            }
            if (seconds > 0 || joiner.length() == 0) {
                if (seconds == 1) {
                    unit = getControl(Files.SETTINGS).getString("timings.second");
                } else {
                    unit = getControl(Files.SETTINGS).getString("timings.seconds");
                }

                joiner.add(seconds + " " + unit);
            }
        } else if(formatType.equalsIgnoreCase("THIRD")) {
            long seconds = time / 1000;
            int unitValue = Math.toIntExact(seconds / TimeUnit.DAYS.toSeconds(7));
            if (unitValue > 0) {
                seconds %= TimeUnit.DAYS.toSeconds(7);
                joiner.add(unitValue + "w,");
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.DAYS.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.DAYS.toSeconds(1);
                joiner.add(unitValue + "d,");
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.HOURS.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.HOURS.toSeconds(1);
                joiner.add(unitValue + "h,");
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.MINUTES.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.MINUTES.toSeconds(1);
                joiner.add(unitValue + "m,");
            }
            if (seconds > 0 || joiner.length() == 0) {
                joiner.add(seconds + "s.");
            }
        }
        if(formatType.equalsIgnoreCase("SECOND")) {
            return joiner.toString().replace(" ","");
        } else {
            return joiner.toString();
        }
    }
    public boolean getWhitelistStatus() {
        return getControl(Files.EDITABLE).getBoolean("whitelist.toggle");
    }
    public String getMotd(boolean isWhitelistMotd) {
        List<String> motdToGet = new ArrayList<>();
        if(isWhitelistMotd) {
            motdToGet.addAll(getControl(Files.WHITELIST_MOTD).getSection("whitelist").getKeys());
            return motdToGet.get(new Random().nextInt(motdToGet.size()));
        }
        motdToGet.addAll(getControl(Files.NORMAL_MOTD).getSection("normal").getKeys());
        return motdToGet.get(new Random().nextInt(motdToGet.size()));

    }
    public static boolean isCommandEnabled() {
        return true;
    }
    public void reloadFile(SaveMode saveMode) {
        try {
            if(saveMode.equals(SaveMode.COMMAND) || saveMode.equals(SaveMode.ALL)) {
                pCommand = ConfigurationProvider.getProvider(YamlConfiguration.class).load(getFile(Files.COMMAND));
            }
            if(saveMode.equals(SaveMode.TIMER_MOTD) || saveMode.equals(SaveMode.ALL)) {
                pTimer = ConfigurationProvider.getProvider(YamlConfiguration.class).load(getFile(Files.TIMER_MOTD));
            }
            if(saveMode.equals(SaveMode.EDITABLE) || saveMode.equals(SaveMode.ALL)) {
                pEditable = ConfigurationProvider.getProvider(YamlConfiguration.class).load(getFile(Files.EDITABLE));
            }
            if(saveMode.equals(SaveMode.MODULES) || saveMode.equals(SaveMode.ALL)) {
                pModules = ConfigurationProvider.getProvider(YamlConfiguration.class).load(getFile(Files.MODULES));
            }
            if(saveMode.equals(SaveMode.SETTINGS) || saveMode.equals(SaveMode.ALL)) {
                pSettings = ConfigurationProvider.getProvider(YamlConfiguration.class).load(getFile(Files.SETTINGS));
            }
            if(saveMode.equals(SaveMode.WHITELIST_MOTD) || saveMode.equals(SaveMode.ALL)) {
                pWhitelist = ConfigurationProvider.getProvider(YamlConfiguration.class).load(getFile(Files.WHITELIST_MOTD));
            }
            if(saveMode.equals(SaveMode.NORMAL_MOTD) || saveMode.equals(SaveMode.ALL)) {
                pNormal = ConfigurationProvider.getProvider(YamlConfiguration.class).load(getFile(Files.NORMAL_MOTD));
            }
        } catch (IOException exp) {
            info("The plugin can't load or save configuration files! (Bungee | Spigot Control Issue - Caused by: IO Exception)");
            if(isDetailed()) {
                error("Information: ");
                if(exp.getMessage() != null) {
                    error("Message: " + exp.getMessage());
                }
                if(exp.getLocalizedMessage() != null) {
                    error("LocalizedMessage: " + exp.getLocalizedMessage());
                }
                if(exp.getStackTrace() != null) {
                    error("StackTrace: ");
                    for(StackTraceElement line : exp.getStackTrace()) {
                        error("(" + line.getLineNumber() + ") " + line.toString());
                    }
                }
                if(Arrays.toString(exp.getSuppressed()) != null) {
                    error("Suppressed: " + Arrays.toString(exp.getSuppressed()));
                }
                error("Class: " + exp.getClass().getName() +".class");
                error("Plugin version:" + PixelBungee.getInstance().getDescription().getVersion());
                error("---------------");
            }
        }
    }
    public void reloadFiles() {
        try {
            pCommand = ConfigurationProvider.getProvider(YamlConfiguration.class).load(getFile(Files.COMMAND));
            pTimer = ConfigurationProvider.getProvider(YamlConfiguration.class).load(getFile(Files.TIMER_MOTD));
            pEditable = ConfigurationProvider.getProvider(YamlConfiguration.class).load(getFile(Files.EDITABLE));
            pModules = ConfigurationProvider.getProvider(YamlConfiguration.class).load(getFile(Files.MODULES));
            pSettings = ConfigurationProvider.getProvider(YamlConfiguration.class).load(getFile(Files.SETTINGS));
            pWhitelist = ConfigurationProvider.getProvider(YamlConfiguration.class).load(getFile(Files.WHITELIST_MOTD));
            pNormal = ConfigurationProvider.getProvider(YamlConfiguration.class).load(getFile(Files.NORMAL_MOTD));
        } catch (IOException exp) {
            info("The plugin can't load or save configuration files! (Bungee | Spigot Control Issue - Caused by: IO Exception)");
            if(isDetailed()) {
                error("&a[Pixel MOTD] [Detailed Error] Information: ");
                //if(exp.getCause().toString() != null) {
                //    bungeePixelMOTD.sendConsole("&a[Pixel MOTD] Cause: " + exp.getCause().toString());
                //}
                if(exp.getMessage() != null) {
                    error("&a[Pixel MOTD] Message: " + exp.getMessage());
                }
                if(exp.getLocalizedMessage() != null) {
                    error("&a[Pixel MOTD] LocalizedMessage: " + exp.getLocalizedMessage());
                }
                if(exp.getStackTrace() != null) {
                    error("&a[Pixel MOTD] StackTrace: ");
                    for(StackTraceElement line : exp.getStackTrace()) {
                        error("&a[Pixel MOTD] (" + line.getLineNumber() + ") " + line.toString());
                    }
                }
                if(Arrays.toString(exp.getSuppressed()) != null) {
                    error("&a[Pixel MOTD] Suppressed: " + Arrays.toString(exp.getSuppressed()));
                }
                error("&a[Pixel MOTD] Class: " + exp.getClass().getName() +".class");
                error("&a[Pixel MOTD] Plugin version:" + PixelBungee.getInstance().getDescription().getVersion());
                error("&a[Pixel MOTD] --------------- [Detailed Error]");
            }
        }
    }
    public boolean isDetailed() {
        return getControl(Files.SETTINGS).getBoolean("settings.show-detailed-errors");
    }
    public Configuration getControl(Files fileToControl) {
        if(fileToControl.equals(Files.SETTINGS)) {
            if(pSettings == null) reloadFiles();
            return pSettings;
        }
        if(fileToControl.equals(Files.MODULES)) {
            if(pModules == null) reloadFiles();
            return pModules;
        }
        if(fileToControl.equals(Files.EDITABLE)) {
            if(pEditable == null) reloadFiles();
            return pEditable;
        }
        if(fileToControl.equals(Files.COMMAND)) {
            if(pCommand == null) reloadFiles();
            return pCommand;
        }
        if(fileToControl.equals(Files.NORMAL_MOTD)) {
            if(pNormal == null) reloadFiles();
            return pNormal;
        }
        if(fileToControl.equals(Files.TIMER_MOTD)) {
            if(pTimer == null) reloadFiles();
            return pTimer;
        }
        if(fileToControl.equals(Files.WHITELIST_MOTD)) {
            if(pWhitelist == null) reloadFiles();
            return pWhitelist;
        }
        info("The plugin can't load or save configuration files! (Bungee Control Issue - Caused by: One plugin is using bad the <getControl() from FileManager.class>)");
        return pSettings;
    }
    public void save(SaveMode Mode) {
        try {
            if(Mode.equals(SaveMode.MODULES) || Mode.equals(SaveMode.ALL)) {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(getControl(Files.MODULES), getFile(Files.MODULES));
            }
            if(Mode.equals(SaveMode.TIMER_MOTD) || Mode.equals(SaveMode.ALL)) {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(getControl(Files.TIMER_MOTD), getFile(Files.TIMER_MOTD));
            }
            if(Mode.equals(SaveMode.EDITABLE) || Mode.equals(SaveMode.ALL)) {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(getControl(Files.EDITABLE), getFile(Files.EDITABLE));
            }
            if(Mode.equals(SaveMode.NORMAL_MOTD) || Mode.equals(SaveMode.ALL)) {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(getControl(Files.NORMAL_MOTD), getFile(Files.NORMAL_MOTD));
            }
            if(Mode.equals(SaveMode.COMMAND) || Mode.equals(SaveMode.ALL)) {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(getControl(Files.COMMAND), getFile(Files.COMMAND));
            }
            if(Mode.equals(SaveMode.WHITELIST_MOTD) || Mode.equals(SaveMode.ALL)) {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(getControl(Files.WHITELIST_MOTD), getFile(Files.WHITELIST_MOTD));
            }
            if(Mode.equals(SaveMode.SETTINGS) || Mode.equals(SaveMode.ALL)) {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(getControl(Files.SETTINGS), getFile(Files.SETTINGS));
            }
        } catch(IOException exception) {
            info("The plugin can't load or save configuration files! (Bungee | Spigot Control Issue - Caused by: IO Exception)");
            if(isDetailed()) {
                error("&a[Pixel MOTD] [Detailed Error] Information: ");
                if (exception.getCause().toString() != null) {
                    error("&a[Pixel MOTD] Cause: " + exception.getCause().toString());
                }
                if (exception.getMessage() != null) {
                    error("&a[Pixel MOTD] Message: " + exception.getMessage());
                }
                if (exception.getLocalizedMessage() != null) {
                    error("&a[Pixel MOTD] LocalizedMessage: " + exception.getLocalizedMessage());
                }
                if(exception.getStackTrace() != null) {
                    error("&a[Pixel MOTD] StackTrace: ");
                    for(StackTraceElement line : exception.getStackTrace()) {
                        error("&a[Pixel MOTD] (" + line.getLineNumber() + ") " + line.toString());
                    }
                }
                if (Arrays.toString(exception.getSuppressed()) != null) {
                    error("&a[Pixel MOTD] Suppressed: " + Arrays.toString(exception.getSuppressed()));
                }
                error("&a[Pixel MOTD] Class: " + exception.getClass().getName() + ".class");
                error("&a[Pixel MOTD] Plugin version:" + PixelBungee.getInstance().getDescription().getVersion());
                error("&a[Pixel MOTD] --------------- [Detailed Error]");
            }
        }
    }
    public boolean pendingPath(MotdType motdType,String motdName) {
        String initial = "timers.";
        Files fileS = Files.TIMER_MOTD;
        if(motdType.equals(MotdType.NORMAL_MOTD)) {
            initial = "normal.";
            fileS = Files.NORMAL_MOTD;
        }
        if(motdType.equals(MotdType.WHITELIST_MOTD)) {
            initial = "whitelist.";
            fileS = Files.WHITELIST_MOTD;
        }
        if(motdType.equals(MotdType.NORMAL_MOTD)) {
            if(getControl(Files.NORMAL_MOTD).get(initial + motdName + ".enabled") == null) return true;
        }
        if(getControl(fileS).get(initial + motdName + ".line1") == null) return true;
        if(getControl(fileS).get(initial + motdName + ".line2") == null) return true;
        if(getControl(fileS).get(initial + motdName + ".otherSettings.customHover.toggle") == null) return true;
        if(getControl(fileS).get(initial + motdName + ".otherSettings.customHover.hover") == null) return true;
        if(getControl(fileS).get(initial + motdName + ".otherSettings.customIcon.toggle") == null) return true;
        if(getControl(fileS).get(initial + motdName + ".otherSettings.customIcon.customFile") == null) return true;
        if(getControl(fileS).get(initial + motdName + ".otherSettings.customProtocol.toggle") == null) return true;
        if(getControl(fileS).get(initial + motdName + ".otherSettings.customProtocol.changeProtocolVersion") == null) return true;
        if(getControl(fileS).get(initial + motdName + ".otherSettings.customProtocol.protocol") == null) return true;
        if(getControl(fileS).get(initial + motdName + ".otherSettings.customHexMotd.toggle") == null) return true;
        if(getControl(fileS).get(initial + motdName + ".otherSettings.customHexMotd.line1") == null) return true;
        if(getControl(fileS).get(initial + motdName + ".otherSettings.customHexMotd.line2") == null) return true;
        if(getControl(fileS).get(initial + motdName + ".otherSettings.customOnlinePlayers.toggle") == null) return true;
        if(getControl(fileS).get(initial + motdName + ".otherSettings.customOnlinePlayers.mode") == null) return true;
        if(getControl(fileS).get(initial + motdName + ".otherSettings.customOnlinePlayers.values") == null) return true;
        if(getControl(fileS).get(initial + motdName + ".otherSettings.customMaxPlayers.toggle") == null) return true;
        if(getControl(fileS).get(initial + motdName + ".otherSettings.customMaxPlayers.mode") == null) return true;
        return getControl(fileS).get(initial + motdName + ".otherSettings.customMaxPlayers.values") == null;
    }
    public void loadMotdPath(MotdType motdType,String motdName) {
        if(pendingPath(motdType,motdName)) {
            List<Object> stringList = new ArrayList<>();
            if (motdType.equals(MotdType.WHITELIST_MOTD)) {
                stringList.add("     &c&lPIXEL MOTD");
                stringList.add("&7SpigotMC Plugin v%plugin_version%");
                stringList.add("");
                stringList.add("&c&lInformation:");
                stringList.add("  &7Whitelist by: &f%whitelist_author%");
                stringList.add("  &7Spigot ID: &f37177");
                stringList.add("  &7Discord: &fMrUniverse#2556");
                stringList.add("  &7Online: &f%online%");
                stringList.add("  &frigox.club/discord/dev");
                stringList.add("");
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".line1", "&8» &aPixelMOTD v%plugin_version% &7| &aSpigotMC");
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".line2", "&f&oThis server is in whitelist. (1.8-1.15 Motd)");
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".otherSettings.customHover.toggle", true);
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".otherSettings.customHover.hover", stringList);
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".otherSettings.customIcon.toggle", true);
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".otherSettings.customIcon.customFile", false);
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".otherSettings.customProtocol.toggle", true);
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".otherSettings.customProtocol.changeProtocolVersion", false);
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".otherSettings.customProtocol.protocol", "PixelMotd Security");
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".otherSettings.customHexMotd.toggle", true);
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".otherSettings.customHexMotd.line1", "&8» &cPixelMOTD v%plugin_version% &7| &cSpigotMC");
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".otherSettings.customHexMotd.line2", "&f&oWhitelist Mode (1.16+ Motd)");
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".otherSettings.customOnlinePlayers.toggle", false);
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".otherSettings.customOnlinePlayers.mode", "HALF-ADD");
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".otherSettings.customMaxPlayers.toggle", true);
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".otherSettings.customMaxPlayers.mode", "HALF");
                stringList = new ArrayList<>();
                stringList.add(2021);
                stringList.add(2022);
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".otherSettings.customOnlinePlayers.values", stringList);
                plugin.getFiles().addConfig(Files.WHITELIST_MOTD, "whitelist." + motdName + ".otherSettings.customMaxPlayers.values", stringList);
                return;
            }
            if (motdType.equals(MotdType.NORMAL_MOTD)) {
                stringList.add("     &9&lPIXEL MOTD");
                stringList.add("&7SpigotMC Plugin v%plugin_version%");
                stringList.add("");
                stringList.add("&b&lInformation:");
                stringList.add("  &7Version: &f%plugin_version%");
                stringList.add("  &7Spigot ID: &f37177");
                stringList.add("  &7Discord: &fMrUniverse#2556");
                stringList.add("  &7Online: &f%online%&7/&f%max%");
                stringList.add("  &frigox.club/discord/dev");
                stringList.add("");
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".line1", "&b&lPixelMOTD v%plugin_version%");
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".line2", "&f&oThis motd only appear for 1.8 - 1.15");
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".otherSettings.customHover.toggle", true);
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".otherSettings.customHover.hover", stringList);
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".otherSettings.customIcon.toggle", true);
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".otherSettings.customIcon.customFile", false);
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".otherSettings.customProtocol.toggle", true);
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".otherSettings.customProtocol.changeProtocolVersion", false);
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".otherSettings.customProtocol.protocol", "PixelMotd System");
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".otherSettings.customHexMotd.toggle", true);
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".otherSettings.customHexMotd.line1", "&b&lPixelMOTD v%plugin_version%");
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".otherSettings.customHexMotd.line2", "&f&oThis motd only appear for 1.16+");
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".otherSettings.customOnlinePlayers.toggle", false);
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".otherSettings.customOnlinePlayers.mode", "HALF-ADD");
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".otherSettings.customMaxPlayers.toggle", true);
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".otherSettings.customMaxPlayers.mode", "HALF-ADD");
                stringList = new ArrayList<>();
                stringList.add(2021);
                stringList.add(2022);
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".otherSettings.customOnlinePlayers.values", stringList);
                plugin.getFiles().addConfig(Files.NORMAL_MOTD, "normal." + motdName + ".otherSettings.customMaxPlayers.values", stringList);
                return;
            }
            stringList.add("     &9&lPIXEL MOTD");
            stringList.add("&7This is a timer motd");
            stringList.add("&7When you enable 1 motd");
            stringList.add("&7And you have 1 event with");
            stringList.add("&7the same name it will be");
            stringList.add("&7Sync. And when an event");
            stringList.add("&7End, it will execute commands");
            stringList.add("&7By the console automatically!");
            stringList.add("&frigox.club/discord/dev");
            stringList.add("");
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".enabled", false);
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".line1", "&6&l%event_timeLeft%");
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".line2", "&f&oThis is a timer motd");
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".otherSettings.customHover.toggle", true);
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".otherSettings.customHover.hover", stringList);
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".otherSettings.customIcon.toggle", true);
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".otherSettings.customIcon.customFile", false);
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".otherSettings.customProtocol.toggle", true);
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".otherSettings.customProtocol.changeProtocolVersion", false);
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".otherSettings.customProtocol.protocol", "PixelMotd System");
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".otherSettings.customHexMotd.toggle", true);
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".otherSettings.customHexMotd.line1", "&6&l%event_timeLeft%");
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".otherSettings.customHexMotd.line2", "&f&oThis motd only appear for 1.16+");
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".otherSettings.customOnlinePlayers.toggle", false);
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".otherSettings.customOnlinePlayers.mode", "HALF-ADD");
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".otherSettings.customMaxPlayers.toggle", true);
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".otherSettings.customMaxPlayers.mode", "HALF-ADD");
            stringList = new ArrayList<>();
            stringList.add("/pmotd whitelist off");
            stringList.add("/alert Maintenance off automatically!");
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".timerSettings.commandsToExecute", stringList);
            stringList = new ArrayList<>();
            stringList.add(2021);
            stringList.add(2022);
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".otherSettings.customOnlinePlayers.values", stringList);
            plugin.getFiles().addConfig(Files.TIMER_MOTD, "timers." + motdName + ".otherSettings.customMaxPlayers.values", stringList);
        }
    }
    public void loadMotdPaths(MotdType motdType) {
        if(motdType.equals(MotdType.NORMAL_MOTD)) {
            for (String motdName : Objects.requireNonNull(getControl(Files.NORMAL_MOTD).getSection("normal")).getKeys()) {
                loadMotdPath(motdType,motdName);
            }
            return;
        }
        if(motdType.equals(MotdType.WHITELIST_MOTD)) {
            for (String motdName : Objects.requireNonNull(getControl(Files.WHITELIST_MOTD).getSection("whitelist")).getKeys()) {
                loadMotdPath(motdType,motdName);
            }
            return;
        }
        for (String motdName : Objects.requireNonNull(getControl(Files.TIMER_MOTD).getSection("timers")).getKeys()) {
            loadMotdPath(motdType,motdName);
        }
    }
    public String getWhitelistAuthor() {
        if(!getControl(Files.EDITABLE).getString("whitelist.author").equalsIgnoreCase("CONSOLE")) {
            return getControl(Files.EDITABLE).getString("whitelist.author");
        } else {
            if(getControl(Files.EDITABLE).getBoolean("whitelist.customConsoleName.toggle")) {
                return getControl(Files.EDITABLE).getString("whitelist.customConsoleName.name");
            }
            return "Console";
        }
    }
}