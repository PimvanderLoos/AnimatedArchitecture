package nl.pim16aap2.bigdoors.commands;

import lombok.Getter;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Represents a command that relates to an existing door.
 *
 * @author Pim
 */
public abstract class DoorTargetCommand extends BaseCommand
{
    @Getter
    protected final DoorRetriever doorRetriever;

    private final DoorAttribute doorAttribute;

    protected DoorTargetCommand(ICommandSender commandSender, CommandContext context,
                                DoorRetriever doorRetriever, DoorAttribute doorAttribute)
    {
        super(commandSender, context);
        this.doorRetriever = doorRetriever;
        this.doorAttribute = doorAttribute;
    }

    @Override
    protected final CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        return getDoor(getDoorRetriever())
            .thenApplyAsync(door -> processDoorResult(door, permissions))
            .exceptionally(t -> Util.exceptionally(t, false));
    }

    /**
     * Handles the result of retrieving the door.
     *
     * @param door
     *     The result of trying to retrieve the door.
     * @param permissions
     *     Whether the ICommandSender has user and/or admin permissions respectively.
     * @return The result of running the command, see {@link BaseCommand#run()}.
     */
    private boolean processDoorResult(Optional<AbstractDoor> door, BooleanPair permissions)
    {
        if (door.isEmpty())
        {
            logger.logMessage(Level.FINE, () ->
                "Failed to find door " + getDoorRetriever() + " for command: " + this);

            getCommandSender().sendMessage(
                localizer.getMessage("commands.door_target_command.base.error.door_not_found"));
            return false;
        }

        if (!isAllowed(door.get(), permissions.second))
        {
            logger.logMessage(Level.FINE,
                              () -> getCommandSender() + " does not have access to door " + door +
                                  " for command " + this);

            getCommandSender().sendMessage(localizer.getMessage(
                "commands.door_target_command.base.error.no_permission_for_action"));
            return true;
        }

        try
        {
            return performAction(door.get()).get(30, TimeUnit.MINUTES);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if execution of this command is allowed for the given {@link AbstractDoor}.
     *
     * @param door
     *     The {@link AbstractDoor} that is the target for this command.
     * @param bypassPermission
     *     Whether or not the {@link ICommandSender} has bypass access.
     * @return True if execution of this command is allowed.
     */
    protected boolean isAllowed(AbstractDoor door, boolean bypassPermission)
    {
        return hasAccessToAttribute(door, doorAttribute, bypassPermission);
    }

    /**
     * Performs the action of this command on the {@link AbstractDoor}.
     *
     * @param door
     *     The {@link DoorBase} to perform the action on.
     * @return True if everything was successful.
     */
    protected abstract CompletableFuture<Boolean> performAction(AbstractDoor door);
}
