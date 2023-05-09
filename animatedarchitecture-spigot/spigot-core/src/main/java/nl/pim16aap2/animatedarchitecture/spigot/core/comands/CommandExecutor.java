package nl.pim16aap2.animatedarchitecture.spigot.core.comands;

import cloud.commandframework.context.CommandContext;
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
import nl.pim16aap2.animatedarchitecture.core.structures.StructureToggleResult;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Flogger
class CommandExecutor
{
    private final CommandFactory commandFactory;
    private final StructureRetrieverFactory structureRetrieverFactory;
    private final StructureAnimationRequestBuilder structureAnimationRequestBuilder;
    private final ITextFactory textFactory;
    private final ILocalizer localizer;

    @Inject CommandExecutor(
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

    // NullAway doesn't see the @Nullable on permissionLevel. Not sure if this is because of Lombok or NullAway.
    @SuppressWarnings("NullAway")
    void addOwner(CommandContext<ICommandSender> context)
    {
        final IPlayer newOwner = SpigotAdapter.wrapPlayer(context.get("newOwner"));
        final @Nullable PermissionLevel permissionLevel = nullable(context, "permissionLevel");
        final @Nullable StructureRetriever structureRetriever = nullable(context, "structureRetriever");

        final ICommandSender commandSender = context.getSender();
        if (structureRetriever != null)
        {
            commandFactory.newAddOwner(commandSender, structureRetriever, newOwner, permissionLevel).run()
                          .exceptionally(Util::exceptionally);
        }
        else
        {
            final var data = new AddOwnerDelayed.DelayedInput(newOwner, permissionLevel);
            commandFactory.getAddOwnerDelayed().provideDelayedInput(commandSender, data)
                          .exceptionally(Util::exceptionally);
        }
    }

    void cancel(CommandContext<ICommandSender> context)
    {
        commandFactory.newCancel(context.getSender()).run().exceptionally(Util::exceptionally);
    }

    void confirm(CommandContext<ICommandSender> context)
    {
        commandFactory.newConfirm(context.getSender()).run().exceptionally(Util::exceptionally);
    }

    void debug(CommandContext<ICommandSender> context)
    {
        commandFactory.newDebug(context.getSender()).run().exceptionally(Util::exceptionally);
    }

    void delete(CommandContext<ICommandSender> context)
    {
        final StructureRetriever structureRetriever = context.get("structureRetriever");
        commandFactory.newDelete(context.getSender(), structureRetriever).run().exceptionally(Util::exceptionally);
    }

    void info(CommandContext<ICommandSender> context)
    {
        final StructureRetriever structureRetriever = context.get("structureRetriever");
        commandFactory.newInfo(context.getSender(), structureRetriever).run().exceptionally(Util::exceptionally);
    }

    void inspectPowerBlock(CommandContext<ICommandSender> context)
    {
        commandFactory.newInspectPowerBlock(context.getSender()).run().exceptionally(Util::exceptionally);
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
            .asRetriever(false);
        commandFactory.newListStructures(context.getSender(), retriever).run().exceptionally(Util::exceptionally);
    }

    void lock(CommandContext<ICommandSender> context)
    {
        final StructureRetriever structureRetriever = context.get("structureRetriever");
        final boolean lockStatus = context.get("lockStatus");
        final boolean sendUpdatedInfo = context.getOrDefault("sendUpdatedInfo", false);
        commandFactory.newLock(context.getSender(), structureRetriever, lockStatus, sendUpdatedInfo).run()
                      .exceptionally(Util::exceptionally);
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

        commandFactory.newMenu(commandSender, targetPlayer).run().exceptionally(Util::exceptionally);
    }

    void movePowerBlock(CommandContext<ICommandSender> context)
    {
        final StructureRetriever structureRetriever = context.get("structureRetriever");
        commandFactory.newMovePowerBlock(context.getSender(), structureRetriever).run()
                      .exceptionally(Util::exceptionally);
    }

    // NullAway doesn't see the @Nullable on structureName. Not sure if this is because of Lombok or NullAway.
    @SuppressWarnings("NullAway")
    void newStructure(CommandContext<ICommandSender> context)
    {
        final StructureType structureType = context.get("structureType");
        final @Nullable String structureName = nullable(context, "structureName");
        commandFactory.newNewStructure(context.getSender(), structureType, structureName).run()
                      .exceptionally(Util::exceptionally);
    }

    void removeOwner(CommandContext<ICommandSender> context)
    {
        final IPlayer targetPlayer = SpigotAdapter.wrapPlayer(context.get("targetPlayer"));
        final @Nullable StructureRetriever structureRetriever = nullable(context, "structureRetriever");

        final ICommandSender commandSender = context.getSender();
        if (structureRetriever != null)
        {
            commandFactory.newRemoveOwner(commandSender, structureRetriever, targetPlayer).run()
                          .exceptionally(Util::exceptionally);
        }
        else
        {
            commandFactory.getRemoveOwnerDelayed().provideDelayedInput(commandSender, targetPlayer)
                          .exceptionally(Util::exceptionally);
        }
    }

    void restart(CommandContext<ICommandSender> context)
    {
        commandFactory.newRestart(context.getSender()).run().exceptionally(Util::exceptionally);
    }

    void setBlocksToMove(CommandContext<ICommandSender> context)
    {
        final int blocksToMove = context.get("blocksToMove");
        final @Nullable StructureRetriever structureRetriever = nullable(context, "structureRetriever");

        final ICommandSender commandSender = context.getSender();
        if (structureRetriever != null)
            commandFactory.newSetBlocksToMove(commandSender, structureRetriever, blocksToMove).run()
                          .exceptionally(Util::exceptionally);
        else
            commandFactory.getSetBlocksToMoveDelayed().provideDelayedInput(commandSender, blocksToMove)
                          .exceptionally(Util::exceptionally);
    }

    void setName(CommandContext<ICommandSender> context)
    {
        commandFactory.newSetName(context.getSender(), context.get("name")).run().exceptionally(Util::exceptionally);
    }

    void setOpenStatus(CommandContext<ICommandSender> context)
    {
        final boolean isOpen = context.get("isOpen");
        final boolean sendUpdatedInfo = context.getOrDefault("sendUpdatedInfo", false);
        final ICommandSender commandSender = context.getSender();
        final @Nullable StructureRetriever structureRetriever = nullable(context, "structureRetriever");

        if (structureRetriever != null)
            commandFactory.newSetOpenStatus(commandSender, structureRetriever, isOpen, sendUpdatedInfo).run()
                          .exceptionally(Util::exceptionally);
        else
            commandFactory.getSetOpenStatusDelayed().provideDelayedInput(commandSender, isOpen)
                          .exceptionally(Util::exceptionally);
    }

    void setOpenDirection(CommandContext<ICommandSender> context)
    {
        final MovementDirection direction = context.get("direction");
        final boolean sendUpdatedInfo = context.getOrDefault("sendUpdatedInfo", false);
        final ICommandSender commandSender = context.getSender();
        final @Nullable StructureRetriever structureRetriever = nullable(context, "structureRetriever");

        if (structureRetriever != null)
            commandFactory.newSetOpenDirection(commandSender, structureRetriever, direction, sendUpdatedInfo).run()
                          .exceptionally(Util::exceptionally);
        else
            commandFactory.getSetOpenDirectionDelayed().provideDelayedInput(commandSender, direction)
                          .exceptionally(Util::exceptionally);
    }

    void specify(CommandContext<ICommandSender> context)
    {
        commandFactory.newSpecify(context.getSender(), context.get("data")).run().exceptionally(Util::exceptionally);
    }

    void stopStructures(CommandContext<ICommandSender> context)
    {
        commandFactory.newStopStructures(context.getSender()).run().exceptionally(Util::exceptionally);
    }

    void toggle(CommandContext<ICommandSender> context)
    {
        structureAnimationRequestBuilder
            .builder()
            .structure(context.<StructureRetriever>get("structureRetriever"))
            .structureActionCause(
                context.getSender().isPlayer() ? StructureActionCause.PLAYER : StructureActionCause.SERVER)
            .structureActionType(StructureActionType.TOGGLE)
            .responsible(context.getSender().getPlayer().orElse(null))
            .messageReceiver(context.getSender())
            .build()
            .execute()
            .exceptionally(ex -> handleException(context, ex, StructureToggleResult.ERROR));
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
            .exceptionally(ex -> handleException(context, ex, StructureToggleResult.ERROR));
    }

    void version(CommandContext<ICommandSender> context)
    {
        commandFactory.newVersion(context.getSender()).run().exceptionally(Util::exceptionally);
    }

    void updateCreator(CommandContext<ICommandSender> context)
    {
        commandFactory.newUpdateCreator(
                          context.getSender(),
                          context.get("stepName"),
                          context.getOrDefault("stepValue", null))
                      .run().exceptionally(Util::exceptionally);
    }

    private <T> @Nullable T nullable(CommandContext<ICommandSender> context, String key)
    {
        return context.<@Nullable T>getOrDefault(key, null);
    }

    /**
     * Sends a generic error message to the command sender.
     *
     * @param context
     *     The command context to get the command sender from.
     */
    private void sendGenericError(CommandContext<ICommandSender> context)
    {
        context.getSender().sendMessage(
            textFactory.newText().append(localizer.getMessage("commands.base.error.generic"), TextType.ERROR));
    }

    /**
     * Logs the exception and sends a generic error message to the command sender.
     * <p>
     * See {@link #sendGenericError(CommandContext)} and {@link Util#exceptionally(Throwable, T)}.
     *
     * @param context
     *     The command context to get the command sender from.
     * @param ex
     *     The exception to log.
     * @param defaultValue
     *     The default value to return.
     * @param <T>
     *     The type of the default value.
     * @return The default value.
     */
    @Contract("_, _, _ -> param3")
    private <T> @Nullable T handleException(
        CommandContext<ICommandSender> context,
        Throwable ex,
        @Nullable T defaultValue)
    {
        sendGenericError(context);
        return Util.exceptionally(ex, defaultValue);
    }
}
