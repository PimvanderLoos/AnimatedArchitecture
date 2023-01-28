package nl.pim16aap2.bigdoors.spigot.comands;

import cloud.commandframework.context.CommandContext;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.commands.AddOwnerDelayed;
import nl.pim16aap2.bigdoors.commands.CommandFactory;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.movable.PermissionLevel;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.moveblocks.AnimationType;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Flogger
class CommandExecutor
{
    private final CommandFactory commandFactory;
    private final MovableRetrieverFactory movableRetrieverFactory;

    @Inject CommandExecutor(
        CommandFactory commandFactory,
        MovableRetrieverFactory movableRetrieverFactory)
    {
        this.commandFactory = commandFactory;
        this.movableRetrieverFactory = movableRetrieverFactory;
    }

    // NullAway doesn't see the @Nullable on permissionLevel. Not sure if this is because of Lombok or NullAway.
    @SuppressWarnings("NullAway")
    void addOwner(CommandContext<ICommandSender> context)
    {
        final IPPlayer newOwner = SpigotAdapter.wrapPlayer(context.get("newOwner"));
        final @Nullable PermissionLevel permissionLevel = nullable(context, "permissionLevel");
        final @Nullable MovableRetriever movableRetriever = nullable(context, "movableRetriever");

        final ICommandSender commandSender = context.getSender();
        if (movableRetriever != null)
        {
            commandFactory.newAddOwner(commandSender, movableRetriever, newOwner, permissionLevel).run();
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
        final MovableRetriever movableRetriever = context.get("movableRetriever");
        commandFactory.newDelete(context.getSender(), movableRetriever).run();
    }

    void info(CommandContext<ICommandSender> context)
    {
        final MovableRetriever movableRetriever = context.get("movableRetriever");
        commandFactory.newInfo(context.getSender(), movableRetriever).run();
    }

    void inspectPowerBlock(CommandContext<ICommandSender> context)
    {
        commandFactory.newInspectPowerBlock(context.getSender()).run();
    }

    void listMovables(CommandContext<ICommandSender> context)
    {
        final @Nullable String query = context.<String>getOptional("movableName").orElse("");
        final MovableRetriever retriever = movableRetrieverFactory.search(
            context.getSender(), query, MovableRetrieverFactory.MovableFinderMode.NEW_INSTANCE).asRetriever(false);
        commandFactory.newListMovables(context.getSender(), retriever).run();
    }

    void lock(CommandContext<ICommandSender> context)
    {
        final MovableRetriever movableRetriever = context.get("movableRetriever");
        final boolean lockStatus = context.get("lockStatus");
        commandFactory.newLock(context.getSender(), movableRetriever, lockStatus).run();
    }

    void menu(CommandContext<ICommandSender> context)
    {
        final @Nullable Player player = nullable(context, "targetPlayer");
        final ICommandSender commandSender = context.getSender();
        final IPPlayer targetPlayer;
        if (player != null)
            targetPlayer = SpigotAdapter.wrapPlayer(player);
        else
            targetPlayer = commandSender.getPlayer().orElseThrow(IllegalArgumentException::new);

        commandFactory.newMenu(commandSender, targetPlayer).run();
    }

    void movePowerBlock(CommandContext<ICommandSender> context)
    {
        final MovableRetriever movableRetriever = context.get("movableRetriever");
        commandFactory.newMovePowerBlock(context.getSender(), movableRetriever).run();
    }

    // NullAway doesn't see the @Nullable on movableName. Not sure if this is because of Lombok or NullAway.
    @SuppressWarnings("NullAway")
    void newMovable(CommandContext<ICommandSender> context)
    {
        final MovableType movableType = context.get("movableType");
        final @Nullable String movableName = nullable(context, "movableName");
        commandFactory.newNewMovable(context.getSender(), movableType, movableName).run();
    }

    void removeOwner(CommandContext<ICommandSender> context)
    {
        final MovableRetriever movableRetriever = context.get("movableRetriever");
        final IPPlayer targetPlayer = SpigotAdapter.wrapPlayer(context.get("targetPlayer"));
        commandFactory.newRemoveOwner(context.getSender(), movableRetriever, targetPlayer).run();
    }

    void restart(CommandContext<ICommandSender> context)
    {
        commandFactory.newRestart(context.getSender()).run();
    }

    void setBlocksToMove(CommandContext<ICommandSender> context)
    {
        final int blocksToMove = context.get("blocksToMove");
        final @Nullable MovableRetriever movableRetriever = nullable(context, "movableRetriever");

        final ICommandSender commandSender = context.getSender();
        if (movableRetriever != null)
            commandFactory.newSetBlocksToMove(commandSender, movableRetriever, blocksToMove).run();
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
        final @Nullable MovableRetriever movableRetriever = nullable(context, "movableRetriever");

        if (movableRetriever != null)
            commandFactory.newSetOpenStatus(commandSender, movableRetriever, isOpen).run();
        else
            commandFactory.getSetOpenStatusDelayed().provideDelayedInput(commandSender, isOpen);
    }

    void setOpenDirection(CommandContext<ICommandSender> context)
    {
        final MovementDirection direction = context.get("direction");
        final ICommandSender commandSender = context.getSender();
        final @Nullable MovableRetriever movableRetriever = nullable(context, "movableRetriever");

        if (movableRetriever != null)
            commandFactory.newSetOpenDirection(commandSender, movableRetriever, direction).run();
        else
            commandFactory.getSetOpenDirectionDelayed().provideDelayedInput(commandSender, direction);
    }

    void specify(CommandContext<ICommandSender> context)
    {
        throw new UnsupportedOperationException("Not implemented!");
    }

    void stopMovables(CommandContext<ICommandSender> context)
    {
        commandFactory.newStopMovables(context.getSender()).run();
    }

    void toggle(CommandContext<ICommandSender> context)
    {
        commandFactory.newToggle(context.getSender(), context.<MovableRetriever>get("movableRetriever")).run();
    }

    void preview(CommandContext<ICommandSender> context)
    {
        commandFactory.newToggle(
            context.getSender(), MovableActionType.TOGGLE, AnimationType.PREVIEW,
            context.<MovableRetriever>get("movableRetriever")).run();
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
