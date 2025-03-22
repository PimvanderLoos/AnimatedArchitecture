package nl.pim16aap2.animatedarchitecture.spigot.core.comands;

import cloud.commandframework.context.CommandContext;
import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationType;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.AddOwnerDelayed;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAnimationRequestBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

/**
 * Class that contains the executors for all commands.
 * <p>
 * This class does not execute the commands themselves, but rather delegates the execution to the
 * {@link CommandFactory}, which in turn delegates the execution to the appropriate command.
 */
@Singleton
@Flogger
@ExtensionMethod(CompletableFutureExtensions.class)
class CommandExecutor
{
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;

    private final CommandFactory commandFactory;
    private final StructureRetrieverFactory structureRetrieverFactory;
    private final StructureAnimationRequestBuilder structureAnimationRequestBuilder;
    private final ITextFactory textFactory;
    private final ILocalizer localizer;

    @Inject
    CommandExecutor(
        CommandFactory commandFactory,
        StructureRetrieverFactory structureRetrieverFactory,
        StructureAnimationRequestBuilder structureAnimationRequestBuilder,
        ITextFactory textFactory,
        ILocalizer localizer)
    {
        this.commandFactory = commandFactory;
        this.structureRetrieverFactory = structureRetrieverFactory;
        this.structureAnimationRequestBuilder = structureAnimationRequestBuilder;
        this.textFactory = textFactory;
        this.localizer = localizer;
    }

    // NullAway doesn't see the @Nullable on permissionLevel.
    @SuppressWarnings("NullAway")
    void addOwner(CommandContext<ICommandSender> context)
    {
        final IPlayer newOwner = SpigotAdapter.wrapPlayer(context.get("newOwner"));
        final @Nullable PermissionLevel permissionLevel = nullable(context, "permissionLevel");
        final @Nullable StructureRetriever structureRetriever = nullable(context, "structureRetriever");

        final ICommandSender commandSender = context.getSender();
        if (structureRetriever != null)
        {
            commandFactory
                .newAddOwner(commandSender, structureRetriever, newOwner, permissionLevel)
                .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .handleExceptional(ex -> handleException(context, ex, "addOwner"));
        }
        else
        {
            final var data = new AddOwnerDelayed.DelayedInput(newOwner, permissionLevel);
            commandFactory
                .getAddOwnerDelayed()
                .provideDelayedInput(commandSender, data)
                .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .handleExceptional(ex -> handleException(context, ex, "addOwner"));
        }
    }

    void cancel(CommandContext<ICommandSender> context)
    {
        commandFactory
            .newCancel(context.getSender())
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "cancel"));
    }

    void confirm(CommandContext<ICommandSender> context)
    {
        commandFactory
            .newConfirm(context.getSender())
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "confirm"));
    }

    void debug(CommandContext<ICommandSender> context)
    {
        commandFactory
            .newDebug(context.getSender())
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "debug"));
    }

    void delete(CommandContext<ICommandSender> context)
    {
        final StructureRetriever structureRetriever = context.get("structureRetriever");
        commandFactory
            .newDelete(context.getSender(), structureRetriever)
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "delete"));
    }

    void info(CommandContext<ICommandSender> context)
    {
        final StructureRetriever structureRetriever = context.get("structureRetriever");
        commandFactory
            .newInfo(context.getSender(), structureRetriever)
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "info"));
    }

    void inspectPowerBlock(CommandContext<ICommandSender> context)
    {
        commandFactory
            .newInspectPowerBlock(context.getSender())
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "inspectPowerBlock"));
    }

    void listStructures(CommandContext<ICommandSender> context)
    {
        final @Nullable String query = context.<String>getOptional("structureName").orElse("");
        final StructureRetriever retriever = structureRetrieverFactory
            .search(
                context.getSender(),
                query,
                StructureRetrieverFactory.StructureFinderMode.NEW_INSTANCE,
                PermissionLevel.USER)
            .asRetriever();
        commandFactory
            .newListStructures(context.getSender(), retriever)
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "listStructures"));
    }

    void lock(CommandContext<ICommandSender> context)
    {
        final StructureRetriever structureRetriever = context.get("structureRetriever");
        final boolean lockStatus = context.get("lockStatus");
        final boolean sendUpdatedInfo = context.getOrDefault("sendUpdatedInfo", false);
        commandFactory
            .newLock(context.getSender(), structureRetriever, lockStatus, sendUpdatedInfo)
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "lock"));
    }

    void menu(CommandContext<ICommandSender> context)
    {
        final @Nullable Player player = nullable(context, "targetPlayer");
        final ICommandSender commandSender = context.getSender();
        final IPlayer targetPlayer;
        if (player != null)
            targetPlayer = SpigotAdapter.wrapPlayer(player);
        else
            targetPlayer = commandSender.getPlayer().orElseThrow(IllegalArgumentException::new);

        commandFactory.newMenu(commandSender, targetPlayer)
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "menu"));
    }

    void movePowerBlock(CommandContext<ICommandSender> context)
    {
        final StructureRetriever structureRetriever = context.get("structureRetriever");
        commandFactory
            .newMovePowerBlock(context.getSender(), structureRetriever)
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "movePowerBlock"));
    }

    // NullAway doesn't see the @Nullable on structureName.
    @SuppressWarnings("NullAway")
    void newStructure(CommandContext<ICommandSender> context)
    {
        final StructureType structureType = context.get("structureType");
        final @Nullable String structureName = nullable(context, "structureName");
        commandFactory
            .newNewStructure(context.getSender(), structureType, structureName)
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "newStructure"));
    }

    void removeOwner(CommandContext<ICommandSender> context)
    {
        final IPlayer targetPlayer = SpigotAdapter.wrapPlayer(context.get("targetPlayer"));
        final @Nullable StructureRetriever structureRetriever = nullable(context, "structureRetriever");

        final ICommandSender commandSender = context.getSender();
        if (structureRetriever != null)
        {
            commandFactory
                .newRemoveOwner(commandSender, structureRetriever, targetPlayer)
                .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .handleExceptional(ex -> handleException(context, ex, "removeOwner"));
        }
        else
        {
            commandFactory
                .getRemoveOwnerDelayed()
                .provideDelayedInput(commandSender, targetPlayer)
                .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .handleExceptional(ex -> handleException(context, ex, "removeOwner"));
        }
    }

    void restart(CommandContext<ICommandSender> context)
    {
        commandFactory.newRestart(context.getSender())
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "restart"));
    }

    void setBlocksToMove(CommandContext<ICommandSender> context)
    {
        final int blocksToMove = context.get("blocksToMove");
        final @Nullable StructureRetriever structureRetriever = nullable(context, "structureRetriever");

        final ICommandSender commandSender = context.getSender();
        if (structureRetriever != null)
            commandFactory
                .newSetBlocksToMove(commandSender, structureRetriever, blocksToMove)
                .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .handleExceptional(ex -> handleException(context, ex, "setBlocksToMove"));
        else
            commandFactory
                .getSetBlocksToMoveDelayed()
                .provideDelayedInput(commandSender, blocksToMove)
                .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .handleExceptional(ex -> handleException(context, ex, "setBlocksToMove"));
    }

    void setName(CommandContext<ICommandSender> context)
    {
        commandFactory
            .newSetName(context.getSender(), context.get("name"))
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "setName"));
    }

    void setOpenStatus(CommandContext<ICommandSender> context)
    {
        final boolean isOpen = context.get("isOpen");
        final boolean sendUpdatedInfo = context.getOrDefault("sendUpdatedInfo", false);
        final ICommandSender commandSender = context.getSender();
        final @Nullable StructureRetriever structureRetriever = nullable(context, "structureRetriever");

        if (structureRetriever != null)
            commandFactory
                .newSetOpenStatus(commandSender, structureRetriever, isOpen, sendUpdatedInfo)
                .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .handleExceptional(ex -> handleException(context, ex, "setOpenStatus"));
        else
            commandFactory
                .getSetOpenStatusDelayed()
                .provideDelayedInput(commandSender, isOpen)
                .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .handleExceptional(ex -> handleException(context, ex, "setOpenStatus"));
    }

    void setOpenDirection(CommandContext<ICommandSender> context)
    {
        final MovementDirection direction = context.get("direction");
        final boolean sendUpdatedInfo = context.getOrDefault("sendUpdatedInfo", false);
        final ICommandSender commandSender = context.getSender();
        final @Nullable StructureRetriever structureRetriever = nullable(context, "structureRetriever");

        if (structureRetriever != null)
            commandFactory
                .newSetOpenDirection(commandSender, structureRetriever, direction, sendUpdatedInfo)
                .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .handleExceptional(ex -> handleException(context, ex, "setOpenDirection"));
        else
            commandFactory
                .getSetOpenDirectionDelayed()
                .provideDelayedInput(commandSender, direction)
                .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .handleExceptional(ex -> handleException(context, ex, "setOpenDirection"));
    }

    void specify(CommandContext<ICommandSender> context)
    {
        commandFactory
            .newSpecify(context.getSender(), context.get("data"))
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "specify"));
    }

    void stopStructures(CommandContext<ICommandSender> context)
    {
        commandFactory
            .newStopStructures(context.getSender())
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "stopStructures"));
    }

    private void toggle(CommandContext<ICommandSender> context, StructureActionType structureActionType)
    {
        structureAnimationRequestBuilder
            .builder()
            .structure(context.<StructureRetriever>get("structureRetriever"))
            .structureActionCause(
                context.getSender().isPlayer() ?
                    StructureActionCause.PLAYER :
                    StructureActionCause.SERVER)
            .structureActionType(structureActionType)
            .responsible(context.getSender().getPlayer().orElse(null))
            .messageReceiver(context.getSender())
            .build()
            .execute()
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "toggle"));
    }

    void toggle(CommandContext<ICommandSender> context)
    {
        toggle(context, StructureActionType.TOGGLE);
    }

    void open(CommandContext<ICommandSender> context)
    {
        toggle(context, StructureActionType.OPEN);
    }

    void close(CommandContext<ICommandSender> context)
    {
        toggle(context, StructureActionType.CLOSE);
    }

    void preview(CommandContext<ICommandSender> context)
    {
        structureAnimationRequestBuilder
            .builder()
            .structure(context.<StructureRetriever>get("structureRetriever"))
            .structureActionCause(StructureActionCause.PLAYER)
            .structureActionType(StructureActionType.TOGGLE)
            .responsible(context.getSender().getPlayer().orElse(null))
            .animationType(AnimationType.PREVIEW)
            .build()
            .execute()
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "preview"));
    }

    void version(CommandContext<ICommandSender> context)
    {
        commandFactory
            .newVersion(context.getSender())
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "version"));
    }

    void updateCreator(CommandContext<ICommandSender> context)
    {
        commandFactory
            .newUpdateCreator(context.getSender(), context.get("stepName"), context.getOrDefault("stepValue", null))
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleException(context, ex, "updateCreator"));
    }

    private <T> @Nullable T nullable(CommandContext<ICommandSender> context, String key)
    {
        final @Nullable T ret = context.getOrDefault(key, null);
        return ret;
    }

    /**
     * Sends a generic "failed to execute command" error message to command sender if the sender is a player.
     *
     * @param context
     *     The command context to get the command sender from.
     */
    private void sendGenericErrorMessageToPlayer(CommandContext<ICommandSender> context)
    {
        context.getSender().getPlayer().ifPresent(player ->
            player.sendMessage(
                textFactory
                    .newText()
                    .append(localizer.getMessage("commands.base.error.generic"), TextType.ERROR)
            ));
    }

    /**
     * Handles an exception that occurred during command execution.
     * <p>
     * This method will send a generic error message to the player (if the sender is a player) and log the exception.
     *
     * @param context
     *     The command context.
     * @param ex
     *     The exception that occurred.
     * @param commandName
     *     The name of the command that failed.
     */
    private void handleException(CommandContext<ICommandSender> context, Throwable ex, String commandName)
    {
        sendGenericErrorMessageToPlayer(context);
        log.atSevere().withCause(ex).log("Failed to execute %s command!", commandName);
    }
}
