package nl.pim16aap2.bigdoors.doors.portcullis;

import nl.pim16aap2.bigdoors.BigDoors;
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

public class CreatorPortcullis extends Creator
{
    private static final DoorType DOOR_TYPE = DoorTypePortcullis.get();

    protected int blocksToMove;

    public CreatorPortcullis(IPPlayer player, @Nullable String name)
    {
        super(player, name);
    }

    public CreatorPortcullis(IPPlayer player)
    {
        this(player, null);
    }

    @Override
    protected List<IStep> generateSteps()
        throws InstantiationException
    {
        final Step stepBlocksToMove = new Step.Factory("SET_BLOCKS_TO_MOVE")
            .messageKey("creator.portcullis.set_blocks_to_move")
            .stepExecutor(new StepExecutorInteger(this::setBlocksToMove))
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

    protected boolean setBlocksToMove(int blocksToMove)
    {
        if (blocksToMove < 1)
            return false;

        final OptionalInt blocksToMoveLimit = BigDoors.get().getLimitsManager()
                                                      .getLimit(getPlayer(), Limit.BLOCKS_TO_MOVE);
        if (blocksToMoveLimit.isPresent() && blocksToMove > blocksToMoveLimit.getAsInt())
        {
            getPlayer().sendMessage(BigDoors.get().getLocalizer()
                                            .getMessage("creator.base.error.blocks_to_move_too_far",
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
    protected AbstractDoor constructDoor()
    {
        Util.requireNonNull(cuboid, "cuboid");
        engine = cuboid.getCenterBlock();
        return new Portcullis(constructDoorData(), blocksToMove);
    }

    @Override
    protected DoorType getDoorType()
    {
        return DOOR_TYPE;
    }
}
