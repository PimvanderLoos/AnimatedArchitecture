package nl.pim16aap2.animatedarchitecture.spigot.core;

import com.google.common.flogger.FluentLogger;
import lombok.AccessLevel;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatformProvider;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.util.updater.UpdateChecker;
import nl.pim16aap2.animatedarchitecture.core.util.versioning.ProjectVersion;
import nl.pim16aap2.animatedarchitecture.spigot.core.config.ConfigSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.DebugReporterSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.TextFactorySpigot;
import nl.pim16aap2.animatedarchitecture.spigot.core.listeners.BackupCommandListener;
import nl.pim16aap2.animatedarchitecture.spigot.core.listeners.LoginMessageListener;
import nl.pim16aap2.animatedarchitecture.spigot.core.logging.ConsoleAppender;
import nl.pim16aap2.util.logging.LogBackConfigurator;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

/**
 * Represents the base {@link JavaPlugin} for AnimatedArchitecture.
 * <p>
 * This is the entry point of the Spigot platform.
 * <p>
 * Refer to {@link nl.pim16aap2.animatedarchitecture.spigot.core} for more information on how to interact with this
 * plugin.
 *
 * @author Pim
 */
@Singleton
public final class AnimatedArchitecturePlugin extends JavaPlugin implements IAnimatedArchitecturePlatformProvider
{
    private static final LogBackConfigurator LOG_BACK_CONFIGURATOR =
        new LogBackConfigurator().addAppender("SpigotConsoleRedirect", ConsoleAppender.class.getName())
                                 .setLevel(Level.FINEST)
                                 .apply();
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final FluentLogger log;

    static
    {
        final String propName = "flogger.backend_factory";
        final @Nullable String oldProp = System.getProperty(propName);
        System.setProperty(propName, "com.google.common.flogger.backend.slf4j.Slf4jBackendFactory#getInstance");
        log = FluentLogger.forEnclosingClass();
        if (oldProp != null)
            System.setProperty(propName, oldProp);
    }

    private final Set<JavaPlugin> registeredPlugins = Collections.synchronizedSet(new LinkedHashSet<>());
    private final AnimatedArchitectureSpigotComponent animatedArchitectureSpigotComponent;
    private final RestartableHolder restartableHolder;

    @Getter(AccessLevel.PACKAGE)
    private final long mainThreadId;

    private @Nullable AnimatedArchitectureSpigotPlatform animatedArchitectureSpigotPlatform;
    // Avoid creating new Optional objects for every invocation; the result is going to be the same anyway.
    private volatile Optional<IAnimatedArchitecturePlatform> optionalPlatform = Optional.empty();

    private boolean successfulInit;
    private boolean initialized = false;

    @Getter
    private @Nullable String initErrorMessage = null;

    private final UpdateChecker updateChecker;

    public AnimatedArchitecturePlugin()
    {
        LOG_BACK_CONFIGURATOR.setLogFile(getDataFolder().toPath().resolve("log.txt")).apply();

        mainThreadId = Thread.currentThread().threadId();
        restartableHolder = new RestartableHolder();

        final ProjectVersion projectVersion = ProjectVersion.parse(getDescription().getVersion());
        this.updateChecker = new UpdateChecker(projectVersion);

        animatedArchitectureSpigotComponent = DaggerAnimatedArchitectureSpigotComponent
            .builder()
            .setPlugin(this)
            .setProjectVersion(projectVersion)
            .setUpdateChecker(this.updateChecker)
            .setRestartableHolder(restartableHolder)
            .build();

        // Update logger again because the config *should* be available now.
        updateLogger();
    }

    /**
     * Tries to update the logger using {@link IConfig#logLevel()}.
     * <p>
     * If the config is not available for some reason, the log level defaults to {@link Level#ALL}.
     */
    private void updateLogger()
    {
        try
        {
            setLogLevel(animatedArchitectureSpigotComponent.getConfig().logLevel());
        }
        catch (Exception e)
        {
            setLogLevel(Level.ALL);
            log.atSevere().withCause(e).log("Failed to read config! Defaulting to logging everything!");
        }
    }

    private void setLogLevel(Level level)
    {
        LOG_BACK_CONFIGURATOR
            .setLogFile(getDataFolder().toPath().resolve("log.txt"))
            .setLevel(level)
            .apply();
    }

    /**
     * Gets the {@link IAnimatedArchitecturePlatform} implementation for Spigot.
     *
     * @param javaPlugin
     *     The plugin requesting access.
     * @return The {@link AnimatedArchitectureSpigotPlatform} if it was initialized properly.
     */
    @SuppressWarnings("unused")
    public Optional<AnimatedArchitectureSpigotPlatform> getAnimatedArchitectureSpigotPlatform(JavaPlugin javaPlugin)
    {
        registeredPlugins.add(javaPlugin);
        return Optional.ofNullable(animatedArchitectureSpigotPlatform);
    }

    /**
     * Retrieves all the plugins that have requested access to AnimatedArchitecture's internals.
     *
     * @return A set of all plugins with access to AnimatedArchitecture.
     */
    public Set<JavaPlugin> getRegisteredPlugins()
    {
        return Collections.unmodifiableSet(registeredPlugins);
    }

    @Override
    public void onEnable()
    {
        // onEnable may be called more than once during the lifetime of the plugin.
        // As such, we make sure to initialize the platform just once and then
        // restart it on all onEnable calls after the first one, provided it was
        // initialized properly.
        boolean firstInit = false;
        if (!initialized)
        {
            firstInit = true;
            initStats();
            animatedArchitectureSpigotPlatform = initPlatform();
        }
        initialized = true;

        if (animatedArchitectureSpigotPlatform == null)
        {
            log.atSevere().log("Failed to enable AnimatedArchitecture: Platform could not be initialized!");
            return;
        }

        LOG_BACK_CONFIGURATOR
            .setLevel(animatedArchitectureSpigotPlatform.getAnimatedArchitectureConfig().logLevel())
            .apply();
        restartableHolder.initialize();

        // Rewrite the config after everything has been loaded to ensure all
        // extensions/addons have their hooks in.
        ((ConfigSpigot) animatedArchitectureSpigotPlatform.getAnimatedArchitectureConfig()).rewriteConfig(false);

        if (firstInit)
            initCommands(animatedArchitectureSpigotPlatform);

        scheduleUpdateChecker(firstInit);
    }

    private void scheduleUpdateChecker(boolean firstInit)
    {
        try
        {
            final long period = 864_000L; // 12 hours
            // The update checker already runs before the first init, so we do not
            // need to check again so soon.
            final long delay = firstInit ? 0 : period;
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, this.updateChecker::checkForUpdates, delay, period);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to schedule update checker!");
        }
    }

    private void initCommands(AnimatedArchitectureSpigotPlatform animatedArchitectureSpigotPlatform)
    {
        try
        {
            animatedArchitectureSpigotPlatform.getCommandListener().init();
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to initialize command listener!");
            onInitFailure();
        }
    }

    @Override
    public void onDisable()
    {
        restartableHolder.shutDown();
    }

    // Synchronized to ensure visibility of the platform.
    private @Nullable AnimatedArchitectureSpigotPlatform initPlatform()
    {
        try
        {
            final AnimatedArchitectureSpigotPlatform platform =
                new AnimatedArchitectureSpigotPlatform(animatedArchitectureSpigotComponent);
            successfulInit = true;
            log.atInfo().log("Successfully enabled AnimatedArchitecture %s", getDescription().getVersion());
            optionalPlatform = Optional.of(platform);
            return platform;
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to initialize AnimatedArchitecture's Spigot platform!");
            initErrorMessage = e.getMessage();
            onInitFailure();
            return null;
        }
    }

    private void onInitFailure()
    {
        restartableHolder.shutDown();
        new BackupCommandListener(this, initErrorMessage);
        registerFailureLoginListener();
        log.atWarning().log("%s", new DebugReporterSpigot(this, this, new DebuggableRegistry()));
        successfulInit = false;
        restartableHolder.shutDown();
    }

    private void initStats()
    {
        try
        {
            new Metrics(this, 18_011);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to enable stats! :(");
        }
    }

    public ClassLoader getPluginClassLoader()
    {
        return super.getClassLoader();
    }

    /**
     * Registers a {@link LoginMessageListener} outside of the Dagger object graph.
     * <p>
     * This listener will inform admins about the issues that came up during plugin initialization.
     */
    private void registerFailureLoginListener()
    {
        new LoginMessageListener(this, new TextFactorySpigot(), null, null);
    }

    @Override
    public Optional<IAnimatedArchitecturePlatform> getPlatform()
    {
        return optionalPlatform;
    }

    @SuppressWarnings("unused")
    public void restart()
    {
        if (!successfulInit)
            return;
        restartableHolder.restart();
    }
}
