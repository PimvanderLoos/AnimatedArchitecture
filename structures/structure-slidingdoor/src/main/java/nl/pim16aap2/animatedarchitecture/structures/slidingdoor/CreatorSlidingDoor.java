package nl.pim16aap2.animatedarchitecture.structures.slidingdoor;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Step;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorInteger;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.Limit;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

@ToString(callSuper = true)
public class CreatorSlidingDoor extends Creator
{
    private static final StructureType STRUCTURE_TYPE = StructureTypeSlidingDoor.get();

    @GuardedBy("this")
    private int blocksToMove;

    public CreatorSlidingDoor(ToolUser.Context context, IPlayer player, @Nullable String name)
    {
        super(context, player, name);
        init();
    }

    @Override
    protected synchronized List<Step> generateSteps()
        throws InstantiationException
    {
        final Step stepBlocksToMove = stepFactory
            .stepName("SET_BLOCKS_TO_MOVE")
            .textSupplier(text -> text.append(
                localizer.getMessage("creator.sliding_door.set_blocks_to_move"), TextType.INFO, getStructureArg()))
            .propertyName(localizer.getMessage("creator.base.property.blocks_to_move"))
            .propertyValueSupplier(this::getBlocksToMove)
            .updatable(true)
            .stepExecutor(new StepExecutorInteger(this::provideBlocksToMove))
            .stepPreparation(this::prepareSetBlocksToMove)
            .waitForUserInput(true).construct();

        return Arrays.asList(
            factoryProvideName.construct(),
            factoryProvideFirstPos
                .textSupplier(text -> text.append(
                    localizer.getMessage("creator.sliding_door.step_1"), TextType.INFO, getStructureArg()))
                .construct(),
            factoryProvideSecondPos
                .textSupplier(text -> text.append(
                    localizer.getMessage("creator.sliding_door.step_2"), TextType.INFO, getStructureArg()))
                .construct(),
            factoryProvidePowerBlockPos.construct(),
            factoryProvideOpenStatus.construct(),
            factoryProvideOpenDir.construct(),
            stepBlocksToMove,
            factoryReviewResult.construct(),
            factoryConfirmPrice.construct(),
            factoryCompleteProcess.construct());
    }

    /**
     * Prepares the step that sets the number of blocks to move.
     */
    protected synchronized void prepareSetBlocksToMove()
    {
        commandFactory
            .getSetBlocksToMoveDelayed()
            .runDelayed(getPlayer(), this, blocks -> CompletableFuture.completedFuture(handleInput(blocks)), null)
            .exceptionally(Util::exceptionally);
    }

    protected synchronized boolean provideBlocksToMove(int blocksToMove)
    {
        if (blocksToMove < 1)
            return false;

        final OptionalInt blocksToMoveLimit = limitsManager.getLimit(getPlayer(), Limit.BLOCKS_TO_MOVE);
        if (blocksToMoveLimit.isPresent() && blocksToMove > blocksToMoveLimit.getAsInt())
        {
            getPlayer().sendMessage(textFactory.newText().append(
                localizer.getMessage("creator.base.error.blocks_to_move_too_far"), TextType.ERROR,
                getStructureArg(),
                arg -> arg.highlight(blocksToMove),
                arg -> arg.highlight(blocksToMoveLimit.getAsInt())));
            return false;
        }

        this.blocksToMove = blocksToMove;
        return true;
    }

    @Override
    protected synchronized void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.sliding_door.stick_lore");
    }

    @Override
    protected synchronized AbstractStructure constructStructure()
    {
        final Cuboid cuboid = Util.requireNonNull(getCuboid(), "cuboid");
        setRotationPoint(cuboid.getCenterBlock());
        return new SlidingDoor(constructStructureData(), blocksToMove);
    }

    @Override
    protected StructureType getStructureType()
    {
        return STRUCTURE_TYPE;
    }

    protected final synchronized int getBlocksToMove()
    {
        return blocksToMove;
    }
}
