package nl.pim16aap2.bigdoors.commands;

import lombok.Getter;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
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
    protected final @NonNull DoorRetriever doorRetriever;

    private final @NonNull DoorAttribute doorAttribute;

    protected DoorTargetCommand(final @NonNull ICommandSender commandSender, final @NonNull DoorRetriever doorRetriever,
                                final @NonNull DoorAttribute doorAttribute)
    {
        super(commandSender);
        this.doorRetriever = doorRetriever;
        this.doorAttribute = doorAttribute;
    }

    @Override
    protected final @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        return getDoor(getDoorRetriever())
            .thenApplyAsync(door -> processDoorResult(door, permissions))
            .exceptionally(t -> Util.exceptionally(t, false));
    }

    /**
     * Handles the result of retrieving the door.
     *
     * @param door        The result of trying to retrieve the door.
     * @param permissions Whether the ICommandSender has user and/or admin permissions respectively.
     * @return The result of running the command, see {@link BaseCommand#run()}.
     */
    private boolean processDoorResult(final @NonNull Optional<AbstractDoorBase> door,
                                      final @NonNull BooleanPair permissions)
    {
        if (door.isEmpty())
        {
            BigDoors.get().getPLogger().logMessage(Level.FINE, () ->
                "Failed to find door " + getDoorRetriever() + " for command: " + this);

            // TODO: Localization
            getCommandSender().sendMessage("Failed to find the specified door!");
            return false;
        }

        if (!isAllowed(door.get(), permissions.second))
        {
            BigDoors.get().getPLogger().logMessage(Level.FINE,
                                                   () -> getCommandSender() + " does not have access to door " + door +
                                                       " for command " + this);

            // TODO: Localization
            getCommandSender().sendMessage("You do not have access to this action for this door!");
            return true;
        }

        try
        {
            return performAction(door.get()).get(30, TimeUnit.MINUTES);
        }
        catch (Throwable t)
        {
            throw new RuntimeException(t);
        }
    }

    /**
     * Checks if execution of this command is allowed for the given {@link AbstractDoorBase}.
     *
     * @param door             The {@link AbstractDoorBase} that is the target for this command.
     * @param bypassPermission Whether or not the {@link ICommandSender} has bypass access.
     * @return True if execution of this command is allowed.
     */
    protected boolean isAllowed(final AbstractDoorBase door, final boolean bypassPermission)
    {
        if (door == null)
            return false;
        return hasAccessToAttribute(door, doorAttribute, bypassPermission);
    }

    /**
     * Performs the action of this command on the {@link AbstractDoorBase}.
     *
     * @param door The {@link AbstractDoorBase} to perform the action on.
     * @return True if everything was successful.
     */
    protected abstract @NonNull CompletableFuture<Boolean> performAction(final @NonNull AbstractDoorBase door);
}
