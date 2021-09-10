package nl.pim16aap2.bigdoors.spigot.compatiblity;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.config.ConfigLoaderSpigot;
import nl.pim16aap2.bigdoors.spigot.managers.VaultManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Class that manages all objects of {@link IProtectionCompat}.
 *
 * @author Pim
 */
@Singleton
public final class ProtectionCompatManagerSpigot extends Restartable implements Listener, IProtectionCompatManager
{
    private final List<IProtectionCompat> protectionCompats;
    private final JavaPlugin plugin;
    private final IPLogger logger;
    private final @Nullable FakePlayerCreator fakePlayerCreator;
    private final VaultManager vaultManager;
    private final ConfigLoaderSpigot config;

    /**
     * Constructor of {@link ProtectionCompatManagerSpigot}.
     *
     * @param plugin
     *     The instance of {@link BigDoorsSpigot}.
     */
    @Inject
    public ProtectionCompatManagerSpigot(JavaPlugin plugin, IPLogger logger, IRestartableHolder holder,
                                         VaultManager vaultManager, ConfigLoaderSpigot config)
    {
        super(holder);
        this.plugin = plugin;
        this.logger = logger;
        this.vaultManager = vaultManager;
        this.config = config;

        @Nullable FakePlayerCreator fakePlayerCreatorTmp = null;
        try
        {
            fakePlayerCreatorTmp = new FakePlayerCreator(plugin, logger);
        }
        catch (NoSuchMethodException | ClassNotFoundException | NoSuchFieldException e)
        {
            logger.logThrowable(new IllegalStateException("Failed to construct FakePlayerCreator!", e));
        }
        fakePlayerCreator = fakePlayerCreatorTmp;
        protectionCompats = new ArrayList<>();
        restart();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Reinitialize all protection compats.
     */
    @Override
    public void restart()
    {
        shutdown();

        for (final Plugin p : plugin.getServer().getPluginManager().getPlugins())
            loadFromPluginName(p.getName());
    }

    @Override
    public void shutdown()
    {
        protectionCompats.clear();
    }

    /**
     * Check if a player is allowed to bypass the compatibility checks. Players can bypass the check if they are OP or
     * if they have the {@link Constants#COMPAT_BYPASS_PERMISSION} permission node.
     *
     * @param player
     *     The {@link Player} to check the permissions for.
     * @return True if the player can bypass the checks.
     */
    private boolean canByPass(Player player)
    {
        if (player.isOp())
            return true;

        // offline players don't have permissions, so use Vault if that's the case.
        if (!player.hasMetadata(FakePlayerCreator.FAKE_PLAYER_METADATA))
            return player.hasPermission(Constants.COMPAT_BYPASS_PERMISSION);
        return vaultManager.hasPermission(player, Constants.COMPAT_BYPASS_PERMISSION);
    }

    /**
     * Get an online player from a player {@link UUID} in a given world. If the player with the given UUID is not
     * online, a fake-online player is created.
     *
     * @param player
     *     The {@link IPPlayer}.
     * @param world
     *     The {@link World} the player is in.
     * @return An online {@link Player}. Either fake or real.
     *
     * @see FakePlayerCreator
     */
    private Optional<Player> getPlayer(IPPlayer player, World world)
    {
        @Nullable Player bukkitPlayer = Bukkit.getPlayer(player.getUUID());
        if (bukkitPlayer == null && fakePlayerCreator != null)
            bukkitPlayer = fakePlayerCreator.getFakePlayer(Bukkit.getOfflinePlayer(player.getUUID()), world)
                                            .orElse(null);
        return Optional.ofNullable(bukkitPlayer);
    }

    @Override
    public Optional<String> canBreakBlock(IPPlayer player, IPLocation pLoc)
    {
        if (protectionCompats.isEmpty())
            return Optional.empty();

        final Location loc = SpigotAdapter.getBukkitLocation(pLoc);
        if (loc.getWorld() == null)
            return Optional.of("InvalidWorld");

        final Optional<Player> fakePlayer = getPlayer(player, loc.getWorld());
        if (fakePlayer.isEmpty())
            return Optional.empty();

        if (canByPass(fakePlayer.get()))
            return Optional.empty();

        for (final IProtectionCompat compat : protectionCompats)
            try
            {
                if (!compat.canBreakBlock(fakePlayer.get(), loc))
                    return Optional.of(compat.getName());
            }
            catch (Exception e)
            {
                logger.logThrowable(e, "Failed to use \"" + compat.getName()
                    + "\"! Please send this error to pim16aap2:");
            }
        return Optional.empty();
    }

    @Override
    public Optional<String> canBreakBlocksBetweenLocs(IPPlayer player, Vector3Di pos1, Vector3Di pos2, IPWorld world)
    {
        if (protectionCompats.isEmpty())
            return Optional.empty();

        final IPLocationFactory locationFactory = BigDoors.get().getPlatform().getPLocationFactory();

        final Location loc1 = SpigotAdapter.getBukkitLocation(locationFactory.create(world, pos1));
        if (loc1.getWorld() == null)
            return Optional.of("InvalidWorld");

        final Location loc2 = SpigotAdapter.getBukkitLocation(locationFactory.create(world, pos2));
        if (loc2.getWorld() == null)
            return Optional.of("InvalidWorld");


        final Optional<Player> fakePlayer = getPlayer(player, loc1.getWorld());
        if (fakePlayer.isEmpty())
            return Optional.empty();

        if (canByPass(fakePlayer.get()))
            return Optional.empty();

        for (final IProtectionCompat compat : protectionCompats)
            try
            {
                if (!compat.canBreakBlocksBetweenLocs(fakePlayer.get(), loc1, loc2))
                    return Optional.of(compat.getName());
            }
            catch (Exception e)
            {
                logger.logThrowable(e, "Failed to use \"" + compat.getName()
                    + "\"! Please send this error to pim16aap2:");
            }
        return Optional.empty();
    }

    /**
     * Check if an {@link IProtectionCompat} is already loaded.
     *
     * @param compatClass
     *     The class of the {@link IProtectionCompat} to check.
     * @return True if the compat has already been loaded.
     */
    private boolean protectionAlreadyLoaded(Class<? extends IProtectionCompat> compatClass)
    {
        for (final IProtectionCompat compat : protectionCompats)
            if (compat.getClass().equals(compatClass))
                return true;
        return false;
    }

    /**
     * Add a {@link IProtectionCompat} to the list of loaded compats if it loaded successfully.
     *
     * @param hook
     *     The compat to add.
     */
    private void addProtectionCompat(IProtectionCompat hook)
    {
        if (hook.success())
        {
            protectionCompats.add(hook);
            logger.info("Successfully hooked into \"" + hook.getName() + "\"!");
        }
        else
            logger.info("Failed to hook into \"" + hook.getName() + "\"!");
    }

    /**
     * Load a compat for the plugin enabled in the event if needed.
     *
     * @param event
     *     The event of the plugin that is loaded.
     */
    @SuppressWarnings("unused")
    @EventHandler
    void onPluginEnable(PluginEnableEvent event)
    {
        loadFromPluginName(event.getPlugin().getName());
    }

    /**
     * Load a compat for a plugin with a given name if allowed and possible.
     *
     * @param compatName
     *     The name of the plugin to load a compat for.
     */
    private void loadFromPluginName(String compatName)
    {
        final @Nullable ProtectionCompat compat = ProtectionCompat.getFromName(compatName);
        if (compat == null)
            return;

        if (!config.isHookEnabled(compat))
            return;

        try
        {
            final @Nullable Plugin otherPlugin = plugin.getServer().getPluginManager()
                                                       .getPlugin(ProtectionCompat.getName(compat));
            if (otherPlugin == null)
            {
                logger.logMessage(Level.FINE, "Failed to obtain instance of \"" + compatName + "\"!");
                return;
            }

            final @Nullable Class<? extends IProtectionCompat> compatClass =
                compat.getClass(plugin.getDescription().getVersion());

            if (compatClass == null)
            {
                logger.severe("Could not find compatibility class for: \"" +
                                  ProtectionCompat.getName(compat) + "\". " +
                                  "This most likely means that this version is not supported!");
                return;
            }

            // No need to load compats twice.
            if (protectionAlreadyLoaded(compatClass))
                return;

            addProtectionCompat(compatClass.getConstructor().newInstance());
        }
        catch (NullPointerException e)
        {
            logger.warn("Could not find \"" + compatName + "\"! Hook not enabled!");
        }
        catch (NoClassDefFoundError | Exception e)
        {
            logger
                .logThrowable(e, "Failed to initialize \"" + compatName + "\" compatibility hook! Hook not enabled!");
        }
    }
}
