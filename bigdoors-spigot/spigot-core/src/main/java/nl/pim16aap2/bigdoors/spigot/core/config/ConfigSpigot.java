package nl.pim16aap2.bigdoors.spigot.core.config;

import com.google.common.flogger.StackSize;
import dagger.Lazy;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.IConfig;
import nl.pim16aap2.bigdoors.core.api.IConfigReader;
import nl.pim16aap2.bigdoors.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.core.api.debugging.IDebuggable;
import nl.pim16aap2.bigdoors.core.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.core.localization.LocalizationUtil;
import nl.pim16aap2.bigdoors.core.managers.StructureTypeManager;
import nl.pim16aap2.bigdoors.core.structures.StructureType;
import nl.pim16aap2.bigdoors.core.util.ConfigEntry;
import nl.pim16aap2.bigdoors.core.util.Constants;
import nl.pim16aap2.bigdoors.core.util.Limit;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.spigot.util.implementations.ConfigReaderSpigot;
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
public final class ConfigSpigot implements IConfig, IDebuggable
{
    @ToString.Exclude
    private final JavaPlugin plugin;
    @ToString.Exclude
    private final Lazy<StructureTypeManager> structureTypeManager;
    private final Path baseDir;

    private static final List<String> DEFAULT_POWERBLOCK_TYPE = List.of("GOLD_BLOCK");
    private static final List<String> DEFAULT_BLACKLIST = Collections.emptyList();

    private final Set<Material> powerBlockTypes = EnumSet.noneOf(Material.class);
    private final Set<Material> materialBlacklist = EnumSet.noneOf(Material.class);
    @ToString.Exclude
    private final List<ConfigEntry<?>> configEntries = new ArrayList<>();
    private final Map<StructureType, String> structurePrices;
    private final Map<StructureType, Double> structureAnimationTimeMultipliers;
    @ToString.Exclude
    private final String header;

    private int coolDown;
    private boolean allowStats;
    private OptionalInt maxStructureSize = OptionalInt.empty();
    private OptionalInt maxPowerBlockDistance = OptionalInt.empty();
    private boolean resourcePackEnabled = false;
    private final String resourcePack =
        "https://www.dropbox.com/s/8vpwzjkd9jnp1xu/BigDoorsResourcePack-Format12.zip?dl=1";
    private OptionalInt maxStructureCount = OptionalInt.empty();
    private OptionalInt maxBlocksToMove = OptionalInt.empty();
    private double maxBlockSpeed;
    private int cacheTimeout;
    private boolean autoDLUpdate;
    private boolean enableRedstone;
    private long downloadDelay;
    private boolean loadChunksForToggle;
    private boolean checkForUpdates;
    private Locale locale = Locale.ROOT;
    private int headCacheTimeout;
    private boolean consoleLogging;
    private Level logLevel = Level.INFO;
    private boolean debug = false;
    private String flagMovementFormula = "";

    /**
     * Constructs a new {@link ConfigSpigot}.
     *
     * @param plugin
     *     The Spigot core.
     */
    @Inject
    public ConfigSpigot(
        RestartableHolder restartableHolder, JavaPlugin plugin, Lazy<StructureTypeManager> structureTypeManager,
        @Named("pluginBaseDirectory") Path baseDir, DebuggableRegistry debuggableRegistry)
    {
        this.plugin = plugin;
        this.structureTypeManager = structureTypeManager;
        this.baseDir = baseDir;
        structurePrices = new HashMap<>();
        structureAnimationTimeMultipliers = new HashMap<>();

        header = "Config file for BigDoors. Don't forget to make a backup before making changes!";

        restartableHolder.registerRestartable(this);
        debuggableRegistry.registerDebuggable(this);
    }

    @Override
    public void initialize()
    {
        plugin.reloadConfig();
        rewriteConfig(true);
    }

    @Override
    public void shutDown()
    {
        configEntries.clear();
        powerBlockTypes.clear();
        structurePrices.clear();
        structureAnimationTimeMultipliers.clear();
    }

    /**
     * Read the current config file and rewrite the file.
     */
    public void rewriteConfig(boolean printResults)
    {
        plugin.reloadConfig();
        shutDown();

        final String loadChunksForToggleComment =
            """
            # Try to load chunks when a structure is toggled. When set to false, structures will not be toggled if more
            # than 1 chunk needs to be loaded.
            # When set to true, the plugin will try to load all chunks the structure will interact with before toggling.
            # If more than 1 chunk needs to be loaded, the structure will skip its animation to avoid spawning a bunch
            # of entities no one can see anyway.
            """;

        final String enableRedstoneComment =
            """
            # Allow structures to be opened using redstone signals.
            """;

        final String powerBlockTypeComment =
            """
            # Choose the type of the power block that is used to open structures using redstone.
            # This is the block that will open the structure attached to it when it receives a redstone signal.
            # Multiple types are allowed.
            #
            # A list of options can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html
            """;

        final String blacklistComment =
            """
            # List of blacklisted materials. Materials on this list can not be animated.
            #
            # Use the same list of materials as for the power blocks. For example, you would blacklist bedrock like so:
            #   - BEDROCK
            """;

        final String maxStructureCountComment = String.format(
            """
            # Global maximum number of structures a player can own. You can set it to -1 to disable it this limit.
            #
            # Not even admins and OPs can bypass this limit!
            #
            # You can use permissions if you need more finely grained control using this node:
            # '%s.x', where 'x' can be any positive value.
            """, Limit.STRUCTURE_COUNT.getUserPermission());

        final String maxBlocksToMoveComment = String.format(
            """
            # Global maximum number of structures a player can own. You can set it to -1 to disable it this limit.
            #
            # Not even admins and OPs can bypass this limit!
            #
            # You can use permissions if you need more finely grained control using this node:
            # '%s.x', where 'x' can be any positive value.
            """, Limit.BLOCKS_TO_MOVE.getUserPermission());

        final String checkForUpdatesComment =
            """
            # Allow this plugin to check for updates on startup. It will not download new versions!
            """;

        final String downloadDelayComment =
            """
            # Time (in minutes) to delay downloading updates after their release.
            # Setting it to 1440 means that updates will be downloaded 24h after their release.
            # This is useful to avoid automatically updating to an update that may be retracted after release.
            # Note that updates cannot be deferred for more than 1 week (10080 minutes)."
            """;

        final String autoDLUpdateComment =
            """
            # Allow this plugin to automatically download new updates. They will be applied on restart.
            """;

        final String allowStatsComment =
            """
            # Allow this plugin to send (anonymized) stats using bStats. Please consider keeping it enabled.
            # It has a negligible impact on performance and it helps me choose what to work on.
            """;

        final String maxStructureSizeComment = String.format(
            """
            # Global maximum number of blocks allowed in a structure. You can set it to -1 to disable it this limit.
            # If this number is exceeded, structures will open instantly and skip the animation.
            #
            # Not even admins and OPs can bypass this limit!
            #
            # You can use permissions if you need more finely grained control using this node:
            # '%s.x', where 'x' can be any positive value.
            """, Limit.STRUCTURE_SIZE.getUserPermission());

        final String maxPowerBlockDistanceComment = String.format(
            """
            # Global maximum distance between a structure and its powerblock.
            # The distance is measured from the center of the structure.
            #
            # Not even admins and OPs can bypass this limit!
            #
            # You can set it to -1 to disable this limit.
            #
            # You can use permissions if you need more finely grained control using this node:
            # '%s.x', where 'x' can be any positive value."
            """, Limit.POWERBLOCK_DISTANCE.getUserPermission());

        final String localeComment =
            """
            # Determines which locale to use. Defaults to root."
            """;

        final String resourcePackComment =
            """
            # This plugin uses a support resource pack for things such as sound.
            # Enabling this may cause issues if you server or another plugin also uses a resource pack!
            # When this is the case, it's recommended to disable this option and merge the pack with the other one.
            """;

        final String maxBlockSpeedComment =
            """
            # Determines the global speed limit of animated blocks measured in blocks/second.
            # Animated objects will slow down when necessary to avoid any of their animated blocks exceeding this limit
            # Higher values may result in choppier and/or glitchier animations."
            """;

        final String animationTimeMultiplierComment =
            """
            # Change the animation time of each structure type.
            # The higher the value, the more time an animation will take.
            #
            # For example, we have a structure with a default animation duration of 10 seconds.
            # With a multiplier of 1.5, the animation will take 15 seconds, while a multiplier of 0.5 will result in a
            # duration of 5 seconds.
            #
            # Note that the maximum speed of the animated blocks is limited by 'maxBlockSpeed', so there is a limit to
            # how fast you can make the structures.
            """;

        final String coolDownComment =
            """
            # Cool-down on using structures. Time is measured in seconds."
            """;

        final String cacheTimeoutComment =
            """
            # Amount of time (in minutes) to cache power block positions in a chunk.
            # -1 means no caching (not recommended!), 0 = infinite cache (not recommended either!).
            # It doesn't take up too much RAM, so it's recommended to leave this value high.
            # It'll get updated automatically when needed anyway.
            """;

        final String flagMovementFormulaComment =
            """
            # The movement formula of the blocks for flags. THe formula is evaluated for each block
            # for each step in the animation.
            #
            # You can find a list of supported operators in the formula here:
            # https://github.com/PimvanderLoos/JCalculator
            #
            # The formula can use the following variables:
            #   'radius':  The distance of the block to the pole it is connected to.
            #   'counter': The number of steps that have passed in the animation.
            #   'length':  The total length of the flag.
            #   'height':  The height of the block for which the formula is used. The bottom row has a height of 0.
            #
            # The return value of the formula is the horizontal displacement of a single block in the flag.
            """;

        final String pricesComment =
            """
            # When Vault is present, you can set the price of creation here for each type of structure.
            # You can use the word "blockCount" (without quotation marks, case sensitive) as a variable that will be
            # replaced by the actual blockCount.
            #
            # You can use the following operators:
            #   -, +, *, /, sqrt(), ^, %, min(a,b), max(a,b), abs(), and parentheses.
            #
            # For example: "price='max(10, sqrt(16)^4/100*blockCount)'
            # would return 10 for a blockCount of 0 to 3 and 10.24 for a blockCount of 4.
            # You must always put the formula or simple value or whatever in quotation marks!
            # Also, these settings do nothing if Vault isn't installed!
            """;

        final String headCacheTimeoutComment =
            """
            # Amount of time (in minutes) to cache player heads.
            #   -1 = no caching (not recommended!)
            #    0 = infinite cache (not recommended either!)
            """;

        final String debugComment =
            """
            # Don't use this. Just leave it on false.
            """;

        final String consoleLoggingComment =
            """
            # Write errors and exceptions to console. If disabled, they will only be written to the bigdoors log.
            # If enabled, they will be written to both the console and the bigdoors log.
            """;

        final String logLevelComment =
            """
            # The log level to use. Note that levels lower than INFO aren't shown in the console by default,
            # regardless of this setting.
            #
            # Supported levels are:
            #   OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL.
            #
            # This will default to INFO in case an invalid option is provided.
            """;


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

        final int maxStructureCount = addNewConfigEntry(config, "maxStructureCount", -1, maxStructureCountComment);
        this.maxStructureCount = maxStructureCount > 0 ? OptionalInt.of(maxStructureCount) : OptionalInt.empty();

        final int maxBlocksToMove = addNewConfigEntry(config, "maxBlocksToMove", 100, maxBlocksToMoveComment);
        this.maxBlocksToMove = maxBlocksToMove > 0 ? OptionalInt.of(maxBlocksToMove) : OptionalInt.empty();

        final int maxStructureSize = addNewConfigEntry(config, "maxStructureSize", 500, maxStructureSizeComment);
        this.maxStructureSize = maxStructureSize > 0 ? OptionalInt.of(maxStructureSize) : OptionalInt.empty();

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

        resourcePackEnabled = addNewConfigEntry(config, "resourcePackEnabled", false, resourcePackComment);
        headCacheTimeout = addNewConfigEntry(config, "headCacheTimeout", 120, headCacheTimeoutComment);
        coolDown = addNewConfigEntry(config, "coolDown", 0, coolDownComment);
        cacheTimeout = addNewConfigEntry(config, "cacheTimeout", 120, cacheTimeoutComment);

        flagMovementFormula = addNewConfigEntry(
            config, "flagMovementFormula",
            "min(0.07 * radius, 3) * sin(radius / 1.7 + height / 12 + counter / 12)",
            flagMovementFormulaComment);

        maxBlockSpeed = addNewConfigEntry(config, "maxBlockSpeed", 5.0D, maxBlockSpeedComment);

        final List<StructureType> enabledStructureTypes = structureTypeManager.get().getEnabledStructureTypes();
        parseForEachStructureType(structureAnimationTimeMultipliers, config, enabledStructureTypes,
                                  animationTimeMultiplierComment,
                                  1.0D,
                                  "animation-time-multiplier_");
        parseForEachStructureType(structurePrices, config, enabledStructureTypes, pricesComment, "0", "price_");

        consoleLogging = addNewConfigEntry(config, "consoleLogging", true, consoleLoggingComment);
        final String logLevelName = addNewConfigEntry(config, "logLevel", "INFO", logLevelComment);
        final @Nullable Level logLevelTmp = Util.parseLogLevelStrict(logLevelName);
        logLevel = logLevelTmp == null ? Level.INFO : logLevelTmp;

        debug = addNewConfigEntry(config, "DEBUG", false, debugComment);
        if (debug && printResults)
            SpigotUtil.setPrintDebugMessages(true);

        writeConfig();

        if (printResults)
            printInfo();
    }

    private <T> void parseForEachStructureType(
        Map<StructureType, T> target, IConfigReader config, List<StructureType> enabledStructureTypes,
        String header, T defaultValue, String startsWith)
    {
        final Map<String, Object> existingMappings = getKeysStartingWith(config, startsWith, defaultValue);

        @Nullable String comment = header;
        for (final StructureType type : enabledStructureTypes)
        {
            final String key = startsWith + type.getSimpleName();
            target.put(type, addNewConfigEntry(config, key, defaultValue, comment));
            existingMappings.remove(key);
            comment = null;
        }

        comment = comment == null ? "# Unloaded StructureTypes " : comment;
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
        IConfigReader config, String optionName, T defaultValue, @Nullable String comment)
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
        IConfigReader config, String optionName, T defaultValue, @Nullable String comment,
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
    public OptionalInt maxStructureSize()
    {
        return maxStructureSize;
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

    public boolean isResourcePackEnabled()
    {
        return resourcePackEnabled;
    }

    public String resourcePack()
    {
        return resourcePack;
    }

    @Override
    public OptionalInt maxStructureCount()
    {
        return maxStructureCount;
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
    public boolean isRedstoneEnabled()
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
    public String getPrice(StructureType type)
    {
        final String ret = structurePrices.get(type);
        if (ret != null)
            return ret;
        log.atSevere().withStackTrace(StackSize.FULL).log("No price found for type: '%s'", type);
        return "0";
    }

    @Override
    public double getAnimationTimeMultiplier(StructureType type)
    {
        return Math.max(0.0001D, structureAnimationTimeMultipliers.getOrDefault(type, 1.0D));
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