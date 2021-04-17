package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.RotateDirection;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that changes the opening direction of doors.
 *
 * @author Pim
 */
@ToString
public class SetOpenDirection extends DoorTargetCommand
{
    private final @NonNull RotateDirection rotateDirection;

    public SetOpenDirection(final @NonNull ICommandSender commandSender, final @NonNull DoorRetriever doorRetriever,
                            final @NonNull RotateDirection rotateDirection)
    {
        super(commandSender, doorRetriever);
        this.rotateDirection = rotateDirection;
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.SET_OPEN_DIR;
    }

    @Override
    protected boolean isAllowed(final @NonNull AbstractDoorBase door, final boolean bypassPermission)
    {
        return hasAccessToAttribute(door, DoorAttribute.OPEN_DIRECTION, bypassPermission);
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> performAction(final @NonNull AbstractDoorBase door)
    {
        if (!door.getDoorType().isValidOpenDirection(rotateDirection))
        {
            // TODO: Localization
            getCommandSender().sendMessage(
                rotateDirection.name() + " is not a valid rotation direction for door " + door.getBasicInfo());
            return CompletableFuture.completedFuture(true);
        }

        return door.setOpenDir(rotateDirection).syncData().thenApply(x -> true);
    }
}
