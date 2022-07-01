package nl.pim16aap2.bigdoors.doors.elevator;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.portcullis.CreatorPortcullis;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorInteger;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CreatorElevator extends CreatorPortcullis
{
    private static final DoorType DOOR_TYPE = DoorTypeElevator.get();

    public CreatorElevator(Creator.Context context, IPPlayer player, @Nullable String name)
    {
        super(context, player, name);
    }

    public CreatorElevator(Creator.Context context, IPPlayer player)
    {
        this(context, player, null);
    }

    @Override
    protected List<IStep> generateSteps()
        throws InstantiationException
    {
        final Step stepBlocksToMove = new Step.Factory(localizer, "SET_BLOCKS_TO_MOVE")
            .messageKey("creator.elevator.set_blocks_to_move")
            .stepExecutor(new StepExecutorInteger(this::setBlocksToMove))
            .stepPreparation(this::prepareSetBlocksToMove)
            .waitForUserInput(true).construct();

        return Arrays.asList(factorySetName.construct(),
                             factorySetFirstPos.messageKey("creator.elevator.step_1").construct(),
                             factorySetSecondPos.messageKey("creator.elevator.step_2").construct(),
                             factorySetPowerBlockPos.construct(),
                             factorySetOpenDir.construct(),
                             stepBlocksToMove,
                             factoryConfirmPrice.construct(),
                             factoryCompleteProcess.messageKey("creator.elevator.success").construct());
    }

    @Override
    protected void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.elevator.stick_lore", "creator.elevator.init");
    }

    @Override
    protected AbstractDoor constructDoor()
    {
        Util.requireNonNull(cuboid, "cuboid");
        rotationPoint = cuboid.getCenterBlock();
        return new Elevator(constructDoorData(), blocksToMove);
    }

    @Override
    protected DoorType getDoorType()
    {
        return DOOR_TYPE;
    }
}
