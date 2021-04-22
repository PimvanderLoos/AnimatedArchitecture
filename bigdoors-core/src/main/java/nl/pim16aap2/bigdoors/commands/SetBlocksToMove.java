package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IBlocksToMoveArchetype;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to change the number of blocks a block will try to move.
 *
 * @author Pim
 */
@ToString
public class SetBlocksToMove extends DoorTargetCommand
{
    private final int blocksToMove;

    protected SetBlocksToMove(final @NonNull ICommandSender commandSender, final @NonNull DoorRetriever doorRetriever,
                              final int blocksToMove)
    {
        super(commandSender, doorRetriever);
        this.blocksToMove = blocksToMove;
    }

    /**
     * Runs the {@link SetBlocksToMove} command.
     *
     * @param commandSender The {@link ICommandSender} responsible for changing the blocks-to-move distance of the
     *                      door.
     * @param doorRetriever A {@link DoorRetriever} representing the {@link AbstractDoorBase} for which the
     *                      blocks-to-move distance will be modified.
     * @param blocksToMove  The new blocks-to-move distance.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NonNull CompletableFuture<Boolean> run(final @NonNull ICommandSender commandSender,
                                                          final @NonNull DoorRetriever doorRetriever,
                                                          final int blocksToMove)
    {
        return new SetBlocksToMove(commandSender, doorRetriever, blocksToMove).run();
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.SET_BLOCKS_TO_MOVE;
    }

    @Override
    protected boolean isAllowed(final @NonNull AbstractDoorBase door, final boolean bypassPermission)
    {
        return hasAccessToAttribute(door, DoorAttribute.BLOCKS_TO_MOVE, bypassPermission);
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> performAction(final @NonNull AbstractDoorBase door)
    {
        if (!(door instanceof IBlocksToMoveArchetype))
        {
            // TODO: Localization
            getCommandSender().sendMessage("This door has no blocks to move property!");
            return CompletableFuture.completedFuture(true);
        }

        ((IBlocksToMoveArchetype) door).setBlocksToMove(blocksToMove);
        return door.syncData().thenApply(x -> true);
    }
}
