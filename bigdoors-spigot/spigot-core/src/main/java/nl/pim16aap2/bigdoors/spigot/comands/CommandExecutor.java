package nl.pim16aap2.bigdoors.spigot.comands;

import cloud.commandframework.context.CommandContext;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.commands.AddOwnerDelayed;
import nl.pim16aap2.bigdoors.commands.CommandFactory;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.doors.PermissionLevel;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.OptionalInt;

@Singleton
@Flogger
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

    private @Nullable PermissionLevel parsePermissionLevel(@Nullable String permissionStr)
    {
        if (permissionStr == null)
            return null;

        final @Nullable PermissionLevel permissionLevel;

        final OptionalInt permissionInt = Util.parseInt(permissionStr);
        if (permissionInt.isPresent())
            permissionLevel = PermissionLevel.fromValue(permissionInt.getAsInt());
        else
            permissionLevel = PermissionLevel.fromName(permissionStr);

        if (permissionLevel == null)
            log.atInfo().log("Unable to parse permission level: '{}'", permissionInt);
        return permissionLevel;
    }

    // NullAway doesn't see the @Nullable on permissionLevel. Not sure if this is because of Lombok or NullAway.
    @SuppressWarnings("NullAway")
    void addOwner(CommandContext<ICommandSender> context)
    {
        final IPPlayer newOwner = context.get("newOwner");
        final @Nullable PermissionLevel permissionLevel = parsePermissionLevel(nullable(context, "permissionLevel"));
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
        final DoorRetriever doorRetriever = context.get("doorRetriever");
        commandFactory.newDelete(context.getSender(), doorRetriever).run();
    }

    void info(CommandContext<ICommandSender> context)
    {
        final DoorRetriever doorRetriever = context.get("doorRetriever");
        commandFactory.newInfo(context.getSender(), doorRetriever).run();
    }

    void inspectPowerBlock(CommandContext<ICommandSender> context)
    {
        commandFactory.newInspectPowerBlock(context.getSender()).run();
    }

    void listDoors(CommandContext<ICommandSender> context)
    {
        final @Nullable String query = context.<String>getOptional("doorName").orElse("");
        final DoorRetriever retriever = doorRetrieverFactory.search(
            context.getSender(), query, DoorRetrieverFactory.DoorFinderMode.NEW_INSTANCE).asRetriever(false);
        commandFactory.newListDoors(context.getSender(), retriever).run();
    }

    void lock(CommandContext<ICommandSender> context)
    {
        final DoorRetriever doorRetriever = context.get("doorRetriever");
        final boolean lockStatus = context.get("lockStatus");
        commandFactory.newLock(context.getSender(), doorRetriever, lockStatus).run();
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
        final DoorRetriever doorRetriever = context.get("doorRetriever");
        commandFactory.newMovePowerBlock(context.getSender(), doorRetriever).run();
    }

    // NullAway doesn't see the @Nullable on doorName. Not sure if this is because of Lombok or NullAway.
    @SuppressWarnings("NullAway")
    void newDoor(CommandContext<ICommandSender> context)
    {
        final DoorType doorType = context.get("doorType");
        final @Nullable String doorName = nullable(context, "doorName");
        commandFactory.newNewDoor(context.getSender(), doorType, doorName).run();
    }

    void removeOwner(CommandContext<ICommandSender> context)
    {
        final DoorRetriever doorRetriever = context.get("doorRetriever");
        final IPPlayer targetPlayer = new PPlayerSpigot(context.get("targetPlayer"));
        commandFactory.newRemoveOwner(context.getSender(), doorRetriever, targetPlayer).run();
    }

    void restart(CommandContext<ICommandSender> context)
    {
        commandFactory.newRestart(context.getSender()).run();
    }

    void setAutoCloseTime(CommandContext<ICommandSender> context)
    {
        final int autoCloseTime = context.get("autoCloseTime");
        final @Nullable DoorRetriever doorRetriever = nullable(context, "doorRetriever");

        final ICommandSender commandSender = context.getSender();
        if (doorRetriever != null)
            commandFactory.newSetAutoCloseTime(commandSender, doorRetriever, autoCloseTime).run();
        else
            commandFactory.getSetAutoCloseTimeDelayed().provideDelayedInput(commandSender, autoCloseTime);
    }

    void setBlocksToMove(CommandContext<ICommandSender> context)
    {
        final int blocksToMove = context.get("blocksToMove");
        final @Nullable DoorRetriever doorRetriever = nullable(context, "doorRetriever");

        final ICommandSender commandSender = context.getSender();
        if (doorRetriever != null)
            commandFactory.newSetBlocksToMove(commandSender, doorRetriever, blocksToMove).run();
        else
            commandFactory.getSetBlocksToMoveDelayed().provideDelayedInput(commandSender, blocksToMove);
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
        throw new UnsupportedOperationException("Not implemented!");
    }

    void stopDoors(CommandContext<ICommandSender> context)
    {
        commandFactory.newStopDoors(context.getSender()).run();
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
