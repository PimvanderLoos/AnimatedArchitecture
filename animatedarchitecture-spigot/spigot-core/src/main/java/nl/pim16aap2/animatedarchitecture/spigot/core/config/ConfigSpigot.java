package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import com.google.common.flogger.StackSize;
import dagger.Lazy;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.IConfigReader;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.localization.LocalizationUtil;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.ConfigEntry;
import nl.pim16aap2.animatedarchitecture.core.util.Limit;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.spigot.core.hooks.ProtectionHookManagerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotUtil;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IBlockAnalyzerConfig;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigotSpecification;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.ConfigReaderSpigot;
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
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Level;

/**
 * Represents the configuration of the plugin for the Spigot platform.
 */
@ToString
@Singleton
@Flogger
public final class ConfigSpigot implements IConfig, IDebuggable, IBlockAnalyzerConfig
{
    private static final List<String> DEFAULT_COMMAND_ALIASES = List.of(
        "animatedarchitecture",
        "AnimatedArchitecture",
        "aa"
    );

    @ToString.Exclude
    private final JavaPlugin plugin;
    @ToString.Exclude
    private final Lazy<StructureTypeManager> structureTypeManager;
    @ToString.Exclude
    private final Lazy<ProtectionHookManagerSpigot> protectionHookManager;
    private final Path baseDir;

    private static final Material DEFAULT_MATERIAL = Material.WARPED_DOOR;
    private static final List<String> DEFAULT_POWERBLOCK_TYPE = List.of("GOLD_BLOCK");
    private static final List<String> DEFAULT_BLACKLIST = Collections.emptyList();

    private final Set<Material> powerBlockTypes = EnumSet.noneOf(Material.class);
    private final Set<Material> materialBlacklist = EnumSet.noneOf(Material.class);
    private final Set<IProtectionHookSpigotSpecification> enabledProtectionHooks = new HashSet<>();
    @ToString.Exclude
    private final List<ConfigEntry<?>> configEntries = new ArrayList<>();

    private final Map<StructureType, String> structurePrices;
    private final Map<StructureType, Double> structureAnimationTimeMultipliers;
    private final Map<StructureType, Material> structureTypeGuiMaterials;
    @ToString.Exclude
    private final String header;

    private final List<String> commandAliases = new ArrayList<>(DEFAULT_COMMAND_ALIASES);

    private int coolDown;
    private OptionalInt maxStructureSize = OptionalInt.empty();
    private OptionalInt maxPowerBlockDistance = OptionalInt.empty();
    private boolean resourcePackEnabled = false;
    private final String resourcePack =
        "https://www.dropbox.com/s/8vpwzjkd9jnp1xu/AnimatedArchitectureResourcePack-Format12.zip?dl=1";
    private OptionalInt maxStructureCount = OptionalInt.empty();
    private OptionalInt maxBlocksToMove = OptionalInt.empty();
    private double maxBlockSpeed;
    private int cacheTimeout;
    private boolean enableRedstone;
    private boolean loadChunksForToggle;
    private Locale locale = Locale.ROOT;
    private int headCacheTimeout;
    private boolean skipAnimationsByDefault;
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
        RestartableHolder restartableHolder,
        JavaPlugin plugin,
        Lazy<StructureTypeManager> structureTypeManager,
        Lazy<ProtectionHookManagerSpigot> protectionHookManager,
        @Named("pluginBaseDirectory") Path baseDir,
        DebuggableRegistry debuggableRegistry)
    {
        this.plugin = plugin;
        this.structureTypeManager = structureTypeManager;
        this.protectionHookManager = protectionHookManager;
        this.baseDir = baseDir;
        structurePrices = new HashMap<>();
        structureAnimationTimeMultipliers = new HashMap<>();
        structureTypeGuiMaterials = new HashMap<>();

        header = """
            # Config file for AnimatedArchitecture. Don't forget to make a backup before making changes!
            #
            # For most options, you can apply your changes using "/animatedarchitecture restart".
            # When an option requires a restart, it will be mentioned in the description.
            """;

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
        structureTypeGuiMaterials.clear();
        structureAnimationTimeMultipliers.clear();
    }

    /**
     * Read the current config file and rewrite the file.
     */
    public void rewriteConfig(boolean printResults)
    {
        plugin.reloadConfig();
        shutDown();

        final String loadChunksForToggleComment = """
            # Try to load chunks when a structure is toggled. When set to false, structures will not be toggled if more
            # than 1 chunk needs to be loaded.
            # When set to true, the plugin will try to load all chunks the structure will interact with before toggling.
            # If more than 1 chunk needs to be loaded, the structure will skip its animation to avoid spawning a bunch
            # of entities no one can see anyway.
            """;

        final String enableRedstoneComment = """
            # Allow structures to be opened using redstone signals.
            """;

        final String powerBlockTypeComment = """
            # Choose the type of the power block that is used to open structures using redstone.
            # This is the block that will open the structure attached to it when it receives a redstone signal.
            # Multiple types are allowed.
            #
            # A list of options can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html
            """;

        final String blacklistComment = """
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
                """,
            Limit.STRUCTURE_COUNT.getUserPermission()
        );

        final String maxBlocksToMoveComment = String.format(
            """
                # Global maximum number of structures a player can own. You can set it to -1 to disable it this limit.
                #
                # Not even admins and OPs can bypass this limit!
                #
                # You can use permissions if you need more finely grained control using this node:
                # '%s.x', where 'x' can be any positive value.
                """,
            Limit.BLOCKS_TO_MOVE.getUserPermission()
        );

        final String maxStructureSizeComment = String.format(
            """
                # Global maximum number of blocks allowed in a structure. You can set it to -1 to disable it this limit.
                # If this number is exceeded, structures will open instantly and skip the animation.
                #
                # Not even admins and OPs can bypass this limit!
                #
                # You can use permissions if you need more finely grained control using this node:
                # '%s.x', where 'x' can be any positive value.
                """,
            Limit.STRUCTURE_SIZE.getUserPermission()
        );

        final String maxPowerBlockDistanceComment = String.format(
            """
                # Global maximum distance between a structure and its powerblock.
                #
                # Not even admins and OPs can bypass this limit!
                #
                # The distance is measured from the edge of the structure to the power block.
                # As such, the distance may exceed this limit if the structure moves away
                # from the power block after creation.
                #
                # You can set it to -1 to disable this limit.
                #
                # You can use permissions if you need more finely grained control using this node:
                # '%s.x', where 'x' can be any positive value.
                """,
            Limit.POWERBLOCK_DISTANCE.getUserPermission()
        );

        final String localeComment = """
            # Determines which locale to use. Defaults to root.
            #
            # A list of supported locales can be found here:
            # https://github.com/PimvanderLoos/AnimatedArchitecture/tree/master/animatedarchitecture-core/src/main/resources
            # For example, to use the Dutch locale, you would set this to 'nl_NL'.
            #
            # After changing this, either restart the plugin or restart the server to apply the changes.
            #
            # Any strings that are not translated for the chosen locale will default to the root locale (English).
            """;

        final String commandAliasesComment = """
            # List of aliases for the /animatedarchitecture command.
            # The first alias will be used as the main command.
            # Aliases are case sensitive, can not contain spaces, and should not have a leading slash.
            # Changing this will require a server restart to take effect.
            """;

        final String resourcePackComment = """
            # This plugin uses a support resource pack for things such as sound.
            # Enabling this may cause issues if you server or another plugin also uses a resource pack!
            # When this is the case, it's recommended to disable this option and merge the pack with the other one.
            """;

        final String maxBlockSpeedComment = """
            # Determines the global speed limit of animated blocks measured in blocks/second.
            # Animated objects will slow down when necessary to avoid any of their animated blocks exceeding this limit
            # Higher values may result in choppier and/or glitchier animations.
            """;

        final String animationTimeMultiplierComment = """
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

        final String coolDownComment = """
            # Cool-down on using structures. Time is measured in seconds.
            """;

        final String cacheTimeoutComment = """
            # Amount of time (in minutes) to cache power block positions in a chunk.
            # -1 means no caching (not recommended!), 0 = infinite cache (not recommended either!).
            # It doesn't take up too much RAM, so it's recommended to leave this value high.
            # It'll get updated automatically when needed anyway.
            """;

        final String flagMovementFormulaComment = """
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

        final String pricesComment = """
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

        final String headCacheTimeoutComment = """
            # Amount of time (in minutes) to cache player heads.
            #   -1 = no caching (not recommended!)
            #    0 = infinite cache (not recommended either!)
            """;

        final String enabledProtectionHooksComment = """
            # Enable or disable compatibility hooks for certain plugins.
            # If the plugins aren't installed, these options do nothing.
            # When enabled, structures cannot be toggled or created in areas not owned by the owner of that structure.
            """;

        final String debugComment = """
            # Don't use this. Just leave it on false.
            """;

        final String skipAnimationsByDefaultComment = """
            # When enabled, all animations will be skipped by default, causing any toggles to simply teleport the blocks
            # to their destination instantly.
            # Note that this only determines the default value. It can be overridden in some cases.
            """;

        final String consoleLoggingComment = """
            # Write errors and exceptions to console.
            # If disabled, they will only be written to the AnimatedArchitecture log.
            # If enabled, they will be written to both the console and the AnimatedArchitecture log.
            """;

        final String logLevelComment = """
            # The log level to use. Note that levels lower than INFO aren't shown in the console by default,
            # regardless of this setting. They are still written to this plugin's log file, though.
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
        addNewConfigEntry(
            config,
            "powerBlockTypes",
            DEFAULT_POWERBLOCK_TYPE,
            powerBlockTypeComment,
            new MaterialVerifier(powerBlockTypes)
        );

        addNewConfigEntry(
            config,
            "materialBlacklist",
            DEFAULT_BLACKLIST,
            blacklistComment,
            new MaterialVerifier(materialBlacklist)
        );

        final int maxStructureCount = addNewConfigEntry(config, "maxStructureCount", -1, maxStructureCountComment);
        this.maxStructureCount = maxStructureCount > 0 ? OptionalInt.of(maxStructureCount) : OptionalInt.empty();

        final int maxBlocksToMove = addNewConfigEntry(config, "maxBlocksToMove", 100, maxBlocksToMoveComment);
        this.maxBlocksToMove = maxBlocksToMove > 0 ? OptionalInt.of(maxBlocksToMove) : OptionalInt.empty();

        final int maxStructureSize = addNewConfigEntry(config, "maxStructureSize", 500, maxStructureSizeComment);
        this.maxStructureSize = maxStructureSize > 0 ? OptionalInt.of(maxStructureSize) : OptionalInt.empty();

        final int maxPowerBlockDistance = addNewConfigEntry(
            config,
            "maxPowerBlockDistance",
            -1,
            maxPowerBlockDistanceComment
        );

        this.maxPowerBlockDistance =
            maxPowerBlockDistance > 0 ? OptionalInt.of(maxPowerBlockDistance) : OptionalInt.empty();

        String localeStr = addNewConfigEntry(config, "locale", "root", localeComment);
        // "root" isn't actually a valid country that can be used by a Locale.
        // So we map it to an empty String to ensure we get Locale#ROOT instead.
        if ("root".equalsIgnoreCase(localeStr))
            localeStr = "";
        locale = Objects.requireNonNullElse(LocalizationUtil.parseLocale(localeStr), Locale.ROOT);

        resourcePackEnabled = addNewConfigEntry(config, "resourcePackEnabled", false, resourcePackComment);

        readCommandAliases(config, commandAliasesComment);

        headCacheTimeout = addNewConfigEntry(config, "headCacheTimeout", 120, headCacheTimeoutComment);
        coolDown = addNewConfigEntry(config, "coolDown", 0, coolDownComment);
        cacheTimeout = addNewConfigEntry(config, "cacheTimeout", 120, cacheTimeoutComment);

        flagMovementFormula = addNewConfigEntry(
            config,
            "flagMovementFormula",
            "min(0.07 * radius, 3) * sin(radius / 1.7 + height / 12 + counter / 12)",
            flagMovementFormulaComment
        );

        maxBlockSpeed = addNewConfigEntry(config, "maxBlockSpeed", 5.0D, maxBlockSpeedComment);

        final List<StructureType> enabledStructureTypes = structureTypeManager.get().getEnabledStructureTypes();
        parseForEachStructureType(
            structureAnimationTimeMultipliers,
            config,
            enabledStructureTypes,
            animationTimeMultiplierComment,
            Collections.emptyMap(),
            1.0D,
            "animation-time-multiplier_"
        );

        parseForEachStructureType(
            structurePrices,
            config,
            enabledStructureTypes,
            pricesComment,
            Collections.emptyMap(),
            "0",
            "price_"
        );
        parseStructureTypeGuiMaterials(config, enabledStructureTypes);

        enabledProtectionHooks.clear();
        enabledProtectionHooks.addAll(parseProtectionHooks(config, enabledProtectionHooksComment));

        skipAnimationsByDefault = addNewConfigEntry(
            config,
            "skipAnimationsByDefault",
            false,
            skipAnimationsByDefaultComment
        );

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

    private void readCommandAliases(IConfigReader config, String commandAliasesComment)
    {
        commandAliases.clear();
        commandAliases.addAll(addNewConfigEntry(
            config,
            "commandAliases",
            DEFAULT_COMMAND_ALIASES,
            commandAliasesComment)
        );

        if (commandAliases.isEmpty())
        {
            log.atWarning().log(
                "No command aliases were found. Using the default aliases: %s", DEFAULT_COMMAND_ALIASES);
            commandAliases.addAll(DEFAULT_COMMAND_ALIASES);
        }
    }

    private Set<IProtectionHookSpigotSpecification> parseProtectionHooks(
        IConfigReader config, String enabledProtectionHooksComment)
    {
        @Nullable String comment = enabledProtectionHooksComment;
        final Set<IProtectionHookSpigotSpecification> ret = new HashSet<>();
        for (final var hook : protectionHookManager.get().getRegisteredHookDefinitions().values())
        {
            if (addNewConfigEntry(config, "hook_" + hook.getName(), true, comment))
                ret.add(hook);
            comment = null;
        }
        return ret;
    }

    private void parseStructureTypeGuiMaterials(IConfigReader config, List<StructureType> enabledStructureTypes)
    {
        final String prefix = "gui-material_";
        final Map<String, String> defaults = Map.of(
            prefix + "bigdoor",
            Material.OAK_DOOR.name(),
            prefix + "clock",
            Material.CLOCK.name(),
            prefix + "drawbridge",
            Material.OAK_TRAPDOOR.name(),
            prefix + "flag",
            Material.BLUE_BANNER.name(),
            prefix + "garagedoor",
            Material.MINECART.name(),
            prefix + "portcullis",
            Material.IRON_BARS.name(),
            prefix + "revolvingdoor",
            Material.MUSIC_DISC_PIGSTEP.name(),
            prefix + "slidingdoor",
            Material.PISTON.name(),
            prefix + "windmill",
            Material.SUNFLOWER.name()
        );

        final String comment = """
            # The materials to use in the GUI when looking at the overview of all structures.
            """;

        final Map<StructureType, String> materialNames = new HashMap<>();
        parseForEachStructureType(
            materialNames,
            config,
            enabledStructureTypes,
            comment,
            defaults,
            DEFAULT_MATERIAL.name(),
            prefix
        );

        final Map<StructureType, Material> result = new HashMap<>(MathUtil.ceil(1.25 * materialNames.size()));
        for (final var entry : materialNames.entrySet())
        {
            @Nullable Material mat = Material.getMaterial(entry.getValue());
            if (mat == null)
            {
                log.atWarning().log(
                    "Could not find material with name '%s'! Defaulting to '%s'!",
                    entry.getValue(),
                    DEFAULT_MATERIAL.name()
                );
                mat = DEFAULT_MATERIAL;
            }
            result.put(entry.getKey(), mat);
        }
        this.structureTypeGuiMaterials.putAll(result);
    }

    private <T> void parseForEachStructureType(
        Map<StructureType, T> target,
        IConfigReader config,
        List<StructureType> enabledStructureTypes,
        String header,
        Map<String, T> defaultValues,
        T defaultValue,
        String startsWith)
    {
        final Map<String, Object> existingMappings = getKeysStartingWith(config, startsWith, defaultValue);

        @Nullable String comment = header;
        for (final StructureType type : enabledStructureTypes)
        {
            final String key = startsWith + type.getSimpleName();
            final T value = defaultValues.getOrDefault(key, defaultValue);

            target.put(type, addNewConfigEntry(config, key, value, comment));
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
        IConfigReader config,
        String optionName,
        T defaultValue,
        @Nullable
        String comment)
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
        IConfigReader config,
        String optionName,
        T defaultValue,
        @Nullable
        String comment,
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

            final StringBuilder sb = new StringBuilder().append(header).append('\n');

            for (int idx = 0; idx < configEntries.size(); ++idx)
            {
                final boolean succeededByCommand = idx < (configEntries.size() - 1) &&
                    configEntries.get(idx + 1).getComment() != null;

                sb.append(configEntries.get(idx).toString())
                    .append('\n')
                    // Only print an additional newLine if the next config option has a comment.
                    .append(succeededByCommand ? "" : '\n');
            }

            Files.writeString(configFile, sb.toString());
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log(
                "Could not save config.yml! Please contact pim16aap2 and show him the following stacktrace:");
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
    public Locale locale()
    {
        return locale;
    }

    @Override
    public boolean skipAnimationsByDefault()
    {
        return skipAnimationsByDefault;
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
    public Set<Material> getMaterialBlacklist()
    {
        return Collections.unmodifiableSet(materialBlacklist);
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

    public Material getGuiMaterial(StructureType type)
    {
        return structureTypeGuiMaterials.getOrDefault(type, DEFAULT_MATERIAL);
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

    public boolean isHookEnabled(IProtectionHookSpigotSpecification spec)
    {
        return enabledProtectionHooks.contains(spec);
    }

    public List<String> getCommandAliases()
    {
        return Collections.unmodifiableList(commandAliases);
    }

    /**
     * Represents a class that attempts to parse a list of materials represented as Strings into a list of Materials.
     * <p>
     * See {@link #verifyMaterials(Collection, Set)}.
     */
    private static final class MaterialVerifier implements ConfigEntry.ITestValue<Collection<String>>
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
        public Collection<String> test(Collection<String> input)
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
        private static Collection<String> verifyMaterials(Collection<String> input, Set<Material> output)
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
                        log.atWarning().log("Failed to add material: \"%s\". Only solid materials are allowed!", str);
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
