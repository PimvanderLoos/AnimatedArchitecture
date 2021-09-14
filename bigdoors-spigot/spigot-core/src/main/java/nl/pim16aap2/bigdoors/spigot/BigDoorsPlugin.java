package nl.pim16aap2.bigdoors.spigot;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.spigot.listeners.BackupCommandListener;
import nl.pim16aap2.bigdoors.spigot.listeners.LoginMessageListener;
import nl.pim16aap2.bigdoors.spigot.managers.UpdateManager;
import nl.pim16aap2.bigdoors.spigot.util.DebugReporterSpigot;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.LinkedHashSet;
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
public final class BigDoorsPlugin extends JavaPlugin implements IRestartable
{
    private final Set<JavaPlugin> registeredPlugins = Collections.synchronizedSet(new LinkedHashSet<>());
    private final BigDoorsSpigotComponent bigDoorsSpigotComponent;
    private final RestartableHolder restartableHolder;
    private final long mainThreadId;
    private final IPLogger logger;

    private @Nullable BigDoorsSpigotPlatform bigDoorsSpigotPlatform;

    private boolean successfulInit;
    private boolean initialized = false;

    @Getter
    private @Nullable String initErrorMessage = null;

    public BigDoorsPlugin()
    {
        mainThreadId = Thread.currentThread().getId();
        restartableHolder = new RestartableHolder();

        bigDoorsSpigotComponent = DaggerBigDoorsSpigotComponent
            .builder()
            .setPlugin(this)
            .setRestartableHolder(restartableHolder)
            .build();
        logger = bigDoorsSpigotComponent.getLogger();
    }

    /**
     * Gets the {@link IBigDoorsPlatform} implementation for Spigot.
     *
     * @param javaPlugin
     *     The plugin requesting access.
     * @return The {@link BigDoorsSpigotPlatform} if it was initialized properly.
     */
    @SuppressWarnings("unused")
    public @Nullable BigDoorsSpigotPlatform getBigDoorsSpigotPlatform(JavaPlugin javaPlugin)
    {
        registeredPlugins.add(javaPlugin);
        return bigDoorsSpigotPlatform;
    }

    @Nullable BigDoorsSpigotPlatform getBigDoorsSpigotPlatform()
    {
        return bigDoorsSpigotPlatform;
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
        Bukkit.getLogger().setLevel(Level.FINER);

        // onEnable may be called more than once during the lifetime of the plugin.
        // As such, we make sure to initialize the platform just once and then
        // restart it on all onEnable calls after the first one, provided it was
        // initialized properly.
        if (!initialized)
            bigDoorsSpigotPlatform = initPlatform();
        else if (bigDoorsSpigotPlatform != null)
            restart();
        else
            logger.severe("Failed to enable BigDoors: Platform could not be initialized!");

        initialized = true;
    }

    @Override
    public void onDisable()
    {
        shutdown();
    }

    private @Nullable BigDoorsSpigotPlatform initPlatform()
    {
        try
        {
            final BigDoorsSpigotPlatform platform =
                new BigDoorsSpigotPlatform(bigDoorsSpigotComponent, this, mainThreadId);
            successfulInit = true;
            logger.info("Successfully enabled BigDoors " + getDescription().getVersion());
            return platform;
        }
        catch (Exception e)
        {
            logger.logThrowable(e, "Failed to initialize BigDoors' Spigot platform!");
            initErrorMessage = e.getMessage();
            onInitFailure();
            return null;
        }
    }

    private void onInitFailure()
    {
        shutdown();
        new BackupCommandListener(this, logger, initErrorMessage);
        registerFailureLoginListener();
        logger.logMessage(Level.WARNING, new DebugReporterSpigot(this, logger, null, null, null, null).getDump());
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
