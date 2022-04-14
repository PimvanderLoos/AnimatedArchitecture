package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.IDiscreteMovement;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
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

    //    /**
//     * Executes the {@link SetBlocksToMove} command without a known {@link #blocksToMove}.
//     * <p>
//     * These missing values will be retrieved using a {@link DelayedCommandInputRequest}. The player will be asked to
//     * use the {@link SetBlocksToMove} command (again, if needed) to supply the missing data.
//     * <p>
//     * These missing data can be supplied using {@link #provideDelayedInput(ICommandSender, IPLogger, ILocalizer,
//     * int)}.
//     *
//     * @param commandSender
//     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
//     *     execution.
//     * @param doorRetriever
//     *     A {@link DoorRetrieverFactory} that references the target door.
//     * @return See {@link BaseCommand#run()}.
//     */
//    public static CompletableFuture<Boolean> runDelayed(ICommandSender commandSender, IPLogger logger,
//                                                        ILocalizer localizer,
//                                                        DoorRetriever doorRetriever)
//    {
//        final int commandTimeout = Constants.COMMAND_WAITER_TIMEOUT;
//        final ILocalizer localizer = logger, localizer.getLocalizer();
//        return new DelayedCommandInputRequest<>(commandTimeout, commandSender, COMMAND_DEFINITION, logger, localizer,
//                                                delayedInput -> delayedInputExecutor(commandSender, logger, localizer,
//                                                                                     doorRetriever, delayedInput),
//                                                () -> SetBlocksToMove.inputRequestMessage(localizer), Integer.class)
//            .getCommandOutput();
//    }
//
//    /**
//     * Provides the delayed input if there is currently an active {@link DelayedCommandInputRequest} for the {@link
//     * ICommandSender}.
//     * <p>
//     * If no active {@link DelayedCommandInputRequest} can be found for the command sender, the command sender will be
//     * informed about it.
//     *
//     * @param commandSender
//     *     The {@link ICommandSender} for which to look for an active {@link DelayedCommandInputRequest} that can be
//     *     fulfilled.
//     * @param blocksToMove
//     *     The distance the door should move measured in number of blocks.
//     * @return See {@link BaseCommand#run()}.
//     */
//    public static CompletableFuture<Boolean> provideDelayedInput(ICommandSender commandSender, IPLogger logger,
//                                                                 ILocalizer localizer,
//                                                                 int blocksToMove)
//    {
//        return delayedCommandInputManager().getInputRequest(commandSender)
//                                           .map(request -> request.provide(blocksToMove))
//                                           .orElse(CompletableFuture.completedFuture(false));
//    }
//
//    /**
//     * The method that is run once delayed input is received.
//     * <p>
//     * It processes the new input and executes the command using the previously-provided data (see {@link
//     * #runDelayed(ICommandSender, IPLogger, ILocalizer, DoorRetriever)}).
//     *
//     * @param commandSender
//     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
//     *     execution.
//     * @param doorRetriever
//     *     A {@link DoorRetrieverFactory} that references the target door.
//     * @param blocksToMove
//     *     The distance the door should move measured in number of blocks.
//     * @return See {@link BaseCommand#run()}.
//     */
//    private static CompletableFuture<Boolean> delayedInputExecutor(ICommandSender commandSender, IPLogger logger,
//                                                                   ILocalizer localizer,
//                                                                   DoorRetriever doorRetriever,
//                                                                   int blocksToMove)
//    {
//        return new SetBlocksToMove(commandSender, logger, localizer, doorRetriever, blocksToMove).run();
//    }
//
//    /**
//     * Retrieves the message that will be sent to the command sender after initialization of a delayed input request.
//     *
//     * @return The init message for the delayed input request.
//     */
//    private static String inputRequestMessage(ILocalizer localizer)
//    {
//        return localizer.getMessage("commands.set_blocks_to_move.init");
//    }

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
            ICommandSender commandSender,
            DoorRetriever doorRetriever,
            int blocksToMove);
    }
}
