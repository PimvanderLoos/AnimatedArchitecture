package nl.pim16aap2.bigdoors.config;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Represents the config loader.
 *
 * @author Pim
 */
public class ConfigLoader
{
    private static final List<String> DEFAULTPOWERBLOCK = new ArrayList<>(Arrays.asList("GOLD_BLOCK"));
    private final String header;
    private final List<ConfigEntry<?>> configEntries;
    private final BigDoors plugin;
    private final Map<DoorType, String> doorPrices;
    private final Map<DoorType, Double> doorMultipliers;
    private String dbFile;
    private int coolDown;
    private boolean makeBackup;
    private boolean allowStats;
    private int maxDoorSize;
    private String resourcePack;
    private String languageFile;
    private int maxDoorCount;
    private int cacheTimeout;
    private boolean autoDLUpdate;
    private int downloadDelay;
    private boolean enableRedstone;
    private HashSet<Material> powerBlockTypesMap;
    private boolean worldGuardHook;
    private boolean checkForUpdates;
    private boolean plotSquaredHook;
    private boolean griefPreventionHook;
    private int headCacheTimeout;
    private boolean consoleLogging;
    private boolean debug = false;

    public ConfigLoader(final BigDoors plugin)
    {
        this.plugin = plugin;
        configEntries = new ArrayList<>();
        powerBlockTypesMap = new HashSet<>();
        doorPrices = new HashMap<>();
        doorMultipliers = new HashMap<>();
        header = "Config file for BigDoors. Don't forget to make a backup before making changes!";
        makeConfig();
    }

    /**
     * Reload the config file and read in the new values.
     */
    public void reloadConfig()
    {
        plugin.reloadConfig();
        configEntries.clear();
        powerBlockTypesMap.clear();
        doorPrices.clear();
        doorMultipliers.clear();
        makeConfig();
    }

    /**
     * Read the current config file and rewrite the file.
     */
    private void makeConfig()
    {
        String defResPackUrl = "https://www.dropbox.com/s/0q6h8jkfjqrn1tp/BigDoorsResourcePack.zip?dl=1";
        String defResPackUrl1_13 = "https://www.dropbox.com/s/al4idl017ggpnuq/BigDoorsResourcePack-1_13.zip?dl=1";

        String[] enableRedstoneComment = {"Allow doors to be opened using redstone signals."};
        String[] powerBlockTypeComment = {
            "Choose the type of the power block that is used to open doors using redstone.",
            "A list can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html",
            "This is the block that will open the DoorBase attached to it when it receives a redstone signal.",
            "Multiple types are allowed."};
        String[] maxDoorCountComment = {"Maximum number of doors a player can own. -1 = infinite."};
        String[] languageFileComment = {
            "Specify a language file to be used. Note that en_US.txt will get regenerated!"};
        String[] dbFileComment = {"Pick the name (and location if you want) of the database."};
        String[] checkForUpdatesComment = {
            "Allow this plugin to check for updates on startup. It will not download new versions!"};
        String[] downloadDelayComment = {"Time (in minutes) to delay auto downloading updates after their release.",
                                         "Setting it to 1440 means that updates will be downloaded 24h after their release.",
                                         "This is useful, as it will mean that the update won't get downloaded if I decide to pull it for some reason",
                                         "(within the specified timeframe, of course)."};
        String[] autoDLUpdateComment = {
            "Allow this plugin to automatically download new updates. They will be applied on restart."};
        String[] allowStatsComment = {
            "Allow this plugin to send (anonymised) stats using bStats. Please consider keeping it enabled.",
            "It has a negligible impact on performance and more users on stats keeps me more motivated to support this plugin!"};
        String[] maxDoorSizeComment = {"Max. number of blocks allowed in a door.",
                                       "If this number is exceeded, doors will open instantly and skip the animation.",
                                       "Note that you can also use permissions for this, if you need more finely grained control using this node: ",
                                       "\"bigdoors.maxsize.amount\". E.g.: \"bigdoors.maxsize.200\""};
        String[] resourcePackComment = {"This plugin uses a support resource pack for things suchs as sound.",
                                        "You can let this plugin load the resource pack for you or load it using your server.properties if you prefer that.",
                                        "Of course, you can also disable the resource pack altogether as well. Just put \"NONE\" (without quotation marks) as url.",
                                        "The default resource pack for 1.11.x/1.12.x is: \'" + defResPackUrl + "'",
                                        "The default resource pack for 1.13.x is: \'" + defResPackUrl1_13 + "\'"};
        String[] multiplierComment = {
            "These multipliers affect the opening/closing speed of their respective doorBase types.",
            "Note that the maximum speed is limited, so beyond a certain point rasising these values won't have any effect.",
            "To use the default values, set them to \"0.0\" or \"1.0\" (without quotation marks).",
            "bd = Big Door, pc = PortCullis, db = DrawBridge, sd = Sliding Door, fl = FLag, el = ELevator",
            "Note that everything is optimized for default values, so it's recommended to leave this setting as-is."};
        String[] compatibilityHooks = {
            "Enable or disable compatibility hooks for certain plugins. If the plugins aren't installed, these options do nothing.",
            "When enabled, doors cannot be opened or created in areas not owned by the door's owner."};
        String[] coolDownComment = {"Cooldown on using doors. Time is measured in seconds."};
        String[] cacheTimeoutComment = {
            "Amount of time (in minutes) to cache powerblock positions. -1 means no caching (not recommended!), 0 = infinite cache.",
            "Doesn't take up a lot of RAM, so it's recommended to leave this value high. It'll get updated automatically when needed anyway."};
        String[] pricesComment = {
            "When Vault is present, you can set the price of doorBase creation here for every type of door.",
            "You can use the word \"blockCount\" (without quotationmarks, case sensitive) as a variable that will be replaced by the actual blockCount.",
            "Furthermore, you can use these operators: -, +, *, /, sqrt(), ^, %, min(a,b), max(a,b), abs(), and parentheses.",
            "For example: \"doorPrice='max(10, sqrt(16)^4/100*blockCount)'\" would return 10 for a blockCount of 0 to 3 and 10.24 for a blockCount of 4.",
            "You must always put the formula or simple value or whatever in quotation marks! Also, these settings do nothing if Vault isn't installed!"};
        String[] headCacheTimeoutComment = {
            "Amount of time (in minutes) to cache player heads. -1 means no caching (not recommended!), 0 = infinite cache.",
            "Takes up a bit more space than the powerblock caching, but makes GUI much faster."};
        String[] debugComment = {"Don't use this. Just leave it on false."};
        String[] consoleLoggingComment = {
            "Write errors and exceptions to console. If disabled, they will only be written to the bigdoors log. ",
            "If enabled, they will be written to both the console and the bigdoors log."};
        String[] backupComment = {
            "Make a backup of the database before upgrading it. I'd recommend leaving this on true. ",
            "In case anything goes wrong, you can just revert to the old version! Only the most recent backup will be kept."};

        FileConfiguration config = plugin.getConfig();

        enableRedstone = addNewConfigEntry(config, "allowRedstone", true, enableRedstoneComment);
        readPowerBlockConfig(config, powerBlockTypeComment);
        maxDoorCount = addNewConfigEntry(config, "maxDoorCount", -1, maxDoorCountComment);
        maxDoorSize = addNewConfigEntry(config, "maxDoorSize", 500, maxDoorSizeComment);
        languageFile = addNewConfigEntry(config, "languageFile", "en_US", languageFileComment);
        dbFile = addNewConfigEntry(config, "dbFile", "doorDB.db", dbFileComment);
        checkForUpdates = addNewConfigEntry(config, "checkForUpdates", true, checkForUpdatesComment);
        autoDLUpdate = addNewConfigEntry(config, "auto-update", true, autoDLUpdateComment);
        downloadDelay = addNewConfigEntry(config, "downloadDelay", 1440, downloadDelayComment);
        allowStats = addNewConfigEntry(config, "allowStats", true, allowStatsComment);
        worldGuardHook = addNewConfigEntry(config, "worldGuard", true, compatibilityHooks);
        plotSquaredHook = addNewConfigEntry(config, "plotSquared", true, null);
        griefPreventionHook = addNewConfigEntry(config, "griefPrevention", true, null);
        resourcePack = addNewConfigEntry(config, "resourcePack", defResPackUrl1_13, resourcePackComment);
        headCacheTimeout = addNewConfigEntry(config, "headCacheTimeout", 120, headCacheTimeoutComment);
        coolDown = addNewConfigEntry(config, "coolDown", 0, coolDownComment);
        makeBackup = addNewConfigEntry(config, "makeBackup", true, backupComment);
        cacheTimeout = addNewConfigEntry(config, "cacheTimeout", 0, cacheTimeoutComment);

        DoorType[] doorTypes = DoorType.values();
        for (int idx = 0; idx != doorTypes.length; ++idx)
            if (DoorType.isEnabled(doorTypes[idx]))
                doorMultipliers.put(doorTypes[idx],
                                    addNewConfigEntry(config, "multiplierOf" + doorTypes[idx].name(),
                                                      0.0D, idx == 0 ? multiplierComment : null));

        for (int idx = 0; idx != doorTypes.length; ++idx)
            if (DoorType.isEnabled(doorTypes[idx]))
                doorPrices.put(doorTypes[idx],
                               addNewConfigEntry(config, "priceOf" + doorTypes[idx].name(), "0",
                                                 idx == 0 ? pricesComment : null));

        consoleLogging = addNewConfigEntry(config, "consoleLogging", true, consoleLoggingComment);
        // This is a bit special, as it's public static (for SpigotUtil debug messages).
        debug = addNewConfigEntry(config, "DEBUG", false, debugComment);
        if (debug)
            SpigotUtil.printDebugMessages = true;

        writeConfig();
    }

    /**
     * Read the allowed powerBlockTypes from the config. Only valid materials are allowed on this list. Invalid
     * materials are removed from the list and from the config.
     *
     * @param config                The config
     * @param powerBlockTypeComment The comment of the powerBlockType option.
     */
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
                {
                    plugin.getPLogger()
                          .warn("Failed to add material: \"" + str + "\". Only solid materials are allowed!");
                    it.remove();
                }
            }
            catch (Exception e)
            {
                plugin.getPLogger().warn("Failed to parse material: \"" + str + "\"");
                it.remove();
            }
        }

        // If the user didn't supply even a single valid block, use the default.
        if (powerBlockTypesMap.size() == 0)
        {
            StringBuilder sb = new StringBuilder();
            DEFAULTPOWERBLOCK.forEach(K -> sb.append(K + " "));
            plugin.getPLogger().warn("No materials found for powerBlockType! Defaulting to:" + sb.toString());
            DEFAULTPOWERBLOCK.forEach(K ->
                                      {
                                          powerBlockTypesMap.add(Material.valueOf(K));
                                          materials.add(K);
                                      });
        }

        addNewConfigEntry(config, "powerBlockTypes", materials, powerBlockTypeComment);
        plugin.getPLogger().info("Power Block Types:");
        powerBlockTypesMap.forEach(K -> plugin.getPLogger().info(" - " + K.toString()));
    }

    /**
     * Read a new config option from the config if it exists. Otherwise, use the default value.
     *
     * @param <T>          The type of the option.
     * @param config       The config.
     * @param optionName   The name of the option in the config file.
     * @param defaultValue The default value of the option. To be used if no/invalid option is in the config already.
     * @param comment      The comment to accompany the option in the config.
     * @return The value as read from the config file if it exists or the default value.
     */
    private <T> T addNewConfigEntry(FileConfiguration config, String optionName, T defaultValue, String[] comment)
    {
        ConfigEntry<T> option = new ConfigEntry<>(plugin, config, optionName, defaultValue, comment);
        configEntries.add(option);
        return option.getValue();
    }

    /**
     * Write the config file.
     */
    private void writeConfig()
    {
        // Write all the config options to the config.yml.
        try
        {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists())
                if (!dataFolder.mkdirs())
                {
                    plugin.getPLogger()
                          .logException(new IOException("Failed to create folder: \"" + dataFolder.toString() + "\""));
                    return;
                }

            File saveTo = new File(plugin.getDataFolder(), "config.yml");
            if (!saveTo.exists())
                if (!saveTo.createNewFile())
                {
                    plugin.getPLogger()
                          .logException(new IOException("Failed to create file: \"" + saveTo.toString() + "\""));
                    return;
                }

            if (!saveTo.canWrite())
            {
                plugin.getPLogger().warn("=======================================");
                plugin.getPLogger().warn("============== !WARNING! ==============");
                plugin.getPLogger().warn("=======================================");
                plugin.getPLogger().warn("====== CANNOT WRITE CONFIG FILE! ======");
                plugin.getPLogger().warn("==== NEW OPTIONS WILL NOT SHOW UP! ====");
                plugin.getPLogger().warn("==== THEY WILL USE DEFAULT VALUES! ====");
                plugin.getPLogger().warn("=======================================");
                plugin.getPLogger().warn("============== !WARNING! ==============");
                plugin.getPLogger().warn("=======================================");
            }

            FileWriter fw = new FileWriter(saveTo, false);
            PrintWriter pw = new PrintWriter(fw);

            if (header != null)
                pw.println("# " + header + "\n");

            for (int idx = 0; idx < configEntries.size(); ++idx)
                pw.println(configEntries.get(idx).toString() +
                               // Only print an additional newLine if the next config option has a comment.
                               (idx < configEntries.size() - 1 && configEntries.get(idx + 1).getComment() == null ?
                                "" : "\n"));

            pw.flush();
            pw.close();
        }
        catch (IOException e)
        {
            plugin.getPLogger().logException(e, "Could not save config.yml! "
                + "Please contact pim16aap2 and show him the following code:");
        }
    }

    public boolean debug()
    {
        return debug;
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

    public boolean autoDLUpdate()
    {
        return autoDLUpdate;
    }

    public int downloadDelay()
    {
        return downloadDelay;
    }

    public boolean enableRedstone()
    {
        return enableRedstone;
    }

    public HashSet<Material> powerBlockTypes()
    {
        return powerBlockTypesMap;
    }

    public boolean worldGuardHook()
    {
        return worldGuardHook;
    }

    public boolean griefPreventionHook()
    {
        return griefPreventionHook;
    }

    public boolean checkForUpdates()
    {
        return checkForUpdates;
    }

    public boolean plotSquaredHook()
    {
        return plotSquaredHook;
    }

    public int headCacheTimeout()
    {
        return headCacheTimeout;
    }

    public String getPrice(DoorType type)
    {
        return doorPrices.get(type);
    }

    public double getMultiplier(DoorType type)
    {
        return doorMultipliers.get(type);
    }

    public boolean consoleLogging()
    {
        return consoleLogging;
    }
}
