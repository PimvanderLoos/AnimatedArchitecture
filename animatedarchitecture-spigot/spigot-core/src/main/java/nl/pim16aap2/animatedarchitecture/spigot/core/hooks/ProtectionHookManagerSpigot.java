package nl.pim16aap2.animatedarchitecture.spigot.core.hooks;

import dagger.Lazy;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IProtectionHookManager;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.IRestartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.spigot.core.config.ConfigSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.PlayerFactorySpigot;
import nl.pim16aap2.animatedarchitecture.spigot.hooks.bundle.AbstractProtectionHookSpecification;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IPermissionsManagerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigotSpecification;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.ProtectionHookContext;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
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
    private final IExecutor executor;

    /**
     * Whether this manager is active.
     */
    private volatile boolean isActive = false;

    private volatile List<IProtectionHookSpigot> protectionHooks = new CopyOnWriteArrayList<>();

    @Inject
    ProtectionHookManagerSpigot(
        JavaPlugin animatedArchitecture,
        RestartableHolder holder,
        DebuggableRegistry debuggableRegistry,
        Lazy<ConfigSpigot> config,
        IPermissionsManagerSpigot permissionsManager,
        FakePlayerCreator fakePlayerCreator,
        IExecutor executor)
    {
        this.animatedArchitecture = animatedArchitecture;
        this.fakePlayerCreator = fakePlayerCreator;
        this.config = config;
        this.permissionsManager = permissionsManager;
        this.executor = executor;

        this.registeredDefinitions = new LinkedHashMap<>(
            AbstractProtectionHookSpecification
                .DEFAULT_HOOK_DEFINITIONS
                .stream()
                .collect(Collectors.toMap(IProtectionHookSpigotSpecification::getName, c -> c))
        );

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
     * Unload a hook.
     *
     * @param hook
     *     The hook to unload.
     * @return True if a hook was unloaded, false otherwise.
     */
    @SuppressWarnings("unused")
    public boolean unloadHook(IProtectionHookSpigot hook)
    {
        return this.protectionHooks.remove(hook);
    }

    /**
     * Unload a hook with a given name.
     *
     * @param pluginName
     *     The name of the plugin to unload the hook for.
     * @return True if a hook was unloaded, false otherwise.
     */
    public boolean unloadHook(String pluginName)
    {
        if (!isActive)
            throw new IllegalStateException("Cannot unload hooks when the manager is not active!");
        return this.protectionHooks.removeIf(hook -> hook.getName().equals(pluginName));
    }

    /**
     * Registers a new hook definition.
     *
     * @param hookDefinition
     *     The hook definition to register.
     */
    @SuppressWarnings("unused")
    public void registerHookDefinition(IProtectionHookSpigotSpecification hookDefinition)
    {
        if (!isActive)
            throw new IllegalStateException("Cannot register hooks when the manager is not active!");

        registeredDefinitions.put(hookDefinition.getName(), hookDefinition);
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
     * Load a hook for a plugin with a given name if allowed and possible.
     *
     * @param pluginName
     *     The name of the plugin to load a hook for.
     */
    private void loadFromPluginName(String pluginName)
    {
        if (!isActive)
            throw new IllegalStateException("Cannot load hooks when the manager is not active!");

        final @Nullable IProtectionHookSpigotSpecification spec = registeredDefinitions.get(pluginName);
        if (spec == null)
        {
            log.atFinest().log("Skipping plugin '%s' because no hook implementation exists for it", pluginName);
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

            final var context = new ProtectionHookContext(animatedArchitecture, spec, permissionsManager, executor);
            this.protectionHooks.add(hookClass.getConstructor(ProtectionHookContext.class).newInstance(context));
            log.atInfo().log(
                "Successfully loaded protection hook for plugin '%s' (version '%s')!",
                pluginName,
                version
            );
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
     * Unload a protection hook for the plugin being disabled in the event if needed.
     *
     * @param event
     *     The event of the plugin that is disabled.
     */
    @EventHandler
    void onPluginDisable(PluginEnableEvent event)
    {
        unloadHook(event.getPlugin().getName());
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
        final OfflinePlayer offlinePlayer = PlayerFactorySpigot.unwrapOfflinePlayer(player);
        final @Nullable Player onlinePlayer = offlinePlayer.getPlayer();
        if (onlinePlayer != null)
            return Optional.of(onlinePlayer);
        return fakePlayerCreator.createPlayer(offlinePlayer, location);
    }

    @Override
    public CompletableFuture<HookCheckResult> canBreakBlock(IPlayer player, ILocation location)
    {
        if (canSkipCheck(player))
            return CompletableFuture.completedFuture(HookCheckResult.allowed());

        final Location bukkitLocation = SpigotAdapter.getBukkitLocation(location);

        return runCheck(player, bukkitLocation, (hook, player0) -> hook.canBreakBlock(player0, bukkitLocation))
            .thenApply(result ->
            {
                if (result.isDenied())
                    log.atInfo().log(
                        "Player %s cannot break block at location %s because of hook '%s'.",
                        player.getName(),
                        location,
                        result.denyingHookName()
                    );
                return result;
            });
    }

    private CompletableFuture<HookCheckResult> runCheck(
        IPlayer player,
        Location location,
        BiFunction<IProtectionHookSpigot, Player, CompletableFuture<Boolean>> function)
    {
        final var result = HookCheckStateContainer.of(protectionHooks);

        final World world = Util.requireNonNull(location.getWorld(), "World");

        return getPlayer(player, location)
            .map(bukkitPlayer ->
                result.runAllChecks(
                    executor,
                    bukkitPlayer,
                    world,
                    hook -> function.apply(hook, bukkitPlayer)))
            .orElseGet(() -> CompletableFuture.completedFuture(HookCheckResult.ERROR))
            .exceptionally(e ->
            {
                log.atSevere().withCause(e).log(
                    "Error while checking protection hooks for player %s.",
                    player.getName()
                );
                return HookCheckResult.ERROR;
            });
    }

    @Override
    public CompletableFuture<HookCheckResult> canBreakBlocksInCuboid(
        IPlayer player,
        Cuboid cuboid,
        IWorld world)
    {
        if (canSkipCheck(player))
            return CompletableFuture.completedFuture(HookCheckResult.allowed());

        final IVector3D vec = cuboid.getMin();
        final World world0 = Util.requireNonNull(SpigotAdapter.getBukkitWorld(world), "World");
        final Location loc0 = new Location(world0, vec.xD(), vec.yD(), vec.zD());

        return runCheck(player, loc0, (hook, player0) -> hook.canBreakBlocksInCuboid(player0, world0, cuboid))
            .thenApply(result ->
            {
                if (result.isDenied())
                    log.atInfo().log(
                        "Player %s cannot break blocks in cuboid %s because of hook '%s'.",
                        player.getName(),
                        cuboid,
                        result.denyingHookName()
                    );
                return result;
            });
    }

    /**
     * Check if the checks can be skipped for a given player.
     *
     * @param player
     *     The player to check.
     * @return True if the checks can be skipped for the given player.
     */
    public boolean canSkipCheck(IPlayer player)
    {
        return canSkipCheck() || player.isOp();
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
        this.protectionHooks = new CopyOnWriteArrayList<>();
        this.isActive = true;
        loadHooks();
    }

    @Override
    public void shutDown()
    {
        this.isActive = false;
        protectionHooks.clear();
        this.protectionHooks = Collections.emptyList();
    }

    @Override
    public String getDebugInformation()
    {
        return String.format(
            """
                Can create fake players: %s
                Protection hooks: %s
                """,
            fakePlayerCreator.canCreatePlayers(),
            StringUtil.formatCollection(protectionHooks, IProtectionHookSpigot::getName)
        );
    }
}
