package nl.pim16aap2.bigdoors.spigot;

import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IEconomyManager;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IPermissionsManager;
import nl.pim16aap2.bigdoors.api.IPowerBlockRedstoneManager;
import nl.pim16aap2.bigdoors.api.ISoundEngine;
import nl.pim16aap2.bigdoors.api.factories.IDoorActionEventFactory;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEvent;
import nl.pim16aap2.bigdoors.extensions.DoorTypeLoader;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.logging.PLogger;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorActivityManager;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.managers.PowerBlockManager;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.spigot.commands.CommandBigDoors;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.commands.CommandMenu;
import nl.pim16aap2.bigdoors.spigot.commands.ICommand;
import nl.pim16aap2.bigdoors.spigot.commands.SuperCommand;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandAddOwner;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandCancel;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandClose;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandConfirm;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandDebug;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandDelete;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandFill;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandInfo;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandInspectPowerBlock;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandListDoors;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandListPlayerDoors;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandMenu;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandMovePowerBlock;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandNew;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandOpen;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandRemoveOwner;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandRestart;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandSetAutoCloseTime;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandSetBlocksToMove;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandSetName;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandSetRotation;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandSpecify;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandStopDoors;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandToggle;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandVersion;
import nl.pim16aap2.bigdoors.spigot.compatiblity.ProtectionCompatManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.config.ConfigLoaderSpigot;
import nl.pim16aap2.bigdoors.spigot.events.BigDoorsSpigotEvent;
import nl.pim16aap2.bigdoors.spigot.factories.DoorActionEventFactorySpigot;
import nl.pim16aap2.bigdoors.spigot.factories.PLocationFactorySpigot;
import nl.pim16aap2.bigdoors.spigot.factories.PPlayerFactorySpigot;
import nl.pim16aap2.bigdoors.spigot.factories.PWorldFactorySpigot;
import nl.pim16aap2.bigdoors.spigot.gui.GUI;
import nl.pim16aap2.bigdoors.spigot.implementations.BigDoorsToolUtilSpigot;
import nl.pim16aap2.bigdoors.spigot.listeners.ChunkListener;
import nl.pim16aap2.bigdoors.spigot.listeners.EventListeners;
import nl.pim16aap2.bigdoors.spigot.listeners.GUIListener;
import nl.pim16aap2.bigdoors.spigot.listeners.LoginMessageListener;
import nl.pim16aap2.bigdoors.spigot.listeners.LoginResourcePackListener;
import nl.pim16aap2.bigdoors.spigot.listeners.RedstoneListener;
import nl.pim16aap2.bigdoors.spigot.listeners.WorldListener;
import nl.pim16aap2.bigdoors.spigot.managers.AbortableTaskManager;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigot.managers.HeadManager;
import nl.pim16aap2.bigdoors.spigot.managers.PlatformManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.managers.PowerBlockRedstoneManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.managers.UpdateManager;
import nl.pim16aap2.bigdoors.spigot.managers.VaultManager;
import nl.pim16aap2.bigdoors.spigot.util.GlowingBlockManager;
import nl.pim16aap2.bigdoors.spigot.util.MessagingInterfaceSpigot;
import nl.pim16aap2.bigdoors.spigot.util.PExecutorSpigot;
import nl.pim16aap2.bigdoors.spigot.util.api.BigDoorsSpigotAbstract;
import nl.pim16aap2.bigdoors.spigot.util.api.IPlatformManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.util.implementations.ChunkManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.util.implementations.MessageableServerSpigot;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PSoundEngineSpigot;
import nl.pim16aap2.bigdoors.spigot.waitforcommand.WaitForCommand;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Represents the implementation of {@link BigDoorsSpigotAbstract}.
 *
 * @author Pim
 */
public final class BigDoorsSpigot extends BigDoorsSpigotAbstract
{
    private static BigDoorsSpigot INSTANCE;
    private static long MAINTHREADID = -1;
    @NotNull
    private static final BigDoors BIGDOORS = BigDoors.get();

    private final PLogger pLogger = new PLogger(new File(getDataFolder(), "log.txt"));

    @Getter(onMethod = @__({@Override}))
    private ConfigLoaderSpigot configLoader;
    private Metrics metrics;

    @Getter(onMethod = @__({@Override}))
    private Messages messages;

    private boolean validVersion = false;
    private CommandManager commandManager;
    private Map<UUID, WaitForCommand> cmdWaiters;
    private Map<UUID, GUI> playerGUIs;
    private final Set<IRestartable> restartables = new HashSet<>();

    @Getter(onMethod = @__({@Override}))
    private ProtectionCompatManagerSpigot protectionCompatManager;
    private LoginResourcePackListener rPackHandler;

    @Getter
    private PowerBlockManager powerBlockManager;

    @Getter
    private VaultManager vaultManager;

    @Getter
    private HeadManager headManager;
    private UpdateManager updateManager;

    private boolean successfulInit = true;

    @Getter
    @NotNull
    private final AbortableTaskManager abortableTaskManager;

    @Getter(onMethod = @__({@Override}))
    @NotNull
    private final IPLocationFactory pLocationFactory = new PLocationFactorySpigot();

    @Getter(onMethod = @__({@Override}))
    @NotNull
    private final IPWorldFactory pWorldFactory = new PWorldFactorySpigot();

    @Getter(onMethod = @__({@Override}))
    @NotNull
    private final IPPlayerFactory pPlayerFactory = new PPlayerFactorySpigot();

    @Getter(onMethod = @__({@Override}))
    @NotNull
    private final ISoundEngine soundEngine = new PSoundEngineSpigot();

    @Getter(onMethod = @__({@Override}))
    @NotNull
    private final IMessagingInterface messagingInterface = new MessagingInterfaceSpigot(this);

    @Getter(onMethod = @__({@Override}))
    @NotNull
    private final IChunkManager chunkManager = ChunkManagerSpigot.get();

    @Getter(onMethod = @__({@Override}))
    @NotNull
    private final IDoorActionEventFactory doorActionEventFactory = new DoorActionEventFactorySpigot();

    @Getter(onMethod = @__({@Override}))
    @NotNull
    private final IPowerBlockRedstoneManager powerBlockRedstoneManager = PowerBlockRedstoneManagerSpigot.get();

    @Getter(onMethod = @__({@Override}))
    @NotNull
    private final BigDoorsToolUtilSpigot bigDoorsToolUtil;

    @Getter
    private DatabaseManager databaseManager;

    @Getter
    private final DoorRegistry doorRegistry;

    public BigDoorsSpigot()
    {
        INSTANCE = this;
        BIGDOORS.setBigDoorsPlatform(this);
        MAINTHREADID = Thread.currentThread().getId();
        bigDoorsToolUtil = new BigDoorsToolUtilSpigot();

        doorRegistry = new DoorRegistry();
        abortableTaskManager = AbortableTaskManager.init(this);
    }

    @Override
    public void onEnable()
    {
        Bukkit.getLogger().setLevel(Level.FINER);

        try
        {
            // Register this here so it can check for updates even when loaded on an incorrect version.
            updateManager = new UpdateManager(this, 58669);

            databaseManager = new DatabaseManager(this, new File(super.getDataFolder(), "doorDB.db"));
            registerDoorTypes();

            Bukkit.getPluginManager().registerEvents(new LoginMessageListener(this), this);
            validVersion = PlatformManagerSpigot.get().initPlatform(this);

            // Load the files for the correct version of Minecraft.
            if (!validVersion)
            {
                pLogger.severe("Trying to load the plugin on an incompatible version of Minecraft! (\""
                                   + (Bukkit.getServer().getClass().getPackage().getName().replace(".", ",")
                                            .split(",")[3])
                                   + "\"). This plugin will NOT be enabled!");
                disablePlugin();
                return;
            }

            configLoader = ConfigLoaderSpigot.init(this, getPLogger());
            init();

            RedstoneListener.init(this);
            LoginResourcePackListener.init(this, configLoader.resourcePack());

            final IStorage.DatabaseState databaseState = databaseManager.getDatabaseState();
            if (databaseState != IStorage.DatabaseState.OK)
            {
                BigDoors.get().getPLogger()
                        .severe("Failed to load database! Found it in the state: " + databaseState.name() +
                                    ". Plugin initialization has been aborted!");
                disablePlugin();
                return;
            }

            DoorActivityManager.init(this);
            vaultManager = VaultManager.init(this);

            headManager = HeadManager.init(this, getConfigLoader());

            Bukkit.getPluginManager().registerEvents(new EventListeners(this), this);
            Bukkit.getPluginManager().registerEvents(new GUIListener(this), this);
            Bukkit.getPluginManager().registerEvents(new ChunkListener(this), this);

            protectionCompatManager = ProtectionCompatManagerSpigot.init(this);
            Bukkit.getPluginManager().registerEvents(protectionCompatManager, this);

            powerBlockManager = new PowerBlockManager(this, configLoader, databaseManager, getPLogger());
            Bukkit.getPluginManager().registerEvents(WorldListener.init(powerBlockManager), this);
            loadCommands();

            pLogger.info("Successfully enabled BigDoors " + getDescription().getVersion());
        }
        catch (Exception exception)
        {
            successfulInit = false;
            pLogger.logThrowable(exception);
        }
    }

    /**
     * Disables this plugin.
     */
    private void disablePlugin()
    {
        successfulInit = false;
        Bukkit.getPluginManager().disablePlugin(this);
    }

    /**
     * Registers all BigDoor's own door types.
     */
    private void registerDoorTypes()
    {
        final @NotNull File extensionsDir = new File(BigDoors.get().getPlatform().getDataDirectory() +
                                                         Constants.BIGDOORS_EXTENSIONS_FOLDER);
        if (!extensionsDir.exists())
            if (!extensionsDir.mkdirs())
            {
                BigDoors.get().getPLogger()
                        .logThrowable(new IOException("Failed to create folder: " + extensionsDir.toString()));
                return;
            }

        Bukkit.getLogger().setLevel(Level.ALL);
        DoorTypeLoader.get().loadDoorTypesFromDirectory();
    }

    @Override
    public @NotNull IPlatformManagerSpigot getPlatformManagerSpigot()
    {
        return PlatformManagerSpigot.get();
    }

    public static @NotNull BigDoorsSpigot get()
    {
        return INSTANCE;
    }

    private void init()
    {
        if (!validVersion)
            return;

        configLoader.restart();
        messages = new Messages(this, getDataFolder(), getConfigLoader().languageFile(), getPLogger());
        playerGUIs = new HashMap<>();
        cmdWaiters = new HashMap<>();

        updateManager.setEnabled(getConfigLoader().checkForUpdates(), getConfigLoader().autoDLUpdate());
    }

    private void loadCommands()
    {
        commandManager = new CommandManager(this);
        SuperCommand commandBigDoors = new CommandBigDoors(this, commandManager);
        {
            commandBigDoors.registerSubCommand(new SubCommandAddOwner(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandCancel(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandConfirm(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandMovePowerBlock(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandClose(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandDebug(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandDelete(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandFill(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandInfo(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandInspectPowerBlock(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandListDoors(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandListPlayerDoors(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandMenu(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandSetName(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandNew(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandOpen(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandRemoveOwner(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandRestart(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandSetAutoCloseTime(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandSetBlocksToMove(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandSetRotation(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandStopDoors(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandToggle(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandVersion(this, commandManager));
            commandBigDoors.registerSubCommand(new SubCommandSpecify(this, commandManager));
        }
        commandManager.registerCommand(commandBigDoors);
        commandManager.registerCommand(new CommandMenu(this, commandManager));
    }

    @Override
    public @NotNull File getDataDirectory()
    {
        return getDataFolder();
    }

    @Override
    public @NotNull IPBlockDataFactory getPBlockDataFactory()
    {
        return PlatformManagerSpigot.get().getSpigotPlatform().getPBlockDataFactory();
    }

    @Override
    public @NotNull IFallingBlockFactory getFallingBlockFactory()
    {
        return PlatformManagerSpigot.get().getSpigotPlatform().getFallingBlockFactory();
    }

    @Override
    public @NotNull IMessageable getMessageableServer()
    {
        return MessageableServerSpigot.get();
    }

    @Override
    public @NotNull IBlockAnalyzer getBlockAnalyzer()
    {
        return PlatformManagerSpigot.get().getSpigotPlatform().getBlockAnalyzer();
    }

    @Override
    public boolean isMainThread(final long compareThread)
    {
        return compareThread == MAINTHREADID;
    }

    @Override
    public @NotNull <T> IPExecutor<T> newPExecutor()
    {
        return new PExecutorSpigot<>(INSTANCE);
    }

    public @NotNull ICommand getCommand(final @NotNull CommandData command)
    {
        return commandManager.getCommand(command);
    }

    public @NotNull Optional<String> canBreakBlock(final @NotNull IPPlayer player, final @NotNull IPLocationConst loc)
    {
        return protectionCompatManager.canBreakBlock(player, loc);
    }

    public @NotNull Optional<String> canBreakBlocksBetweenLocs(final @NotNull IPPlayer player,
                                                               final @NotNull Vector3DiConst pos1,
                                                               final @NotNull Vector3DiConst pos2,
                                                               final @NotNull IPWorld world)
    {
        return protectionCompatManager.canBreakBlocksBetweenLocs(player, pos1, pos2, world);
    }

    @Override
    public void registerRestartable(final @NotNull IRestartable restartable)
    {
        restartables.add(restartable);
    }

    @Override
    public boolean isRestartableRegistered(final @NotNull IRestartable restartable)
    {
        return restartables.contains(restartable);
    }

    @Override
    public void deregisterRestartable(final @NotNull IRestartable restartable)
    {
        restartables.remove(restartable);
    }

    public void restart()
    {
        if (!validVersion)
            return;

        configLoader.restart();

        shutdown();
        playerGUIs.forEach((key, value) -> value.close());
        playerGUIs.clear();

        HandlerList.unregisterAll(rPackHandler);
        rPackHandler = null;

        init();

        restartables.forEach(IRestartable::restart);
    }

    @Override
    public void onDisable()
    {
        shutdown();
        restartables.forEach(IRestartable::shutdown);
    }

    private void shutdown()
    {
        if (!validVersion)
            return;

        cmdWaiters.clear();
    }

    @Override
    public @NotNull IEconomyManager getEconomyManager()
    {
        return vaultManager;
    }

    @Override
    public @NotNull IPermissionsManager getPermissionsManager()
    {
        return vaultManager;
    }

    public @NotNull IFallingBlockFactory getFABF()
    {
        return PlatformManagerSpigot.get().getSpigotPlatform().getFallingBlockFactory();
    }

    @Override
    public @NotNull IGlowingBlockSpawner getGlowingBlockSpawner()
    {
        return GlowingBlockManager.get();
    }

    public @NotNull BigDoorsSpigot getPlugin()
    {
        return this;
    }

    public @NotNull Optional<GUI> getGUIUser(final @NotNull Player player)
    {
        GUI gui = null;
        if (playerGUIs.containsKey(player.getUniqueId()))
            gui = playerGUIs.get(player.getUniqueId());
        return Optional.ofNullable(gui);
    }

    public void addGUIUser(final @NotNull GUI gui)
    {
        playerGUIs.put(gui.getGuiHolder().getUUID(), gui);
    }

    public void removeGUIUser(final @NotNull GUI gui)
    {
        playerGUIs.remove(gui.getGuiHolder().getUUID());
    }

    public @NotNull Optional<WaitForCommand> getCommandWaiter(final @NotNull Player player)
    {
        if (cmdWaiters.containsKey(player.getUniqueId()))
            return Optional.of(cmdWaiters.get(player.getUniqueId()));
        return Optional.empty();
    }

    public void addCommandWaiter(final @NotNull WaitForCommand cmdWaiter)
    {
        cmdWaiters.put(cmdWaiter.getPlayer().getUniqueId(), cmdWaiter);
    }

    public void removeCommandWaiter(final @NotNull WaitForCommand cmdWaiter)
    {
        cmdWaiters.remove(cmdWaiter.getPlayer().getUniqueId());
    }

    public void onPlayerLogout(final @NotNull Player player)
    {
        getCommandWaiter(player).ifPresent(WaitForCommand::abortSilently);
        cmdWaiters.remove(player.getUniqueId());
        playerGUIs.remove(player.getUniqueId());
        ToolUserManager.get().abortToolUser(player.getUniqueId());
    }

    // Get the logger.
    @Override
    public @NotNull IPLogger getPLogger()
    {
        return pLogger;
    }

    /**
     * Gets the message to send to admins and OPs when they log in. This message can contain all kinds of information,
     * including but not limited to: The current build is a dev build, the plugin could not be initialized properly, an
     * update is available.
     *
     * @return The message to send to admins and OPs when they log in.
     */
    public @NotNull String getLoginMessage()
    {
        String ret = "";
        if (Constants.DEVBUILD)
            ret += "[BigDoors] Warning: You are running a devbuild!\n";
        if (!validVersion)
            ret += "[BigDoors] Error: Trying to load the game on an invalid version! Plugin disabled!\n";
        if (!successfulInit)
            ret += "[BigDoors] Error: Failed to initialize the plugin! Some functions may not work as expected. " +
                "Please contact pim16aap2! Don't forget to attach both the server log AND the BigDoors log!\n";
        if (updateManager.updateAvailable())
        {
            if (getConfigLoader().autoDLUpdate() && updateManager.hasUpdateBeenDownloaded())
                ret += "[BigDoors] A new update (" + updateManager.getNewestVersion() +
                    ") has been downloaded! "
                    + "Restart your server to apply the update!\n";
            else if (updateManager.updateAvailable())
                ret += "[BigDoors] A new update is available: " + updateManager.getNewestVersion() + "\n";
        }
        return ret;
    }

    @Override
    public void callDoorActionEvent(final @NotNull IDoorEvent doorEvent)
    {
        if (!(doorEvent instanceof BigDoorsSpigotEvent))
        {
            getPLogger().logThrowable(new IllegalArgumentException(
                "Event " + doorEvent.getEventName() +
                    ", is not a Spigot event, but it was called on the Spigot platform!"));
            return;
        }

        // Async events can only be called asynchronously and Sync events can only be called from the main thread.
        final boolean isMainThread = isMainThread(Thread.currentThread().getId());
        if (isMainThread && doorEvent.isAsynchronous())
            BigDoors.get().getPlatform().newPExecutor()
                    .runAsync(() -> Bukkit.getPluginManager().callEvent((BigDoorsSpigotEvent) doorEvent));
        else if ((!isMainThread) && (!doorEvent.isAsynchronous()))
            BigDoors.get().getPlatform().newPExecutor()
                    .runSync(() -> Bukkit.getPluginManager().callEvent((BigDoorsSpigotEvent) doorEvent));
        else
            Bukkit.getPluginManager().callEvent((BigDoorsSpigotEvent) doorEvent);
    }
}
