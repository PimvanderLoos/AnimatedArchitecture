package nl.pim16aap2.bigdoors;

import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IRestartable;
import nl.pim16aap2.bigdoors.api.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.ISoundEngine;
import nl.pim16aap2.bigdoors.api.factories.IDoorActionEventFactory;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.commands.CommandBigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandMenu;
import nl.pim16aap2.bigdoors.commands.ICommand;
import nl.pim16aap2.bigdoors.commands.SuperCommand;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandAddOwner;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandCancel;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandClose;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandDebug;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandDelete;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandFill;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandInfo;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandInspectPowerBlock;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandListDoors;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandListPlayerDoors;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandMenu;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandMovePowerBlock;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandNew;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandOpen;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandRemoveOwner;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandRestart;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetAutoCloseTime;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetBlocksToMove;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetName;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetRotation;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandStopDoors;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandToggle;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandVersion;
import nl.pim16aap2.bigdoors.compatiblity.ProtectionCompatManagerSpigot;
import nl.pim16aap2.bigdoors.config.ConfigLoaderSpigot;
import nl.pim16aap2.bigdoors.doors.DoorOpeningUtility;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionEventSpigot;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorActionEvent;
import nl.pim16aap2.bigdoors.factories.DoorActionEventFactorySpigot;
import nl.pim16aap2.bigdoors.factories.PLocationFactorySpigot;
import nl.pim16aap2.bigdoors.factories.PPlayerFactorySpigot;
import nl.pim16aap2.bigdoors.factories.PWorldFactorySpigot;
import nl.pim16aap2.bigdoors.gui.GUI;
import nl.pim16aap2.bigdoors.listeners.ChunkUnloadListener;
import nl.pim16aap2.bigdoors.listeners.DoorActionListener;
import nl.pim16aap2.bigdoors.listeners.EventListeners;
import nl.pim16aap2.bigdoors.listeners.GUIListener;
import nl.pim16aap2.bigdoors.listeners.LoginMessageListener;
import nl.pim16aap2.bigdoors.listeners.LoginResourcePackListener;
import nl.pim16aap2.bigdoors.listeners.RedstoneListener;
import nl.pim16aap2.bigdoors.listeners.WorldListener;
import nl.pim16aap2.bigdoors.managers.AbortableTaskManager;
import nl.pim16aap2.bigdoors.managers.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorManager;
import nl.pim16aap2.bigdoors.managers.HeadManager;
import nl.pim16aap2.bigdoors.managers.PowerBlockManager;
import nl.pim16aap2.bigdoors.managers.UpdateManager;
import nl.pim16aap2.bigdoors.managers.VaultManager;
import nl.pim16aap2.bigdoors.spigot.spigot_v1_14_R1.BlockAnalyzer_V1_14_R1;
import nl.pim16aap2.bigdoors.spigot.spigot_v1_14_R1.FallingBlockFactory_V1_14_R1;
import nl.pim16aap2.bigdoors.spigot.spigot_v1_14_R1.GlowingBlockSpawner_V1_14_R1;
import nl.pim16aap2.bigdoors.spigot.spigot_v1_14_R1.PBlockDataFactorySpigot_V1_14_R1;
import nl.pim16aap2.bigdoors.spigotutil.MessagingInterfaceSpigot;
import nl.pim16aap2.bigdoors.spigotutil.PExecutorSpigot;
import nl.pim16aap2.bigdoors.spigotutil.implementations.ChunkManagerSpigot;
import nl.pim16aap2.bigdoors.spigotutil.implementations.PSoundEngineSpigot;
import nl.pim16aap2.bigdoors.toolusers.ToolUser;
import nl.pim16aap2.bigdoors.toolusers.ToolVerifier;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForCommand;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

public final class BigDoorsSpigot extends JavaPlugin implements Listener, IRestartableHolder, IBigDoorsPlatform
{
    private static BigDoorsSpigot INSTANCE;
    private static final BigDoors BIGDOORS = BigDoors.get();

    private ToolVerifier tf;
    private ConfigLoaderSpigot config;
    private PLogger pLogger;
    private Metrics metrics;
    private Messages messages;
    private DatabaseManager databaseManager = null;

    private RedstoneListener redstoneListener;
    private boolean validVersion;
    private CommandManager commandManager;
    private Map<UUID, WaitForCommand> cmdWaiters;
    private Map<UUID, ToolUser> toolUsers;
    private Map<UUID, GUI> playerGUIs;
    private List<IRestartable> restartables = new ArrayList<>();
    private ProtectionCompatManagerSpigot protCompatMan;
    private LoginResourcePackListener rPackHandler;
    private VaultManager vaultManager;
    private AutoCloseScheduler autoCloseScheduler;
    private HeadManager headManager;
    private IGlowingBlockSpawner glowingBlockSpawner;
    private UpdateManager updateManager;
    private DoorManager doorManager;
    private PowerBlockManager powerBlockManager;
    private boolean successfulInit = true;
    private static long mainThreadID = -1;

    // These are specific to MC versions (e.g. 1.14.4, or 1.12.2, etc).
    private IFallingBlockFactory fallingBlockFactory;
    private IPBlockDataFactory pBlockDataFactory;
    private AbortableTaskManager abortableTaskManager;
    private IBlockAnalyzer blockAnalyzer;

    @NotNull
    private IPLocationFactory locationFactory = new PLocationFactorySpigot();
    @NotNull
    private IPWorldFactory worldFactory = new PWorldFactorySpigot();
    @NotNull
    private IPPlayerFactory pPlayerFactory = new PPlayerFactorySpigot();
    @NotNull
    private ISoundEngine soundEngine = new PSoundEngineSpigot();
    @NotNull
    private final IMessagingInterface messagingInterface = new MessagingInterfaceSpigot(this);
    @NotNull
    private final IChunkManager chunkManager = ChunkManagerSpigot.get();
    @NotNull
    private final IDoorActionEventFactory doorActionEventFactory = new DoorActionEventFactorySpigot();

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEnable()
    {
        INSTANCE = this;
        BIGDOORS.setBigDoorsPlatform(this);
        pLogger = PLogger.init(new File(getDataFolder(), "log.txt"));
        mainThreadID = Thread.currentThread().getId();

        try
        {
            // Register this here so it can check for updates even when loaded on an incorrect version.
            updateManager = new UpdateManager(this, 58669);

            Bukkit.getPluginManager().registerEvents(new LoginMessageListener(this), this);

            validVersion = compatibleMCVer();

            // Load the files for the correct version of Minecraft.
            if (!validVersion)
            {
                pLogger.severe("Trying to load the plugin on an incompatible version of Minecraft! (\""
                                   + (Bukkit.getServer().getClass().getPackage().getName().replace(".", ",")
                                            .split(",")[3])
                                   + "\"). This plugin will NOT be enabled!");
                return;
            }

            abortableTaskManager = AbortableTaskManager.init(this);

            config = ConfigLoaderSpigot.init(this, getPLogger());
            doorManager = DoorManager.init(this);

            init();
            tf = new ToolVerifier(messages, this);
            vaultManager = new VaultManager(this);
            autoCloseScheduler = AutoCloseScheduler.init(this);

            headManager = HeadManager.init(this, (ConfigLoaderSpigot) getConfigLoader());

            Bukkit.getPluginManager().registerEvents(new EventListeners(this), this);
            Bukkit.getPluginManager().registerEvents(new GUIListener(this), this);
            Bukkit.getPluginManager().registerEvents(new ChunkUnloadListener(this), this);
            protCompatMan = ProtectionCompatManagerSpigot.init(this);
            Bukkit.getPluginManager().registerEvents(protCompatMan, this);
            databaseManager = DatabaseManager.init(this, config, new File(super.getDataFolder(), config.dbFile()));
            Bukkit.getPluginManager().registerEvents(DoorActionListener.get(), this);
            powerBlockManager = PowerBlockManager.init(this, config, databaseManager, getPLogger());
            Bukkit.getPluginManager().registerEvents(WorldListener.init(powerBlockManager), this);
            DoorOpeningUtility.init(getPLogger(), getGlowingBlockSpawner(), config, protCompatMan);
            commandManager = new CommandManager(this);
            SuperCommand commandBigDoors = new CommandBigDoors(this, commandManager);
            {
                commandBigDoors.registerSubCommand(new SubCommandAddOwner(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandCancel(this, commandManager));
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
            }
            commandManager.registerCommand(commandBigDoors);
            commandManager.registerCommand(new CommandMenu(this, commandManager));

            pLogger.info("Successfully enabled BigDoors " + getDescription().getVersion());
        }
        catch (Exception exception)
        {
            successfulInit = false;
            pLogger.logException(exception);
        }
    }

    public static BigDoorsSpigot get()
    {
        return INSTANCE;
    }

    private void init()
    {
        if (!validVersion)
            return;

        config.restart();
        getPLogger().setConsoleLogging(getConfigLoader().consoleLogging());
        messages = new Messages(this, getDataFolder(), getConfigLoader().languageFile(), getPLogger());
        toolUsers = new HashMap<>();
        playerGUIs = new HashMap<>();
        cmdWaiters = new HashMap<>();

        if (config.enableRedstone())
        {
            redstoneListener = new RedstoneListener(this);
            Bukkit.getPluginManager().registerEvents(redstoneListener, this);
        }
        // If the resourcepack is set to "NONE", don't load it.
        if (!config.resourcePack().equals("NONE"))
        {
            // If a resource pack was set for the current version of Minecraft, send that
            // pack to the client on login.
            rPackHandler = new LoginResourcePackListener(this, config.resourcePack());
            Bukkit.getPluginManager().registerEvents(rPackHandler, this);
        }

        // Load stats collector if allowed, otherwise unload it if needed or simply
        // don't load it in the first place.
        if (config.allowStats())
        {
            pLogger.info("Enabling stats! Thanks, it really helps!");
            if (metrics == null)
                try
                {
                    metrics = new Metrics(this);
                }
                catch (Exception e)
                {
                    pLogger.logException(e, "Failed to intialize stats! Please contact pim16aap2!");
                }
        }
        else
        {
            // Y u do dis? :(
            metrics = null;
            pLogger.info("Stats disabled; not loading stats :(... Please consider enabling it! "
                             + "It helps me stay motivated to keep working on this plugin!");
        }

        updateManager.setEnabled(getConfigLoader().checkForUpdates(), getConfigLoader().autoDLUpdate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IPLocationFactory getPLocationFactory()
    {
        return locationFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IPWorldFactory getPWorldFactory()
    {
        return worldFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IPBlockDataFactory getPBlockDataFactory()
    {
        return pBlockDataFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IFallingBlockFactory getFallingBlockFactory()
    {
        return fallingBlockFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IBlockAnalyzer getBlockAnalyzer()
    {
        return blockAnalyzer;
    }

    @Override
    public @NotNull IChunkManager getChunkManager()
    {
        return chunkManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IPPlayerFactory getPPlayerFactory()
    {
        return pPlayerFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMainThread(final long compareThread)
    {
        return compareThread == mainThreadID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ISoundEngine getSoundEngine()
    {
        return soundEngine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull IMessagingInterface getMessagingInterface()
    {
        return messagingInterface;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public <T> IPExecutor<T> newPExecutor()
    {
        return new PExecutorSpigot<>(INSTANCE, INSTANCE.getPLogger());
    }

    @NotNull
    public ICommand getCommand(final @NotNull CommandData command)
    {
        return commandManager.getCommand(command);
    }

    @NotNull
    public Optional<String> canBreakBlock(final @NotNull IPPlayer player, final @NotNull IPLocation loc)
    {
        return protCompatMan.canBreakBlock(player, loc);
    }

    @NotNull
    public Optional<String> canBreakBlocksBetweenLocs(final @NotNull IPPlayer player,
                                                      final @NotNull Vector3Di pos1,
                                                      final @NotNull Vector3Di pos2,
                                                      final @NotNull IPWorld world)
    {
        return protCompatMan.canBreakBlocksBetweenLocs(player, pos1, pos2, world);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerRestartable(final @NotNull IRestartable restartable)
    {
        restartables.add(restartable);
    }

    public void restart()
    {
        if (!validVersion)
            return;

        config.restart();

        shutdown();
        playerGUIs.forEach((key, value) -> value.close());
        playerGUIs.clear();

        HandlerList.unregisterAll(redstoneListener);
        redstoneListener = null;
        HandlerList.unregisterAll(rPackHandler);
        rPackHandler = null;

        init();

        restartables.forEach(IRestartable::restart);
    }

    /**
     * {@inheritDoc}
     */
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

        Iterator<Entry<UUID, ToolUser>> it = toolUsers.entrySet().iterator();
        while (it.hasNext())
        {
            Entry<UUID, ToolUser> entry = it.next();
            entry.getValue().abort();
        }

        toolUsers.clear();
        cmdWaiters.clear();
    }

    @NotNull
    public IFallingBlockFactory getFABF()
    {
        return fallingBlockFactory;
    }

    @NotNull
    public IGlowingBlockSpawner getGlowingBlockSpawner()
    {
        return glowingBlockSpawner;
    }

    @NotNull
    public BigDoorsSpigot getPlugin()
    {
        return this;
    }

    @NotNull
    public AutoCloseScheduler getAutoCloseScheduler()
    {
        return autoCloseScheduler;
    }

    @NotNull
    public Optional<ToolUser> getToolUser(final @NotNull Player player)
    {
        return Optional.ofNullable(toolUsers.get(player.getUniqueId()));
    }

    public void addToolUser(final @NotNull ToolUser toolUser)
    {
        toolUsers.put(toolUser.getPlayer().getUniqueId(), toolUser);
    }

    public void removeToolUser(final @NotNull ToolUser toolUser)
    {
        toolUsers.remove(toolUser.getPlayer().getUniqueId());
    }

    @NotNull
    public Optional<GUI> getGUIUser(final @NotNull Player player)
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

    @NotNull
    public Optional<WaitForCommand> getCommandWaiter(final @NotNull Player player)
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
        getToolUser(player).ifPresent(ToolUser::abortSilently);
        toolUsers.remove(player.getUniqueId());
    }

    @NotNull
    public AbortableTaskManager getAbortableTaskManager()
    {
        return abortableTaskManager;
    }

    // Get the logger.
    @NotNull
    public PLogger getPLogger()
    {
        return PLogger.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Messages getMessages()
    {
        return messages;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorActionEventFactory getDoorActionEventFactory()
    {
        return doorActionEventFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IConfigLoader getConfigLoader()
    {
        return config;
    }

    @NotNull
    public VaultManager getVaultManager()
    {
        return vaultManager;
    }

    // Get the ToolVerifier.
    @NotNull
    public ToolVerifier getTF()
    {
        return tf;
    }

    // Check + initialize for the correct version of Minecraft.
    private boolean compatibleMCVer()
    {
        String version;

        try
        {
            version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        }
        catch (final ArrayIndexOutOfBoundsException useAVersionMentionedInTheDescriptionPleaseException)
        {
            getPLogger().logException(useAVersionMentionedInTheDescriptionPleaseException);
            return false;
        }

        fallingBlockFactory = null;
        if (version.equals("v1_14_R1"))
        {
            glowingBlockSpawner = new GlowingBlockSpawner_V1_14_R1(this, getPLogger());
            fallingBlockFactory = new FallingBlockFactory_V1_14_R1();
            pBlockDataFactory = new PBlockDataFactorySpigot_V1_14_R1();
            blockAnalyzer = new BlockAnalyzer_V1_14_R1();
        }

        // Return true if compatible.
        return fallingBlockFactory != null;
    }

    @NotNull
    public String getLoginMessage()
    {
        String ret = "";
        if (Constants.DEVBUILD)
            ret += "[BigDoors] Warning: You are running a devbuild!\n";
        if (!validVersion)
            ret += "[BigDoors] Error: Trying to load the game on an invalid version! Plugin disabled!";
        if (!successfulInit)
            ret += "[BigDoors] Error: Failed to initialize the plugin! Some functions may not work as expected. " +
                "Please contact pim16aap2!";
        if (updateManager.updateAvailable())
        {
            if (getConfigLoader().autoDLUpdate() && updateManager.hasUpdateBeenDownloaded())
                ret += "[BigDoors] A new update (" + updateManager.getNewestVersion() + ") has been downloaded! "
                    + "Restart your server to apply the update!\n";
            else if (updateManager.updateAvailable())
                ret += "[BigDoors] A new update is available: " + updateManager.getNewestVersion() + "\n";
        }
        return ret;
    }

    @NotNull
    public HeadManager getHeadManager()
    {
        return headManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void callDoorActionEvent(final @NotNull IDoorActionEvent doorActionEvent)
    {
        if (!(doorActionEvent instanceof DoorActionEventSpigot))
        {
            PLogger.get().logException(new IllegalArgumentException(
                "Trying to log a " + doorActionEvent.getClass().getName() + " as a Spigot event! Event aborted!"));
            return;
        }
        // Asynchronous events may not be called from the main thread.
        if (isMainThread(Thread.currentThread().getId()))
            BigDoors.get().getPlatform().newPExecutor().runAsync(
                () -> Bukkit.getPluginManager().callEvent((DoorActionEventSpigot) doorActionEvent));
        else
            Bukkit.getPluginManager().callEvent((DoorActionEventSpigot) doorActionEvent);
    }
}
