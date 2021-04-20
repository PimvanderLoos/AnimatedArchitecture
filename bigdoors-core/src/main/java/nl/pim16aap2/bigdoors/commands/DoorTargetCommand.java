package nl.pim16aap2.bigdoors.commands;

import lombok.Getter;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a command that relates to an existing door.
 *
 * @author Pim
 */
public abstract class DoorTargetCommand extends BaseCommand
{
    @Getter
    protected final @NonNull DoorRetriever doorRetriever;

    protected DoorTargetCommand(final @NonNull ICommandSender commandSender, final @NonNull DoorRetriever doorRetriever)
    {
        super(commandSender);
        this.doorRetriever = doorRetriever;
    }

    @Override
    protected final @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        final CompletableFuture<Boolean> ret = new CompletableFuture<>();

        getDoor(getDoorRetriever())
            .thenApply(door ->
                       {
                           if (door.isEmpty() || !isAllowed(door.get(), permissions.second))
                               ret.complete(false);
                           return door.orElse(null);
                       })
            .thenCompose(this::performAction)
            .thenAccept(ret::complete)
            .exceptionally(t -> Util.exceptionallyCompletion(t, null, ret));
        return ret;
    }

    /**
     * Checks if execution of this command is allowed for the given {@link AbstractDoorBase}.
     *
     * @param door             The {@link AbstractDoorBase} that is the target for this command.
     * @param bypassPermission Whether or not the {@link ICommandSender} has bypass access.
     * @return True if execution of this command is allowed.
     */
    protected abstract boolean isAllowed(@NonNull AbstractDoorBase door, boolean bypassPermission);

    /**
     * Performs the action of this command on the {@link AbstractDoorBase}.
     *
     * @param door The {@link AbstractDoorBase} to perform the action on.
     * @return True if everything was successful.
     */
    protected abstract @NonNull CompletableFuture<Boolean> performAction(@NonNull AbstractDoorBase door);
}
