package nl.pim16aap2.bigdoors.config;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the config loader.
 *
 * @author Pim
 */
public final class ConfigLoader
{
    private static ConfigLoader instance;

    private final BigDoors plugin;
    private final PLogger logger;

    private static final List<String> DEFAULTPOWERBLOCK = new ArrayList<>(Collections.singletonList("GOLD_BLOCK"));
    private final String header;
    private final List<ConfigEntry<?>> configEntries;
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
    private long downloadDelay;
    private boolean enableRedstone;
    private Set<Material> powerBlockTypesMap;
    private boolean worldGuardHook;
    private boolean checkForUpdates;
    private boolean plotSquaredHook;
    private boolean griefPreventionHook;
    private int headCacheTimeout;
    private boolean consoleLogging;
    private boolean debug = false;

    /**
     * Constructs a new {@link ConfigLoader}.
     *
     * @param plugin The Spigot core.
     * @param logger The logger used for error logging.
     */
    private ConfigLoader(final @NotNull BigDoors plugin, final @NotNull PLogger logger)
    {
        this.plugin = plugin;
        this.logger = logger;
        configEntries = new ArrayList<>();
        powerBlockTypesMap = new HashSet<>();
        doorPrices = new EnumMap<>(DoorType.class);
        doorMultipliers = new EnumMap<>(DoorType.class);

        header = "Config file for BigDoors. Don't forget to make a backup before making changes!";
    }

    /**
     * Initializes the {@link ConfigLoader}. If it has already been initialized, it'll return that instance instead.
     *
     * @param plugin The spigot core.
     * @param logger The logger used for error logging.
     * @return The instance of this {@link ConfigLoader}.
     */
    @NotNull
    public static ConfigLoader init(final @NotNull BigDoors plugin, final @NotNull PLogger logger)
    {
        return (instance == null) ? instance = new ConfigLoader(plugin, logger) : instance;
    }

    /**
     * Gets the instance of the {@link ConfigLoader} if it exists.
     *
     * @return The instance of the {@link ConfigLoader}.
     */
    @Nullable
    @Contract(pure = true)
    public static ConfigLoader get()
    {
        return instance;
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

        String[] enableRedstoneComment = {
            "Allow doors to be opened using redstone signals."};
        String[] powerBlockTypeComment = {
            "Choose the type of the power block that is used to open doors using redstone.",
            "A list can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html",
            "This is the block that will open the DoorBase attached to it when it receives a redstone signal.",
            "Multiple types are allowed."};
        String[] maxDoorCountComment = {
            "Maximum number of doors a player can own. -1 = infinite."};
        String[] languageFileComment = {
            "Specify a language file to be used. Note that en_US.txt will get regenerated!"};
        String[] dbFileComment = {
            "Pick the name (and location if you want) of the database."};
        String[] checkForUpdatesComment = {
            "Allow this plugin to check for updates on startup. It will not download new versions!"};
        String[] downloadDelayComment = {
            "Time (in minutes) to delay auto downloading updates after their release.",
            "Setting it to 1440 means that updates will be downloaded 24h after their release.",
            "This is useful, as it will mean that the update won't get downloaded if I decide to pull it for some reason",
            "(within the specified timeframe, of course). Note that updates cannot be deferred for more than 1 week (10080 minutes)."};
        String[] autoDLUpdateComment = {
            "Allow this plugin to automatically download new updates. They will be applied on restart."};
        String[] allowStatsComment = {
            "Allow this plugin to send (anonymized) stats using bStats. Please consider keeping it enabled.",
            "It has a negligible impact on performance and more users on stats keeps me more motivated to support this plugin!"};
        String[] maxDoorSizeComment = {
            "Max. number of blocks allowed in a door.",
            "If this number is exceeded, doors will open instantly and skip the animation.",
            "Note that you can also use permissions for this, if you need more finely grained control using this node: ",
            "\"bigdoors.maxsize.amount\". E.g.: \"bigdoors.maxsize.200\""};
        String[] resourcePackComment = {
            "This plugin uses a support resource pack for things suchs as sound.",
            "You can let this plugin load the resource pack for you or load it using your server.properties if you prefer that.",
            "Of course, you can also disable the resource pack altogether as well. Just put \"NONE\" (without quotation marks) as url.",
            "The default resource pack for 1.11.x/1.12.x is: \'" + defResPackUrl + "'",
            "The default resource pack for 1.13.x is: \'" + defResPackUrl1_13 + "\'"};
        String[] multiplierComment = {
            "These multipliers affect the opening/closing speed of their respective doorBase types.",
            "Note that the maximum speed is limited, so beyond a certain point raising these values won't have any effect.",
            "To use the default values, set them to \"0.0\" or \"1.0\" (without quotation marks).",
            "Note that everything is optimized for default values, so it's recommended to leave this setting as-is."};
        String[] compatibilityHooks = {
            "Enable or disable compatibility hooks for certain plugins. If the plugins aren't installed, these options do nothing.",
            "When enabled, doors cannot be opened or created in areas not owned by the door's owner."};
        String[] coolDownComment = {
            "Cooldown on using doors. Time is measured in seconds."};
        String[] cacheTimeoutComment = {
            "Amount of time (in minutes) to cache powerblock positions in a chunk. -1 means no caching (not recommended!), 0 = infinite cache (not recommended either!).",
            "It doesn't take up too much RAM, so it's recommended to leave this value high. It'll get updated automatically when needed anyway."};
        String[] pricesComment = {
            "When Vault is present, you can set the price of doorBase creation here for every type of door.",
            "You can use the word \"blockCount\" (without quotationmarks, case sensitive) as a variable that will be replaced by the actual blockCount.",
            "Furthermore, you can use these operators: -, +, *, /, sqrt(), ^, %, min(a,b), max(a,b), abs(), and parentheses.",
            "For example: \"price='max(10, sqrt(16)^4/100*blockCount)'\" would return 10 for a blockCount of 0 to 3 and 10.24 for a blockCount of 4.",
            "You must always put the formula or simple value or whatever in quotation marks! Also, these settings do nothing if Vault isn't installed!"};
        String[] headCacheTimeoutComment = {
            "Amount of time (in minutes) to cache player heads. -1 means no caching (not recommended!), 0 = infinite cache (not recommended either!).",
            "Takes up a bit more space than the powerblock caching, but makes GUI much faster."};
        String[] debugComment = {
            "Don't use this. Just leave it on false."};
        String[] consoleLoggingComment = {
            "Write errors and exceptions to console. If disabled, they will only be written to the bigdoors log. ",
            "If enabled, they will be written to both the console and the bigdoors log."};
        String[] backupComment = {
            "Make a backup of the database before upgrading it. I'd recommend leaving this on true. ",
            "In case anything goes wrong, you can just revert to the old version! Only the most recent backup will be kept."};

        FileConfiguration config = plugin.getConfig();

        enableRedstone = addNewConfigEntry(config, "allowRedstone", true, enableRedstoneComment);

        // No need to store the result here. It would be a list of Strings anyway, while we want blocks.
        // Because all entries need to be verified as valid blocks anyway, the list of power block types is
        // populated in the verification method.
        addNewConfigEntry(config, "powerBlockTypes", DEFAULTPOWERBLOCK, powerBlockTypeComment, this::verifyBlocks);
        maxDoorCount = addNewConfigEntry(config, "maxDoorCount", -1, maxDoorCountComment);
        maxDoorSize = addNewConfigEntry(config, "maxDoorSize", 500, maxDoorSizeComment);
        languageFile = addNewConfigEntry(config, "languageFile", "en_US", languageFileComment);
        dbFile = addNewConfigEntry(config, "dbFile", "doorDB.db", dbFileComment);
        checkForUpdates = addNewConfigEntry(config, "checkForUpdates", true, checkForUpdatesComment);
        autoDLUpdate = addNewConfigEntry(config, "auto-update", true, autoDLUpdateComment);
        // Multiply by 60 to get the time in seconds. Also, it's capped to 10080 minutes, better known as 1 week.
        downloadDelay = addNewConfigEntry(config, "downloadDelay", 1440, downloadDelayComment,
                                          (Integer x) -> Math.min(10080, x)) * 60;
        allowStats = addNewConfigEntry(config, "allowStats", true, allowStatsComment);
        worldGuardHook = addNewConfigEntry(config, "worldGuard", false, compatibilityHooks);
        plotSquaredHook = addNewConfigEntry(config, "plotSquared", false, null);
        griefPreventionHook = addNewConfigEntry(config, "griefPrevention", false, null);
        resourcePack = addNewConfigEntry(config, "resourcePack", defResPackUrl1_13, resourcePackComment);
        headCacheTimeout = addNewConfigEntry(config, "headCacheTimeout", 120, headCacheTimeoutComment);
        coolDown = addNewConfigEntry(config, "coolDown", 0, coolDownComment);
        makeBackup = addNewConfigEntry(config, "makeBackup", true, backupComment);
        cacheTimeout = addNewConfigEntry(config, "cacheTimeout", 120, cacheTimeoutComment);

        for (DoorType type : DoorType.cachedValues())
            if (DoorType.isEnabled(type))
                doorMultipliers.put(type, addNewConfigEntry(config, "multiplierOf" + type, 0.0D,
                                                            DoorType.getValue(type) == 0 ? multiplierComment : null));

        for (DoorType type : DoorType.cachedValues())
            if (DoorType.isEnabled(type))
                doorPrices.put(type, addNewConfigEntry(config, "priceOf" + type.name(), "0",
                                                       DoorType.getValue(type) == 0 ? pricesComment : null));

        consoleLogging = addNewConfigEntry(config, "consoleLogging", true, consoleLoggingComment);
        // This is a bit special, as it's public static (for SpigotUtil debug messages).
        debug = addNewConfigEntry(config, "DEBUG", false, debugComment);
        if (debug)
            SpigotUtil.printDebugMessages = true;

        writeConfig();
    }

    /**
     * Verifies that all Strings in a list are valid (solid) materials. All invalid and duplicated entries are removed
     * from the list. If there are exactly 0 valid materials, {@link #DEFAULTPOWERBLOCK} is used instead.
     * <p>
     * All valid materials are added to {@link #powerBlockTypesMap}.
     *
     * @param input The list of Strings of potential materials.
     * @return The list of names of all valid materials in the list without duplication or {@link #DEFAULTPOWERBLOCK} if
     * none were found.
     */
    @NotNull
    @Contract(value = "_ -> param1")
    private List<String> verifyBlocks(final @NotNull List<String> input)
    {
        Iterator<String> it = input.iterator();
        while (it.hasNext())
        {
            String str = it.next();
            try
            {
                Material mat = Material.valueOf(str);
                if (powerBlockTypesMap.contains(mat))
                {
                    logger.warn("Failed to add material: \"" + str + "\". It was already on the list!");
                    it.remove();
                }
                else if (mat.isSolid())
                {
                    powerBlockTypesMap.add(mat);
                }
                else
                {
                    logger.warn("Failed to add material: \"" + str + "\". Only solid materials are allowed!");
                    it.remove();
                }
            }
            catch (Exception e)
            {
                logger.warn("Failed to parse material: \"" + str + "\"");
                it.remove();
            }
        }

        logger.info("Power Block Types:");
        powerBlockTypesMap.forEach(K -> logger.info(" - " + K.toString()));
        return input;
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
    @NotNull
    private <T> T addNewConfigEntry(final @NotNull FileConfiguration config, final @NotNull String optionName,
                                    final @NotNull T defaultValue, final @Nullable String[] comment)
    {
        ConfigEntry<T> option = new ConfigEntry<>(plugin.getPLogger(), config, optionName, defaultValue, comment);
        configEntries.add(option);
        return option.getValue();
    }

    /**
     * Read a new config option from the config if it exists. Otherwise, use the default value.
     *
     * @param <T>          The type of the option.
     * @param config       The config.
     * @param optionName   The name of the option in the config file.
     * @param defaultValue The default value of the option. To be used if no/invalid option is in the config already.
     * @param comment      The comment to accompany the option in the config.
     * @param verifyValue  Function to use to verify the validity of a value and change it if necessary.
     * @return The value as read from the config file if it exists or the default value.
     */
    @NotNull
    private <T> T addNewConfigEntry(final @NotNull FileConfiguration config, final @NotNull String optionName,
                                    final @NotNull T defaultValue, final @NotNull String[] comment,
                                    final @NotNull ConfigEntry.TestValue<T> verifyValue)
    {
        ConfigEntry<T> option = new ConfigEntry<>(plugin.getPLogger(), config, optionName, defaultValue, comment,
                                                  verifyValue);
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
                    logger.logException(new IOException("Failed to create folder: \"" + dataFolder.toString() + "\""));
                    return;
                }

            File saveTo = new File(plugin.getDataFolder(), "config.yml");
            if (!saveTo.exists())
                if (!saveTo.createNewFile())
                {
                    logger.logException(new IOException("Failed to create file: \"" + saveTo.toString() + "\""));
                    return;
                }

            if (!saveTo.canWrite())
            {
                logger.warn("=======================================");
                logger.warn("============== !WARNING! ==============");
                logger.warn("=======================================");
                logger.warn("====== CANNOT WRITE CONFIG FILE! ======");
                logger.warn("==== NEW OPTIONS WILL NOT SHOW UP! ====");
                logger.warn("==== THEY WILL USE DEFAULT VALUES! ====");
                logger.warn("=======================================");
                logger.warn("============== !WARNING! ==============");
                logger.warn("=======================================");
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
            logger.logException(e, "Could not save config.yml! "
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

    /**
     * Gets the amount time (in seconds) to wait before downloading an update. If set to 24 hours (86400 seconds), and
     * an update was released on Monday June 1 at 12PM, it will not download this update before Tuesday June 2 at 12PM.
     * When running a dev-build, however, this value is overridden to 0.
     *
     * @return The amount time (in seconds) to wait before downloading an update.
     */
    public long downloadDelay()
    {
        if (BigDoors.DEVBUILD)
            return 0L;
        return downloadDelay;
    }

    public boolean enableRedstone()
    {
        return enableRedstone;
    }

    public Set<Material> powerBlockTypes()
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
