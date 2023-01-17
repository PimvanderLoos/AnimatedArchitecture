package nl.pim16aap2.bigdoors.movable.portcullis;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorInteger;
import nl.pim16aap2.bigdoors.util.Limit;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

public class CreatorPortcullis extends Creator
{
    private static final MovableType MOVABLE_TYPE = MovableTypePortcullis.get();

    protected int blocksToMove;

    public CreatorPortcullis(Creator.Context context, IPPlayer player, @Nullable String name)
    {
        super(context, player, name);
    }

    public CreatorPortcullis(Creator.Context context, IPPlayer player)
    {
        this(context, player, null);
    }

    @Override
    protected List<IStep> generateSteps()
        throws InstantiationException
    {
        final Step stepBlocksToMove = stepFactory
            .stepName("SET_BLOCKS_TO_MOVE")
            .messageKey("creator.portcullis.set_blocks_to_move")
            .stepExecutor(new StepExecutorInteger(this::setBlocksToMove))
            .stepPreparation(this::prepareSetBlocksToMove)
            .waitForUserInput(true).construct();

        return Arrays.asList(factorySetName.construct(),
                             factorySetFirstPos.messageKey("creator.portcullis.step_1").construct(),
                             factorySetSecondPos.messageKey("creator.portcullis.step_2").construct(),
                             factorySetPowerBlockPos.construct(),
                             factorySetOpenDir.construct(),
                             stepBlocksToMove,
                             factoryConfirmPrice.construct(),
                             factoryCompleteProcess.messageKey("creator.portcullis.success").construct());
    }

    /**
     * Prepares the step that sets the number of blocks to move.
     */
    protected void prepareSetBlocksToMove()
    {
        commandFactory.getSetBlocksToMoveDelayed().runDelayed(getPlayer(), this, blocks ->
            CompletableFuture.completedFuture(handleInput(blocks)), null);
    }

    protected boolean setBlocksToMove(int blocksToMove)
    {
        if (blocksToMove < 1)
            return false;

        final OptionalInt blocksToMoveLimit = limitsManager.getLimit(getPlayer(), Limit.BLOCKS_TO_MOVE);
        if (blocksToMoveLimit.isPresent() && blocksToMove > blocksToMoveLimit.getAsInt())
        {
            getPlayer().sendError(textFactory, localizer.getMessage("creator.base.error.blocks_to_move_too_far",
                                                                    localizer.getMovableType(getMovableType()),
                                                                    Integer.toString(blocksToMove),
                                                                    Integer.toString(blocksToMoveLimit.getAsInt())));
            return false;
        }

        this.blocksToMove = blocksToMove;
        return true;
    }

    @Override
    protected void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.portcullis.stick_lore", "creator.portcullis.init");
    }

    @Override
    protected AbstractMovable constructMovable()
    {
        Util.requireNonNull(cuboid, "cuboid");
        rotationPoint = cuboid.getCenterBlock();
        return new Portcullis(constructMovableData(), blocksToMove);
    }

    @Override
    protected MovableType getMovableType()
    {
        return MOVABLE_TYPE;
    }
}
