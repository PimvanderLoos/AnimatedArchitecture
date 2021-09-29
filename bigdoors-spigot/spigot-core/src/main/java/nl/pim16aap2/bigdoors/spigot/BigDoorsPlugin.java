package nl.pim16aap2.bigdoors.spigot;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatformProvider;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.spigot.listeners.BackupCommandListener;
import nl.pim16aap2.bigdoors.spigot.listeners.LoginMessageListener;
import nl.pim16aap2.bigdoors.spigot.logging.ConsoleAppender;
import nl.pim16aap2.bigdoors.spigot.managers.UpdateManager;
import nl.pim16aap2.bigdoors.spigot.util.DebugReporterSpigot;
import nl.pim16aap2.logging.LogBackConfigurator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

/**
 * Represents the base {@link JavaPlugin} for BigDoors.
 * <p>
 * This is the entry point of the Spigot platform.
 *
 * @author Pim
 */
@Singleton
@Flogger
public final class BigDoorsPlugin extends JavaPlugin implements IRestartable, IBigDoorsPlatformProvider
{
    private static final LogBackConfigurator LOG_BACK_CONFIGURATOR =
        new LogBackConfigurator().addAppender("SpigotConsoleRedirect", ConsoleAppender.class.getName())
                                 .setLevel(Level.FINEST)
                                 .apply();

    private final Set<JavaPlugin> registeredPlugins = Collections.synchronizedSet(new LinkedHashSet<>());
    private final BigDoorsSpigotComponent bigDoorsSpigotComponent;
    private final RestartableHolder restartableHolder;

    @Getter(AccessLevel.PACKAGE)
    private final long mainThreadId;

    private @Nullable BigDoorsSpigotPlatform bigDoorsSpigotPlatform;
    // Avoid creating new Optional objects for every invocation; the result is going to be the same anyway.
    private volatile Optional<IBigDoorsPlatform> optionalBigDoorsSpigotPlatform = Optional.empty();

    private boolean successfulInit;
    private boolean initialized = false;

    @Getter
    private @Nullable String initErrorMessage = null;

    public BigDoorsPlugin()
    {
        LOG_BACK_CONFIGURATOR.setLogFile(getDataFolder().toPath().resolve("log.txt")).apply();

        mainThreadId = Thread.currentThread().getId();
        restartableHolder = new RestartableHolder();

        bigDoorsSpigotComponent = DaggerBigDoorsSpigotComponent
            .builder()
            .setPlugin(this)
            .setRestartableHolder(restartableHolder)
            .build();

        // Update logger again because the config *should* be available now.
        updateLogger();
    }

    /**
     * Tries to update the logger using {@link IConfigLoader#logLevel()}.
     * <p>
     * If the config is not available for some reason, the log level defaults to {@link Level#ALL}.
     */
    private void updateLogger()
    {
        Level level;
        try
        {
            level = bigDoorsSpigotComponent.getConfig().logLevel();
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to read config! Defaulting to logging everything!");
            level = Level.ALL;
        }
        LOG_BACK_CONFIGURATOR
            .setLogFile(getDataFolder().toPath().resolve("log.txt"))
            .setLevel(level)
            .apply();
    }

    /**
     * Gets the {@link IBigDoorsPlatform} implementation for Spigot.
     *
     * @param javaPlugin
     *     The plugin requesting access.
     * @return The {@link BigDoorsSpigotPlatform} if it was initialized properly.
     */
    @SuppressWarnings("unused")
    public Optional<BigDoorsSpigotPlatform> getBigDoorsSpigotPlatform(JavaPlugin javaPlugin)
    {
        registeredPlugins.add(javaPlugin);
        return Optional.ofNullable(bigDoorsSpigotPlatform);
    }

    /**
     * Retrieves all the plugins that have requested access to BigDoors' internals.
     *
     * @return A set of all plugins with access to BigDoors.
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
        if (!initialized)
            bigDoorsSpigotPlatform = initPlatform();
        else if (bigDoorsSpigotPlatform != null)
            restart();
        else
            log.at(Level.SEVERE).log("Failed to enable BigDoors: Platform could not be initialized!");

        if (bigDoorsSpigotPlatform != null)
            LOG_BACK_CONFIGURATOR.setLevel(bigDoorsSpigotPlatform.getBigDoorsConfig().logLevel()).apply();

        initialized = true;
    }

    @Override
    public void onDisable()
    {
        shutdown();
    }

    // Synchronized to ensure visibility of the platform.
    private synchronized @Nullable BigDoorsSpigotPlatform initPlatform()
    {
        try
        {
            final BigDoorsSpigotPlatform platform =
                new BigDoorsSpigotPlatform(bigDoorsSpigotComponent, this);
            successfulInit = true;
            log.at(Level.INFO).log("Successfully enabled BigDoors %s", getDescription().getVersion());
            return platform;
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to initialize BigDoors' Spigot platform!");
            initErrorMessage = e.getMessage();
            onInitFailure();
            return null;
        }
    }

    private void onInitFailure()
    {
        shutdown();
        new BackupCommandListener(this, initErrorMessage);
        registerFailureLoginListener();
        log.at(Level.WARNING).log("%s", new DebugReporterSpigot(this, this, null, null, null));
        successfulInit = false;
    }

    /**
     * Registers a {@link LoginMessageListener} outside of the Dagger object graph.
     * <p>
     * This listener will inform admins about the issues that came up during plugin initialization.
     */
    private void registerFailureLoginListener()
    {
        @Nullable UpdateManager updateManager;
        try
        {
            updateManager = bigDoorsSpigotComponent.getUpdateManager();
        }
        catch (Exception e)
        {
            updateManager = null;
        }
        new LoginMessageListener(this, updateManager);
    }

    @Override
    public Optional<IBigDoorsPlatform> getPlatform()
    {
        if (optionalBigDoorsSpigotPlatform.isPresent() || bigDoorsSpigotPlatform == null)
            return optionalBigDoorsSpigotPlatform;
        return updatePlatform();
    }

    private synchronized Optional<IBigDoorsPlatform> updatePlatform()
    {
        if (optionalBigDoorsSpigotPlatform.isPresent())
            return optionalBigDoorsSpigotPlatform;
        return optionalBigDoorsSpigotPlatform = Optional.ofNullable(bigDoorsSpigotPlatform);
    }

    @Override
    public void restart()
    {
        if (!successfulInit)
            return;

        if (bigDoorsSpigotPlatform != null)
            bigDoorsSpigotPlatform.getBigDoorsConfig().restart();
        restartableHolder.restart();
    }

    @Override
    public void shutdown()
    {
        restartableHolder.shutdown();
    }
}
