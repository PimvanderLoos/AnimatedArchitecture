package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableAttribute;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IDiscreteMovement;
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to change the number of blocks a block will try to move.
 *
 * @author Pim
 */
@ToString
public class SetBlocksToMove extends MovableTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_BLOCKS_TO_MOVE;

    private final int blocksToMove;

    @AssistedInject //
    SetBlocksToMove(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted MovableRetriever movableRetriever, @Assisted int blocksToMove)
    {
        super(commandSender, localizer, textFactory, movableRetriever, MovableAttribute.BLOCKS_TO_MOVE);
        this.blocksToMove = blocksToMove;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected void handleDatabaseActionSuccess()
    {
        final var desc = getRetrievedMovableDescription();
        getCommandSender().sendSuccess(textFactory, localizer.getMessage("commands.set_blocks_to_move.success",
                                                                         desc.typeName(), desc.id()));
    }

    @Override
    protected CompletableFuture<?> performAction(AbstractMovable movable)
    {
        if (!(movable instanceof IDiscreteMovement))
        {
            getCommandSender()
                .sendMessage(textFactory, TextType.ERROR,
                             localizer.getMessage("commands.set_blocks_to_move.error.invalid_movable_type",
                                                  localizer.getMovableType(movable), movable.getBasicInfo()));
            return CompletableFuture.completedFuture(null);
        }

        ((IDiscreteMovement) movable).setBlocksToMove(blocksToMove);
        return movable.syncData().thenAccept(this::handleDatabaseActionResult);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link SetBlocksToMove} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing the blocks-to-move distance of the movable.
         * @param movableRetriever
         *     A {@link MovableRetrieverFactory} representing the {@link MovableBase} for which the blocks-to-move
         *     distance will be modified.
         * @param blocksToMove
         *     The new blocks-to-move distance.
         * @return See {@link BaseCommand#run()}.
         */
        SetBlocksToMove newSetBlocksToMove(
            ICommandSender commandSender, MovableRetriever movableRetriever, int blocksToMove);
    }
}
