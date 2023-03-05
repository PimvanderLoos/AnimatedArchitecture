package nl.pim16aap2.animatedarchitecture.structures.portcullis;


import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Step;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorInteger;
import nl.pim16aap2.animatedarchitecture.core.util.Limit;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

public class CreatorPortcullis extends Creator
{
    private static final StructureType STRUCTURE_TYPE = StructureTypePortcullis.get();

    protected int blocksToMove;

    public CreatorPortcullis(Creator.Context context, IPlayer player, @Nullable String name)
    {
        super(context, player, name);
    }

    @Override
    protected List<Step> generateSteps()
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
                             factorySetOpenStatus.construct(),
                             factorySetOpenDir.construct(),
                             stepBlocksToMove,
                             factoryReviewResult.construct(),
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
            getPlayer().sendMessage(textFactory.newText().append(
                localizer.getMessage("creator.base.error.blocks_to_move_too_far"), TextType.ERROR,
                arg -> arg.highlight(localizer.getStructureType(getStructureType())),
                arg -> arg.highlight(blocksToMove),
                arg -> arg.highlight(blocksToMoveLimit.getAsInt())));
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
    protected AbstractStructure constructStructure()
    {
        Util.requireNonNull(cuboid, "cuboid");
        rotationPoint = cuboid.getCenterBlock();
        return new Portcullis(constructStructureData(), blocksToMove);
    }

    @Override
    protected StructureType getStructureType()
    {
        return STRUCTURE_TYPE;
    }
}
