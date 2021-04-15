package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Represents a command that toggles a door.
 * <p>
 * See also {@link Open} and {@link Close}.
 *
 * @author Pim
 */
@ToString
public class Toggle extends BaseCommand
{
    private final @NonNull DoorRetriever[] doorRetrievers;
    private final double speedMultiplier;

    public Toggle(final @NonNull ICommandSender commandSender, final double speedMultiplier,
                  final @NonNull DoorRetriever... doorRetrievers)
    {
        super(commandSender);
        this.speedMultiplier = speedMultiplier;
        this.doorRetrievers = doorRetrievers;
    }

    public Toggle(final @NonNull ICommandSender commandSender, final @NonNull DoorRetriever... doorRetrievers)
    {
        this(commandSender, 0D, doorRetrievers);
    }

    /**
     * Gets the {@link DoorActionType} for this command.
     */
    public @NonNull DoorActionType getDoorActionType()
    {
        return DoorActionType.TOGGLE;
    }

    @Override
    protected boolean validInput()
    {
        return doorRetrievers.length > 0;
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.TOGGLE;
    }

    /**
     * Checks if the provided {@link AbstractDoorBase} can be toggled with the action provided by {@link
     * #getDoorActionType()}.
     * <p>
     * For example, if the action is {@link DoorActionType#CLOSE} and the door is already closed, the action is not
     * possible.
     *
     * @param door The door for which to check whether it can be toggled.
     * @return True if the toggle action is possible, otherwise false.
     */
    protected final boolean canToggle(final @NonNull AbstractDoorBase door)
    {
        switch (getDoorActionType())
        {
            case TOGGLE:
                return true;
            case OPEN:
                return door.isCloseable();
            case CLOSE:
                return door.isOpenable();
        }
        return false;
    }

    private void toggleDoor(final @NonNull AbstractDoorBase door, final @NonNull DoorActionCause doorActionCause,
                            final boolean hasBypassPermission)
    {
        if (!hasAccessToAttribute(door, DoorAttribute.TOGGLE, hasBypassPermission))
        {
            BigDoors.get().getPLogger()
                    .logMessage(Level.FINE, () -> "No access access for command " + this + " for door: " + door);
            return;
        }
        if (!canToggle(door))
        {
            BigDoors.get().getPLogger()
                    .logMessage(Level.FINER, () -> "Blocked action for command " + this + " for door: " + door);
            return;
        }

        BigDoors.get().getDoorOpener()
                .animateDoorAsync(door, doorActionCause, getCommandSender().getPlayer().orElse(null),
                                  speedMultiplier, false, getDoorActionType());
    }

    private CompletableFuture<Void> handleDoorRequest(final @NonNull DoorRetriever doorRetriever,
                                                      final @NonNull DoorActionCause doorActionCause,
                                                      final boolean hasBypassPermission)
    {
        return getDoor(doorRetriever)
            .thenAccept(doorOpt -> doorOpt.ifPresent(door -> toggleDoor(door, doorActionCause, hasBypassPermission)));
    }

    @Override
    protected final @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        val actionCause = getCommandSender().isPlayer() ? DoorActionCause.PLAYER : DoorActionCause.SERVER;
        val actions = new CompletableFuture[doorRetrievers.length];
        for (int idx = 0; idx < actions.length; ++idx)
            actions[idx] = handleDoorRequest(doorRetrievers[idx], actionCause, permissions.second);
        return CompletableFuture.allOf(actions).thenApply(ignored -> true);
    }
}
