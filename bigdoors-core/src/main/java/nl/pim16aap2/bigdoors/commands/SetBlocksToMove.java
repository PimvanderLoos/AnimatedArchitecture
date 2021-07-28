package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IBlocksToMoveArchetype;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to change the number of blocks a block will try to move.
 *
 * @author Pim
 */
@ToString
public class SetBlocksToMove extends DoorTargetCommand
{
    private static final @NotNull CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_BLOCKS_TO_MOVE;

    private final int blocksToMove;

    protected SetBlocksToMove(final @NotNull ICommandSender commandSender, final @NotNull DoorRetriever doorRetriever,
                              final int blocksToMove)
    {
        super(commandSender, doorRetriever, DoorAttribute.BLOCKS_TO_MOVE);
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
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender,
                                                          final @NotNull DoorRetriever doorRetriever,
                                                          final int blocksToMove)
    {
        return new SetBlocksToMove(commandSender, doorRetriever, blocksToMove).run();
    }

    @Override
    public @NotNull CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected @NotNull CompletableFuture<Boolean> performAction(final @NotNull AbstractDoorBase door)
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

    /**
     * Executes the {@link SetBlocksToMove} command without a known {@link #blocksToMove}.
     * <p>
     * These missing values will be retrieved using a {@link DelayedCommandInputRequest}. The player will be asked to
     * use the {@link SetBlocksToMove} command (again, if needed) to supply the missing data.
     * <p>
     * These missing data can be supplied using {@link #provideDelayedInput(ICommandSender, int)}.
     *
     * @param commandSender The entity that sent the command and is held responsible (i.e. permissions, communication)
     *                      for its execution.
     * @param doorRetriever A {@link DoorRetriever} that references the target door.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NotNull CompletableFuture<Boolean> runDelayed(final @NotNull ICommandSender commandSender,
                                                                 final @NotNull DoorRetriever doorRetriever)
    {
        final int commandTimeout = Constants.COMMAND_WAITER_TIMEOUT;
        return new DelayedCommandInputRequest<>(commandTimeout, commandSender, COMMAND_DEFINITION,
                                                delayedInput -> delayedInputExecutor(commandSender,
                                                                                     doorRetriever,
                                                                                     delayedInput),
                                                SetBlocksToMove::inputRequestMessage, Integer.class)
            .getCommandOutput();
    }

    /**
     * Provides the delayed input if there is currently an active {@link DelayedCommandInputRequest} for the {@link
     * ICommandSender}.
     * <p>
     * If no active {@link DelayedCommandInputRequest} can be found for the command sender, the command sender will be
     * informed about it.
     *
     * @param commandSender The {@link ICommandSender} for which to look for an active {@link
     *                      DelayedCommandInputRequest} that can be fulfilled.
     * @param blocksToMove  The distance the door should move measured in number of blocks.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NotNull CompletableFuture<Boolean> provideDelayedInput(final @NotNull ICommandSender commandSender,
                                                                          final int blocksToMove)
    {
        return BigDoors.get().getDelayedCommandInputManager().getInputRequest(commandSender)
                       .map(request -> request.provide(blocksToMove))
                       .orElse(CompletableFuture.completedFuture(false));
    }

    /**
     * The method that is run once delayed input is received.
     * <p>
     * It processes the new input and executes the command using the previously-provided data (see {@link
     * #runDelayed(ICommandSender, DoorRetriever)}).
     *
     * @param commandSender The entity that sent the command and is held responsible (i.e. permissions, communication)
     *                      for its execution.
     * @param doorRetriever A {@link DoorRetriever} that references the target door.
     * @param blocksToMove  The distance the door should move measured in number of blocks.
     * @return See {@link BaseCommand#run()}.
     */
    private static @NotNull CompletableFuture<Boolean> delayedInputExecutor(final @NotNull ICommandSender commandSender,
                                                                            final @NotNull DoorRetriever doorRetriever,
                                                                            final int blocksToMove)
    {
        return new SetBlocksToMove(commandSender, doorRetriever, blocksToMove).run();
    }

    /**
     * Retrieves the message that will be sent to the command sender after initialization of a delayed input request.
     *
     * @return The init message for the delayed input request.
     */
    private static @NotNull String inputRequestMessage()
    {
        return BigDoors.get().getPlatform().getMessages().getString(Message.COMMAND_BLOCKSTOMOVE_INIT);
    }
}
