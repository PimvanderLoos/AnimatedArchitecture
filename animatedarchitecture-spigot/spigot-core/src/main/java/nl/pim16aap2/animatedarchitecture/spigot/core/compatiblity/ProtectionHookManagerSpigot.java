package nl.pim16aap2.animatedarchitecture.spigot.core.compatiblity;

import dagger.Lazy;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.compatibility.bundle.AbstractProtectionHookSpecification;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IProtectionHookManager;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.IRestartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.spigot.core.config.ConfigSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IPermissionsManagerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.IProtectionHookSpigotSpecification;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.ProtectionHookContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Class that manages all {@link IProtectionHookSpigot} instances and provides a unified interface to them.
 */
@Singleton
@Flogger
public final class ProtectionHookManagerSpigot
    implements IRestartable, Listener, IProtectionHookManager, IDebuggable
{
    private final FakePlayerCreator fakePlayerCreator;
    private final Lazy<ConfigSpigot> config;
    private final IPermissionsManagerSpigot permissionsManager;
    private final Map<String, IProtectionHookSpigotSpecification> registeredDefinitions;
    private final JavaPlugin animatedArchitecture;

    private List<IProtectionHookSpigot> protectionHooks = new ArrayList<>();

    @Inject ProtectionHookManagerSpigot(
        JavaPlugin animatedArchitecture,
        RestartableHolder holder,
        DebuggableRegistry debuggableRegistry,
        Lazy<ConfigSpigot> config,
        IPermissionsManagerSpigot permissionsManager,
        FakePlayerCreator fakePlayerCreator)
    {
        this.animatedArchitecture = animatedArchitecture;
        this.fakePlayerCreator = fakePlayerCreator;
        this.config = config;
        this.permissionsManager = permissionsManager;

        this.registeredDefinitions = new LinkedHashMap<>(
            AbstractProtectionHookSpecification.DEFAULT_COMPAT_DEFINITIONS
                .stream().collect(Collectors.toMap(IProtectionHookSpigotSpecification::getName, c -> c)));

        holder.registerRestartable(this);
        debuggableRegistry.registerDebuggable(this);
    }

    /**
     * Getter for all registered protection hook specifications.
     * <p>
     * A registered specification can be used to load the hook it specifies when needed.
     *
     * @return The registered {@link IProtectionHookSpigotSpecification} instances.
     */
    public Map<String, IProtectionHookSpigotSpecification> getRegisteredHookDefinitions()
    {
        return new LinkedHashMap<>(registeredDefinitions);
    }

    /**
     * Registers a new compat definition.
     *
     * @param compatDefinition
     *     The compat definition to register.
     */
    @SuppressWarnings("unused")
    public void registerCompatDefinition(IProtectionHookSpigotSpecification compatDefinition)
    {
        registeredDefinitions.put(compatDefinition.getName(), compatDefinition);
    }

    /**
     * Attempts to load protection hooks for any plugins that are currently enabled.
     * <p>
     * Plugins that will be enabled later will be handled by {@link #onPluginEnable(PluginEnableEvent)}.
     */
    private void loadHooks()
    {
        for (final Plugin plugin : Bukkit.getServer().getPluginManager().getPlugins())
            loadFromPluginName(plugin.getName());
    }

    /**
     * Load a compat for a plugin with a given name if allowed and possible.
     *
     * @param pluginName
     *     The name of the plugin to load a compat for.
     */
    private void loadFromPluginName(String pluginName)
    {
        final @Nullable IProtectionHookSpigotSpecification spec = registeredDefinitions.get(pluginName);
        if (spec == null)
        {
            log.atFinest().log("Not loading hook for plugin '%s' because it is not registered.", pluginName);
            return;
        }

        if (!config.get().isHookEnabled(spec))
        {
            log.atFine().log("Not loading hook for plugin '%s' because it is disabled in the config.", pluginName);
            return;
        }

        @Nullable String version = null;
        try
        {
            final @Nullable Plugin plugin = Bukkit.getPluginManager().getPlugin(spec.getName());
            if (plugin == null)
            {
                log.atInfo().log("Not loading hook for plugin '%s' because it is not loaded!", pluginName);
                return;
            }
            version = plugin.getDescription().getVersion();

            final Class<? extends IProtectionHookSpigot> hookClass =
                Objects.requireNonNull(spec.getClass(version), "Hook class cannot be null!");

            // No need to load hooks twice.
            if (this.protectionHooks.stream().map(IProtectionHookSpigot::getClass).anyMatch(hookClass::equals))
                return;

            final var context = new ProtectionHookContext(animatedArchitecture, spec, permissionsManager);
            this.protectionHooks.add(hookClass.getConstructor(ProtectionHookContext.class).newInstance(context));
            log.atInfo()
               .log("Successfully loaded protection hook for plugin '%s' (version '%s')!", pluginName, version);
        }
        catch (NoClassDefFoundError | ExceptionInInitializerError | Exception e)
        {
            log.atSevere().withCause(e).log(
                "Failed to initialize protection hook for plugin '%s' (version '%s')!", pluginName, version);
        }
    }

    /**
     * Load a protection hook for the plugin enabled in the event if needed.
     *
     * @param event
     *     The event of the plugin that is loaded.
     */
    @EventHandler
    void onPluginEnable(PluginEnableEvent event)
    {
        loadFromPluginName(event.getPlugin().getName());
    }

    /**
     * Used to apply a predicate to all registered protection hooks.
     *
     * @param predicate
     *     The predicate to apply.
     * @return The name of the protection hook that returned false, or an empty Optional if all returned true.
     */
    private Optional<String> checkForEachHook(Predicate<IProtectionHookSpigot> predicate)
    {
        for (final IProtectionHookSpigot hook : protectionHooks)
        {
            try
            {
                if (!predicate.test(hook))
                    return Optional.of(hook.getName());
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log("Error while checking protection hook %s", hook.getName());
                return Optional.of(hook.getName());
            }
        }
        return Optional.empty();
    }

    /**
     * Get the player to use for protection checks.
     * <p>
     * This will return the online player if they are online, otherwise it will return a fake-online player.
     *
     * @param player
     *     The player to get the bukkit player for.
     * @param location
     *     The location to provide for the fake player.
     * @return The player to use for protection checks.
     */
    private Optional<Player> getPlayer(IPlayer player, Location location)
    {
        final OfflinePlayer offlinePlayer = SpigotAdapter.getOfflineBukkitPlayer(player);
        final @Nullable Player onlinePlayer = offlinePlayer.getPlayer();
        if (onlinePlayer != null)
            return Optional.of(onlinePlayer);
        return fakePlayerCreator.createPlayer(offlinePlayer, location);
    }

    @Override
    public Optional<String> canBreakBlock(IPlayer player, ILocation location)
    {
        if (protectionHooks.isEmpty())
            return Optional.empty();

        final Location bukkitLocation = SpigotAdapter.getBukkitLocation(location);
        return getPlayer(player, bukkitLocation)
            .map(bukkitPlayer -> checkForEachHook(hook -> hook.canBreakBlock(bukkitPlayer, bukkitLocation)))
            .orElseGet(() -> Optional.of("ERROR!"));
    }

    @Override
    public Optional<String> canBreakBlocksBetweenLocs(IPlayer player, Cuboid cuboid, IWorld world)
    {
        if (protectionHooks.isEmpty())
            return Optional.empty();

        final IVector3D vec = cuboid.getMin();
        final Location loc0 = new Location(SpigotAdapter.getBukkitWorld(world), vec.xD(), vec.yD(), vec.zD());

        final World world0 = Util.requireNonNull(SpigotAdapter.getBukkitWorld(world), "World");

        return getPlayer(player, loc0)
            .map(bukkitPlayer -> checkForEachHook(hook -> hook.canBreakBlocksBetweenLocs(bukkitPlayer, world0, cuboid)))
            .orElseGet(() -> Optional.of("ERROR!"));
    }

    @Override
    public boolean canSkipCheck()
    {
        return protectionHooks.isEmpty();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Reinitialize all protection hooks.
     */
    @Override
    public void initialize()
    {
        this.protectionHooks = new ArrayList<>();
        loadHooks();
    }

    @Override
    public void shutDown()
    {
        protectionHooks.clear();
        this.protectionHooks = List.of();
    }

    @Override
    public String getDebugInformation()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("Can create fake players: ").append(fakePlayerCreator.canCreatePlayers()).append('\n')
          .append("Protection hooks: \n");
        for (final IProtectionHookSpigot protectionHook : protectionHooks)
            sb.append("  ").append(protectionHook.getName()).append('\n');

        return sb.toString();
    }
}
