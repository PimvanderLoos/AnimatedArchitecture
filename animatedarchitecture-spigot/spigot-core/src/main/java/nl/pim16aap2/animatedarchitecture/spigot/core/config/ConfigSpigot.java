package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import dagger.Lazy;
import lombok.EqualsAndHashCode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import lombok.ToString;
import lombok.experimental.Delegate;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.config.AbstractConfig;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.spigot.core.hooks.ProtectionHookManagerSpigot;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.Collection;

/**
 * Represents the configuration of the plugin for the Spigot platform.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Singleton
@CustomLog
final class ConfigSpigot extends AbstractConfig implements IConfigSpigot, IDebuggable
{
    @ToString.Exclude
    private final JavaPlugin plugin;

    private volatile boolean skipPrintInfo = true;

    @Delegate
    private volatile GeneralSectionSpigot.Result generalSectionResult =
        GeneralSectionSpigot.Result.DEFAULT;

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
        Lazy<StructureTypeManager> lazyStructureTypeManager,
        Lazy<ProtectionHookManagerSpigot> lazyProtectionHookManager,
        @Named("pluginBaseDirectory") Path baseDir,
        DebuggableRegistry debuggableRegistry)
    {
        super(baseDir);

        this.plugin = plugin;

        super.addSections(
            new GeneralSectionSpigot(this::setGeneralSectionResult),
            new AnimationsSectionSpigot(this::setAnimationsSectionResult),
            new LimitsSectionSpigot(this::setLimitsSectionResult),
            new ProtectionHooksSectionSpigot(lazyProtectionHookManager, this::setProtectionHooksSectionResult),
            new StructuresSectionSpigot(lazyStructureTypeManager, this::setStructureSectionsResult),
            new LocaleSectionSpigot(this::setLocaleSectionResult),
            new CachingSectionSpigot(this::setCachingSectionResult),
            new LoggingSectionSpigot(this::setLoggingSectionResult)
        );

        restartableHolder.registerRestartable(this);
        debuggableRegistry.registerDebuggable(this);
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

        logMaterialsList("Power Block Types", powerblockTypes());
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

    private void setGeneralSectionResult(GeneralSectionSpigot.Result result)
    {
        this.generalSectionResult = result;
    }

    private void setAnimationsSectionResult(AnimationsSectionSpigot.Result result)
    {
        this.animationsSectionResult = result;
    }

    private void setLimitsSectionResult(LimitsSectionSpigot.Result result)
    {
        this.limitsSectionResult = result;
    }

    private void setProtectionHooksSectionResult(ProtectionHooksSectionSpigot.Result result)
    {
        this.protectionHooksSectionResult = result;
    }

    private void setStructureSectionsResult(StructuresSectionSpigot.Result result)
    {
        this.structuresSectionResult = result;
    }

    private void setLocaleSectionResult(LocaleSectionSpigot.Result result)
    {
        this.localeSectionResult = result;
    }

    private void setCachingSectionResult(CachingSectionSpigot.Result result)
    {
        this.cachingSectionResult = result;
    }

    private void setLoggingSectionResult(LoggingSectionSpigot.Result result)
    {
        this.loggingSectionResult = result;
    }

    @Override
    public String getDebugInformation()
    {
        return "Config: " + this;
    }
}
