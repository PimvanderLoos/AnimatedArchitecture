package nl.pim16aap2.bigDoors.util;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.BigDoors.MCVersion;
import nl.pim16aap2.bigDoors.compatibility.IProtectionCompatDefinition;
import nl.pim16aap2.bigDoors.compatiblity.ProtectionCompatDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class ConfigLoader
{
    private String dbFile;
    private final String header;
    private int coolDown;
    private boolean makeBackup;
    private boolean allowStats;
    private int maxDoorSize;
    private double pcMultiplier = 1.0;
    private double dbMultiplier = 1.0;
    private double bdMultiplier = 1.0;
    private double sdMultiplier = 1.0;
    private double flMultiplier = 1.0;
    private double elMultiplier = 1.0;
    private boolean resourcePackEnabled;
    private String resourcePack;
    private String languageFile;
    private int maxDoorCount;
    private int cacheTimeout;
    private boolean announceUpdateCheck;
    private boolean autoDLUpdate;
    private long downloadDelay;
    private boolean enableRedstone;
    private int commandWaiterTimeout;
    private boolean enableFileLogging;
    private int maxBlocksToMove;
    private int soundRange;
    private boolean unsafeMode;
    private boolean loadChunksForToggle;
    private int maxPowerBlockDistance;
    private int maxAutoCloseTimer;
    private boolean skipUnloadedAutoCloseToggle;
    private boolean allowNotifications;
    private boolean allowCodeGeneration;
    private boolean forceCodeGeneration;

    private HashSet<Material> powerBlockTypesMap;
    private Map<IProtectionCompatDefinition, Boolean> hooksMap;
    private Set<Material> blacklist;
    private Set<Material> whitelist;

    private String doorPrice, drawbridgePrice, portcullisPrice, slidingDoorPrice;

    private final ArrayList<ConfigOption> configOptionsList;
    public static boolean DEBUG = false;
    private final BigDoors plugin;

    private static final List<String> DEFAULTPOWERBLOCK = new ArrayList<>(Arrays.asList("GOLD_BLOCK"));

    private static final EnumMap<MCVersion, String> RESOURCEPACKS = new EnumMap<>(MCVersion.class);
    static
    {
        RESOURCEPACKS.put(MCVersion.v1_11,
                          "https://www.dropbox.com/s/6zdkg4jr90pc1mi/BigDoorsResourcePack-1_11.zip?dl=1");
        RESOURCEPACKS.put(MCVersion.v1_12,
                          "https://www.dropbox.com/s/6zdkg4jr90pc1mi/BigDoorsResourcePack-1_11.zip?dl=1");
        RESOURCEPACKS.put(MCVersion.v1_13,
                          "https://www.dropbox.com/s/al4idl017ggpnuq/BigDoorsResourcePack-1_13.zip?dl=1");
        RESOURCEPACKS.put(MCVersion.v1_14,
                          "https://www.dropbox.com/s/al4idl017ggpnuq/BigDoorsResourcePack-1_13.zip?dl=1");
        RESOURCEPACKS.put(MCVersion.v1_15,
                          "https://www.dropbox.com/s/frkik8qpv3jep9v/BigDoorsResourcePack-1_15.zip?dl=1");
        RESOURCEPACKS.put(MCVersion.v1_16,
                          "https://www.dropbox.com/s/frkik8qpv3jep9v/BigDoorsResourcePack-1_15.zip?dl=1");
        RESOURCEPACKS.put(MCVersion.v1_17,
                          "https://www.dropbox.com/s/frkik8qpv3jep9v/BigDoorsResourcePack-Format7.zip?dl=1");
    }

    public ConfigLoader(BigDoors plugin)
    {
        this.plugin = plugin;
        resourcePack = RESOURCEPACKS.get(BigDoors.getMCVersion());
        configOptionsList = new ArrayList<>();
        powerBlockTypesMap = new HashSet<>();
        hooksMap = new HashMap<>(ProtectionCompatDefinition.DEFAULT_COMPAT_DEFINITIONS.size());
        header = "Config file for BigDoors. Don't forget to make a backup before making changes!";
        makeConfig();
    }

    // Read the current config, the make a new one based on the old one or default
    // values, whichever is applicable.
    public void makeConfig()
    {
        String[] enableRedstoneComment = { "Allow doors to be opened using redstone signals." };
        String[] powerBlockTypeComment = { "Choose the type of the power block that is used to open doors using redstone.",
                                           "A list can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html",
                                           "This is the block that will open the door attached to it when it receives a redstone signal." };
        String[] blacklistComment = { "List of blacklisted materials. Materials on this list can not be animated.",
                                      "Use the same list of materials as for the power blocks. For example, you would blacklist bedrock like so:",
                                      "  - BEDROCK" };
        String[] whitelistComment = { "List of whitelisted materials. Materials on this list can be animated even if they're blacklisted ",
                                      "(either in the blacklist setting or hardcoded in the plugin).",
                                      "Use the same list of materials as for the power blocks. For example, you would whitelist a bell like so:",
                                      "  - BELL" };
        String[] maxDoorCountComment = { "Maximum number of doors a player can own. -1 = infinite." };
        String[] languageFileComment = { "Specify a language file to be used. Note that en_US.txt will get regenerated!" };
        String[] dbFileComment = { "Pick the name (and location if you want) of the database." };
        String[] downloadDelayComment = { "Time (in minutes) to delay auto downloading updates after their release.",
                                          "Setting it to 2160 means that updates will be downloaded 36h after their release.",
                                          "This is useful, as it will mean that the update won't get downloaded if I decide to pull it for some reason",
                                          "(within the specified timeframe, of course). Note that updates cannot be deferred for more than 1 week (10080 minutes)." };
        String[] announceUpdateCheckComment = { "Whether to announce that the plugin is looking for updates when no updates are available.",
                                                "This does not affect the actual check, just the messages that it is checking (once per 12 hours)." };
        String[] autoDLUpdateComment = { "Allow this plugin to automatically download new updates. They will be applied on restart." };
        String[] allowStatsComment = { "Allow this plugin to send (anonymised) stats using bStats. Please consider keeping it enabled.",
                                       "It has a negligible impact on performance and more users on stats keeps me more motivated to support this plugin!",
                                       "You can see all the stats that are gathered here: https://bstats.org/plugin/bukkit/BigDoors" };
        String[] maxDoorSizeComment = { "Max. number of blocks allowed in a door (including air blocks). Doors exceeding this limit cannt be created or used.",
                                        "For example, If you'd want to allow doors of 20 by 10 blocks, you'd need a limit of at least 200.",
                                        "This is a global limit that not even OPs can bypass. Use permissions for more fine-grained control.",
                                        "Use -1 to disable this limit." };
        String[] maxPowerBlockDistanceComment = { "The maximum distance between a door's engine and its powerblock. Like maxDoorSize, this is a global limit. ",
                                                  "Not even OPs can bypass it! Use -1 to disable this limit." };
        String[] maxAutocloseTimerComment = { "The maximum value of an autoCloseTimer (specified in ticks). ",
                                              "A value of 6000 is 5 minutes. Use a negative value to allow unlimited values. " };
        String[] resourcePackComment = { "This plugin uses a support resource pack for things suchs as sound.",
                                         "Different packs will be used for different versions of Minecraft:",
                                         "The resource pack for 1.11.x/1.12.x is: '"
                                             + RESOURCEPACKS.get(MCVersion.v1_11) + "'",
                                         "The resource pack for 1.13.x/1.14.x is: '"
                                             + RESOURCEPACKS.get(MCVersion.v1_13) + "'",
                                         "The resource pack for 1.15.x/1.16.x is: '"
                                             + RESOURCEPACKS.get(MCVersion.v1_15) + "'",
                                         "The resource pack for 1.17.x is: '"
                                             + RESOURCEPACKS.get(MCVersion.v1_17) + "'",
                                             };
        String[] multiplierComment = { "These multipliers affect the opening/closing speed of their respective door types.",
                                       "Note that the maximum speed is limited, so beyond a certain point rasising these values won't have any effect.",
                                       "To use the default values, set them to \"0.0\" or \"1.0\" (without quotation marks).",
                                       "bd = Big Door, pc = PortCullis, db = DrawBridge, sd = Sliding Door.",
                                       "Note that everything is optimized for default values, so it's recommended to leave this setting as-is." };
        String[] compatibilityHooks = { "Enable or disable compatibility hooks for certain plugins. If the plugins aren't installed, these options do nothing.",
                                        "When enabled, doors cannot be opened or created in areas not owned by the door's owner." };
        String[] coolDownComment = { "Cooldown on using doors. Time is measured in seconds." };
        String[] cacheTimeoutComment = { "Amount of time (in minutes) to cache powerblock positions. -1 means no caching (not recommended!), 0 = infinite cache.",
                                         "Doesn't take up a lot of RAM, so it's recommended to leave this value high. It'll get updated automatically when needed anyway." };
        String[] pricesComment = { "When Vault is present, you can set the price of door creation here for every type of door.",
                                   "You can use the word \"blockCount\" (without quotationmarks, case sensitive) as a variable that will be replaced by the actual blockCount.",
                                   "Furthermore, you can use these operators: -, +, *, /, sqrt(), ^, %, min(a,b), max(a,b), abs(), log(a), ln(a), e, pi, and parentheses.",
                                   "For example: \"doorPrice='max(10, sqrt(16)^4/100*blockCount)'\" would return 10 for a blockCount of 0 to 3 and 10.24 for a blockCount of 4.",
                                   "You must always put the formula or simple value or whatever in quotation marks! Also, these settings do nothing if Vault isn't installed!" };
        String[] commandWaiterTimeoutComment = { "Amount of time (measured in seconds) until a command waiter times out.",
                                                 "Don't forget to update the language file if you change this!" };
        String[] maxBlocksToMoveComment = { "The maximum number of blocks a door can move. This only applies to doors that move in a straight line (e.g. sliding door).",
                                            "Values less than 1 are invalid. " };
        String[] loadChunksForToggleComment = { "Try to load chunks when a door is toggled. When set to false, doors will not be toggled if more than 1 chunk needs to be loaded.",
                                                "When set to true, the plugin will try to load all chunks the door will interact with before toggling. If more than 1 chunk ",
                                                "needs to be loaded, the door will skip its animation to avoid spawning a bunch of entities no one can see anyway." };
        String[] skipUnloadedAutoCloseToggleComment = { "Skip toggles in unloaded chunks if the door has an autoCloseTimer set. ",
                                                        "This avoids loading chunks only to load them again a little bit later for the autoCloseTimer. ",
                                                        "The autoCloseTimer itself is not affected, so the timer can still toggle doors regardless of ",
                                                        "this specific setting. ",
                                                        "Note that this setting has not effect if \"loadChunksForToggle\" is disabled." };
        String[] soundRangeComment = {"The range of the sounds the doors make, counted in number of blocks. Note that using too high ranges may cause lag.",
                                      "The sound is only played at the engine of a door.",
                                      "Use a value of 0 or less to completely disable all sounds."};

        String[] unsafeModeComment = { "Only load this plugin in supported environments.",
                                       "Enabling this is NOT SUPPORTED and you WILL run into issues. ",
                                       "By enabling this option you agree that you will not complain if any issues arise and that it is completely",
                                       "your own responsibility.",
                                       "If you need to enable this option you are doing it wrong and you should rethink your life choices." };

        String[] allowNotificationsComment = { "Whether or not to allow toggle notifications. ",
                                               "When enabled, door creators can opt-in to receive notifications whenever a door is toggled.",
                                               "This is on a per-door basis."};

        String[] allowCodeGenerationComment = { "On unsupported versions of Minecraft, BigDoors can try to generate the required code itself.",
                                                "This means that the plugin may work even on unsupported versions, ",
                                                "but also that unexpected issues might pop up! Be sure to test everything when using this!",
                                                "Note that this is geared only towards NEWER version of Minecraft!",
                                                "In other words: No, this cannot be used to support 1.8! It. Will. Not. Work.",
                                                "There are many other reasons than the code to be generated why it will not work.",
                                                "The code being generated is the code used to create/move/etc animated blocks.",
                                                "So when testing this on your TEST SERVER and AFTER MAKING A BACKUP, be sure to check that that still works."};

        String[] forceCodeGenerationComment = { "Forces BigDoors to use generated code even on supported versions.",
                                                "This may be useful in case the mappings change within a single version.",
                                                "In general, however, you will not need this and you're better off not using it!",
                                                "When this option is enabled, it overrides the \"" + allowCodeGeneration + "\" option."};

        String[] debugComment = { "Don't use this. Just leave it on false." };
        String[] enableFileLoggingComment = { "Whether to write stuff to BigDoor's own log file. Please keep this enabled if you want to receive support." };
        String[] backupComment = { "Make a backup of the database before upgrading it. I'd recommend leaving this on true. ",
                                   "In case anything goes wrong, you can just revert to the old version! Only the most recent backup will be kept." };

        FileConfiguration config = plugin.getConfig();

        // Read all the options from the config, then put them in a configOption with
        // their name, value and comment.
        // Then put all configOptions into an ArrayList.
        enableRedstone = config.getBoolean("allowRedstone", true);
        configOptionsList.add(new ConfigOption("allowRedstone", enableRedstone, enableRedstoneComment));

        readPowerBlockConfig(config, powerBlockTypeComment);

        blacklist = readMaterialConfig(config, blacklistComment, "materialBlacklist", "Blacklisted");
        whitelist = readMaterialConfig(config, whitelistComment, "materialWhitelist", "Whitelisted");

        maxDoorCount = config.getInt("maxDoorCount", -1);
        configOptionsList.add(new ConfigOption("maxDoorCount", maxDoorCount, maxDoorCountComment));

        maxBlocksToMove = Math.max(1, config.getInt("maxBlocksToMove", 100));
        configOptionsList.add(new ConfigOption("maxBlocksToMove", maxBlocksToMove, maxBlocksToMoveComment));

        languageFile = config.getString("languageFile", "en_US");
        configOptionsList.add(new ConfigOption("languageFile", languageFile, languageFileComment));

        dbFile = config.getString("dbFile", "doorDB.db");
        configOptionsList.add(new ConfigOption("dbFile", dbFile, dbFileComment));

        announceUpdateCheck = config.getBoolean("announceUpdateCheck", true);
        configOptionsList.add(new ConfigOption("announceUpdateCheck", announceUpdateCheck, announceUpdateCheckComment));

        autoDLUpdate = config.getBoolean("auto-update", true);
        configOptionsList.add(new ConfigOption("auto-update", autoDLUpdate, autoDLUpdateComment));

        downloadDelay = Math.min(10080, config.getLong("downloadDelay", 2160));
        configOptionsList.add(new ConfigOption("downloadDelay", downloadDelay, downloadDelayComment));
        downloadDelay *= 60; // Convert to seconds after adding the option to the config.

        allowStats = config.getBoolean("allowStats", true);
        configOptionsList.add(new ConfigOption("allowStats", allowStats, allowStatsComment));

        int idx = 0;
        for (IProtectionCompatDefinition compat : ProtectionCompatDefinition.DEFAULT_COMPAT_DEFINITIONS)
        {
            final String name = compat.getName().toLowerCase(Locale.US);
            final boolean isEnabled = config.getBoolean(name, true);
            configOptionsList.add(new ConfigOption(name, isEnabled, ((idx++ == 0) ? compatibilityHooks : null)));
            hooksMap.put(compat, isEnabled);
        }

        maxDoorSize = config.getInt("maxDoorSize", -1);
        configOptionsList.add(new ConfigOption("maxDoorSize", maxDoorSize, maxDoorSizeComment));

        maxPowerBlockDistance = config.getInt("maxPowerBlockDistance", -1);
        configOptionsList
            .add(new ConfigOption("maxPowerBlockDistance", maxPowerBlockDistance, maxPowerBlockDistanceComment));

        maxAutoCloseTimer = config.getInt("maxAutoCloseTimer", 6000);
        configOptionsList.add(new ConfigOption("maxAutoCloseTimer", maxAutoCloseTimer, maxAutocloseTimerComment));

        resourcePackEnabled = config.getBoolean("resourcePackEnabled", true);
        configOptionsList.add(new ConfigOption("resourcePackEnabled", resourcePackEnabled, resourcePackComment));

        soundRange = config.getInt("soundRange", 15);
        configOptionsList.add(new ConfigOption("soundRange", soundRange, soundRangeComment));

        bdMultiplier = config.getDouble("bdMultiplier", 1.0D);
        configOptionsList.add(new ConfigOption("bdMultiplier", bdMultiplier, multiplierComment));

        pcMultiplier = config.getDouble("pcMultiplier", 1.0D);
        configOptionsList.add(new ConfigOption("pcMultiplier", pcMultiplier, null));

        dbMultiplier = config.getDouble("dbMultiplier", 1.0D);
        configOptionsList.add(new ConfigOption("dbMultiplier", dbMultiplier, null));

        sdMultiplier = config.getDouble("sdMultiplier", 1.0D);
        configOptionsList.add(new ConfigOption("sdMultiplier", sdMultiplier, null));

        flMultiplier = config.getDouble("flMultiplier", 1.0D);
        configOptionsList.add(new ConfigOption("flMultiplier", flMultiplier, null));

        elMultiplier = config.getDouble("elMultiplier", 1.0D);
        configOptionsList.add(new ConfigOption("elMultiplier", elMultiplier, null));

        coolDown = config.getInt("coolDown", 0);
        configOptionsList.add(new ConfigOption("coolDown", coolDown, coolDownComment));

        makeBackup = config.getBoolean("makeBackup", true);
        configOptionsList.add(new ConfigOption("makeBackup", makeBackup, backupComment));

        cacheTimeout = config.getInt("cacheTimeout", 120);
        configOptionsList.add(new ConfigOption("cacheTimeout", cacheTimeout, cacheTimeoutComment));

        doorPrice = config.getString("doorPrice", "0");
        configOptionsList.add(new ConfigOption("doorPrice", doorPrice, pricesComment));

        drawbridgePrice = config.getString("drawbridgePrice", "0");
        configOptionsList.add(new ConfigOption("drawbridgePrice", drawbridgePrice, null));

        portcullisPrice = config.getString("portcullisPrice", "0");
        configOptionsList.add(new ConfigOption("portcullisPrice", portcullisPrice, null));

        slidingDoorPrice = config.getString("slidingDoorPrice", "0");
        configOptionsList.add(new ConfigOption("slidingDoorPrice", slidingDoorPrice, null));

        commandWaiterTimeout = config.getInt("commandWaiterTimeout", 40);
        configOptionsList
            .add(new ConfigOption("commandWaiterTimeout", commandWaiterTimeout, commandWaiterTimeoutComment));

        loadChunksForToggle = config.getBoolean("loadChunksForToggle", true);
        configOptionsList.add(new ConfigOption("loadChunksForToggle", loadChunksForToggle, loadChunksForToggleComment));

        skipUnloadedAutoCloseToggle = config.getBoolean("skipUnloadedAutoCloseToggle", true);
        configOptionsList.add(new ConfigOption("skipUnloadedAutoCloseToggle", skipUnloadedAutoCloseToggle,
                                               skipUnloadedAutoCloseToggleComment));

        allowNotifications = config.getBoolean("allowNotifications", true);
        configOptionsList.add(new ConfigOption("allowNotifications", allowNotifications, allowNotificationsComment));

        allowCodeGeneration = config.getBoolean("allowCodeGeneration", false);
        configOptionsList.add(new ConfigOption("allowCodeGeneration", allowCodeGeneration, allowCodeGenerationComment));

        forceCodeGeneration = config.getBoolean("forceCodeGeneration", false);
        configOptionsList.add(new ConfigOption("forceCodeGeneration", forceCodeGeneration, forceCodeGenerationComment));

        enableFileLogging = config.getBoolean("enableFileLogging", true);
        configOptionsList.add(new ConfigOption("enableFileLogging", enableFileLogging, enableFileLoggingComment));

        // This is a bit special, as it's public static (for util debug messages).
        unsafeMode = config.getBoolean("unsafeMode", false);
        configOptionsList.add(new ConfigOption("unsafeMode", unsafeMode, unsafeModeComment));

        ConfigLoader.DEBUG = config.getBoolean("DEBUG", false);
        configOptionsList.add(new ConfigOption("DEBUG", ConfigLoader.DEBUG, debugComment));

        writeConfig();
    }

    private void readPowerBlockConfig(FileConfiguration config, String[] powerBlockTypeComment)
    {
        List<String> materials;

        {
            List<?> materialsTmp = config.getList("powerBlockTypes", DEFAULTPOWERBLOCK);
            // If the user is illiterate and failed to read the part saying it should be an
            // enum string and used
            // non-String values, put those in a new String list.
            materials = new ArrayList<>();
            materialsTmp.forEach(K -> materials.add(K.toString()));
        }

        // Try to put all the found materials into a String map.
        // Only valid and solid material Strings are allowed.
        // Everything else is thrown out.
        Iterator<String> it = materials.iterator();
        while (it.hasNext())
        {
            String str = it.next();
            try
            {
                Material mat = Material.valueOf(str);
                if (mat.isSolid())
                    powerBlockTypesMap.add(mat);
                else
                    plugin.getMyLogger().logMessage("Failed to add material: \"" + str + "\". Only solid materials are "
                        + "allowed to be powerblocks!", true, false);
            }
            catch (Exception e)
            {
                plugin.getMyLogger().logMessage("Failed to parse material: \"" + str + "\"", true, false);
            }
        }

        // If the user didn't supply even a single valid block, use the default.
        if (powerBlockTypesMap.size() == 0)
        {
            StringBuilder sb = new StringBuilder();
            DEFAULTPOWERBLOCK.forEach(K -> sb.append(K).append(" "));
            plugin.getMyLogger().logMessage("No materials found for powerBlockType! Defaulting to:" + sb, true, false);
            DEFAULTPOWERBLOCK.forEach(K ->
            {
                powerBlockTypesMap.add(Material.valueOf(K));
                materials.add(K);
            });
        }
        configOptionsList.add(new ConfigOption("powerBlockTypes", materials, powerBlockTypeComment));
        plugin.getMyLogger().logMessageToConsoleOnly("Power Block Types:");
        powerBlockTypesMap.forEach(K -> plugin.getMyLogger().logMessageToConsoleOnly(" - " + K.toString()));
    }

    /**
     * Read material names from a config option.
     *
     * @param config     The config to read the values from.
     * @param comment    The comment of the config option.
     * @param optionName The name of the option
     * @param reportName The name to use for reporting. E.g.: "blacklisted" +
     *                   "materials:" (when "blacklisted" is the reportName.
     * @return The set of materials. Only solid materials are added.
     */
    private Set<Material> readMaterialConfig(final FileConfiguration config, final String[] comment,
                                             final String optionName, final String reportName)
    {
        List<String> materialNames;

        {
            List<?> materialsTmp = config.getList(optionName, new ArrayList<Material>());
            // If the user is illiterate and failed to read the part saying it should be an
            // enum string and used
            // non-String values, put those in a new String list.
            materialNames = new ArrayList<>();
            materialsTmp.forEach(K -> materialNames.add(K.toString()));
        }

        List<Material> materials = new ArrayList<>(materialNames.size());
        // Try to put all the found materials into a String map.
        // Only valid and solid material Strings are allowed.
        // Everything else is thrown out.
        Iterator<String> it = materialNames.iterator();
        while (it.hasNext())
        {
            String str = it.next();
            try
            {
                Material mat = Material.valueOf(str);
                if (mat != null)
                    materials.add(mat);
                else
                    plugin.getMyLogger().logMessageToConsoleOnly("Failed to parse material: \"" + str + "\"");
            }
            catch (Exception e)
            {
                plugin.getMyLogger().logMessageToConsoleOnly("Failed to parse material: \"" + str + "\"");
            }
        }

        if (materials.size() == 0)
            plugin.getMyLogger().logMessageToConsoleOnly("No materials " + reportName + "!");
        else
        {
            plugin.getMyLogger().logMessageToConsoleOnly(reportName + " materials:");
            materialNames.forEach(K -> plugin.getMyLogger().logMessageToConsoleOnly(" - " + K));
        }
        configOptionsList.add(new ConfigOption(optionName, materialNames, comment));
        return new HashSet<>(Collections.unmodifiableList(materials));
    }

    // Write new config file.
    public void writeConfig()
    {
        // Write all the config options to the config.yml.
        try
        {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists())
                dataFolder.mkdirs();

            File saveTo = new File(plugin.getDataFolder(), "config.yml");
            if (!saveTo.exists())
                saveTo.createNewFile();

            if (!saveTo.canWrite())
            {
                plugin.getMyLogger().warn("=======================================");
                plugin.getMyLogger().warn("============== !WARNING! ==============");
                plugin.getMyLogger().warn("=======================================");
                plugin.getMyLogger().warn("====== CANNOT WRITE CONFIG FILE! ======");
                plugin.getMyLogger().warn("==== NEW OPTIONS WILL NOT SHOW UP! ====");
                plugin.getMyLogger().warn("==== THEY WILL USE DEFAULT VALUES! ====");
                plugin.getMyLogger().warn("=======================================");
                plugin.getMyLogger().warn("============== !WARNING! ==============");
                plugin.getMyLogger().warn("=======================================");
            }

            FileWriter fw = new FileWriter(saveTo, false);
            PrintWriter pw = new PrintWriter(fw);

            if (header != null)
                pw.println("# " + header + "\n");

            for (int idx = 0; idx < configOptionsList.size(); ++idx)
            {
                try
                {
                    pw.println(configOptionsList.get(idx).toString() +
                    // Only print an additional newLine if the next config option has a comment.
                        (idx < configOptionsList.size() - 1 && configOptionsList.get(idx + 1).getComment() == null ?
                            "" : "\n"));
                }
                catch (Exception e)
                {
                    plugin.getMyLogger().warn("Failed to write config option \"" + configOptionsList.get(idx).getName()
                        + "\"! " + "Please contact pim16aap2 and attach the error below:");
                    e.printStackTrace();
                    plugin.getMyLogger().logMessageToLogFile(Util.exceptionToString(e));
                }
            }

            pw.flush();
            pw.close();
        }
        catch (IOException e)
        {
            Bukkit.getLogger()
                .log(Level.SEVERE,
                     "Could not save config.yml! Please contact pim16aap2 and show him the following code:");
            e.printStackTrace();
        }
    }

    public String dbFile()
    {
        return dbFile;
    }

    public boolean dbBackup()
    {
        return makeBackup;
    }

    public int coolDown()
    {
        return coolDown;
    }

    public boolean allowStats()
    {
        return allowStats;
    }

    public int maxDoorSize()
    {
        return maxDoorSize;
    }

    public int cacheTimeout()
    {
        return cacheTimeout;
    }

    public double pcMultiplier()
    {
        return pcMultiplier;
    }

    public double dbMultiplier()
    {
        return dbMultiplier;
    }

    public double bdMultiplier()
    {
        return bdMultiplier;
    }

    public double sdMultiplier()
    {
        return sdMultiplier;
    }

    public double flMultiplier()
    {
        return flMultiplier;
    }

    public double elMultiplier()
    {
        return elMultiplier;
    }

    public String resourcePack()
    {
        return resourcePack;
    }

    public String languageFile()
    {
        return languageFile;
    }

    public int maxdoorCount()
    {
        return maxDoorCount;
    }

    public boolean announceUpdateCheck()
    {
        return announceUpdateCheck;
    }

    public boolean autoDLUpdate()
    {
        return autoDLUpdate;
    }

    /**
     * Gets the amount time (in seconds) to wait before downloading an update. If
     * set to 24 hours (86400 seconds), and an update was released on Monday June 1
     * at 12PM, it will not download this update before Tuesday June 2 at 12PM. When
     * running a dev-build, however, this value is overridden to 0.
     *
     * @return The amount time (in seconds) to wait before downloading an update.
     */
    public long downloadDelay()
    {
        return downloadDelay;
    }

    public boolean enableRedstone()
    {
        return enableRedstone;
    }

    public HashSet<Material> getPowerBlockTypes()
    {
        return powerBlockTypesMap;
    }

    public boolean isHookEnabled(final IProtectionCompatDefinition hook)
    {
        return hooksMap.get(hook);
    }

    public int getMaxBlocksToMove()
    {
        return maxBlocksToMove;
    }

    public String doorPrice()
    {
        return doorPrice;
    }

    public String drawbridgePrice()
    {
        return drawbridgePrice;
    }

    public String portcullisPrice()
    {
        return portcullisPrice;
    }

    public String slidingDoorPrice()
    {
        return slidingDoorPrice;
    }

    public int commandWaiterTimeout()
    {
        return commandWaiterTimeout;
    }

    public ChunkUtils.ChunkLoadMode getChunkLoadMode()
    {
        return loadChunksForToggle ? ChunkUtils.ChunkLoadMode.ATTEMPT_LOAD : ChunkUtils.ChunkLoadMode.VERIFY_LOADED;
    }

    public int maxPowerBlockDistance()
    {
        return maxPowerBlockDistance;
    }

    public int maxAutoCloseTimer()
    {
        return maxAutoCloseTimer;
    }

    public boolean skipUnloadedAutoCloseToggle()
    {
        return skipUnloadedAutoCloseToggle;
    }

    public boolean allowNotifications()
    {
        return allowNotifications;
    }

    public boolean allowCodeGeneration()
    {
        return allowNotifications;
    }

    public boolean forceCodeGeneration()
    {
        return forceCodeGeneration;
    }

    /**
     * Gets a set of blacklisted materials as defined in the config.
     *
     * @return A set of blacklisted materials as defined in the config.
     */
    public Set<Material> getBlacklist()
    {
        return blacklist;
    }

    /**
     * Gets a set of whitelisted materials as defined in the config.
     *
     * @return A set of whitelisted materials as defined in the config.
     */
    public Set<Material> getWhitelist()
    {
        return whitelist;
    }

    public boolean resourcePackEnabled()
    {
        return resourcePackEnabled;
    }

    /**
     * Checks if the plugin should log to BigDoor's own log file.
     *
     * @return True if the option to log to the log file is enabled.
     */
    public boolean enableFileLogging()
    {
        return enableFileLogging;
    }

    /**
     * Checks if the plugin should abort initialization if it detects an invalid
     * environment on startup.
     *
     * @return False if the plugin should abort initialization in invalid
     *         environments.
     */
    public boolean unsafeMode()
    {
        return unsafeMode;
    }

    /**
     * Gets the range around an engine for playing sound.
     *
     * @return The sound range.
     */
    public int getSoundRange()
    {
        return soundRange;
    }
}
