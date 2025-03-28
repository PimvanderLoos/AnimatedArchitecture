package nl.pim16aap2.animatedarchitecture.spigot.core.comands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.CommandArgument;
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
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPermissionsManager;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandDefinition;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.spigot.core.config.ConfigSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.PlayerFactorySpigot;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.text;

@Singleton
@Flogger
public final class CommandManager
{
    private final JavaPlugin plugin;
    private final ConfigSpigot config;
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final IPermissionsManager permissionsManager;
    private final StructureTypeManager structureTypeManager;
    private final StructureRetrieverFactory structureRetrieverFactory;
    private volatile @Nullable PaperCommandManager<ICommandSender> manager;
    private boolean asyncCompletions = false;
    private final BukkitAudiences bukkitAudiences;
    private final PlayerFactorySpigot playerFactory;
    private final StructureTypeParser structureTypeParser;
    private final DirectionParser directionParser;
    private final IsOpenParser isOpenParser;
    private final CommandExecutor commandExecutor;
    private final IExecutor executor;

    @Inject
    CommandManager(
        JavaPlugin plugin,
        ConfigSpigot config,
        ILocalizer localizer,
        ITextFactory textFactory,
        IPermissionsManager permissionsManager,
        StructureTypeManager structureTypeManager,
        StructureRetrieverFactory structureRetrieverFactory,
        PlayerFactorySpigot playerFactory,
        StructureTypeParser structureTypeParser,
        DirectionParser directionParser,
        IsOpenParser isOpenParser,
        CommandExecutor commandExecutor,
        IExecutor executor)
    {
        this.plugin = plugin;
        this.config = config;
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.permissionsManager = permissionsManager;
        this.structureTypeManager = structureTypeManager;
        this.structureRetrieverFactory = structureRetrieverFactory;
        this.playerFactory = playerFactory;
        this.structureTypeParser = structureTypeParser;
        this.directionParser = directionParser;
        this.bukkitAudiences = BukkitAudiences.create(plugin);
        this.isOpenParser = isOpenParser;
        this.commandExecutor = commandExecutor;
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
            manager.registerAsynchronousCompletions();
            asyncCompletions = true;
        }

        final CommandConfirmationManager<ICommandSender> confirmationManager = new CommandConfirmationManager<>(
            30L,
            TimeUnit.SECONDS,
            context -> context
                .getCommandContext()
                .getSender()
                .sendMessage(textFactory.newText().append(
                    localizer.getMessage("commands.spigot.confirmation.message"),
                    TextType.INFO,
                    arg -> arg.clickable(
                        localizer.getMessage("commands.spigot.confirmation.message.arg0.message"),
                        "/AnimatedArchitecture confirm",
                        localizer.getMessage("commands.spigot.confirmation.message.arg0.hint")))),
            sender -> sender.sendError(
                textFactory,
                localizer.getMessage("commands.spigot.confirmation.error.no_pending"))
        );

        confirmationManager.registerConfirmationProcessor(this.manager);

        new MinecraftExceptionHandler<ICommandSender>()
            .withInvalidSyntaxHandler()
            .withInvalidSenderHandler()
            .withNoPermissionHandler()
            .withArgumentParsingHandler()
            .withCommandExecutionHandler()
            .withDecorator(component -> text()
                .append(text("[", NamedTextColor.DARK_GRAY))
                .append(text("AnimatedArchitecture", NamedTextColor.GOLD))
                .append(text("] ", NamedTextColor.DARK_GRAY))
                .append(component)
                .build())
            .apply(manager, sender -> this.bukkitAudiences.sender(PlayerFactorySpigot.unwrapCommandSender(sender)));

        initCommands(manager);
    }

    private void initCommands(BukkitCommandManager<ICommandSender> manager)
    {
        final String[] commandNames = config.getCommandAliases().toArray(new String[0]);
        if (commandNames.length == 0)
            throw new IllegalArgumentException("No command aliases specified!");

        final String[] aliases = Arrays.copyOfRange(commandNames, 1, commandNames.length);
        final Command.Builder<ICommandSender> builder = manager.commandBuilder(commandNames[0], aliases);

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
        initCmdSetOpenDirection(manager, builder);
        initCmdSetOpenStatus(manager, builder);
        initCmdSpecify(manager, builder);
        initCmdStopStructures(manager, builder);
        initCmdToggle(manager, builder);
        initCmdOpen(manager, builder);
        initCmdClose(manager, builder);
        initCmdPreview(manager, builder);
        initCmdVersion(manager, builder);
        initCmdUpdateCreator(manager, builder);

        builder.build();
    }

    private void initCmdHelp(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        final MinecraftHelp<ICommandSender> minecraftHelp = new MinecraftHelp<>(
            "/animatedarchitecture help",
            sender -> this.bukkitAudiences.sender(PlayerFactorySpigot.unwrapCommandSender(sender)),
            manager
        );

        manager.command(builder
            .literal("help")
            .argument(StringArgument.optional("query", StringArgument.StringMode.GREEDY))
            .handler(context -> minecraftHelp
                .queryCommands(Objects.requireNonNull(context.getOrDefault("query", "")), context.getSender()))
        );
    }

    private Command.Builder<ICommandSender> baseInit(
        Command.Builder<ICommandSender> builder,
        String name,
        CommandDefinition cmd,
        String descriptionKey)
    {
        return builder.literal(name.replace("_", "").toLowerCase(Locale.ROOT))
            .permission(cmd.getLowestPermission())
            .meta(CommandMeta.DESCRIPTION, localizer.getMessage(descriptionKey));
    }

    private Command.Builder<ICommandSender> baseInit(
        Command.Builder<ICommandSender> builder,
        CommandDefinition cmd,
        String descriptionKey)
    {
        return baseInit(builder, cmd.getName(), cmd, descriptionKey);
    }

    private void initCmdAddOwner(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
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
                .argument(defaultStructureArgument(false, StructureAttribute.ADD_OWNER).build())
                .handler(commandExecutor::addOwner)
        );
    }

    private void initCmdCancel(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.CANCEL, "commands.cancel.description")
                .handler(commandExecutor::cancel)
        );
    }

    private void initCmdConfirm(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.CONFIRM, "commands.confirm.description")
                .handler(commandExecutor::confirm)
        );
    }

    private void initCmdDebug(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.DEBUG, "commands.debug.description")
                .handler(commandExecutor::debug)
        );
    }

    private void initCmdDelete(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.DELETE, "commands.delete.description")
                .argument(defaultStructureArgument(true, StructureAttribute.DELETE).build())
                .handler(commandExecutor::delete)
        );
    }

    private void initCmdInfo(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.INFO, "commands.info.description")
                .argument(defaultStructureArgument(true, StructureAttribute.INFO).build())
                .handler(commandExecutor::info)
        );
    }

    private void initCmdInspectPowerBlock(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.INSPECT_POWER_BLOCK, "commands.inspect_power_block.description")
                .handler(commandExecutor::inspectPowerBlock)
        );
    }

    private void initCmdListStructures(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.LIST_STRUCTURES, "commands.list_structures.description")
                .argument(StringArgument.optional("structureName"))
                .handler(commandExecutor::listStructures)
        );
    }

    private void initCmdLock(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.LOCK, "commands.lock.description")
                .argument(BooleanArgument.<ICommandSender>builder("lockStatus").withLiberal(true).build())
                .argument(defaultStructureArgument(true, StructureAttribute.INFO).build())
                .argument(newHiddenSendInfoArgument().build())
                .handler(commandExecutor::lock)
        );
    }

    private void initCmdMenu(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.MENU, "commands.menu.description")
                .argument(PlayerArgument.<ICommandSender>builder("targetPlayer").asOptional())
                .handler(commandExecutor::menu)
        );
    }

    private void initCmdMovePowerBlock(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.MOVE_POWER_BLOCK, "commands.move_power_block.description")
                .argument(defaultStructureArgument(true, StructureAttribute.RELOCATE_POWERBLOCK).build())
                .handler(commandExecutor::movePowerBlock)
        );
    }

    private void initCmdNewStructure(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.NEW_STRUCTURE, "commands.new_structure.description")
                .argument(defaultStructureTypeArgument(true).build())
                .argument(StringArgument.<ICommandSender>builder("structureName").asOptional().build())
                .permission(this::hasPermissionForNewStructure)
                .handler(commandExecutor::newStructure)
        );
    }

    private boolean hasPermissionForNewStructure(ICommandSender commandSender)
    {
        if (!(commandSender instanceof IPlayer player))
            return true;

        if (!permissionsManager.hasPermission(player, CommandDefinition.NEW_STRUCTURE.getLowestPermission()))
            return false;

        return structureTypeManager
            .getEnabledStructureTypes().stream()
            .anyMatch(type -> permissionsManager.hasPermissionToCreateStructure(player, type));
    }

    private void initCmdRemoveOwner(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.REMOVE_OWNER, "commands.remove_owner.description")
                .argument(PlayerArgument.of("targetPlayer"))
                .argument(defaultStructureArgument(false, StructureAttribute.REMOVE_OWNER).build())
                .handler(commandExecutor::removeOwner)
        );
    }

    private void initCmdRestart(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.RESTART, "commands.restart.description")
                .handler(commandExecutor::restart)
        );
    }

    private void initCmdSetBlocksToMove(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.SET_BLOCKS_TO_MOVE, "commands.set_blocks_to_move.description")
                .argument(IntegerArgument.of("blocksToMove"))
                .argument(
                    defaultStructureArgument(
                        false,
                        StructureAttribute.BLOCKS_TO_MOVE,
                        Property.BLOCKS_TO_MOVE
                    ).build())
                .handler(commandExecutor::setBlocksToMove)
        );
    }

    private void initCmdSetName(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.SET_NAME, "commands.set_name.description")
                .argument(StringArgument.of("name"))
                .handler(commandExecutor::setName)
        );
    }

    private void initCmdSetOpenStatus(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.SET_OPEN_STATUS, "commands.set_open_status.description")
                .argument(defaultOpenStatusArgument(true).build())
                .argument(defaultStructureArgument(false, StructureAttribute.OPEN_STATUS, Property.OPEN_STATUS).build())
                .argument(newHiddenSendInfoArgument().build())
                .handler(commandExecutor::setOpenStatus)
        );
    }

    private void initCmdSetOpenDirection(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.SET_OPEN_DIRECTION, "commands.set_open_direction.description")
                .argument(defaultDirectionArgument(true).build())
                .argument(defaultStructureArgument(false, StructureAttribute.OPEN_DIRECTION).build())
                .argument(newHiddenSendInfoArgument().build())
                .handler(commandExecutor::setOpenDirection)
        );
    }

    private void initCmdSpecify(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.SPECIFY, "commands.specify.description")
                .argument(StringArgument.of("data"))
                .handler(commandExecutor::specify)
        );
    }

    private void initCmdStopStructures(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.STOP_STRUCTURES, "commands.stop_structures.description")
                .handler(commandExecutor::stopStructures)
        );
    }

    private void initCmdToggle(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.TOGGLE, "commands.toggle.description")
                .argument(defaultStructureArgument(true, StructureAttribute.TOGGLE).build())
                .handler(commandExecutor::toggle)
        );
    }

    private void initCmdOpen(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, "open", CommandDefinition.TOGGLE, "commands.open.description")
                .argument(defaultStructureArgument(true, StructureAttribute.TOGGLE, Property.OPEN_STATUS).build())
                .handler(commandExecutor::open)
        );
    }

    private void initCmdClose(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, "close", CommandDefinition.TOGGLE, "commands.close.description")
                .argument(defaultStructureArgument(true, StructureAttribute.TOGGLE, Property.OPEN_STATUS).build())
                .handler(commandExecutor::close)
        );
    }

    private void initCmdPreview(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        final CommandDefinition previewDefinition =
            new CommandDefinition(
                "PREVIEW",
                Constants.PERMISSION_PREFIX_USER + "preview",
                Constants.PERMISSION_PREFIX_ADMIN + "bypass.preview");

        manager.command(
            baseInit(builder, previewDefinition, "commands.preview.description")
                .argument(defaultStructureArgument(true, StructureAttribute.TOGGLE).build())
                .handler(commandExecutor::preview)
        );
    }

    private void initCmdVersion(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            baseInit(builder, CommandDefinition.VERSION, "commands.version.description")
                .handler(commandExecutor::version)
        );
    }

    private void initCmdUpdateCreator(
        BukkitCommandManager<ICommandSender> manager,
        Command.Builder<ICommandSender> builder)
    {
        manager.command(
            builder.literal(CommandDefinition.UPDATE_CREATOR.getName().replace("_", "").toLowerCase(Locale.ROOT))
                .permission(CommandDefinition.UPDATE_CREATOR.getLowestPermission())
                .argument(StringArgument.of("stepName"))
                .argument(StringArgument.optional("stepValue"))
                .hidden()
                .handler(commandExecutor::updateCreator)
        );
    }

    /**
     * Creates a new {@link StructureArgument} with the default values.
     *
     * @param required
     *     True if the argument is required, false otherwise.
     * @param structureAttribute
     *     The {@link StructureAttribute} to use for the permission level.
     * @param properties
     *     The (optional) properties that the structure must have.
     *     <p>
     *     When provided, structures without these properties will be filtered out of the suggestions.
     * @return A new {@link StructureArgument} with the default values.
     */
    private StructureArgument.StructureArgumentBuilder defaultStructureArgument(
        boolean required,
        StructureAttribute structureAttribute,
        Property<?>... properties)
    {
        return StructureArgument
            .builder()
            .required(required)
            .name("structureRetriever")
            .asyncSuggestions(asyncCompletions)
            .executor(executor)
            .structureRetrieverFactory(structureRetrieverFactory)
            .properties(properties)
            .maxPermission(structureAttribute.getPermissionLevel());
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

    private static CommandArgument.Builder<ICommandSender, Boolean> newHiddenSendInfoArgument()
    {
        return BooleanArgument
            .<ICommandSender>builder("sendUpdatedInfo")
            .withLiberal(true)
            .asOptionalWithDefault("false")
            .withSuggestionsProvider((iCommandSenderCommandContext, s) -> Collections.emptyList())
            .withDefaultDescription(ArgumentDescription.empty());
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
            playerFactory::wrapCommandSender,
            PlayerFactorySpigot::unwrapCommandSender
        );
    }
}
