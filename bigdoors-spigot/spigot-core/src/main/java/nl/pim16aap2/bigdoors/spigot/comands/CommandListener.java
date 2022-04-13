package nl.pim16aap2.bigdoors.spigot.comands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.commands.CommandFactory;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.text;

@Singleton
public final class CommandListener
{
    private final JavaPlugin plugin;
    private final ILocalizer localizer;
    private final CommandFactory commandFactory;
    private final DoorRetrieverFactory doorRetrieverFactory;
    private final DoorTypeManager doorTypeManager;
    private volatile @Nullable BukkitCommandManager<ICommandSender> manager;
    private boolean asyncCompletions = false;
    private @Nullable BukkitAudiences bukkitAudiences;
    private @Nullable MinecraftHelp<ICommandSender> minecraftHelp;
    private @Nullable CommandConfirmationManager<ICommandSender> confirmationManager;
    private final RestartableHolder restartableHolder;
    private final DoorTypeParser doorTypeParser;

    @Inject//
    CommandListener(
        JavaPlugin plugin, ILocalizer localizer, CommandFactory commandFactory,
        DoorRetrieverFactory doorRetrieverFactory, DoorTypeManager doorTypeManager,
        RestartableHolder restartableHolder, DoorTypeParser doorTypeParser)
    {
        this.plugin = plugin;
        this.localizer = localizer;
        this.commandFactory = commandFactory;
        this.doorRetrieverFactory = doorRetrieverFactory;
        this.doorTypeManager = doorTypeManager;
        this.restartableHolder = restartableHolder;
        this.doorTypeParser = doorTypeParser;
    }

    // IntelliJ struggles to understand that the manager cannot be null.
    @SuppressWarnings("ConstantConditions")
    public synchronized void init()
        throws Exception
    {
        if (manager != null)
            throw new IllegalStateException("Trying to instantiate Cloud manage again!");
        manager = Util.requireNonNull(newManager(), "Cloud manager");

        this.bukkitAudiences = BukkitAudiences.create(plugin);
        this.minecraftHelp = new MinecraftHelp<>(
            "/bigdoors help",
            sender -> this.bukkitAudiences.sender(SpigotAdapter.unwrapCommandSender(sender)),
            manager
        );
        if (manager.queryCapability(CloudBukkitCapabilities.BRIGADIER))
            manager.registerBrigadier();

        if (manager.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION))
        {
            ((PaperCommandManager<ICommandSender>) manager).registerAsynchronousCompletions();
            asyncCompletions = true;
        }

        this.confirmationManager = new CommandConfirmationManager<>(
            30L, TimeUnit.SECONDS,
            context -> context.getCommandContext().getSender().sendMessage(
                ChatColor.RED + "Confirmation required. Confirm using /bigdoors confirm."),
            sender -> sender.sendMessage(ChatColor.RED + "You don't have any pending commands.")
        );

        this.confirmationManager.registerConfirmationProcessor(this.manager);

        new MinecraftExceptionHandler<ICommandSender>()
            .withInvalidSyntaxHandler()
            .withInvalidSenderHandler()
            .withNoPermissionHandler()
            .withArgumentParsingHandler()
            .withCommandExecutionHandler()
            .withDecorator(
                component -> text()
                    .append(text("[", NamedTextColor.DARK_GRAY))
                    .append(text("BigDoors", NamedTextColor.GOLD))
                    .append(text("] ", NamedTextColor.DARK_GRAY))
                    .append(component).build()
            ).apply(manager, sender -> this.bukkitAudiences.sender(SpigotAdapter.unwrapCommandSender(sender)));

        initCommands(manager);
    }

    private void initCommands(BukkitCommandManager<ICommandSender> manager)
    {
        final Command.Builder<ICommandSender> builder = manager.commandBuilder("bigdoors");

        initCmdAddOwner(manager, builder);
        initCmdCancel(manager, builder);
        initCmdConfirm(manager, builder);
        initCmdDebug(manager, builder);
        initCmdDelete(manager, builder);
        initCmdInfo(manager, builder);
        initCmdInspectPowerBlock(manager, builder);
        initCmdListDoors(manager, builder);
        initCmdLock(manager, builder);
        initCmdMenu(manager, builder);
        initCmdMovePowerBlock(manager, builder);
        initCmdNewDoor(manager, builder);
        initCmdRemoveOwner(manager, builder);
        initCmdRestart(manager, builder);
        initCmdSetAutoCloseTime(manager, builder);
        initCmdSetBlocksToMove(manager, builder);
        initCmdSetName(manager, builder);
        initCmdSetOpenDirection(manager, builder);
        initCmdSpecify(manager, builder);
        initCmdStopDoors(manager, builder);
        initCmdToggle(manager, builder);
        initCmdVersion(manager, builder);

        builder.build();
    }

    private void initCmdAddOwner(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            builder.literal("addowner")
                   .meta(CommandMeta.DESCRIPTION, localizer.getMessage("commands.add_owner.description"))
                   .permission("bigdoors.user.addowner")
                   .argument(new DoorArgument(true, "doorRetriever", "MyDoor", null, ArgumentDescription.empty(),
                                              asyncCompletions, doorRetrieverFactory, 1))
                   .argument(PlayerArgument.of("newOwner"))
                   .argument(IntegerArgument.<ICommandSender>newBuilder("permissionLevel")
                                            .withMin(0).withMax(2).asOptional()
                                            .withDefaultDescription(ArgumentDescription.of(localizer.getMessage(
                                                "commands.add_owner.param.permission_level.description"))).build())
                   .handler(
                       commandContext ->
                       {
                           final IPPlayer newOwner = new PPlayerSpigot(commandContext.get("newOwner"));
                           final DoorRetriever doorRetriever = commandContext.get("doorRetriever");
                           commandFactory.newAddOwner(commandContext.getSender(), doorRetriever, newOwner,
                                                      commandContext.getOrDefault("permissionLevel", null)).run();
                       })
        );
    }

    private void initCmdCancel(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdConfirm(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdDebug(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            builder.literal("debug")
                   .meta(CommandMeta.DESCRIPTION, localizer.getMessage("commands.debug.description"))
                   .permission("bigdoors.debug")
                   .handler(commandContext -> commandFactory.newDebug(commandContext.getSender()).run())
        );
    }

    private void initCmdDelete(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdInfo(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdInspectPowerBlock(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdListDoors(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdLock(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdMenu(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdMovePowerBlock(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdNewDoor(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            builder.literal("newdoor")
                   .meta(CommandMeta.DESCRIPTION, localizer.getMessage("commands.new_door.description"))
                   .permission("bigdoors.user.newdoor")
                   .argument(new DoorTypeArgument(true, "doorType", "BigDoor", null, ArgumentDescription.empty(),
                                                  doorTypeParser))
                   .argument(StringArgument.<ICommandSender>newBuilder("doorName").asOptional().build())
                   .argument(IntegerArgument.<ICommandSender>newBuilder("permissionLevel")
                                            .withMin(0).withMax(2).asOptional()
                                            .withDefaultDescription(ArgumentDescription.of(localizer.getMessage(
                                                "commands.new_door.param.permission_level.description"))).build())
                   .handler(
                       commandContext ->
                       {
                           final DoorType doorType = commandContext.get("doorType");
                           final @Nullable String doorName = commandContext.getOrDefault("doorName", null);
                           commandFactory.newNewDoor(commandContext.getSender(), doorType, doorName).run();
                       })
        );
    }

    private void initCmdRemoveOwner(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdRestart(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdSetAutoCloseTime(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdSetBlocksToMove(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdSetName(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdSetOpenDirection(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdSpecify(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdStopDoors(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdToggle(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdVersion(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {

    }

    private BukkitCommandManager<ICommandSender> newManager()
        throws Exception
    {
        return new BukkitCommandManager<>(
            plugin,
            CommandExecutionCoordinator.simpleCoordinator(),
            SpigotAdapter::wrapCommandSender,
            SpigotAdapter::unwrapCommandSender
        );
    }
}
