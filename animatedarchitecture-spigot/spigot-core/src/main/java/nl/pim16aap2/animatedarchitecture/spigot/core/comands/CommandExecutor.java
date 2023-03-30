package nl.pim16aap2.animatedarchitecture.spigot.core.comands;

import cloud.commandframework.context.CommandContext;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationType;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.commands.AddOwnerDelayed;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Flogger
class CommandExecutor
{
    private final CommandFactory commandFactory;
    private final StructureRetrieverFactory structureRetrieverFactory;

    @Inject CommandExecutor(
        CommandFactory commandFactory,
        StructureRetrieverFactory structureRetrieverFactory)
    {
        this.commandFactory = commandFactory;
        this.structureRetrieverFactory = structureRetrieverFactory;
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
        commandFactory.newLock(context.getSender(), structureRetriever, lockStatus).run()
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
        final StructureRetriever structureRetriever = context.get("structureRetriever");
        final IPlayer targetPlayer = SpigotAdapter.wrapPlayer(context.get("targetPlayer"));
        commandFactory.newRemoveOwner(context.getSender(), structureRetriever, targetPlayer).run()
                      .exceptionally(Util::exceptionally);
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
        final ICommandSender commandSender = context.getSender();
        final @Nullable StructureRetriever structureRetriever = nullable(context, "structureRetriever");

        if (structureRetriever != null)
            commandFactory.newSetOpenStatus(commandSender, structureRetriever, isOpen).run()
                          .exceptionally(Util::exceptionally);
        else
            commandFactory.getSetOpenStatusDelayed().provideDelayedInput(commandSender, isOpen)
                          .exceptionally(Util::exceptionally);
    }

    void setOpenDirection(CommandContext<ICommandSender> context)
    {
        final MovementDirection direction = context.get("direction");
        final ICommandSender commandSender = context.getSender();
        final @Nullable StructureRetriever structureRetriever = nullable(context, "structureRetriever");

        if (structureRetriever != null)
            commandFactory.newSetOpenDirection(commandSender, structureRetriever, direction).run()
                          .exceptionally(Util::exceptionally);
        else
            commandFactory.getSetOpenDirectionDelayed().provideDelayedInput(commandSender, direction)
                          .exceptionally(Util::exceptionally);
    }

    void specify(CommandContext<ICommandSender> context)
    {
        throw new UnsupportedOperationException("Not implemented!");
    }

    void stopStructures(CommandContext<ICommandSender> context)
    {
        commandFactory.newStopStructures(context.getSender()).run().exceptionally(Util::exceptionally);
    }

    void toggle(CommandContext<ICommandSender> context)
    {
        commandFactory.newToggle(context.getSender(), context.<StructureRetriever>get("structureRetriever")).run()
                      .exceptionally(Util::exceptionally);
    }

    void preview(CommandContext<ICommandSender> context)
    {
        commandFactory.newToggle(
            context.getSender(), StructureActionType.TOGGLE, AnimationType.PREVIEW,
            context.<StructureRetriever>get("structureRetriever")).run().exceptionally(Util::exceptionally);
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
}
