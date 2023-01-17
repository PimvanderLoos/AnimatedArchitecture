package nl.pim16aap2.bigdoors.spigot.config;

import com.google.common.flogger.StackSize;
import dagger.Lazy;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IConfigReader;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.debugging.IDebuggable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.localization.LocalizationUtil;
import nl.pim16aap2.bigdoors.managers.MovableTypeManager;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.spigot.util.implementations.ConfigReaderSpigot;
import nl.pim16aap2.bigdoors.util.ConfigEntry;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.Limit;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Level;

/**
 * Represents the config loader.
 *
 * @author Pim
 */
@ToString
@Singleton
@Flogger
public final class ConfigLoaderSpigot implements IConfigLoader, IDebuggable
{
    @ToString.Exclude
    private final JavaPlugin plugin;
    @ToString.Exclude
    private final Lazy<MovableTypeManager> movableTypeManager;
    private final Path baseDir;

    private static final List<String> DEFAULT_POWERBLOCK_TYPE = List.of("GOLD_BLOCK");
    private static final List<String> DEFAULT_BLACKLIST = Collections.emptyList();

    private final Set<Material> powerBlockTypes = EnumSet.noneOf(Material.class);
    private final Set<Material> materialBlacklist = EnumSet.noneOf(Material.class);
    @ToString.Exclude
    private final List<ConfigEntry<?>> configEntries = new ArrayList<>();
    private final Map<MovableType, String> movablePrices;
    private final Map<MovableType, Double> movableSpeedMultipliers;
    @ToString.Exclude
    private final String header;

    private int coolDown;
    private boolean allowStats;
    private OptionalInt maxMovableSize = OptionalInt.empty();
    private OptionalInt maxPowerBlockDistance = OptionalInt.empty();
    private String resourcePack = "";
    private OptionalInt maxMovableCount = OptionalInt.empty();
    private OptionalInt maxBlocksToMove = OptionalInt.empty();
    private double maxBlockSpeed;
    private int cacheTimeout;
    private boolean autoDLUpdate;
    private long downloadDelay;
    private boolean enableRedstone;
    private boolean loadChunksForToggle;
    private boolean checkForUpdates;
    private Locale locale = Locale.ROOT;
    private int headCacheTimeout;
    private boolean consoleLogging;
    private Level logLevel = Level.INFO;
    private boolean debug = false;
    private String flagMovementFormula = "";

    /**
     * Constructs a new {@link ConfigLoaderSpigot}.
     *
     * @param plugin
     *     The Spigot core.
     */
    @Inject
    public ConfigLoaderSpigot(
        RestartableHolder restartableHolder, JavaPlugin plugin, Lazy<MovableTypeManager> movableTypeManager,
        @Named("pluginBaseDirectory") Path baseDir, DebuggableRegistry debuggableRegistry)
    {
        this.plugin = plugin;
        this.movableTypeManager = movableTypeManager;
        this.baseDir = baseDir;
        movablePrices = new HashMap<>();
        movableSpeedMultipliers = new HashMap<>();

        header = "Config file for BigDoors. Don't forget to make a backup before making changes!";

        restartableHolder.registerRestartable(this);
        debuggableRegistry.registerDebuggable(this);
    }

    @Override
    public void initialize()
    {
        plugin.reloadConfig();
        rewriteConfig();
    }

    @Override
    public void shutDown()
    {
        configEntries.clear();
        powerBlockTypes.clear();
        movablePrices.clear();
        movableSpeedMultipliers.clear();
    }

    /**
     * Read the current config file and rewrite the file.
     */
    public void rewriteConfig()
    {
        plugin.reloadConfig();
        shutDown();

        final String defResPackUrl = "https://www.dropbox.com/s/0q6h8jkfjqrn1tp/BigDoorsResourcePack.zip?dl=1";
        final String defResPackUrl1_13 = "https://www.dropbox.com/s/al4idl017ggpnuq/BigDoorsResourcePack-1_13.zip?dl=1";

        final String[] loadChunksForToggleComment = {
            "Try to load chunks when a movable is toggled. When set to false, movables will not be toggled " +
                "if more than 1 chunk needs to be loaded.",
            "When set to true, the plugin will try to load all chunks the movable will interact with before " +
                "toggling. If more than 1 chunk ",
            "needs to be loaded, the movable will skip its animation to avoid spawning a bunch of entities " +
                "no one can see anyway."};
        final String[] enableRedstoneComment = {
            "Allow movables to be opened using redstone signals."};
        final String[] powerBlockTypeComment = {
            "Choose the type of the power block that is used to open movables using redstone.",
            "A list can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html",
            "This is the block that will open the movable attached to it when it receives a redstone signal.",
            "Multiple types are allowed."};
        final String[] blacklistComment = {
            "List of blacklisted materials. Materials on this list can not be animated.",
            "Use the same list of materials as for the power blocks. For example, you would blacklist bedrock like so:",
            "  - BEDROCK"};
        final String[] maxMovableCountComment = {
            "Global maximum number of movables a player can own. You can set it to -1 to disable it this limit.",
            "Not even admins and OPs can bypass this limit!",
            "Note that you can also use permissions for this, if you need more finely grained control using this node:",
            "'" + Limit.MOVABLE_COUNT.getUserPermission() + "x', where 'x' can be any positive value."};
        final String[] maxBlocksToMoveComment = {
            "Global maximum number of movables a player can own. You can set it to -1 to disable it this limit.",
            "Not even admins and OPs can bypass this limit!",
            "Note that you can also use permissions for this, if you need more finely grained control using this node:",
            "'" + Limit.BLOCKS_TO_MOVE.getUserPermission() + "x', where 'x' can be any positive value."};
        final String[] checkForUpdatesComment = {
            "Allow this plugin to check for updates on startup. It will not download new versions!"};
        final String[] downloadDelayComment = {
            "Time (in minutes) to delay auto downloading updates after their release.",
            "Setting it to 1440 means that updates will be downloaded 24h after their release.",
            "This is useful, as it will mean that the update won't get downloaded " +
                "if I decide to pull it for some reason",
            "(within the specified timeframe, of course). Note that updates cannot be " +
                "deferred for more than 1 week (10080 minutes)."};
        final String[] autoDLUpdateComment = {
            "Allow this plugin to automatically download new updates. They will be applied on restart."};
        final String[] allowStatsComment = {
            "Allow this plugin to send (anonymized) stats using bStats. Please consider keeping it enabled.",
            "It has a negligible impact on performance and more users on " +
                "stats keeps me more motivated to support this plugin!"};
        final String[] maxMovableSizeComment = {
            "Global maximum number of blocks allowed in a movable. You can set it to -1 to disable it this limit.",
            "If this number is exceeded, movables will open instantly and skip the animation.",
            "Not even admins and OPs can bypass this limit!",
            "Note that you can also use permissions for this, " +
                "if you need more finely grained control using this node: ",
            "'" + Limit.MOVABLE_SIZE.getUserPermission() + "x', where 'x' can be any positive value."};
        final String[] maxPowerBlockDistanceComment = {
            "Global maximum distance between a movable and its powerblock. You can set it to -1 to disable this limit.",
            "The distance is measured from the center of the movable.",
            "Not even admins and OPs can bypass this limit!",
            "Note that you can also use permissions for this, " +
                "if you need more finely grained control using this node: ",
            "'" + Limit.POWERBLOCK_DISTANCE.getUserPermission() + "x', where 'x' can be any positive value."};
        final String[] localeComment = {
            "Determines which locale to use. Defaults to root."
        };
        final String[] resourcePackComment = {
            "This plugin uses a support resource pack for things such as sound.",
            "Note that this may cause issues if you server or another plugin also uses a resource pack!",
            "When this is the case, it's recommended to disable this option and merge the pack with the other one.",
            "The default resource pack for 1.11.x/1.12.x is: '" + defResPackUrl + "'",
            "The default resource pack for 1.13.x is: '" + defResPackUrl1_13 + "'"};
        final String[] maxBlockSpeedComment = {
            "Determines the global speed limit of animated blocks measured in blocks/second.",
            "Animated objects will slow down when necessary to avoid any of their animated blocks exceeding this limit",
            "Higher values may result in choppier and/or glitchier animations."
        };
        final String[] speedMultiplierComment = {
            "Change the animation time of each movable type.",
            "Note that the maximum speed is limited by 'maxBlockSpeed', " +
                "so there is a limit to how fast you can make the movables",
            "The higher the value, the more time an animation will take. ",
            "For example, So 1.5 means it will take 50% as long, and 0.5 means it will only take half as long.",
            "To use the default values, set them to \"1.0\" (without quotation marks).",
            "Note that everything is optimized for default values, so it's recommended to leave this setting as-is."};
        final String[] coolDownComment = {
            "Cool-down on using movables. Time is measured in seconds."};
        final String[] cacheTimeoutComment = {
            "Amount of time (in minutes) to cache power block positions in a chunk. " +
                "-1 means no caching (not recommended!), 0 = infinite cache (not recommended either!).",
            "It doesn't take up too much RAM, so it's recommended to leave this value high. " +
                "It'll get updated automatically when needed anyway."};
        final String[] flagMovementFormulaComment = {
            "The movement formula of the blocks for flags.",
            "You can find a list of supported operators in the formula here: " +
                "https://github.com/PimvanderLoos/JCalculator",
            "The formula can use the following variables:",
            "'radius':  The distance of the block to the pole it is connected to.",
            "'counter': The number of steps that have passed in the animation.",
            "'length':  The total length of the flag.",
            "'height':  The height of the block for which the formula is used. The bottom row has a height of 0.",
            "The return value of the formula is the horizontal displacement of a single block in the flag."
        };
        final String[] pricesComment = {
            "When Vault is present, you can set the price of creation here for each type of movable.",
            "You can use the word \"blockCount\" (without quotation marks, case sensitive) as a " +
                "variable that will be replaced by the actual blockCount.",
            "Furthermore, you can use these operators: -, +, *, /, sqrt(), ^, %, " +
                "min(a,b), max(a,b), abs(), and parentheses.",
            "For example: \"price='max(10, sqrt(16)^4/100*blockCount)'\" " +
                "would return 10 for a blockCount of 0 to 3 and 10.24 for a blockCount of 4.",
            "You must always put the formula or simple value or whatever in quotation marks! " +
                "Also, these settings do nothing if Vault isn't installed!"};
        final String[] headCacheTimeoutComment = {
            "Amount of time (in minutes) to cache player heads. -1 means no caching (not recommended!), " +
                "0 = infinite cache (not recommended either!).",
            "Takes up a bit more space than the powerblock caching, but makes GUI much faster."};
        final String[] debugComment = {
            "Don't use this. Just leave it on false."};
        final String[] consoleLoggingComment = {
            "Write errors and exceptions to console. If disabled, they will only be written to the bigdoors log. ",
            "If enabled, they will be written to both the console and the bigdoors log."};
        final String[] logLevelComment = {
            "The log level to use. Note that levels lower than INFO aren't shown in the console by default, " +
                "regardless of this setting.",
            "Supported levels are: OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL.",
            "This will default to INFO in case an invalid option is provided."};


        final IConfigReader config = new ConfigReaderSpigot(plugin.getConfig());


        enableRedstone = addNewConfigEntry(config, "allowRedstone", true, enableRedstoneComment);
        loadChunksForToggle = addNewConfigEntry(config, "loadChunksForToggle", true, loadChunksForToggleComment);

        // No need to store the result here. It would be a list of Strings anyway, while we want blocks.
        // Because all entries need to be verified as valid blocks anyway, the list of power block types is
        // populated in the verification method.
        addNewConfigEntry(config, "powerBlockTypes", DEFAULT_POWERBLOCK_TYPE, powerBlockTypeComment,
                          new MaterialVerifier(powerBlockTypes));
        addNewConfigEntry(config, "materialBlacklist", DEFAULT_BLACKLIST, blacklistComment,
                          new MaterialVerifier(materialBlacklist));

        final int maxMovableCount = addNewConfigEntry(config, "maxMovableCount", -1, maxMovableCountComment);
        this.maxMovableCount = maxMovableCount > 0 ? OptionalInt.of(maxMovableCount) : OptionalInt.empty();

        final int maxBlocksToMove = addNewConfigEntry(config, "maxBlocksToMove", 100, maxBlocksToMoveComment);
        this.maxBlocksToMove = maxBlocksToMove > 0 ? OptionalInt.of(maxBlocksToMove) : OptionalInt.empty();

        final int maxMovableSize = addNewConfigEntry(config, "maxMovableSize", 500, maxMovableSizeComment);
        this.maxMovableSize = maxMovableSize > 0 ? OptionalInt.of(maxMovableSize) : OptionalInt.empty();

        final int maxPowerBlockDistance = addNewConfigEntry(config, "maxPowerBlockDistance", -1,
                                                            maxPowerBlockDistanceComment);
        this.maxPowerBlockDistance = maxPowerBlockDistance > 0 ?
                                     OptionalInt.of(maxPowerBlockDistance) : OptionalInt.empty();

        checkForUpdates = addNewConfigEntry(config, "checkForUpdates", true, checkForUpdatesComment);
        autoDLUpdate = addNewConfigEntry(config, "auto-update", true, autoDLUpdateComment);
        // Multiply by 60 to get the time in seconds. Also, it's capped to 10080 minutes, better known as 1 week.
        downloadDelay = addNewConfigEntry(config, "downloadDelay", 1_440, downloadDelayComment,
                                          (Integer x) -> Math.min(10_080, x)) * 60L;
        allowStats = addNewConfigEntry(config, "allowStats", true, allowStatsComment);

        String localeStr = addNewConfigEntry(config, "locale", "root", localeComment);
        // "root" isn't actually a valid country that can be used by a Locale.
        // So we map it to an empty String to ensure we get Locale#ROOT instead.
        if ("root".equalsIgnoreCase(localeStr))
            localeStr = "";
        locale = LocalizationUtil.getLocale(localeStr);

        resourcePack = addNewConfigEntry(config, "resourcePack", defResPackUrl1_13, resourcePackComment);
        headCacheTimeout = addNewConfigEntry(config, "headCacheTimeout", 120, headCacheTimeoutComment);
        coolDown = addNewConfigEntry(config, "coolDown", 0, coolDownComment);
        cacheTimeout = addNewConfigEntry(config, "cacheTimeout", 120, cacheTimeoutComment);

        flagMovementFormula = addNewConfigEntry(
            config, "flagMovementFormula",
            "min(0.07 * radius, 3) * sin(radius / 1.7 + height / 12 + counter / 12)",
            flagMovementFormulaComment);

        maxBlockSpeed = addNewConfigEntry(config, "maxBlockSpeed", 5.0D, maxBlockSpeedComment);

        final List<MovableType> enabledMovableTypes = movableTypeManager.get().getEnabledMovableTypes();
        parseForEachMovableType(movableSpeedMultipliers, config, enabledMovableTypes, speedMultiplierComment, 1.0D,
                                "speed-multiplier_");
        parseForEachMovableType(movablePrices, config, enabledMovableTypes, pricesComment, "0", "price_");

        consoleLogging = addNewConfigEntry(config, "consoleLogging", true, consoleLoggingComment);
        final String logLevelName = addNewConfigEntry(config, "logLevel", "INFO", logLevelComment);
        final @Nullable Level logLevelTmp = Util.parseLogLevelStrict(logLevelName);
        logLevel = logLevelTmp == null ? Level.INFO : logLevelTmp;


        // This is a bit special, as it's public static (for SpigotUtil debug messages).
        debug = addNewConfigEntry(config, "DEBUG", false, debugComment);
        if (debug)
            SpigotUtil.setPrintDebugMessages(true);

        writeConfig();
        printInfo();
    }

    private <T> void parseForEachMovableType(
        Map<MovableType, T> target, IConfigReader config, List<MovableType> enabledMovableTypes,
        String[] header, T defaultValue, String startsWith)
    {
        final Map<String, Object> existingMappings = getKeysStartingWith(config, startsWith, defaultValue);

        String @Nullable [] comment = header;
        for (final MovableType type : enabledMovableTypes)
        {
            final String key = startsWith + type.getSimpleName();
            target.put(type, addNewConfigEntry(config, key, defaultValue, comment));
            existingMappings.remove(key);
            comment = null;
        }

        comment = comment == null ? new String[]{"# Unloaded MovableTypes "} : comment;
        // Add the unmapped entries so they aren't ignored.
        for (final var entry : existingMappings.entrySet())
        {
            addNewConfigEntry(config, entry.getKey(), entry.getValue(), comment);
            comment = null;
        }
    }

    private Map<String, Object> getKeysStartingWith(IConfigReader config, String startsWith, Object defaultValue)
    {
        final Map<String, Object> ret = new LinkedHashMap<>();
        final List<String> keys = config.getKeys().stream().filter(key -> key.startsWith(startsWith)).toList();
        for (final String key : keys)
            ret.put(key, config.get(key, defaultValue));
        return ret;
    }

    /**
     * Logs some basic info read from the config, such as the list of power block materials.
     */
    private void printInfo()
    {
        final StringBuilder powerBlockTypes = new StringBuilder();
        this.powerBlockTypes.forEach(mat -> powerBlockTypes.append(String.format(" - %s\n", mat)));
        log.atInfo().log("Power Block Types:\n%s", powerBlockTypes);

        if (materialBlacklist.isEmpty())
            log.atInfo().log("No materials are blacklisted!");
        else
        {
            final StringBuilder blackListedMaterials = new StringBuilder();
            materialBlacklist.forEach(mat -> blackListedMaterials.append(String.format(" - %s\n", mat)));
            log.atInfo().log("Blacklisted materials:\n%s", blackListedMaterials);
        }
    }

    /**
     * Read a new config option from the config if it exists. Otherwise, use the default value.
     *
     * @param <T>
     *     The type of the option.
     * @param config
     *     The config.
     * @param optionName
     *     The name of the option in the config file.
     * @param defaultValue
     *     The default value of the option. To be used if no/invalid option is in the config already.
     * @param comment
     *     The comment to accompany the option in the config.
     * @return The value as read from the config file if it exists or the default value.
     */
    private <T> T addNewConfigEntry(
        IConfigReader config, String optionName, T defaultValue, String @Nullable ... comment)
    {
        final ConfigEntry<T> option = new ConfigEntry<>(config, optionName, defaultValue, comment);
        configEntries.add(option);
        return option.getValue();
    }

    /**
     * Read a new config option from the config if it exists. Otherwise, use the default value.
     *
     * @param <T>
     *     The type of the option.
     * @param config
     *     The config.
     * @param optionName
     *     The name of the option in the config file.
     * @param defaultValue
     *     The default value of the option. To be used if no/invalid option is in the config already.
     * @param comment
     *     The comment to accompany the option in the config.
     * @param verifyValue
     *     Function to use to verify the validity of a value and change it if necessary.
     * @return The value as read from the config file if it exists or the default value.
     */
    private <T> T addNewConfigEntry(
        IConfigReader config, String optionName, T defaultValue, String[] comment,
        ConfigEntry.ITestValue<T> verifyValue)
    {
        final ConfigEntry<T> option = new ConfigEntry<>(config, optionName, defaultValue, comment, verifyValue);
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
            if (!Files.isDirectory(baseDir))
                Files.createDirectories(baseDir);

            final Path configFile = baseDir.resolve("config.yml");
            if (!Files.isRegularFile(configFile))
                Files.createFile(configFile);

            if (!Files.isWritable(configFile))
            {
                log.atWarning().log("=======================================");
                log.atWarning().log("============== !WARNING! ==============");
                log.atWarning().log("=======================================");
                log.atWarning().log("====== CANNOT WRITE CONFIG FILE! ======");
                log.atWarning().log("==== NEW OPTIONS WILL NOT SHOW UP! ====");
                log.atWarning().log("==== THEY WILL USE DEFAULT VALUES! ====");
                log.atWarning().log("=======================================");
                log.atWarning().log("============== !WARNING! ==============");
                log.atWarning().log("=======================================");
            }

            final StringBuilder sb = new StringBuilder()
                .append("# ").append(header).append('\n');

            for (int idx = 0; idx < configEntries.size(); ++idx)
                sb.append(configEntries.get(idx).toString()).append('\n')
                  // Only print an additional newLine if the next config option has a comment.
                  .append((idx < configEntries.size() - 1 && configEntries.get(idx + 1).getComment() == null ?
                           "" : '\n'));

            Files.writeString(configFile, sb.toString());
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e)
               .log("Could not save config.yml! Please contact pim16aap2 and show him the following stacktrace:");
        }
    }

    @Override
    public boolean debug()
    {
        return debug;
    }

    @Override
    public String flagMovementFormula()
    {
        return flagMovementFormula;
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
    public Locale locale()
    {
        return locale;
    }

    @Override
    public OptionalInt maxMovableSize()
    {
        return maxMovableSize;
    }

    @Override
    public OptionalInt maxPowerBlockDistance()
    {
        return maxPowerBlockDistance;
    }

    @Override
    public int cacheTimeout()
    {
        return cacheTimeout;
    }

    public String resourcePack()
    {
        return resourcePack;
    }

    @Override
    public OptionalInt maxMovableCount()
    {
        return maxMovableCount;
    }

    @Override
    public OptionalInt maxBlocksToMove()
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
     * an update was released on Wednesday, June 1, 2022, at 12PM, it will not download this update before Thursday,
     * June 2, 2022, at 12PM. When running a dev-build, however, this value is overridden to 0.
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

    @Override
    public boolean loadChunksForToggle()
    {
        return loadChunksForToggle;
    }

    public Set<Material> powerBlockTypes()
    {
        return powerBlockTypes;
    }

    /**
     * Gets the amount of time to keep heads cached.
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
    public String getPrice(MovableType type)
    {
        final String ret = movablePrices.get(type);
        if (ret != null)
            return ret;
        log.atSevere().withStackTrace(StackSize.FULL).log("No price found for type: '%s'", type);
        return "0";
    }

    @Override
    public double getAnimationSpeedMultiplier(MovableType type)
    {
        return Math.max(0.0001D, movableSpeedMultipliers.getOrDefault(type, 1.0D));
    }

    @Override
    public double maxBlockSpeed()
    {
        return maxBlockSpeed;
    }

    @Override
    public Level logLevel()
    {
        return logLevel;
    }

    @Override
    public boolean consoleLogging()
    {
        return consoleLogging;
    }

    @Override
    public String getDebugInformation()
    {
        return "Config: " + this;
    }

    /**
     * Represents a class that attempts to parse a list of materials represented as Strings into a list of Materials.
     * <p>
     * See {@link #verifyMaterials(List, Set)}.
     *
     * @author Pim
     */
    private static class MaterialVerifier implements ConfigEntry.ITestValue<List<String>>
    {
        private final Set<Material> output;

        /**
         * Constructs a new MaterialVerifier.
         * <p>
         * Note that the output set is cleared!
         *
         * @param output
         *     The set to write the parsed materials to.
         */
        private MaterialVerifier(Set<Material> output)
        {
            this.output = output;
            output.clear();
        }

        @Override
        public List<String> test(List<String> input)
        {
            return MaterialVerifier.verifyMaterials(input, output);
        }

        /**
         * Verifies that all Strings in a list are valid (solid) materials. All invalid and duplicated entries are
         * removed from the list.
         * <p>
         * All valid materials are added to the output set.
         *
         * @param input
         *     The list of Strings of potential materials.
         * @param output
         *     The set to put all valid materials in.
         * @return The list of names of all valid materials in the list without duplication.
         */
        private static List<String> verifyMaterials(List<String> input, Set<Material> output)
        {
            output.clear();
            final Iterator<String> it = input.iterator();
            while (it.hasNext())
            {
                final String str = it.next();
                try
                {
                    final Material mat = Material.valueOf(str);
                    if (output.contains(mat))
                    {
                        log.atWarning().log("Failed to add material: \"%s\". It was already on the list!", str);
                        it.remove();
                    }
                    else if (mat.isSolid())
                    {
                        output.add(mat);
                    }
                    else
                    {
                        log.atWarning()
                           .log("Failed to add material: \"%s\". Only solid materials are allowed!", str);
                        it.remove();
                    }
                }
                catch (Exception e)
                {
                    log.atWarning().log("Failed to parse material: \"%s\".", str);
                    it.remove();
                }
            }

            return input;
        }
    }
}
