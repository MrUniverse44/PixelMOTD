package dev.mruniverse.pixelmotd.files;

import dev.mruniverse.pixelmotd.enums.Files;
import dev.mruniverse.pixelmotd.enums.MotdType;
import dev.mruniverse.pixelmotd.enums.SaveMode;
import dev.mruniverse.pixelmotd.init.spigotPixelMOTD;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static dev.mruniverse.pixelmotd.init.spigotPixelMOTD.getFiles;

public class spigotControl {
    private static FileConfiguration rEditable, rModules, rSettings, rWhitelist, rNormal,rTimer,rCommand;
    public static void reloadFiles() {
        getFiles().loadFiles();
        rEditable = YamlConfiguration.loadConfiguration(getFiles().getFile(Files.EDITABLE));
        rCommand = YamlConfiguration.loadConfiguration(getFiles().getFile(Files.COMMAND));
        rModules = YamlConfiguration.loadConfiguration(getFiles().getFile(Files.MODULES));
        rSettings = YamlConfiguration.loadConfiguration(getFiles().getFile(Files.SETTINGS));
        rWhitelist = YamlConfiguration.loadConfiguration(getFiles().getFile(Files.WHITELIST_MOTD));
        rNormal = YamlConfiguration.loadConfiguration(getFiles().getFile(Files.NORMAL_MOTD));
        rTimer = YamlConfiguration.loadConfiguration(getFiles().getFile(Files.TIMER_MOTD));
    }
    public static boolean isCommandEnabled() {
        return true;
    }
    public static FileConfiguration getControl(Files fileToControl) {
        if(fileToControl.equals(Files.WHITELIST_MOTD)) {
            if(rWhitelist == null) reloadFiles();
            return rWhitelist;
        }
        if(fileToControl.equals(Files.COMMAND)) {
            if(rCommand == null) reloadFiles();
            return rCommand;
        }
        if(fileToControl.equals(Files.NORMAL_MOTD)) {
            if(rNormal == null) reloadFiles();
            return rNormal;
        }
        if(fileToControl.equals(Files.TIMER_MOTD)) {
            if(rTimer == null) reloadFiles();
            return rTimer;
        }
        if(fileToControl.equals(Files.EDITABLE)) {
            if(rEditable == null) reloadFiles();
            return rEditable;
        }
        if(fileToControl.equals(Files.MODULES)) {
            if(rModules == null) reloadFiles();
            return rModules;
        }
        if(fileToControl.equals(Files.SETTINGS)) {
            if(rSettings == null) reloadFiles();
            return rSettings;
        }
        getFiles().reportSpigotGetControlError();
        return rSettings;
    }
    public static boolean getWhitelistStatus() {
        return getControl(Files.EDITABLE).getBoolean("whitelist.toggle");
    }
    public static MotdType getMotdType(boolean whitelistStatus) {
        if(whitelistStatus) {
            return MotdType.WHITELIST_MOTD;
        }
        return MotdType.NORMAL_MOTD;
    }
    public static String getMotd(boolean isWhitelistMotd) {
        List<String> motdToGet = new ArrayList<>();
        if(isWhitelistMotd) {
            motdToGet.addAll(Objects.requireNonNull(getControl(Files.WHITELIST_MOTD).getConfigurationSection("whitelist")).getKeys(false));
            return motdToGet.get(new Random().nextInt(motdToGet.size()));
        }
        motdToGet.addAll(Objects.requireNonNull(getControl(Files.NORMAL_MOTD).getConfigurationSection("normal")).getKeys(false));
        return motdToGet.get(new Random().nextInt(motdToGet.size()));

    }
    public static String getWorlds(String msg) throws ParseException {
        if(msg.contains("%online_")) {
            for (World world : spigotPixelMOTD.getInstance().getServer().getWorlds()) {
                msg = msg.replace("%online_" + world.getName() + "%", world.getPlayers().size() + "");
            }
        }
        return replaceEventInfo(msg);
    }

    public static Date getEventDate(String eventName) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone(getControl(Files.SETTINGS).getString("events." + eventName + ".TimeZone")));
        return format.parse(getControl(Files.SETTINGS).getString("events." + eventName + ".eventDate"));
    }

    public static String replaceEventInfo(String motdLineOrHoverLine) throws ParseException {
        if(motdLineOrHoverLine.contains("%event_")) {
            Date CurrentDate;
            CurrentDate = new Date();
            for(String event : Objects.requireNonNull(getControl(Files.SETTINGS).getConfigurationSection("events")).getKeys(false)) {
                String TimeLeft = "<Invalid format-Type>";
                long difference = getEventDate(event).getTime() - CurrentDate.getTime();
                if(difference >= 0L) {
                    if(Objects.requireNonNull(getControl(Files.SETTINGS).getString("events." + event + ".format-Type")).equalsIgnoreCase("FIRST")) {
                        TimeLeft = convertToFinalResult(difference,"FIRST");
                    } else if(Objects.requireNonNull(getControl(Files.SETTINGS).getString("events." + event + ".format-Type")).equalsIgnoreCase("SECOND")) {
                        TimeLeft = convertToFinalResult(difference, "SECOND");
                    } else if(Objects.requireNonNull(getControl(Files.SETTINGS).getString("events." + event + ".format-Type")).equalsIgnoreCase("THIRD")) {
                        TimeLeft = convertToFinalResult(difference,"THIRD");
                    }
                } else {
                    TimeLeft = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getControl(Files.SETTINGS).getString("events." + event + ".endMessage")));
                }
                motdLineOrHoverLine = motdLineOrHoverLine.replace("%event_" + event +  "_name%", Objects.requireNonNull(getControl(Files.SETTINGS).getString("events." + event + ".eventName")))
                        .replace("%event_" + event + "_TimeZone%", Objects.requireNonNull(getControl(Files.SETTINGS).getString("events." + event + ".TimeZone")))
                        .replace("%event_" + event + "_TimeLeft%",TimeLeft);
            }
        }
        return motdLineOrHoverLine;
    }

    public static String convertToFinalResult(long time,String formatType) {
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

    public static boolean callMotds(MotdType motdType) {
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
    public static boolean isDetailed() {
        return getControl(Files.SETTINGS).getBoolean("settings.show-detailed-errors");
    }
    public static void reloadFile(SaveMode Mode) {
        getFiles().loadFiles();
        if(Mode.equals(SaveMode.EDITABLE) || Mode.equals(SaveMode.ALL)) {
            rEditable = YamlConfiguration.loadConfiguration(getFiles().getFile(Files.EDITABLE));
        }
        if(Mode.equals(SaveMode.COMMAND) || Mode.equals(SaveMode.ALL)) {
            rCommand = YamlConfiguration.loadConfiguration(getFiles().getFile(Files.COMMAND));
        }
        if(Mode.equals(SaveMode.MODULES) || Mode.equals(SaveMode.ALL)) {
            rModules = YamlConfiguration.loadConfiguration(getFiles().getFile(Files.MODULES));
        }
        if(Mode.equals(SaveMode.SETTINGS) || Mode.equals(SaveMode.ALL)) {
            rSettings = YamlConfiguration.loadConfiguration(getFiles().getFile(Files.SETTINGS));
        }
        if(Mode.equals(SaveMode.MOTDS) || Mode.equals(SaveMode.ALL)) {
            rWhitelist = YamlConfiguration.loadConfiguration(getFiles().getFile(Files.WHITELIST_MOTD));
            rNormal = YamlConfiguration.loadConfiguration(getFiles().getFile(Files.NORMAL_MOTD));
            rTimer = YamlConfiguration.loadConfiguration(getFiles().getFile(Files.TIMER_MOTD));
        }
    }
    public static void save(SaveMode Mode) {
        try {
            if(Mode.equals(SaveMode.MODULES) || Mode.equals(SaveMode.ALL)) {
                getControl(Files.MODULES).save(getFiles().getFile(Files.MODULES));
            }
            if(Mode.equals(SaveMode.TIMER_MOTD) || Mode.equals(SaveMode.ALL)) {
                getControl(Files.TIMER_MOTD).save(getFiles().getFile(Files.TIMER_MOTD));
            }
            if(Mode.equals(SaveMode.COMMAND) || Mode.equals(SaveMode.ALL)) {
                getControl(Files.COMMAND).save(getFiles().getFile(Files.COMMAND));
            }
            if(Mode.equals(SaveMode.EDITABLE) || Mode.equals(SaveMode.ALL)) {
                getControl(Files.EDITABLE).save(getFiles().getFile(Files.EDITABLE));
            }
            if(Mode.equals(SaveMode.NORMAL_MOTD) || Mode.equals(SaveMode.ALL)) {
                getControl(Files.NORMAL_MOTD).save(getFiles().getFile(Files.NORMAL_MOTD));
            }
            if(Mode.equals(SaveMode.WHITELIST_MOTD) || Mode.equals(SaveMode.ALL)) {
                getControl(Files.WHITELIST_MOTD).save(getFiles().getFile(Files.WHITELIST_MOTD));
            }
            if(Mode.equals(SaveMode.SETTINGS) || Mode.equals(SaveMode.ALL)) {
                getControl(Files.SETTINGS).save(getFiles().getFile(Files.SETTINGS));
            }
            if(Mode.equals(SaveMode.MOTDS) || Mode.equals(SaveMode.ALL)) {
                getControl(Files.NORMAL_MOTD).save(getFiles().getFile(Files.NORMAL_MOTD));
                getControl(Files.WHITELIST_MOTD).save(getFiles().getFile(Files.WHITELIST_MOTD));
                getControl(Files.TIMER_MOTD).save(getFiles().getFile(Files.TIMER_MOTD));
            }
        } catch(IOException exception) {
            spigotPixelMOTD.getFiles().reportControlError();
        }
    }
    public static String getWhitelistAuthor() {
        if(!Objects.requireNonNull(getControl(Files.EDITABLE).getString("whitelist.author")).equalsIgnoreCase("CONSOLE")) {
            return getControl(Files.EDITABLE).getString("whitelist.author");
        } else {
            if(getControl(Files.EDITABLE).getBoolean("whitelist.customConsoleName.toggle")) {
                return getControl(Files.EDITABLE).getString("whitelist.customConsoleName.name");
            }
            return "Console";
        }
    }
}