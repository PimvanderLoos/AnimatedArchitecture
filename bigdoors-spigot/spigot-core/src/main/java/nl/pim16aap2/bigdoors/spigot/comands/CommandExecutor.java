package nl.pim16aap2.bigdoors.spigot.comands;

import cloud.commandframework.context.CommandContext;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.commands.AddOwnerDelayed;
import nl.pim16aap2.bigdoors.commands.CommandFactory;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class CommandExecutor
{
    private final CommandFactory commandFactory;
    private final DoorRetrieverFactory doorRetrieverFactory;

    @Inject CommandExecutor(
        CommandFactory commandFactory,
        DoorRetrieverFactory doorRetrieverFactory)
    {
        this.commandFactory = commandFactory;
        this.doorRetrieverFactory = doorRetrieverFactory;
    }

    void addOwner(CommandContext<ICommandSender> context)
    {
        final IPPlayer newOwner = context.get("newOwner");
        final @Nullable Integer permissionLevel = nullable(context, "permissionLevel");
        final @Nullable DoorRetriever doorRetriever = nullable(context, "doorRetriever");

        final ICommandSender commandSender = context.getSender();
        if (doorRetriever != null)
        {
            commandFactory.newAddOwner(commandSender, doorRetriever, newOwner, permissionLevel).run();
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

    }

    void info(CommandContext<ICommandSender> context)
    {

    }

    void inspectPowerBlock(CommandContext<ICommandSender> context)
    {

    }

    void listDoors(CommandContext<ICommandSender> context)
    {
        final @Nullable String doorName = context.getOrDefault("doorName", null);
        if (doorName == null)
            // TODO: Implement this
            throw new UnsupportedOperationException("Not implemented!");

        final DoorRetriever retriever = doorRetrieverFactory.of(doorName);
        commandFactory.newListDoors(context.getSender(), retriever).run();
    }

    void lock(CommandContext<ICommandSender> context)
    {

    }

    void menu(CommandContext<ICommandSender> context)
    {
        final @Nullable Player player = nullable(context, "targetPlayer");
        final ICommandSender commandSender = context.getSender();
        final IPPlayer targetPlayer;
        if (player != null)
            targetPlayer = new PPlayerSpigot(player);
        else
            targetPlayer = commandSender.getPlayer().orElseThrow(IllegalArgumentException::new);

        commandFactory.newMenu(commandSender, targetPlayer).run();
    }

    void movePowerBlock(CommandContext<ICommandSender> context)
    {

    }

    // NullAway doesn't see the @Nullable on permissionLevel. Not sure if this is because of Lombok or NullAway.
    @SuppressWarnings("NullAway")
    void newDoor(CommandContext<ICommandSender> context)
    {
        final DoorType doorType = context.get("doorType");
        final @Nullable String doorName = nullable(context, "doorName");
        commandFactory.newNewDoor(context.getSender(), doorType, doorName).run();
    }

    void removeOwner(CommandContext<ICommandSender> context)
    {
        final DoorRetriever retriever = context.get("doorRetriever");
        final IPPlayer targetPlayer = new PPlayerSpigot(context.get("targetPlayer"));
        commandFactory.newRemoveOwner(context.getSender(), retriever, targetPlayer).run();
    }

    void restart(CommandContext<ICommandSender> context)
    {
        commandFactory.newRestart(context.getSender()).run();
    }

    void setAutoCloseTime(CommandContext<ICommandSender> context)
    {

    }

    void setBlocksToMove(CommandContext<ICommandSender> context)
    {

    }

    void setName(CommandContext<ICommandSender> context)
    {
        commandFactory.newSetName(context.getSender(), context.get("name")).run();
    }

    void setOpenDirection(CommandContext<ICommandSender> context)
    {
        final RotateDirection direction = context.get("direction");
        final ICommandSender commandSender = context.getSender();
        final @Nullable DoorRetriever doorRetriever = nullable(context, "doorRetriever");

        if (doorRetriever != null)
            commandFactory.newSetOpenDirection(commandSender, doorRetriever, direction).run();
        else
            commandFactory.getSetOpenDirectionDelayed().provideDelayedInput(commandSender, direction);
    }

    void specify(CommandContext<ICommandSender> context)
    {

    }

    void stopDoors(CommandContext<ICommandSender> context)
    {

    }

    void toggle(CommandContext<ICommandSender> context)
    {
        commandFactory.newToggle(context.getSender(), context.<DoorRetriever>get("doorRetriever")).run();
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
