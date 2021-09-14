package nl.pim16aap2.bigdoors.spigot;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.spigot.listeners.BackupCommandListener;
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

    private boolean validVersion;
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

        if (!validVersion)
            logger.severe("Failed to enable BigDoors: The current version is invalid!");
        else if (!initialized)
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
        catch (RuntimeException e)
        {
            logger.logThrowable(e, "Failed to initialize BigDoors' Spigot platform!");
            initErrorMessage = e.getMessage();
        }
        catch (Exception e)
        {
            logger.logThrowable(e, "Failed to initialize BigDoors' Spigot platform!");
        }
        onInitFailure();
        return null;
    }

    private void onInitFailure()
    {
        shutdown();
        new BackupCommandListener(this, logger, initErrorMessage);
        successfulInit = false;
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

//    /**
//     * Gets the message to send to admins and OPs when they log in. This message can contain all kinds of information,
//     * including but not limited to: The current build is a dev build, the plugin could not be initialized properly,
//     * an update is available.
//     *
//     * @return The message to send to admins and OPs when they log in.
//     */
//    public String getLoginMessage()
//    {
//        String ret = "";
//        if (Constants.DEV_BUILD)
//            ret += "[BigDoors] Warning: You are running a devbuild!\n";
//        if (!validVersion)
//            ret += "[BigDoors] Error: Trying to load the game on an invalid version! Plugin disabled!\n";
//        if (!successfulInit)
//            ret += "[BigDoors] Error: Failed to initialize the plugin! Some functions may not work as expected. " +
//                "Please contact pim16aap2! Don't forget to attach both the server log AND the BigDoors log!\n";
//        if (updateManager.updateAvailable())
//        {
//            if (getBigDoorsConfig().autoDLUpdate() && updateManager.hasUpdateBeenDownloaded())
//                ret += "[BigDoors] A new update (" + updateManager.getNewestVersion() +
//                    ") has been downloaded! "
//                    + "Restart your server to apply the update!\n";
//            else if (updateManager.updateAvailable())
//                ret += "[BigDoors] A new update is available: " + updateManager.getNewestVersion() + "\n";
//        }
//        return ret;
//    }
}
