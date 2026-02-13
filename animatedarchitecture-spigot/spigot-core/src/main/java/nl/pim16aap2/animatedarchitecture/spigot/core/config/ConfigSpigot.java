package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import dagger.Lazy;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Delegate;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.config.AbstractConfig;
import nl.pim16aap2.animatedarchitecture.core.config.StructureTypeConfigurationOption;
import nl.pim16aap2.animatedarchitecture.core.config.StructuresSection;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.spigot.core.hooks.ProtectionHookManagerSpigot;
import org.bukkit.Material;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

/**
 * Represents the configuration of the plugin for the Spigot platform.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Singleton
@CustomLog
@Setter(AccessLevel.PRIVATE)
final class ConfigSpigot extends AbstractConfig implements IConfigSpigot, IDebuggable
{
    static final int CONFIG_VERSION = 0;

    private static final Map<String, String> LEGACY_STRUCTURE_TYPE_KEYS = Map.ofEntries(
        Map.entry("animatedarchitecture:bigdoor", "bigdoor"),
        Map.entry("animatedarchitecture:clock", "clock"),
        Map.entry("animatedarchitecture:drawbridge", "drawbridge"),
        Map.entry("animatedarchitecture:flag", "flag"),
        Map.entry("animatedarchitecture:garagedoor", "garagedoor"),
        Map.entry("animatedarchitecture:portcullis", "portcullis"),
        Map.entry("animatedarchitecture:revolvingdoor", "revolvingdoor"),
        Map.entry("animatedarchitecture:slidingdoor", "slidingdoor"),
        Map.entry("animatedarchitecture:windmill", "windmill")
    );

    private volatile boolean skipPrintInfo = true;

    @Delegate
    private volatile GeneralSectionSpigot.Result generalSectionResult =
        GeneralSectionSpigot.Result.DEFAULT;

    @Delegate
    private volatile RedstoneSectionSpigot.Result redstoneSectionResult =
        RedstoneSectionSpigot.Result.DEFAULT;

    @Delegate
    private volatile AnimationsSectionSpigot.Result animationsSectionResult =
        AnimationsSectionSpigot.Result.DEFAULT;

    @Delegate
    private volatile LimitsSectionSpigot.Result limitsSectionResult =
        LimitsSectionSpigot.Result.DEFAULT;

    @Delegate
    private volatile ProtectionHooksSectionSpigot.Result protectionHooksSectionResult =
        ProtectionHooksSectionSpigot.Result.DEFAULT;

    @Delegate
    private volatile StructuresSectionSpigot.Result structuresSectionResult =
        StructuresSectionSpigot.Result.DEFAULT;

    @Delegate
    private volatile LocaleSectionSpigot.Result localeSectionResult =
        LocaleSectionSpigot.Result.DEFAULT;

    @Delegate
    private volatile CachingSectionSpigot.Result cachingSectionResult =
        CachingSectionSpigot.Result.DEFAULT;

    @Delegate
    private volatile LoggingSectionSpigot.Result loggingSectionResult =
        LoggingSectionSpigot.Result.DEFAULT;

    private final Lazy<StructureTypeManager> lazyStructureTypeManager;

    @Inject
    public ConfigSpigot(
        RestartableHolder restartableHolder,
        Lazy<StructureTypeManager> lazyStructureTypeManager,
        Lazy<ProtectionHookManagerSpigot> lazyProtectionHookManager,
        @Named("pluginBaseDirectory") Path baseDir,
        DebuggableRegistry debuggableRegistry)
    {
        super(baseDir);
        this.lazyStructureTypeManager = lazyStructureTypeManager;

        super.addSections(
            new GeneralSectionSpigot(this::setGeneralSectionResult),
            new RedstoneSectionSpigot(this::setRedstoneSectionResult),
            new AnimationsSectionSpigot(this::setAnimationsSectionResult),
            new LimitsSectionSpigot(this::setLimitsSectionResult),
            new ProtectionHooksSectionSpigot(lazyProtectionHookManager, this::setProtectionHooksSectionResult),
            new StructuresSectionSpigot(lazyStructureTypeManager, this::setStructuresSectionResult),
            new LocaleSectionSpigot(this::setLocaleSectionResult),
            new CachingSectionSpigot(this::setCachingSectionResult),
            new LoggingSectionSpigot(this::setLoggingSectionResult)
        );

        restartableHolder.registerRestartable(this);
        debuggableRegistry.registerDebuggable(this);
    }

    private void setStructuresSectionResult(StructuresSectionSpigot.Result result)
    {
        this.structuresSectionResult = result;

        this.lazyStructureTypeManager.get()
            .updateEnabledStatusForStructureTypes(result::isEnabled);
    }

    @Override
    public synchronized void initialize()
    {
        super.parseConfig(skipPrintInfo);
        printResults();
    }

    /**
     * Reloads the configuration.
     */
    @Override
    public void reloadConfig()
    {
        initialize();
    }

    private void printResults()
    {
        /* The config is initialized twice on startup to resolve circular dependencies.
         *
         * Once as part of the initialization process and once after the initialization is complete.
         *
         * This is done because the config needs other classes to provide data for the config (e.g. registered structure
         * types), which is not yet available during the first initialization.
         *
         * However, those other classes _do_ need information from the config to initialize themselves.
         */
        if (skipPrintInfo)
        {
            skipPrintInfo = false;
            return;
        }

        logMaterialsList("Powerblock Types", powerblockTypes());
        logMaterialsList("Blacklisted Materials", materialBlacklist());
    }

    private void logMaterialsList(String title, Collection<Material> materials)
    {
        if (materials.isEmpty())
        {
            log.atInfo().log("%s: []", title);
            return;
        }

        log.atInfo().log("%s:", title);
        materials.forEach(material -> log.atInfo().log(" - %s", material.name()));
    }

    @Override
    public String getDebugInformation()
    {
        return "Config: " + this;
    }

    @Override
    protected void addTransformations(ConfigurationTransformation.VersionedBuilder builder)
    {
        builder.makeVersion(2, transformationBuilder ->
            transformationBuilder.addAction(NodePath.path(StructuresSection.SECTION_TITLE), (path, value) ->
            {
                if (!value.isMap())
                    return null;

                for (final var entry : LEGACY_STRUCTURE_TYPE_KEYS.entrySet())
                {
                    final var oldNode = value.node(entry.getKey());
                    if (oldNode.virtual())
                        continue;

                    final var newNode = value.node(entry.getValue());
                    if (newNode.virtual())
                    {
                        newNode.node(StructureTypeConfigurationOption.ENABLED.name()).set(true);

                        for (final var childEntry : oldNode.childrenMap().entrySet())
                            newNode.node(childEntry.getKey()).from(childEntry.getValue());
                    }

                    value.removeChild(entry.getKey());
                }

                return null;
            })
        );
    }

    @Override
    protected int initialVersion()
    {
        return CONFIG_VERSION;
    }
}
