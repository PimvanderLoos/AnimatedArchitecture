package nl.pim16aap2.animatedarchitecture.spigot.core;

import com.google.common.flogger.FluentLogger;
import lombok.AccessLevel;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatformProvider;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.spigot.core.config.ConfigSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.DebugReporterSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.core.listeners.BackupCommandListener;
import nl.pim16aap2.animatedarchitecture.spigot.core.listeners.LoginMessageListener;
import nl.pim16aap2.animatedarchitecture.spigot.core.logging.ConsoleAppender;
import nl.pim16aap2.util.logging.LogBackConfigurator;
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

    private @Nullable IAnimatedArchitectureSpigotPlatform animatedArchitectureSpigotPlatform;
    // Avoid creating new Optional objects for every invocation; the result is going to be the same anyway.
    private volatile Optional<IAnimatedArchitecturePlatform> optionalPlatform = Optional.empty();

    private boolean successfulInit;
    private boolean initialized = false;

    @Getter
    private @Nullable String initErrorMessage = null;

    public AnimatedArchitecturePlugin()
    {
        LOG_BACK_CONFIGURATOR.setLogFile(getDataFolder().toPath().resolve("log.txt")).apply();

        mainThreadId = Thread.currentThread().threadId();
        restartableHolder = new RestartableHolder();

        animatedArchitectureSpigotComponent = DaggerAnimatedArchitectureSpigotComponent
            .builder()
            .setPlugin(this)
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
     * @return The {@link IAnimatedArchitectureSpigotPlatform} if it was initialized properly.
     */
    @SuppressWarnings("unused")
    public Optional<IAnimatedArchitectureSpigotPlatform> getAnimatedArchitectureSpigotPlatform(JavaPlugin javaPlugin)
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
    }

    private void initCommands(IAnimatedArchitectureSpigotPlatform animatedArchitectureSpigotPlatform)
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
    private @Nullable IAnimatedArchitectureSpigotPlatform initPlatform()
    {
        try
        {
            final IAnimatedArchitectureSpigotPlatform platform =
                new IAnimatedArchitectureSpigotPlatform(animatedArchitectureSpigotComponent, this);
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
        log.atWarning().log("%s", new DebugReporterSpigot(this, this, null, new DebuggableRegistry()));
        successfulInit = false;
        restartableHolder.shutDown();
    }

    /**
     * Registers a {@link LoginMessageListener} outside of the Dagger object graph.
     * <p>
     * This listener will inform admins about the issues that came up during plugin initialization.
     */
    private void registerFailureLoginListener()
    {
        new LoginMessageListener(this, null);
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
