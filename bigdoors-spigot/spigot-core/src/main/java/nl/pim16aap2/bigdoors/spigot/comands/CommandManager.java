package nl.pim16aap2.bigdoors.spigot.comands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.BooleanArgument;
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
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.commands.CommandDefinition;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.doors.PermissionLevel;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.text;

@Singleton
public final class CommandManager
{
    private final JavaPlugin plugin;
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final DoorRetrieverFactory doorRetrieverFactory;
    private volatile @Nullable BukkitCommandManager<ICommandSender> manager;
    private boolean asyncCompletions = false;
    private final BukkitAudiences bukkitAudiences;
    private final DoorTypeParser doorTypeParser;
    private final DirectionParser directionParser;
    private final CommandExecutor executor;

    @Inject//
    CommandManager(
        JavaPlugin plugin, ILocalizer localizer, ITextFactory textFactory, DoorRetrieverFactory doorRetrieverFactory,
        DoorTypeParser doorTypeParser, DirectionParser directionParser, CommandExecutor executor)
    {
        this.plugin = plugin;
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.doorRetrieverFactory = doorRetrieverFactory;
        this.doorTypeParser = doorTypeParser;
        this.directionParser = directionParser;
        this.bukkitAudiences = BukkitAudiences.create(plugin);
        this.executor = executor;
    }

    // IntelliJ struggles to understand that the manager cannot be null.
    @SuppressWarnings("ConstantConditions")
    public synchronized void init()
        throws Exception
    {
        if (manager != null)
            throw new IllegalStateException("Trying to instantiate Cloud manage again!");
        manager = Util.requireNonNull(newManager(), "Cloud manager");

        if (manager.hasCapability(CloudBukkitCapabilities.BRIGADIER) &&
            manager.hasCapability(CloudBukkitCapabilities.COMMODORE_BRIGADIER))
            manager.registerBrigadier();

        if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION))
        {
            ((PaperCommandManager<ICommandSender>) manager).registerAsynchronousCompletions();
            asyncCompletions = true;
        }

        final CommandConfirmationManager<ICommandSender> confirmationManager = new CommandConfirmationManager<>(
            30L, TimeUnit.SECONDS,
            context -> context.getCommandContext().getSender()
                              .sendInfo(textFactory, "Confirmation required. Confirm using /bigdoors confirm."),
            sender -> sender.sendError(textFactory, "You don't have any pending commands.")
        );

        confirmationManager.registerConfirmationProcessor(this.manager);

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

        initCmdHelp(manager, builder);

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

    private void initCmdHelp(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        final MinecraftHelp<ICommandSender> minecraftHelp = new MinecraftHelp<>(
            "/bigdoors help",
            sender -> this.bukkitAudiences.sender(SpigotAdapter.unwrapCommandSender(sender)),
            manager
        );

        manager.command(
            builder.literal("help")
                   .argument(StringArgument.optional("query", StringArgument.StringMode.GREEDY))
                   .handler(context ->
                                minecraftHelp.queryCommands(Objects.requireNonNull(context.getOrDefault("query", "")),
                                                            context.getSender()))
        );
    }

    private Command.Builder<ICommandSender> baseInit(
        Command.Builder<ICommandSender> builder, CommandDefinition cmd, String descriptionKey)
    {
        return builder.literal(cmd.getName().replace("_", "").toLowerCase(Locale.ROOT))
                      .permission(cmd.getLowestPermission())
                      .meta(CommandMeta.DESCRIPTION, localizer.getMessage(descriptionKey));
    }

    private void initCmdAddOwner(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.ADD_OWNER, "commands.add_owner.description")
                .argument(PlayerArgument.of("newOwner"))
                .argument(PermissionLevelArgument
                              .builder()
                              .name("permissionLevel")
                              .required(true)
                              .minimumLevel(PermissionLevel.ADMIN)
                              .maximumLevel(PermissionLevel.USER)
                              .localizer(localizer)
                              .defaultDescription(ArgumentDescription.of(
                                  localizer.getMessage("commands.add_owner.param.permission_level.description")))
                              .build())
                .argument(defaultDoorArgument(false, PermissionLevel.ADMIN).build())
                .handler(executor::addOwner)
        );
    }

    private void initCmdCancel(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.CANCEL, "commands.cancel.description")
                .handler(executor::cancel)
        );
    }

    private void initCmdConfirm(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.CONFIRM, "commands.cancel.description")
                .handler(executor::confirm)
        );
    }

    private void initCmdDebug(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.DEBUG, "commands.debug.description")
                .handler(executor::debug)
        );
    }

    private void initCmdDelete(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.DELETE, "commands.delete.description")
                .argument(defaultDoorArgument(true, PermissionLevel.ADMIN).build())
                .handler(executor::delete)
        );
    }

    private void initCmdInfo(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.INFO, "commands.info.description")
                .argument(defaultDoorArgument(true, PermissionLevel.USER).build())
                .handler(executor::info)
        );
    }

    private void initCmdInspectPowerBlock(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.INSPECT_POWER_BLOCK, "commands.inspect_power_block.description")
                .handler(executor::inspectPowerBlock)
        );
    }

    private void initCmdListDoors(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.LIST_DOORS, "commands.list_doors.description")
                .argument(StringArgument.optional("doorName"))
                .handler(executor::listDoors)
        );
    }

    private void initCmdLock(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.LOCK, "commands.lock.description")
                .argument(BooleanArgument.of("lockStatus"))
                .handler(executor::lock)
        );
    }

    private void initCmdMenu(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.MENU, "commands.menu.description")
                .argument(PlayerArgument.<ICommandSender>builder("targetPlayer").asOptional())
                .handler(executor::menu)
        );
    }

    private void initCmdMovePowerBlock(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.MOVE_POWER_BLOCK, "commands.move_power_block.description")
                .argument(defaultDoorArgument(true, PermissionLevel.ADMIN).build())
                .handler(executor::movePowerBlock)
        );
    }

    private void initCmdNewDoor(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.NEW_DOOR, "commands.new_door.description")
                .argument(defaultDoorTypeArgument(true).build())
                .argument(StringArgument.<ICommandSender>builder("doorName").asOptional().build())
                .handler(executor::newDoor)
        );
    }

    private void initCmdRemoveOwner(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.REMOVE_OWNER, "commands.remove_owner.description")
                .argument(defaultDoorArgument(true, PermissionLevel.ADMIN).build())
                .argument(PlayerArgument.of("targetPlayer"))
                .handler(executor::removeOwner)
        );
    }

    private void initCmdRestart(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.RESTART, "commands.restart.description")
                .handler(executor::restart)
        );
    }

    private void initCmdSetAutoCloseTime(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.SET_AUTO_CLOSE_TIME, "commands.set_auto_close_time.description")
                .argument(IntegerArgument.of("autoCloseTime"))
                .argument(defaultDoorArgument(false, PermissionLevel.ADMIN).build())
                .handler(executor::setAutoCloseTime)
        );
    }

    private void initCmdSetBlocksToMove(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.SET_BLOCKS_TO_MOVE, "commands.set_blocks_to_move.description")
                .argument(IntegerArgument.of("blocksToMove"))
                .argument(defaultDoorArgument(false, PermissionLevel.ADMIN).build())
                .handler(executor::setBlocksToMove)
        );
    }

    private void initCmdSetName(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.SET_NAME, "commands.set_name.description")
                .argument(StringArgument.of("name"))
                .handler(executor::setName)
        );
    }

    private void initCmdSetOpenDirection(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.SET_OPEN_DIR, "commands.set_open_direction.description")
                .argument(defaultDirectionArgument(true).build())
                .argument(defaultDoorArgument(false, PermissionLevel.ADMIN).build())
                .handler(executor::setOpenDirection)
        );
    }

    private void initCmdSpecify(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.SPECIFY, "commands.specify.description")
                .handler(executor::specify)
        );
    }

    private void initCmdStopDoors(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.STOP_DOORS, "commands.stop_doors.description")
                .handler(executor::stopDoors)
        );
    }

    private void initCmdToggle(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.TOGGLE, "commands.toggle.description")
                .argument(defaultDoorArgument(true, PermissionLevel.USER).build())
                .handler(executor::toggle)
        );
    }

    private void initCmdVersion(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.VERSION, "commands.version.description")
                .handler(executor::version)
        );
    }

    private DoorArgument.DoorArgumentBuilder defaultDoorArgument(boolean required, PermissionLevel maxPermission)
    {
        return DoorArgument.builder().required(required).name("doorRetriever")
                           .asyncSuggestions(asyncCompletions)
                           .doorRetrieverFactory(doorRetrieverFactory).maxPermission(maxPermission);
    }

    private DirectionArgument.DirectionArgumentBuilder defaultDirectionArgument(boolean required)
    {
        return DirectionArgument.builder().required(required).name("direction")
                                .parser(directionParser);
    }

    private DoorTypeArgument.DoorTypeArgumentBuilder defaultDoorTypeArgument(boolean required)
    {
        return DoorTypeArgument.builder().required(required).name("doorType").parser(doorTypeParser);
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
