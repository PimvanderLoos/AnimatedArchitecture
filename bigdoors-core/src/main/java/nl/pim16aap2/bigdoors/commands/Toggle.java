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

    public Toggle(@NonNull ICommandSender commandSender, double speedMultiplier,
                  @NonNull DoorRetriever... doorRetrievers)
    {
        super(commandSender);
        this.speedMultiplier = speedMultiplier;
        this.doorRetrievers = doorRetrievers;
    }

    public Toggle(@NonNull ICommandSender commandSender, @NonNull DoorRetriever... doorRetrievers)
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

    private boolean hasAccess(@NonNull AbstractDoorBase door, boolean hasBypassPermission)
    {
        if (hasBypassPermission || !getCommandSender().isPlayer())
            return true;

        return getCommandSender()
            .getPlayer()
            .flatMap(door::getDoorOwner)
            .map(doorOwner -> doorOwner.getPermission() <= DoorAttribute.getPermissionLevel(DoorAttribute.TOGGLE))
            .orElse(false);
    }

    private boolean canToggle(@NonNull AbstractDoorBase door)
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

    private void toggleDoor(@NonNull AbstractDoorBase door, @NonNull DoorActionCause doorActionCause,
                            boolean hasBypassPermission)
    {
        if (!hasAccess(door, hasBypassPermission))
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

    private void handleDoorRequest(@NonNull DoorRetriever doorRetriever, @NonNull DoorActionCause doorActionCause,
                                   boolean hasBypassPermission)
    {
        getDoor(doorRetriever)
            .thenAccept(doorOpt -> doorOpt.ifPresent(door -> toggleDoor(door, doorActionCause, hasBypassPermission)));
    }

    @Override
    protected final @NonNull CompletableFuture<Boolean> executeCommand(@NonNull BooleanPair permissions)
    {
        val actionCause = getCommandSender().isPlayer() ? DoorActionCause.PLAYER : DoorActionCause.SERVER;
        for (val doorRetriever : doorRetrievers)
            handleDoorRequest(doorRetriever, actionCause, permissions.second);
        return CompletableFuture.completedFuture(true);
    }
}
