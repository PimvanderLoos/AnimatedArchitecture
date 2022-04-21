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
import nl.pim16aap2.bigdoors.commands.CommandFactory;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.text;

@Singleton
public final class CommandManager
{
    private final JavaPlugin plugin;
    private final ILocalizer localizer;
    private final CommandFactory commandFactory;
    private final DoorRetrieverFactory doorRetrieverFactory;
    private volatile @Nullable BukkitCommandManager<ICommandSender> manager;
    private boolean asyncCompletions = false;
    private final BukkitAudiences bukkitAudiences;
    private final DoorTypeParser doorTypeParser;
    private final DirectionParser directionParser;

    @Inject//
    CommandManager(
        JavaPlugin plugin, ILocalizer localizer, CommandFactory commandFactory,
        DoorRetrieverFactory doorRetrieverFactory, DoorTypeParser doorTypeParser, DirectionParser directionParser)
    {
        this.plugin = plugin;
        this.localizer = localizer;
        this.commandFactory = commandFactory;
        this.doorRetrieverFactory = doorRetrieverFactory;
        this.doorTypeParser = doorTypeParser;
        this.directionParser = directionParser;
        this.bukkitAudiences = BukkitAudiences.create(plugin);
    }

    // IntelliJ struggles to understand that the manager cannot be null.
    @SuppressWarnings("ConstantConditions")
    public synchronized void init()
        throws Exception
    {
        if (manager != null)
            throw new IllegalStateException("Trying to instantiate Cloud manage again!");
        manager = Util.requireNonNull(newManager(), "Cloud manager");

        if (manager.queryCapability(CloudBukkitCapabilities.BRIGADIER))
            manager.registerBrigadier();

        if (manager.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION))
        {
            ((PaperCommandManager<ICommandSender>) manager).registerAsynchronousCompletions();
            asyncCompletions = true;
        }

        final CommandConfirmationManager<ICommandSender> confirmationManager = new CommandConfirmationManager<>(
            30L, TimeUnit.SECONDS,
            context -> context.getCommandContext().getSender().sendMessage(
                ChatColor.RED + "Confirmation required. Confirm using /bigdoors confirm."),
            sender -> sender.sendMessage(ChatColor.RED + "You don't have any pending commands.")
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

    private void initCmdAddOwner(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            builder.literal("addowner")
                   .meta(CommandMeta.DESCRIPTION, localizer.getMessage("commands.add_owner.description"))
                   .permission("bigdoors.user.addowner")
                   .argument(new DoorArgument(true, "doorRetriever", "MyDoor", null, ArgumentDescription.empty(),
                                              asyncCompletions, doorRetrieverFactory, 1))
                   .argument(PlayerArgument.of("newOwner"))
                   .argument(IntegerArgument.<ICommandSender>newBuilder("permissionLevel")
                                            .withMin(1).withMax(2).asOptionalWithDefault(2)
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
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            builder.literal("cancel")
                   .meta(CommandMeta.DESCRIPTION, localizer.getMessage("commands.cancel.description"))
                   .permission("bigdoors.user.base")
                   .handler(commandContext -> commandFactory.newCancel(commandContext.getSender()).run())
        );
    }

    private void initCmdConfirm(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            builder.literal("confirm")
                   .meta(CommandMeta.DESCRIPTION, localizer.getMessage("commands.confirm.description"))
                   .permission("bigdoors.user.base")
                   .handler(commandContext -> commandFactory.newConfirm(commandContext.getSender()).run())
        );
    }

    private void initCmdDebug(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            builder.literal("debug")
                   .meta(CommandMeta.DESCRIPTION, localizer.getMessage("commands.debug.description"))
                   .permission("bigdoors.debug")
                   .handler(commandContext -> commandFactory.newDebug(commandContext.getSender()).run())
        );
    }

    private void initCmdDelete(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdInfo(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdInspectPowerBlock(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdListDoors(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            builder.literal("listdoors")
                   .meta(CommandMeta.DESCRIPTION, localizer.getMessage("commands.list_doors.description"))
                   .permission("bigdoors.user.base")
                   .argument(StringArgument.optional("doorName"))
                   .handler(commandContext ->
                            {
                                final @Nullable String doorName = commandContext.getOrDefault("doorName", null);
                                if (doorName == null)
                                    throw new UnsupportedOperationException("Not implemented!"); // TODO: Implement this

                                final DoorRetriever retriever = doorRetrieverFactory.of(doorName);
                                commandFactory.newListDoors(commandContext.getSender(), retriever).run();
                            })
        );
    }

    private void initCmdLock(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdMenu(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            builder.literal("menu")
                   .meta(CommandMeta.DESCRIPTION, localizer.getMessage("commands.menu.description"))
                   .permission("bigdoors.user.menu")
                   .argument(PlayerArgument.<ICommandSender>newBuilder("targetPlayer").asOptional())
                   .handler(commandContext ->
                            {
                                final IPPlayer targetPlayer =
                                    commandContext.contains("targetPlayer") ?
                                    new PPlayerSpigot(commandContext.get("targetPlayer")) :
                                    commandContext.getSender().getPlayer()
                                                  .orElseThrow(IllegalArgumentException::new);
                                commandFactory.newMenu(commandContext.getSender(), targetPlayer).run();
                            })
        );
    }

    private void initCmdMovePowerBlock(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdNewDoor(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            builder.literal("newdoor")
                   .meta(CommandMeta.DESCRIPTION, localizer.getMessage("commands.new_door.description"))
                   .permission("bigdoors.user.newdoor")
                   .argument(new DoorTypeArgument(true, "doorType", "", null, ArgumentDescription.empty(),
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
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            builder.literal("removeowner")
                   .meta(CommandMeta.DESCRIPTION, localizer.getMessage("commands.remove_owner.descriptions"))
                   .permission("bigdoors.user.removeowner")
                   .argument(new DoorArgument(true, "doorRetriever", "", null, ArgumentDescription.empty(),
                                              asyncCompletions, doorRetrieverFactory, 1))
                   .argument(PlayerArgument.of("targetPlayer"))
                   .handler(commandContext ->
                            {
                                final DoorRetriever retriever = commandContext.get("doorRetriever");
                                final IPPlayer targetPlayer = new PPlayerSpigot(commandContext.get("targetPlayer"));
                                commandFactory.newRemoveOwner(commandContext.getSender(), retriever, targetPlayer)
                                              .run();
                            })
        );
    }

    private void initCmdRestart(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            builder.literal("restart")
                   .meta(CommandMeta.DESCRIPTION, localizer.getMessage("commands.restart.description"))
                   .permission("bigdoors.admin.restart")
                   .handler(commandContext -> commandFactory.newRestart(commandContext.getSender()).run())
        );
    }

    private void initCmdSetAutoCloseTime(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdSetBlocksToMove(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdSetName(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            builder.literal("setname")
                   .meta(CommandMeta.DESCRIPTION, localizer.getMessage("commands.set_name.description"))
                   .permission("bigdoors.user.base")
                   .argument(StringArgument.of("name"))
                   .handler(commandContext -> commandFactory.newSetName(commandContext.getSender(),
                                                                        commandContext.get("name")).run())
        );
    }

    private void initCmdSetOpenDirection(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            builder.literal("setopendirection")
                   .meta(CommandMeta.DESCRIPTION, localizer.getMessage("commands.set_open_direction.description"))
                   .permission("bigdoors.user.base")
                   .argument(new DirectionArgument(true, "direction", "", null,
                                                   ArgumentDescription.empty(), directionParser))
                   .argument(new DoorArgument(false, "door", "", null, ArgumentDescription.empty(), asyncCompletions,
                                              doorRetrieverFactory, 1))
                   .handler(commandContext ->
                            {
                                final RotateDirection direction = commandContext.get("direction");
                                final ICommandSender commandSender = commandContext.getSender();
                                final @Nullable DoorRetriever doorRetriever = commandContext.getOrDefault("door", null);

                                if (doorRetriever != null)
                                    commandFactory.newSetOpenDirection(commandSender, doorRetriever, direction).run();
                                else
                                    commandFactory.getSetOpenDirectionDelayed()
                                                  .provideDelayedInput(commandSender, direction);
                            })
        );
    }

    private void initCmdSpecify(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdStopDoors(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {

    }

    private void initCmdToggle(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            builder.literal("toggledoor", "toggle")
                   .meta(CommandMeta.DESCRIPTION, localizer.getMessage("commands.toggle.description"))
                   .permission("bigdoors.user.toggle")
                   .argument(new DoorArgument(true, "door", "", null, ArgumentDescription.empty(), asyncCompletions,
                                              doorRetrieverFactory, 2))
                   .handler(commandContext ->
                                commandFactory.newToggle(commandContext.getSender(),
                                                         commandContext.<DoorRetriever>get("door")).run())
        );
    }

    private void initCmdVersion(
        BukkitCommandManager<ICommandSender> manager, Command.Builder<ICommandSender> builder)
    {
        manager.command(
            builder.literal("version")
                   .meta(CommandMeta.DESCRIPTION, localizer.getMessage("commands.version.description"))
                   .permission("bigdoors.admin.version")
                   .handler(commandContext -> commandFactory.newVersion(commandContext.getSender()).run())
        );
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
