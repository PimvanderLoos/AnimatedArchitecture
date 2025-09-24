package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import nl.altindag.log.LogCaptor;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.CollectionsUtil;
import nl.pim16aap2.animatedarchitecture.spigot.core.hooks.ProtectionHookManagerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.hooks.bundle.AbstractProtectionHookSpecification;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigotSpecification;
import nl.pim16aap2.animatedarchitecture.structures.bigdoor.StructureTypeBigDoor;
import nl.pim16aap2.animatedarchitecture.structures.clock.StructureTypeClock;
import nl.pim16aap2.animatedarchitecture.structures.drawbridge.StructureTypeDrawbridge;
import nl.pim16aap2.animatedarchitecture.structures.flag.StructureTypeFlag;
import nl.pim16aap2.animatedarchitecture.structures.garagedoor.StructureTypeGarageDoor;
import nl.pim16aap2.animatedarchitecture.structures.portcullis.StructureTypePortcullis;
import nl.pim16aap2.animatedarchitecture.structures.revolvingdoor.StructureTypeRevolvingDoor;
import nl.pim16aap2.animatedarchitecture.structures.slidingdoor.StructureTypeSlidingDoor;
import nl.pim16aap2.animatedarchitecture.structures.windmill.StructureTypeWindmill;
import nl.pim16aap2.testing.MockInjector;
import nl.pim16aap2.testing.annotations.FileSystemTest;
import nl.pim16aap2.testing.annotations.WithLogCapture;
import org.bukkit.Material;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nl.pim16aap2.testing.assertions.LogCaptorAssert.assertThatLogCaptor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@WithLogCapture
@ExtendWith(MockitoExtension.class)
class ConfigSpigotTest
{

    @FileSystemTest
    void initialize_shouldCreateConfig(Path rootDirectory)
        throws Exception
    {
        // setup
        final var availableProtectionHooks = AbstractProtectionHookSpecification.DEFAULT_HOOK_DEFINITIONS;
        final ProtectionHookManagerSpigot protectionHookManager = mock();
        when(protectionHookManager
            .getRegisteredHookDefinitions())
            .thenReturn(availableProtectionHooks.stream()
                .collect(Collectors.toMap(IProtectionHookSpigotSpecification::getName, Function.identity())));

        final List<StructureType> structureTypes = List.of(
            StructureTypeBigDoor.get(),
            StructureTypeClock.get(),
            StructureTypeDrawbridge.get(),
            StructureTypeFlag.get(),
            StructureTypeGarageDoor.get(),
            StructureTypePortcullis.get(),
            StructureTypeRevolvingDoor.get(),
            StructureTypeSlidingDoor.get(),
            StructureTypeWindmill.get()
        );
        final StructureTypeManager structureTypeManager = mock();
        when(structureTypeManager
            .getRegisteredStructureTypes())
            .thenReturn(new HashSet<>(structureTypes));

        final ConfigSpigot config = newConfig(rootDirectory, protectionHookManager, structureTypeManager);

        // execute
        config.initialize();

        // verify
        final List<String> lines = Files.readAllLines(config.configPath());

        assertThat(lines)
            .contains(
                "  resource_pack_enabled: %b".formatted(GeneralSectionSpigot.DEFAULT_RESOURCE_PACK_ENABLED),
                "  material_blacklist: []",
                "  allow_redstone: %b".formatted(RedstoneSectionSpigot.DEFAULT_ALLOW_REDSTONE),
                "  powerblock_types:",
                "  load_chunks_for_toggle: %b".formatted(AnimationsSectionSpigot.DEFAULT_LOAD_CHUNKS_FOR_TOGGLE),
                "  skip_animations_by_default: %b".formatted(
                    AnimationsSectionSpigot.DEFAULT_SKIP_ANIMATIONS_BY_DEFAULT),
                "  max_structure_count: %d".formatted(LimitsSectionSpigot.DEFAULT_MAX_STRUCTURE_COUNT),
                "  max_structure_size: %d".formatted(LimitsSectionSpigot.DEFAULT_MAX_STRUCTURE_SIZE),
                "  max_blocks_to_move: %d".formatted(LimitsSectionSpigot.DEFAULT_MAX_BLOCKS_TO_MOVE),
                "  max_powerblock_distance: %d".formatted(LimitsSectionSpigot.DEFAULT_MAX_POWERBLOCK_DISTANCE),
                "  max_block_speed: %.1f".formatted(LimitsSectionSpigot.DEFAULT_MAX_BLOCK_SPEED),
                "  locale: %s".formatted(LocaleSectionSpigot.DEFAULT_LOCALE),
                "  allow_client_locale: %b".formatted(LocaleSectionSpigot.DEFAULT_ALLOW_CLIENT_LOCALE),
                "  powerblock_cache_timeout: %d".formatted(CachingSectionSpigot.DEFAULT_POWERBLOCK_CACHE_TIMEOUT),
                "  head_cache_timeout: %d".formatted(CachingSectionSpigot.DEFAULT_HEAD_CACHE_TIMEOUT),
                "  log_level: %s".formatted(LoggingSectionSpigot.DEFAULT_LOG_LEVEL.getName()),
                "  debug: %b".formatted(LoggingSectionSpigot.DEFAULT_DEBUG)
            );

        for (final var block : RedstoneSectionSpigot.DEFAULT_POWERBLOCK_TYPES)
            assertThat(lines)
                .contains("    - %s".formatted(block));

        for (final var hook : AbstractProtectionHookSpecification.DEFAULT_HOOK_DEFINITIONS)
            assertThat(lines)
                .containsSequence(
                    "  %s:".formatted(hook.getName()),
                    "    enabled: true"
                );

        for (final var entry : structureTypes)
        {
            assertThat(lines)
                .containsSequence(
                    "  %s:".formatted(entry.getFullKey()),
                    "    animation_speed_multiplier: 1.0",
                    "    price_formula: \"0\"",
                    "    gui_material: %s".formatted(StructureSubSectionSpigot.getDefaultGuiMaterial(entry))
                );
        }

        // Check that comments are written correctly
        assertThat(lines)
            .containsSequence(
                Stream.concat(
                    GeneralSectionSpigot.SECTION_COMMENT.lines().map(str -> "# " + str),
                    Stream.of(GeneralSectionSpigot.SECTION_TITLE + ":")
                ).toList()
            );
    }

    @FileSystemTest
    void initialize_shouldPrintResultsTheSecondTime(Path rootDirectory, LogCaptor logCaptor)
        throws Exception
    {
        // setup
        final String expectedPartialMessage = "Blacklisted Materials";
        logCaptor.setLogLevelToInfo();
        final ConfigSpigot config = newConfig(rootDirectory);

        // execute & verify
        config.initialize();
        assertThat(logCaptor.getInfoLogs()).doesNotMatch(entry -> entry.contains(expectedPartialMessage));

        config.initialize();

        assertThatLogCaptor(logCaptor)
            .atInfo()
            .singleWithMessageContaining(expectedPartialMessage);
    }

    @FileSystemTest
    void initialize_shouldReadExistingValues(Path rootDirectory)
        throws Exception
    {
        // setup
        // general
        final boolean resourcePackEnabled = !GeneralSectionSpigot.DEFAULT_RESOURCE_PACK_ENABLED;
        final List<Material> materialBlacklist = List.of(Material.DIAMOND_BLOCK, Material.STONE);
        final List<String> commandAliases = List.of("testCommand0", "testCommand1");

        // redstone
        final boolean allowRedstone = !RedstoneSectionSpigot.DEFAULT_ALLOW_REDSTONE;
        final List<Material> powerblockTypes = List.of(Material.GRASS_BLOCK, Material.DIRT);

        // animations
        final boolean loadChunksForToggle = !AnimationsSectionSpigot.DEFAULT_LOAD_CHUNKS_FOR_TOGGLE;
        final boolean skipAnimationsByDefault = !AnimationsSectionSpigot.DEFAULT_SKIP_ANIMATIONS_BY_DEFAULT;

        // limits
        final int maxStructureCount = LimitsSectionSpigot.DEFAULT_MAX_STRUCTURE_COUNT + 9;
        final int maxStructureSize = LimitsSectionSpigot.DEFAULT_MAX_STRUCTURE_SIZE + 900;
        final int maxBlocksToMove = LimitsSectionSpigot.DEFAULT_MAX_BLOCKS_TO_MOVE + 9000;
        final double maxBlockSpeed = LimitsSectionSpigot.DEFAULT_MAX_BLOCK_SPEED + 900.0D;
        final int maxPowerblockDistance = LimitsSectionSpigot.DEFAULT_MAX_POWERBLOCK_DISTANCE + 10;

        // protection hooks
        final var availableProtectionHooks = AbstractProtectionHookSpecification.DEFAULT_HOOK_DEFINITIONS;
        final var enabledProtectionHooks = new LinkedList<>(availableProtectionHooks);
        // Remove every second element starting from the end. So the hooks are enabled/disabled/enabled/...
        for (int idx = (enabledProtectionHooks.size() & ~1) - 1; idx >= 0; idx -= 2)
        {
            enabledProtectionHooks.remove(idx);
        }

        final ProtectionHookManagerSpigot protectionHookManager = mock();
        when(protectionHookManager
            .getRegisteredHookDefinitions())
            .thenReturn(availableProtectionHooks.stream()
                .collect(Collectors.toMap(IProtectionHookSpigotSpecification::getName, Function.identity())));

        // structures
        final String flagMovementFormula = "a ^ 2 + b ^ 2 = c ^ 2";
        final Map<StructureType, StructureEntry> structureEntries = new LinkedHashMap<>();
        structureEntries.put(StructureTypeBigDoor.get(), new StructureEntry(2.0, "bigdoor", Material.GOAT_HORN));
        structureEntries.put(StructureTypeClock.get(), new StructureEntry(3.0, "clock", Material.ACACIA_BOAT));
        structureEntries.put(StructureTypeDrawbridge.get(), new StructureEntry(4.0, "drawbridge", Material.APPLE));
        structureEntries.put(StructureTypeFlag.get(), new StructureEntry(5.0, "flag", Material.BIRCH_BOAT));
        structureEntries.put(StructureTypeGarageDoor.get(), new StructureEntry(6.0, "garagedoor", Material.CHEST));
        structureEntries.put(StructureTypePortcullis.get(), new StructureEntry(7.0, "portcullis", Material.DIAMOND));
        structureEntries.put(StructureTypeRevolvingDoor.get(), new StructureEntry(8.0, "revolvingdoor", Material.DIRT));
        structureEntries.put(StructureTypeSlidingDoor.get(), new StructureEntry(10.0, "slidingdoor", Material.FURNACE));
        structureEntries.put(StructureTypeWindmill.get(), new StructureEntry(9.0, "windmill", Material.EMERALD));

        final StructureTypeManager structureTypeManager = mock();
        when(structureTypeManager
            .getRegisteredStructureTypes())
            .thenReturn(new HashSet<>(structureEntries.keySet()));

        // locale
        final Locale locale = Locale.of("nl", "NL");
        final boolean allowClientLocale = !LocaleSectionSpigot.DEFAULT_ALLOW_CLIENT_LOCALE;

        // caching
        final int powerblockCacheTimeout = CachingSectionSpigot.DEFAULT_POWERBLOCK_CACHE_TIMEOUT + 10;
        final int headCacheTimeout = CachingSectionSpigot.DEFAULT_HEAD_CACHE_TIMEOUT + 20;

        // logging
        final Level logLevel = Level.SEVERE;
        final boolean debug = !LoggingSectionSpigot.DEFAULT_DEBUG;

        final ConfigSpigot config = newConfig(rootDirectory, protectionHookManager, structureTypeManager);

        final String content = """
            general:
              resource_pack_enabled: %b
              material_blacklist: %s
              command_aliases: %s
            redstone:
              allow_redstone: %b
              powerblock_types: %s
            animations:
              load_chunks_for_toggle: %b
              skip_animations_by_default: %b
            limits:
              max_structure_count: %d
              max_structure_size: %d
              max_blocks_to_move: %d
              max_powerblock_distance: %d
              max_block_speed: %.1f
            protection_hooks:
              GriefDefender:
                enabled: %b
              GriefPrevention:
                enabled: %b
              Lands:
                enabled: %b
              PlotSquared:
                enabled: %b
              RedProtect:
                enabled: %b
              Towny:
                enabled: %b
              WorldGuard:
                enabled: %b
            structures:
              animatedarchitecture:bigdoor:%s
              animatedarchitecture:clock:%s
              animatedarchitecture:drawbridge:%s
              animatedarchitecture:flag:%s
                movement_formula: "%s"
              animatedarchitecture:garagedoor:%s
              animatedarchitecture:portcullis:%s
              animatedarchitecture:revolvingdoor:%s
              animatedarchitecture:slidingdoor:%s
              animatedarchitecture:windmill:%s
            locale:
              locale: %s
              allow_client_locale: %b
            caching:
              powerblock_cache_timeout: %d
              head_cache_timeout: %d
            logging:
              log_level: %s
              debug: %b
            version: 0
            """.formatted(
            // general
            resourcePackEnabled,
            formatConfigEntryList(4, materialBlacklist),
            formatConfigEntryList(4, commandAliases),
            // redstone
            allowRedstone,
            formatConfigEntryList(4, powerblockTypes),
            // animations
            loadChunksForToggle,
            skipAnimationsByDefault,
            // limits
            maxStructureCount,
            maxStructureSize,
            maxBlocksToMove,
            maxPowerblockDistance,
            maxBlockSpeed,
            // protection hooks
            enabledProtectionHooks.contains(AbstractProtectionHookSpecification.GRIEF_DEFENDER),
            enabledProtectionHooks.contains(AbstractProtectionHookSpecification.GRIEF_PREVENTION),
            enabledProtectionHooks.contains(AbstractProtectionHookSpecification.LANDS),
            enabledProtectionHooks.contains(AbstractProtectionHookSpecification.PLOT_SQUARED),
            enabledProtectionHooks.contains(AbstractProtectionHookSpecification.RED_PROTECT),
            enabledProtectionHooks.contains(AbstractProtectionHookSpecification.TOWNY),
            enabledProtectionHooks.contains(AbstractProtectionHookSpecification.WORLD_GUARD),
            // structures
            structureEntries.get(StructureTypeBigDoor.get()).asConfigString(),
            structureEntries.get(StructureTypeClock.get()).asConfigString(),
            structureEntries.get(StructureTypeDrawbridge.get()).asConfigString(),
            structureEntries.get(StructureTypeFlag.get()).asConfigString(),
            flagMovementFormula,
            structureEntries.get(StructureTypeGarageDoor.get()).asConfigString(),
            structureEntries.get(StructureTypePortcullis.get()).asConfigString(),
            structureEntries.get(StructureTypeRevolvingDoor.get()).asConfigString(),
            structureEntries.get(StructureTypeSlidingDoor.get()).asConfigString(),
            structureEntries.get(StructureTypeWindmill.get()).asConfigString(),
            // locale
            locale,
            allowClientLocale,
            // caching
            powerblockCacheTimeout,
            headCacheTimeout,
            // logging
            logLevel.getName(),
            debug
        );

        Files.writeString(config.configPath(), content);


        // execute
        config.reloadConfig();


        // verify
        // general
        assertThat(config.resourcePackEnabled()).isEqualTo(resourcePackEnabled);
        assertThat(config.materialBlacklist()).containsExactlyInAnyOrderElementsOf(materialBlacklist);
        assertThat(config.commandAliases()).containsExactlyElementsOf(commandAliases);

        // redstone
        assertThat(config.allowRedstone()).isEqualTo(allowRedstone);
        assertThat(config.powerblockTypes()).containsExactlyInAnyOrderElementsOf(powerblockTypes);

        // animations
        assertThat(config.loadChunksForToggle()).isEqualTo(loadChunksForToggle);
        assertThat(config.skipAnimationsByDefault()).isEqualTo(skipAnimationsByDefault);

        // limits
        assertThat(config.maxStructureCount()).hasValue(maxStructureCount);
        assertThat(config.maxStructureSize()).hasValue(maxStructureSize);
        assertThat(config.maxBlocksToMove()).hasValue(maxBlocksToMove);
        assertThat(config.maxPowerblockDistance()).hasValue(maxPowerblockDistance);
        assertThat(config.maxBlockSpeed()).hasValue(maxBlockSpeed);

        // protection hooks
        for (final IProtectionHookSpigotSpecification hook : availableProtectionHooks)
        {
            assertThat(config.isProtectionHookEnabled(hook))
                .as("Protection hook '%s' is enabled", hook.getName())
                .isEqualTo(enabledProtectionHooks.contains(hook));
        }

        // structures
        assertThat(config.flagMovementFormula()).isEqualTo(flagMovementFormula);
        for (final var entry : structureEntries.entrySet())
        {
            final StructureType structureType = entry.getKey();
            final StructureEntry structureEntry = entry.getValue();

            assertThat(config.guiMaterial(structureType))
                .as("GUI material for structure type: %s", structureType)
                .isEqualTo(structureEntry.guiMaterial());

            assertThat(config.animationTimeMultiplier(structureType))
                .as("Animation speed multiplier for structure type: %s", structureType)
                .isEqualTo(structureEntry.animationSpeedMultiplier());

            assertThat(config.priceFormula(structureType))
                .as("Price formula for structure type: %s", structureType)
                .isEqualTo(structureEntry.priceFormula());
        }

        // locale
        assertThat(config.locale()).isEqualTo(locale);
        assertThat(config.allowClientLocale()).isEqualTo(allowClientLocale);

        // caching
        assertThat(config.powerblockCacheTimeout()).isEqualTo(powerblockCacheTimeout);
        assertThat(config.headCacheTimeout()).isEqualTo(headCacheTimeout);

        // logging
        assertThat(config.logLevel()).isEqualTo(logLevel);
        assertThat(config.debug()).isEqualTo(debug);
    }

    private static String formatConfigEntryList(int indent, List<?> entries)
    {
        return entries.stream()
            .map(entry -> " ".repeat(indent) + "- " + entry)
            .collect(Collectors.joining("\n", "\n", ""));
    }

    /**
     * Creates a new instance of {@link ConfigSpigot} with mocked dependencies.
     * <p>
     * The base directory path is set to a new directory named "pluginBaseDirectory" within the provided root
     * directory.
     *
     * @param rootDirectory
     *     The root directory where the plugin base directory will be created.
     * @return A new instance of {@link ConfigSpigot} with mocked dependencies.
     *
     * @throws Exception
     *     If an error occurs while creating the plugin base directory.
     */
    private static ConfigSpigot newConfig(Path rootDirectory, Object... additionalMocks)
        throws Exception
    {
        final Path pluginBaseDirectory = Files.createDirectory(rootDirectory.resolve("pluginBaseDirectory"));

        final Object[] additionalParameters =
            CollectionsUtil.concat(new Object[]{pluginBaseDirectory}, additionalMocks);

        return new MockInjector<>(ConfigSpigot.class).createInstance(additionalParameters);
    }

    private record StructureEntry(
        double animationSpeedMultiplier,
        String priceFormula,
        Material guiMaterial
    )
    {
        public String asConfigString()
        {
            return """
                
                    animation_speed_multiplier: %f
                    price_formula: "%s"
                    gui_material: %s
                """.formatted(
                animationSpeedMultiplier,
                priceFormula,
                guiMaterial.name()
            );
        }
    }
}
