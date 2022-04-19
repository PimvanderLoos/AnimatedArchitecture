package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that changes the opening direction of doors.
 *
 * @author Pim
 */
@ToString
public class SetOpenDirection extends DoorTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_OPEN_DIR;

    private final RotateDirection rotateDirection;

    @AssistedInject //
    SetOpenDirection(
        @Assisted ICommandSender commandSender, ILocalizer localizer,
        @Assisted DoorRetriever doorRetriever, @Assisted RotateDirection rotateDirection)
    {
        super(commandSender, localizer, doorRetriever, DoorAttribute.OPEN_DIRECTION);
        this.rotateDirection = rotateDirection;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractDoor door)
    {
        if (!door.getDoorType().isValidOpenDirection(rotateDirection))
        {
            getCommandSender().sendMessage(
                localizer.getMessage("commands.set_open_direction.error.invalid_rotation",
                                     rotateDirection.name(), door.getBasicInfo()));

            return CompletableFuture.completedFuture(true);
        }

        door.setOpenDir(rotateDirection);
        return door.syncData().thenApply(x -> true);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link SetOpenDirection} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing open direction of the door.
         * @param doorRetriever
         *     A {@link DoorRetrieverFactory} representing the {@link DoorBase} for which the open direction will be
         *     modified.
         * @param rotateDirection
         *     The new open direction.
         * @return See {@link BaseCommand#run()}.
         */
        SetOpenDirection newSetOpenDirection(
            ICommandSender commandSender, DoorRetriever doorRetriever, RotateDirection rotateDirection);
    }
}
