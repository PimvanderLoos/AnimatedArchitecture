package nl.pim16aap2.bigdoors.spigot.compatiblity;

import com.google.common.base.Preconditions;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
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
import org.jetbrains.annotations.Nullable;

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
public final class ProtectionCompatManagerSpigot extends Restartable implements Listener, IProtectionCompatManager
{
    private final List<IProtectionCompat> protectionCompats;
    private final BigDoorsSpigot plugin;
    private final @Nullable FakePlayerCreator fakePlayerCreator;

    @SuppressWarnings({"NullAway.Init"})
    private static ProtectionCompatManagerSpigot INSTANCE;

    /**
     * Constructor of {@link ProtectionCompatManagerSpigot}.
     *
     * @param plugin The instance of {@link BigDoorsSpigot}.
     */
    private ProtectionCompatManagerSpigot(BigDoorsSpigot plugin)
    {
        super(plugin);
        this.plugin = plugin;
        @Nullable FakePlayerCreator fakePlayerCreatorTmp = null;
        try
        {
            fakePlayerCreatorTmp = new FakePlayerCreator(plugin);
        }
        catch (NoSuchMethodException | ClassNotFoundException | NoSuchFieldException e)
        {
            BigDoors.get().getPLogger()
                    .logThrowable(new IllegalStateException("Failed to construct FakePlayerCreator!", e));
        }
        fakePlayerCreator = fakePlayerCreatorTmp;
        protectionCompats = new ArrayList<>();
        restart();
    }

    /**
     * Initializes the {@link ProtectionCompatManagerSpigot}. If it has already been initialized, it'll return that
     * instance instead.
     *
     * @param plugin The Spigot plugin.
     * @return The instance of this {@link ProtectionCompatManagerSpigot}.
     */
    public static ProtectionCompatManagerSpigot init(BigDoorsSpigot plugin)
    {
        return (INSTANCE == null) ?
               INSTANCE = new ProtectionCompatManagerSpigot(plugin) : INSTANCE;
    }

    /**
     * Gets the instance of the {@link ProtectionCompatManagerSpigot} if it exists.
     *
     * @return The instance of the {@link ProtectionCompatManagerSpigot}.
     */
    public static ProtectionCompatManagerSpigot get()
    {
        Preconditions.checkState(INSTANCE != null,
                                 "Instance has not yet been initialized. Be sure #init() has been invoked");
        return INSTANCE;
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

        for (Plugin p : plugin.getServer().getPluginManager().getPlugins())
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
     * @param player The {@link Player} to check the permissions for.
     * @return True if the player can bypass the checks.
     */
    private boolean canByPass(Player player)
    {
        if (player.isOp())
            return true;

        // offline players don't have permissions, so use Vault if that's the case.
        if (!player.hasMetadata(FakePlayerCreator.FAKE_PLAYER_METADATA))
            return player.hasPermission(Constants.COMPAT_BYPASS_PERMISSION);
        return plugin.getVaultManager().hasPermission(player, Constants.COMPAT_BYPASS_PERMISSION);
    }

    /**
     * Get an online player from a player {@link UUID} in a given world. If the player with the given UUID is not
     * online, a fake-online player is created.
     *
     * @param player The {@link IPPlayer}.
     * @param world  The {@link World} the player is in.
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

        Location loc = SpigotAdapter.getBukkitLocation(pLoc);
        if (loc.getWorld() == null)
            return Optional.of("InvalidWorld");

        Optional<Player> fakePlayer = getPlayer(player, loc.getWorld());
        if (fakePlayer.isEmpty())
            return Optional.empty();

        if (canByPass(fakePlayer.get()))
            return Optional.empty();

        for (IProtectionCompat compat : protectionCompats)
            try
            {
                if (!compat.canBreakBlock(fakePlayer.get(), loc))
                    return Optional.of(compat.getName());
            }
            catch (Exception e)
            {
                plugin.getPLogger().logThrowable(e, "Failed to use \"" + compat.getName()
                    + "\"! Please send this error to pim16aap2:");
            }
        return Optional.empty();
    }

    @Override
    public Optional<String> canBreakBlocksBetweenLocs(IPPlayer player, Vector3Di pos1, Vector3Di pos2, IPWorld world)
    {
        if (protectionCompats.isEmpty())
            return Optional.empty();

        IPLocationFactory locationFactory = BigDoors.get().getPlatform().getPLocationFactory();

        Location loc1 = SpigotAdapter.getBukkitLocation(locationFactory.create(world, pos1));
        if (loc1.getWorld() == null)
            return Optional.of("InvalidWorld");

        Location loc2 = SpigotAdapter.getBukkitLocation(locationFactory.create(world, pos2));
        if (loc2.getWorld() == null)
            return Optional.of("InvalidWorld");


        Optional<Player> fakePlayer = getPlayer(player, loc1.getWorld());
        if (fakePlayer.isEmpty())
            return Optional.empty();

        if (canByPass(fakePlayer.get()))
            return Optional.empty();

        for (IProtectionCompat compat : protectionCompats)
            try
            {
                if (!compat.canBreakBlocksBetweenLocs(fakePlayer.get(), loc1, loc2))
                    return Optional.of(compat.getName());
            }
            catch (Exception e)
            {
                plugin.getPLogger().logThrowable(e, "Failed to use \"" + compat.getName()
                    + "\"! Please send this error to pim16aap2:");
            }
        return Optional.empty();
    }

    /**
     * Check if an {@link IProtectionCompat} is already loaded.
     *
     * @param compatClass The class of the {@link IProtectionCompat} to check.
     * @return True if the compat has already been loaded.
     */
    private boolean protectionAlreadyLoaded(Class<? extends IProtectionCompat> compatClass)
    {
        for (IProtectionCompat compat : protectionCompats)
            if (compat.getClass().equals(compatClass))
                return true;
        return false;
    }

    /**
     * Add a {@link IProtectionCompat} to the list of loaded compats if it loaded successfully.
     *
     * @param hook The compat to add.
     */
    private void addProtectionCompat(IProtectionCompat hook)
    {
        if (hook.success())
        {
            protectionCompats.add(hook);
            plugin.getPLogger().info("Successfully hooked into \"" + hook.getName() + "\"!");
        }
        else
            plugin.getPLogger().info("Failed to hook into \"" + hook.getName() + "\"!");
    }

    /**
     * Load a compat for the plugin enabled in the event if needed.
     *
     * @param event The event of the plugin that is loaded.
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
     * @param compatName The name of the plugin to load a compat for.
     */
    private void loadFromPluginName(String compatName)
    {
        @Nullable ProtectionCompat compat = ProtectionCompat.getFromName(compatName);
        if (compat == null)
            return;

        if (!BigDoorsSpigot.get().getConfigLoader().isHookEnabled(compat))
            return;

        try
        {
            final @Nullable Plugin otherPlugin = plugin.getServer().getPluginManager()
                                                       .getPlugin(ProtectionCompat.getName(compat));
            if (otherPlugin == null)
            {
                BigDoors.get().getPLogger()
                        .logMessage(Level.FINE, "Failed to obtain instance of \"" + compatName + "\"!");
                return;
            }

            final @Nullable Class<? extends IProtectionCompat> compatClass =
                compat.getClass(plugin.getDescription().getVersion());

            if (compatClass == null)
            {
                BigDoors.get().getPLogger().severe("Could not find compatibility class for: \"" +
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
            plugin.getPLogger().warn("Could not find \"" + compatName + "\"! Hook not enabled!");
        }
        catch (NoClassDefFoundError | Exception e)
        {
            plugin.getPLogger()
                  .logThrowable(e, "Failed to initialize \"" + compatName + "\" compatibility hook! Hook not enabled!");
        }
    }
}
