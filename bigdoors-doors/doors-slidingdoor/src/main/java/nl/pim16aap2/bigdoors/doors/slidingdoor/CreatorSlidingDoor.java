package nl.pim16aap2.bigdoors.doors.slidingdoor;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
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

public class CreatorSlidingDoor extends Creator
{
    private static final DoorType DOOR_TYPE = DoorTypeSlidingDoor.get();

    protected int blocksToMove;

    public CreatorSlidingDoor(Creator.Context context, IPPlayer player, @Nullable String name)
    {
        super(context, player, name);
    }

    public CreatorSlidingDoor(Creator.Context context, IPPlayer player)
    {
        this(context, player, null);
    }

    @Override
    protected List<IStep> generateSteps()
        throws InstantiationException
    {
        final Step stepBlocksToMove = new Step.Factory(localizer, "SET_BLOCKS_TO_MOVE")
            .messageKey("creator.sliding_door.set_blocks_to_move")
            .stepExecutor(new StepExecutorInteger(this::setBlocksToMove))
            .waitForUserInput(true).construct();

        return Arrays.asList(factorySetName.construct(),
                             factorySetFirstPos.messageKey("creator.sliding_door.step_1").construct(),
                             factorySetSecondPos.messageKey("creator.sliding_door.step_2").construct(),
                             factorySetPowerBlockPos.construct(),
                             factorySetOpenDir.construct(),
                             stepBlocksToMove,
                             factoryConfirmPrice.construct(),
                             factoryCompleteProcess.messageKey("creator.sliding_door.success").construct());
    }

    protected boolean setBlocksToMove(int blocksToMove)
    {
        if (blocksToMove < 1)
            return false;

        final OptionalInt blocksToMoveLimit = limitsManager.getLimit(getPlayer(), Limit.BLOCKS_TO_MOVE);
        if (blocksToMoveLimit.isPresent() && blocksToMove > blocksToMoveLimit.getAsInt())
        {
            getPlayer().sendMessage(localizer.getMessage("creator.base.error.blocks_to_move_too_far",
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
        giveTool("tool_user.base.stick_name", "creator.sliding_door.stick_lore", "creator.sliding_door.init");
    }

    @Override
    protected AbstractDoor constructDoor()
    {
        Util.requireNonNull(cuboid, "cuboid");
        rotationPoint = cuboid.getCenterBlock();
        return new SlidingDoor(constructDoorData(), blocksToMove);
    }

    @Override
    protected DoorType getDoorType()
    {
        return DOOR_TYPE;
    }
}
