package nl.pim16aap2.bigdoors.spigot.config;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IConfigReader;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.compatiblity.ProtectionCompat;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.spigot.util.implementations.ConfigReaderSpigot;
import nl.pim16aap2.bigdoors.util.ConfigEntry;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.Limit;
import org.bukkit.Material;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Represents the config loader.
 *
 * @author Pim
 */
@ToString
public final class ConfigLoaderSpigot implements IConfigLoader
{
    private static @Nullable ConfigLoaderSpigot INSTANCE;
    private final @NonNull BigDoorsSpigot plugin;
    @ToString.Exclude
    private final @NonNull IPLogger logger;

    private static final List<String> DEFAULTPOWERBLOCK = Collections
        .unmodifiableList(new ArrayList<>(Collections.singletonList("GOLD_BLOCK")));
    private static final List<String> DEFAULTBLACKLIST = Collections.emptyList();

    private final Set<Material> powerBlockTypes;
    private final @NonNull Set<Material> materialBlacklist;
    private final @NonNull Map<ProtectionCompat, Boolean> hooksMap;
    @ToString.Exclude
    private final @NonNull List<ConfigEntry<?>> configEntries;
    private final @NonNull Map<DoorType, String> doorPrices;
    private final @NonNull Map<DoorType, Double> doorMultipliers;

    private final @NonNull String header;
    private int coolDown;
    private boolean allowStats;
    private OptionalInt maxDoorSize;
    private OptionalInt maxPowerBlockDistance;
    private String resourcePack;
    private String languageFile;
    private OptionalInt maxDoorCount;
    private OptionalInt maxBlocksToMove;
    private int cacheTimeout;
    private boolean autoDLUpdate;
    private long downloadDelay;
    private boolean enableRedstone;
    private boolean checkForUpdates;
    private int headCacheTimeout;
    private boolean consoleLogging;
    private boolean debug = false;
    private String flagFormula;

    /**
     * Constructs a new {@link ConfigLoaderSpigot}.
     *
     * @param plugin The Spigot core.
     * @param logger The logger used for error logging.
     */
    private ConfigLoaderSpigot(final @NonNull BigDoorsSpigot plugin, final @NonNull IPLogger logger)
    {
        this.plugin = plugin;
        this.logger = logger;
        configEntries = new ArrayList<>();
        powerBlockTypes = EnumSet.noneOf(Material.class);
        materialBlacklist = EnumSet.noneOf(Material.class);
        hooksMap = new EnumMap<>(ProtectionCompat.class);
        doorPrices = new HashMap<>();
        doorMultipliers = new HashMap<>();

        header = "Config file for BigDoors. Don't forget to make a backup before making changes!";
    }

    /**
     * Initializes the {@link ConfigLoaderSpigot}. If it has already been initialized, it'll return that instance
     * instead.
     *
     * @param plugin The spigot core.
     * @param logger The logger used for error logging.
     * @return The instance of this {@link ConfigLoaderSpigot}.
     */
    public static @NonNull ConfigLoaderSpigot init(final @NonNull BigDoorsSpigot plugin, final @NonNull IPLogger logger)
    {
        return (INSTANCE == null) ? INSTANCE = new ConfigLoaderSpigot(plugin, logger) : INSTANCE;
    }

    /**
     * Gets the instance of the {@link ConfigLoaderSpigot} if it exists.
     *
     * @return The instance of the {@link ConfigLoaderSpigot}.
     */
    public static @NonNull ConfigLoaderSpigot get()
    {
        Preconditions.checkState(INSTANCE != null,
                                 "Instance has not yet been initialized. Be sure #init() has been invoked");
        return INSTANCE;
    }

    @Override
    public void restart()
    {
        reloadConfig();
    }

    @Override
    public void shutdown()
    {
        reloadConfig();
    }

    /**
     * Reload the config file and read in the new values.
     */
    public void reloadConfig()
    {
        plugin.reloadConfig();
        configEntries.clear();
        powerBlockTypes.clear();
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
        String[] blacklistComment = {
            "List of blacklisted materials. Materials on this list can not be animated.",
            "Use the same list of materials as for the power blocks. For example, you would blacklist bedrock like so:",
            "  - BEDROCK"};
        String[] maxDoorCountComment = {
            "Global maximum number of doors a player can own. You can set it to -1 to disable it this limit.",
            "Not even admins and OPs can bypass this limit!",
            "Note that you can also use permissions for this, if you need more finely grained control using this node: ",
            "'" + Limit.DOOR_COUNT.getUserPermission() + "x', where 'x' can be any positive value."};
        String[] maxBlocksToMoveComment = {
            "Global maximum number of doors a player can own. You can set it to -1 to disable it this limit.",
            "Not even admins and OPs can bypass this limit!",
            "Note that you can also use permissions for this, if you need more finely grained control using this node: ",
            "'" + Limit.BLOCKS_TO_MOVE.getUserPermission() + "x', where 'x' can be any positive value."};
        String[] languageFileComment = {
            "Specify a language file to be used. Note that en_US.txt will get regenerated!"};
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
            "Global maximum number of blocks allowed in a door. You can set it to -1 to disable it this limit.",
            "If this number is exceeded, doors will open instantly and skip the animation.",
            "Not even admins and OPs can bypass this limit!",
            "Note that you can also use permissions for this, if you need more finely grained control using this node: ",
            "'" + Limit.DOOR_SIZE.getUserPermission() + "x', where 'x' can be any positive value."};
        String[] maxPowerBlockDistanceComment = {
            "Global maximum distance between a door and its powerblock. You can set it to -1 to disable it this limit.",
            "The distance is measured from the center of the door.",
            "Not even admins and OPs can bypass this limit!",
            "Note that you can also use permissions for this, if you need more finely grained control using this node: ",
            "'" + Limit.POWERBLOCK_DISTANCE.getUserPermission() + "x', where 'x' can be any positive value."};
        String[] resourcePackComment = {
            "This plugin uses a support resource pack for things suchs as sound.",
            "You can let this plugin load the resource pack for you or load it using your server.properties if you prefer that.",
            "Of course, you can also disable the resource pack altogether as well. Just put \"NONE\" (without quotation marks) as url.",
            "The default resource pack for 1.11.x/1.12.x is: '" + defResPackUrl + "'",
            "The default resource pack for 1.13.x is: '" + defResPackUrl1_13 + "'"};
        String[] multiplierComment = {
            "These multipliers affect the opening/closing speed of their respective doorBase types.",
            "Note that the maximum speed is limited, so beyond a certain point raising these values won't have any effect.",
            "To use the default values, set them to \"0.0\" or \"1.0\" (without quotation marks).",
            "Note that everything is optimized for default values, so it's recommended to leave this setting as-is."};
        String[] compatibilityHooksComment = {
            "Enable or disable compatibility hooks for certain plugins. If the plugins aren't installed, these options do nothing.",
            "When enabled, doors cannot be opened or created in areas not owned by the door's owner."};
        String[] coolDownComment = {
            "Cooldown on using doors. Time is measured in seconds."};
        String[] cacheTimeoutComment = {
            "Amount of time (in minutes) to cache power block positions in a chunk. -1 means no caching (not recommended!), 0 = infinite cache (not recommended either!).",
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


        IConfigReader config = new ConfigReaderSpigot(plugin.getConfig());


        enableRedstone = addNewConfigEntry(config, "allowRedstone", true, enableRedstoneComment);

        // No need to store the result here. It would be a list of Strings anyway, while we want blocks.
        // Because all entries need to be verified as valid blocks anyway, the list of power block types is
        // populated in the verification method.
        addNewConfigEntry(config, "powerBlockTypes", DEFAULTPOWERBLOCK, powerBlockTypeComment,
                          new MaterialVerifier(powerBlockTypes));
        addNewConfigEntry(config, "materialBlacklist", DEFAULTBLACKLIST, blacklistComment,
                          new MaterialVerifier(materialBlacklist));

        int maxDoorCount = addNewConfigEntry(config, "maxDoorCount", -1, maxDoorCountComment);
        this.maxDoorCount = maxDoorCount > 0 ? OptionalInt.of(maxDoorCount) : OptionalInt.empty();

        int maxBlocksToMove = addNewConfigEntry(config, "maxBlocksToMove", 100, maxBlocksToMoveComment);
        this.maxBlocksToMove = maxBlocksToMove > 0 ? OptionalInt.of(maxBlocksToMove) : OptionalInt.empty();

        int maxDoorSize = addNewConfigEntry(config, "maxDoorSize", 500, maxDoorSizeComment);
        this.maxDoorSize = maxDoorSize > 0 ? OptionalInt.of(maxDoorSize) : OptionalInt.empty();

        int maxPowerBlockDistance = addNewConfigEntry(config, "maxPowerBlockDistance", -1,
                                                      maxPowerBlockDistanceComment);
        this.maxPowerBlockDistance = maxPowerBlockDistance > 0 ?
                                     OptionalInt.of(maxPowerBlockDistance) : OptionalInt.empty();

        languageFile = addNewConfigEntry(config, "languageFile", "en_US", languageFileComment);
        checkForUpdates = addNewConfigEntry(config, "checkForUpdates", true, checkForUpdatesComment);
        autoDLUpdate = addNewConfigEntry(config, "auto-update", true, autoDLUpdateComment);
        // Multiply by 60 to get the time in seconds. Also, it's capped to 10080 minutes, better known as 1 week.
        downloadDelay = addNewConfigEntry(config, "downloadDelay", 1440, downloadDelayComment,
                                          (Integer x) -> Math.min(10080, x)) * 60L;
        allowStats = addNewConfigEntry(config, "allowStats", true, allowStatsComment);

        int idx = 0;
        for (ProtectionCompat compat : ProtectionCompat.values())
        {
            final String name = ProtectionCompat.getName(compat).toLowerCase();
            final boolean isEnabled = (boolean) config.get(name, false);
            addNewConfigEntry(config, ProtectionCompat.getName(compat), false,
                              ((idx++ == 0) ? compatibilityHooksComment : null));
            hooksMap.put(compat, isEnabled);
        }

        resourcePack = addNewConfigEntry(config, "resourcePack", defResPackUrl1_13, resourcePackComment);
        headCacheTimeout = addNewConfigEntry(config, "headCacheTimeout", 120, headCacheTimeoutComment);
        coolDown = addNewConfigEntry(config, "coolDown", 0, coolDownComment);
        cacheTimeout = addNewConfigEntry(config, "cacheTimeout", 120, cacheTimeoutComment);


        flagFormula = addNewConfigEntry(config, "flagFormula",
                                        "Math.min(0.3 * radius, 3) * Math.sin((counter / 4) * 3)", null);


        String[] usedMulitplierComment = multiplierComment;
        String[] usedPricesComment = pricesComment;
        for (final @NonNull DoorType type : BigDoors.get().getDoorTypeManager().getEnabledDoorTypes())
        {
            doorMultipliers
                .put(type, addNewConfigEntry(config, "multiplier_" + type.toString(), 0.0D, usedMulitplierComment));
            doorPrices.put(type, addNewConfigEntry(config, "price_" + type.toString(), "0", usedPricesComment));

            usedMulitplierComment = null;
            usedPricesComment = null;
        }

        consoleLogging = addNewConfigEntry(config, "consoleLogging", true, consoleLoggingComment);
        // This is a bit special, as it's public static (for SpigotUtil debug messages).
        debug = addNewConfigEntry(config, "DEBUG", false, debugComment);
        if (debug)
            SpigotUtil.setPrintDebugMessages(true);

        writeConfig();
        printInfo();
    }

    /**
     * Logs some basic info read from the config, such as the list of power block materials.
     */
    private void printInfo()
    {
        logger.info("Power Block Types:");
        powerBlockTypes.forEach(mat -> logger.info(" - " + mat.toString()));

        if (materialBlacklist.isEmpty())
            logger.info("No materials are blacklisted!");
        else
        {
            logger.info("Blacklisted materials:");
            materialBlacklist.forEach(mat -> logger.info(" - " + mat.toString()));
        }
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
    private @NonNull <T> T addNewConfigEntry(final @NonNull IConfigReader config, final @NonNull String optionName,
                                             final @NonNull T defaultValue, final @Nullable String[] comment)
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
    private @NonNull <T> T addNewConfigEntry(final @NonNull IConfigReader config, final @NonNull String optionName,
                                             final @NonNull T defaultValue, final @NonNull String[] comment,
                                             final @NonNull ConfigEntry.TestValue<T> verifyValue)
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
            if (!dataFolder.exists() && !dataFolder.mkdirs())
            {
                logger.logThrowable(new IOException("Failed to create folder: \"" + dataFolder.toString() + "\""));
                return;
            }

            File saveTo = new File(plugin.getDataFolder(), "config.yml");
            if (!saveTo.exists() && !saveTo.createNewFile())
            {
                logger.logThrowable(new IOException("Failed to create file: \"" + saveTo.toString() + "\""));
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

            try (FileWriter fw = new FileWriter(saveTo, false);
                 PrintWriter pw = new PrintWriter(fw))
            {
                if (header != null)
                    pw.println("# " + header + "\n");

                for (int idx = 0; idx < configEntries.size(); ++idx)
                    pw.println(configEntries.get(idx).toString() +
                                   // Only print an additional newLine if the next config option has a comment.
                                   (idx < configEntries.size() - 1 && configEntries.get(idx + 1).getComment() == null ?
                                    "" : "\n"));
            }
            catch (IOException e)
            {
                logger.logThrowable(e, "Could not write to config.yml! "
                    + "Please contact pim16aap2 and show him the following stacktrace:");
            }


        }
        catch (IOException e)
        {
            logger.logThrowable(e, "Could not save config.yml! "
                + "Please contact pim16aap2 and show him the following stacktrace:");
        }
    }

    @Override
    public boolean debug()
    {
        return debug;
    }

    @Override
    public @NonNull String flagFormula()
    {
        return flagFormula;
    }

    @Override
    public int coolDown()
    {
        return coolDown;
    }

    @Override
    public boolean allowStats()
    {
        return allowStats;
    }

    @Override
    public @NonNull OptionalInt maxDoorSize()
    {
        return maxDoorSize;
    }

    @Override
    public @NonNull OptionalInt maxPowerBlockDistance()
    {
        return maxPowerBlockDistance;
    }

    @Override
    public int cacheTimeout()
    {
        return cacheTimeout;
    }

    public @NonNull String resourcePack()
    {
        return resourcePack;
    }

    @Override
    public @NonNull String languageFile()
    {
        return languageFile;
    }

    @Override
    public @NonNull OptionalInt maxDoorCount()
    {
        return maxDoorCount;
    }

    @Override
    public @NonNull OptionalInt maxBlocksToMove()
    {
        return maxBlocksToMove;
    }

    @Override
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
    @Override
    public long downloadDelay()
    {
        if (Constants.DEV_BUILD)
            return 0L;
        return downloadDelay;
    }

    @Override
    public boolean enableRedstone()
    {
        return enableRedstone;
    }

    public @NonNull Set<Material> powerBlockTypes()
    {
        return powerBlockTypes;
    }

    /**
     * Checks if a {@link ProtectionCompat} is enabled or not.
     *
     * @param hook The {@link ProtectionCompat}.
     * @return True if this {@link ProtectionCompat} is enabled.
     */
    public boolean isHookEnabled(final @NonNull ProtectionCompat hook)
    {
        return hooksMap.get(hook);
    }

    /**
     * Gets the amount of time to keeps heads cached.
     *
     * @return The amount of time (in minutes) to cache head items.
     */
    public int headCacheTimeout()
    {
        return headCacheTimeout;
    }

    @Override
    public boolean checkForUpdates()
    {
        return checkForUpdates;
    }

    @Override
    public @NonNull String getPrice(final @NonNull DoorType type)
    {
        return doorPrices.get(type);
    }

    @Override
    public double getMultiplier(final @NonNull DoorType type)
    {
        return doorMultipliers.getOrDefault(type, 0.0D);
    }

    @Override
    public boolean consoleLogging()
    {
        return consoleLogging;
    }

    /**
     * Represents a class that attempts to parse a list of materials represented as Strings into a list of Materials.
     * <p>
     * See {@link #verifyMaterials(List, Set)}.
     *
     * @author Pim
     */
    private static class MaterialVerifier implements ConfigEntry.TestValue<List<String>>
    {
        private final @NonNull Set<Material> output;

        /**
         * Constructs a new MaterialVerifier.
         * <p>
         * Note that the output set is cleared!
         *
         * @param output The set to write the parsed materials to.
         */
        private MaterialVerifier(final @NonNull Set<Material> output)
        {
            this.output = output;
            output.clear();
        }

        @Override
        public @NonNull List<String> test(@NonNull List<String> input)
        {
            return MaterialVerifier.verifyMaterials(input, output);
        }

        /**
         * Verifies that all Strings in a list are valid (solid) materials. All invalid and duplicated entries are
         * removed from the list.
         * <p>
         * All valid materials are added to the output set.
         *
         * @param input  The list of Strings of potential materials.
         * @param output The set to put all valid materials in.
         * @return The list of names of all valid materials in the list without duplication.
         */
        @Contract(value = "_ -> param1")
        private static @NonNull List<String> verifyMaterials(final @NonNull List<String> input,
                                                             final @NonNull Set<Material> output)
        {
            output.clear();
            Iterator<String> it = input.iterator();
            while (it.hasNext())
            {
                String str = it.next();
                try
                {
                    Material mat = Material.valueOf(str);
                    if (output.contains(mat))
                    {
                        BigDoors.get().getPLogger()
                                .warn("Failed to add material: \"" + str + "\". It was already on the list!");
                        it.remove();
                    }
                    else if (mat.isSolid())
                    {
                        output.add(mat);
                    }
                    else
                    {
                        BigDoors.get().getPLogger()
                                .warn("Failed to add material: \"" + str + "\". Only solid materials are allowed!");
                        it.remove();
                    }
                }
                catch (Exception e)
                {
                    BigDoors.get().getPLogger().warn("Failed to parse material: \"" + str + "\"");
                    it.remove();
                }
            }

            return input;
        }
    }
}
