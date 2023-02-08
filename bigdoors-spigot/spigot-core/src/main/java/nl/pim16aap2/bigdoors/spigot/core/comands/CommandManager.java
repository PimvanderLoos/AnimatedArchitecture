package nl.pim16aap2.bigdoors.spigot.core.comands;

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
import lombok.extern.flogger.Flogger;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.commands.CommandDefinition;
import nl.pim16aap2.bigdoors.core.commands.ICommandSender;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.structures.PermissionLevel;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.core.util.structureretriever.StructureRetrieverFactory;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.text;

@Singleton
@Flogger
public final class CommandManager
{
    private final JavaPlugin plugin;
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final StructureRetrieverFactory structureRetrieverFactory;
    private volatile @Nullable PaperCommandManager<ICommandSender> manager;
    private boolean asyncCompletions = false;
    private final BukkitAudiences bukkitAudiences;
    private final StructureTypeParser structureTypeParser;
    private final DirectionParser directionParser;
    private final IsOpenParser isOpenParser;
    private final CommandExecutor executor;

    @Inject//
    CommandManager(
        JavaPlugin plugin, ILocalizer localizer, ITextFactory textFactory,
        StructureRetrieverFactory structureRetrieverFactory, StructureTypeParser structureTypeParser,
        DirectionParser directionParser, IsOpenParser isOpenParser, CommandExecutor executor)
    {
        this.plugin = plugin;
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.structureRetrieverFactory = structureRetrieverFactory;
        this.structureTypeParser = structureTypeParser;
        this.directionParser = directionParser;
        this.bukkitAudiences = BukkitAudiences.create(plugin);
        this.isOpenParser = isOpenParser;
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
        registerBrigadier(manager);

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
        initCmdListStructures(manager, builder);
        initCmdLock(manager, builder);
        initCmdMenu(manager, builder);
        initCmdMovePowerBlock(manager, builder);
        initCmdNewStructure(manager, builder);
        initCmdRemoveOwner(manager, builder);
        initCmdRestart(manager, builder);
        initCmdSetBlocksToMove(manager, builder);
        initCmdSetName(manager, builder);
        initCmdSetOpenStatus(manager, builder);
        initCmdSetOpenDirection(manager, builder);
        initCmdSpecify(manager, builder);
        initCmdStopStructures(manager, builder);
        initCmdToggle(manager, builder);
        initCmdPreview(manager, builder);
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
                .argument(defaultStructureArgument(false, PermissionLevel.ADMIN).build())
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
            baseInit(builder, CommandDefinition.CONFIRM, "commands.confirm.description")
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
                .argument(defaultStructureArgument(true, PermissionLevel.ADMIN).build())
                .handler(executor::delete)
        );
    }

    private void initCmdInfo(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.INFO, "commands.info.description")
                .argument(defaultStructureArgument(true, PermissionLevel.USER).build())
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

    private void initCmdListStructures(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.LIST_STRUCTURES, "commands.list_structures.description")
                .argument(StringArgument.optional("structureName"))
                .handler(executor::listStructures)
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
                .argument(defaultStructureArgument(true, PermissionLevel.ADMIN).build())
                .handler(executor::movePowerBlock)
        );
    }

    private void initCmdNewStructure(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.NEW_STRUCTURE, "commands.new_structure.description")
                .argument(defaultStructureTypeArgument(true).build())
                .argument(StringArgument.<ICommandSender>builder("structureName").asOptional().build())
                .handler(executor::newStructure)
        );
    }

    private void initCmdRemoveOwner(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.REMOVE_OWNER, "commands.remove_owner.description")
                .argument(defaultStructureArgument(true, PermissionLevel.ADMIN).build())
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

    private void initCmdSetBlocksToMove(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.SET_BLOCKS_TO_MOVE, "commands.set_blocks_to_move.description")
                .argument(IntegerArgument.of("blocksToMove"))
                .argument(defaultStructureArgument(false, PermissionLevel.ADMIN).build())
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

    private void initCmdSetOpenStatus(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.SET_OPEN_STATUS, "commands.set_open_status.description")
                .argument(defaultOpenStatusArgument(true).build())
                .argument(defaultStructureArgument(false, PermissionLevel.ADMIN).build())
                .handler(executor::setOpenStatus)
        );
    }

    private void initCmdSetOpenDirection(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.SET_OPEN_DIRECTION, "commands.set_open_direction.description")
                .argument(defaultDirectionArgument(true).build())
                .argument(defaultStructureArgument(false, PermissionLevel.ADMIN).build())
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

    private void initCmdStopStructures(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.STOP_STRUCTURES, "commands.stop_structures.description")
                .handler(executor::stopStructures)
        );
    }

    private void initCmdToggle(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.TOGGLE, "commands.toggle.description")
                .argument(defaultStructureArgument(true, PermissionLevel.USER).build())
                .handler(executor::toggle)
        );
    }

    private void initCmdPreview(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        final CommandDefinition previewDefinition =
            new CommandDefinition("PREVIEW",
                                  CommandDefinition.PREFIX_USER + "preview",
                                  CommandDefinition.PREFIX_ADMIN + "bypass.preview");

        manager.command(
            baseInit(builder, previewDefinition, "commands.preview.description")
                .argument(defaultStructureArgument(true, PermissionLevel.USER).build())
                .handler(executor::preview)
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

    private StructureArgument.StructureArgumentBuilder defaultStructureArgument(
        boolean required, PermissionLevel maxPermission)
    {
        return StructureArgument.builder().required(required).name("structureRetriever")
                                .asyncSuggestions(asyncCompletions)
                                .structureRetrieverFactory(structureRetrieverFactory).maxPermission(maxPermission);
    }

    private IsOpenArgument.IsOpenArgumentBuilder defaultOpenStatusArgument(boolean required)
    {
        return IsOpenArgument.builder().required(required).name("isOpen").parser(isOpenParser);
    }

    private DirectionArgument.DirectionArgumentBuilder defaultDirectionArgument(boolean required)
    {
        return DirectionArgument.builder().required(required).name("direction").parser(directionParser);
    }

    private StructureTypeArgument.StructureTypeArgumentBuilder defaultStructureTypeArgument(boolean required)
    {
        return StructureTypeArgument.builder().required(required).name("structureType").parser(structureTypeParser);
    }

    private static void registerBrigadier(BukkitCommandManager<ICommandSender> manager)
    {
        try
        {
            if (manager.hasCapability(CloudBukkitCapabilities.BRIGADIER) &&
                manager.hasCapability(CloudBukkitCapabilities.COMMODORE_BRIGADIER))
                manager.registerBrigadier();
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to register brigadier!");
        }
    }

    private PaperCommandManager<ICommandSender> newManager()
        throws Exception
    {
        return new PaperCommandManager<>(
            plugin,
            CommandExecutionCoordinator.simpleCoordinator(),
            SpigotAdapter::wrapCommandSender,
            SpigotAdapter::unwrapCommandSender
        );
    }
}
