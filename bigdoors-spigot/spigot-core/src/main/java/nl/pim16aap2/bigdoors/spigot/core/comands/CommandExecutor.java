package nl.pim16aap2.bigdoors.spigot.core.comands;

import cloud.commandframework.context.CommandContext;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.commands.AddOwnerDelayed;
import nl.pim16aap2.bigdoors.core.commands.CommandFactory;
import nl.pim16aap2.bigdoors.core.commands.ICommandSender;
import nl.pim16aap2.bigdoors.core.events.StructureActionType;
import nl.pim16aap2.bigdoors.core.moveblocks.AnimationType;
import nl.pim16aap2.bigdoors.core.structures.PermissionLevel;
import nl.pim16aap2.bigdoors.core.structures.StructureType;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.structureretriever.StructureRetriever;
import nl.pim16aap2.bigdoors.core.util.structureretriever.StructureRetrieverFactory;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
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
            commandFactory.newAddOwner(commandSender, structureRetriever, newOwner, permissionLevel).run();
        }
        else
        {
            final var data = new AddOwnerDelayed.DelayedInput(newOwner, permissionLevel);
            commandFactory.getAddOwnerDelayed().provideDelayedInput(commandSender, data);
        }
    }

    void cancel(CommandContext<ICommandSender> context)
    {
        commandFactory.newCancel(context.getSender()).run();
    }

    void confirm(CommandContext<ICommandSender> context)
    {
        commandFactory.newConfirm(context.getSender()).run();
    }

    void debug(CommandContext<ICommandSender> context)
    {
        commandFactory.newDebug(context.getSender()).run();
    }

    void delete(CommandContext<ICommandSender> context)
    {
        final StructureRetriever structureRetriever = context.get("structureRetriever");
        commandFactory.newDelete(context.getSender(), structureRetriever).run();
    }

    void info(CommandContext<ICommandSender> context)
    {
        final StructureRetriever structureRetriever = context.get("structureRetriever");
        commandFactory.newInfo(context.getSender(), structureRetriever).run();
    }

    void inspectPowerBlock(CommandContext<ICommandSender> context)
    {
        commandFactory.newInspectPowerBlock(context.getSender()).run();
    }

    void listStructures(CommandContext<ICommandSender> context)
    {
        final @Nullable String query = context.<String>getOptional("structureName").orElse("");
        final StructureRetriever retriever = structureRetrieverFactory.search(
            context.getSender(), query, StructureRetrieverFactory.StructureFinderMode.NEW_INSTANCE).asRetriever(false);
        commandFactory.newListStructures(context.getSender(), retriever).run();
    }

    void lock(CommandContext<ICommandSender> context)
    {
        final StructureRetriever structureRetriever = context.get("structureRetriever");
        final boolean lockStatus = context.get("lockStatus");
        commandFactory.newLock(context.getSender(), structureRetriever, lockStatus).run();
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

        commandFactory.newMenu(commandSender, targetPlayer).run();
    }

    void movePowerBlock(CommandContext<ICommandSender> context)
    {
        final StructureRetriever structureRetriever = context.get("structureRetriever");
        commandFactory.newMovePowerBlock(context.getSender(), structureRetriever).run();
    }

    // NullAway doesn't see the @Nullable on structureName. Not sure if this is because of Lombok or NullAway.
    @SuppressWarnings("NullAway")
    void newStructure(CommandContext<ICommandSender> context)
    {
        final StructureType structureType = context.get("structureType");
        final @Nullable String structureName = nullable(context, "structureName");
        commandFactory.newNewStructure(context.getSender(), structureType, structureName).run();
    }

    void removeOwner(CommandContext<ICommandSender> context)
    {
        final StructureRetriever structureRetriever = context.get("structureRetriever");
        final IPlayer targetPlayer = SpigotAdapter.wrapPlayer(context.get("targetPlayer"));
        commandFactory.newRemoveOwner(context.getSender(), structureRetriever, targetPlayer).run();
    }

    void restart(CommandContext<ICommandSender> context)
    {
        commandFactory.newRestart(context.getSender()).run();
    }

    void setBlocksToMove(CommandContext<ICommandSender> context)
    {
        final int blocksToMove = context.get("blocksToMove");
        final @Nullable StructureRetriever structureRetriever = nullable(context, "structureRetriever");

        final ICommandSender commandSender = context.getSender();
        if (structureRetriever != null)
            commandFactory.newSetBlocksToMove(commandSender, structureRetriever, blocksToMove).run();
        else
            commandFactory.getSetBlocksToMoveDelayed().provideDelayedInput(commandSender, blocksToMove);
    }

    void setName(CommandContext<ICommandSender> context)
    {
        commandFactory.newSetName(context.getSender(), context.get("name")).run();
    }

    void setOpenStatus(CommandContext<ICommandSender> context)
    {
        final boolean isOpen = context.get("isOpen");
        final ICommandSender commandSender = context.getSender();
        final @Nullable StructureRetriever structureRetriever = nullable(context, "structureRetriever");

        if (structureRetriever != null)
            commandFactory.newSetOpenStatus(commandSender, structureRetriever, isOpen).run();
        else
            commandFactory.getSetOpenStatusDelayed().provideDelayedInput(commandSender, isOpen);
    }

    void setOpenDirection(CommandContext<ICommandSender> context)
    {
        final MovementDirection direction = context.get("direction");
        final ICommandSender commandSender = context.getSender();
        final @Nullable StructureRetriever structureRetriever = nullable(context, "structureRetriever");

        if (structureRetriever != null)
            commandFactory.newSetOpenDirection(commandSender, structureRetriever, direction).run();
        else
            commandFactory.getSetOpenDirectionDelayed().provideDelayedInput(commandSender, direction);
    }

    void specify(CommandContext<ICommandSender> context)
    {
        throw new UnsupportedOperationException("Not implemented!");
    }

    void stopStructures(CommandContext<ICommandSender> context)
    {
        commandFactory.newStopStructures(context.getSender()).run();
    }

    void toggle(CommandContext<ICommandSender> context)
    {
        commandFactory.newToggle(context.getSender(), context.<StructureRetriever>get("structureRetriever")).run();
    }

    void preview(CommandContext<ICommandSender> context)
    {
        commandFactory.newToggle(
            context.getSender(), StructureActionType.TOGGLE, AnimationType.PREVIEW,
            context.<StructureRetriever>get("structureRetriever")).run();
    }

    void version(CommandContext<ICommandSender> context)
    {
        commandFactory.newVersion(context.getSender()).run();
    }

    private <T> @Nullable T nullable(CommandContext<ICommandSender> context, String key)
    {
        return context.getOrDefault(key, null);
    }
}
