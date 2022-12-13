package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.IDiscreteMovement;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.doors.DoorAttribute;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to change the number of blocks a block will try to move.
 *
 * @author Pim
 */
@ToString
public class SetBlocksToMove extends DoorTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_BLOCKS_TO_MOVE;

    private final int blocksToMove;

    @AssistedInject //
    SetBlocksToMove(
        @Assisted ICommandSender commandSender, ILocalizer localizer,
        @Assisted DoorRetriever doorRetriever, @Assisted int blocksToMove)
    {
        super(commandSender, localizer, doorRetriever, DoorAttribute.BLOCKS_TO_MOVE);
        this.blocksToMove = blocksToMove;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractDoor door)
    {
        if (!(door instanceof IDiscreteMovement))
        {
            getCommandSender().sendMessage(localizer
                                               .getMessage("commands.set_blocks_to_move.error.invalid_door_type",
                                                           door.getBasicInfo()));
            return CompletableFuture.completedFuture(true);
        }

        ((IDiscreteMovement) door).setBlocksToMove(blocksToMove);
        return door.syncData().thenApply(x -> true);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link SetBlocksToMove} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing the blocks-to-move distance of the door.
         * @param doorRetriever
         *     A {@link DoorRetrieverFactory} representing the {@link DoorBase} for which the blocks-to-move distance
         *     will be modified.
         * @param blocksToMove
         *     The new blocks-to-move distance.
         * @return See {@link BaseCommand#run()}.
         */
        SetBlocksToMove newSetBlocksToMove(
            ICommandSender commandSender, DoorRetriever doorRetriever, int blocksToMove);
    }
}
