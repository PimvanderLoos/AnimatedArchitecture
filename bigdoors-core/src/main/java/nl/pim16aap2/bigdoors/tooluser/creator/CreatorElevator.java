package nl.pim16aap2.bigdoors.tooluser.creator;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Elevator;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeElevator;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorInteger;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CreatorElevator extends CreatorPortcullis
{
    @Getter(onMethod = @__({@Override}))
    @NotNull
    private final DoorType doorType = DoorTypeElevator.get();

    public CreatorElevator(final @NotNull IPPlayer player, final @Nullable String name)
    {
        super(player, name);
    }

    public CreatorElevator(final @NotNull IPPlayer player)
    {
        this(player, null);
    }

    @Override
    @NotNull
    protected List<IStep> generateSteps()
        throws InstantiationException
    {
        Step<CreatorElevator> stepBlocksToMove = new Step.Factory<CreatorElevator>("SET_BLOCKS_TO_MOVE")
            .message(Message.CREATOR_ELEVATOR_BLOCKSTOMOVE)
            .stepExecutor(new StepExecutorInteger(this::setBlocksToMove))
            .waitForUserInput(true).construct();

        return Arrays.asList(factorySetName.message(Message.CREATOR_GENERAL_GIVENAME).construct(),
                             factorySetFirstPos.message(Message.CREATOR_ELEVATOR_STEP1).construct(),
                             factorySetSecondPos.message(Message.CREATOR_ELEVATOR_STEP2).construct(),
                             factorySetPowerBlockPos.message(Message.CREATOR_GENERAL_SETPOWERBLOCK).construct(),
                             factorySetOpenDir.message(Message.CREATOR_GENERAL_SETOPENDIR).construct(),
                             stepBlocksToMove,
                             factoryConfirmPrice.message(Message.CREATOR_GENERAL_CONFIRMPRICE).construct(),
                             factoryCompleteProcess.message(Message.CREATOR_ELEVATOR_SUCCESS).construct());
    }

    @Override
    protected void giveTool()
    {
        giveTool(Message.CREATOR_GENERAL_STICKNAME, Message.CREATOR_ELEVATOR_STICKLORE, Message.CREATOR_ELEVATOR_INIT);
    }

    @Override
    @NotNull
    protected AbstractDoorBase constructDoor()
    {
        engine = cuboid.getCenterBlock();
        return new Elevator(constructDoorData(), blocksToMove);
    }
}
