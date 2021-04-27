package nl.pim16aap2.bigdoors.doors.portcullis;

import lombok.Getter;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorInteger;
import nl.pim16aap2.bigdoors.util.Limit;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

public class CreatorPortcullis extends Creator
{
    @Getter
    private final @NonNull DoorType doorType = DoorTypePortcullis.get();

    protected int blocksToMove;

    public CreatorPortcullis(final @NonNull IPPlayer player, final @Nullable String name)
    {
        super(player);
        if (name != null)
            completeNamingStep(name);
        prepareCurrentStep();
    }

    public CreatorPortcullis(final @NonNull IPPlayer player)
    {
        this(player, null);
    }

    @Override
    protected @NonNull List<IStep> generateSteps()
        throws InstantiationException
    {
        Step stepBlocksToMove = new Step.Factory("SET_BLOCKS_TO_MOVE")
            .message(Message.CREATOR_PORTCULLIS_BLOCKSTOMOVE)
            .stepExecutor(new StepExecutorInteger(this::setBlocksToMove))
            .waitForUserInput(true).construct();

        return Arrays.asList(factorySetName.message(Message.CREATOR_GENERAL_GIVENAME).construct(),
                             factorySetFirstPos.message(Message.CREATOR_PORTCULLIS_STEP1).construct(),
                             factorySetSecondPos.message(Message.CREATOR_PORTCULLIS_STEP2).construct(),
                             factorySetPowerBlockPos.message(Message.CREATOR_GENERAL_SETPOWERBLOCK).construct(),
                             factorySetOpenDir.message(Message.CREATOR_GENERAL_SETOPENDIR).construct(),
                             stepBlocksToMove,
                             factoryConfirmPrice.message(Message.CREATOR_GENERAL_CONFIRMPRICE).construct(),
                             factoryCompleteProcess.message(Message.CREATOR_PORTCULLIS_SUCCESS).construct());
    }

    protected boolean setBlocksToMove(final int blocksToMove)
    {
        if (blocksToMove < 1)
            return false;

        final @NonNull OptionalInt blocksToMoveLimit = BigDoors.get().getLimitsManager()
                                                               .getLimit(getPlayer(), Limit.BLOCKS_TO_MOVE);
        if (blocksToMoveLimit.isPresent() && blocksToMove > blocksToMoveLimit.getAsInt())
        {
            getPlayer().sendMessage(BigDoors.get().getPlatform().getMessages()
                                            .getString(Message.CREATOR_GENERAL_BLOCKSTOMOVETOOFAR,
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
        giveTool(Message.CREATOR_GENERAL_STICKNAME, Message.CREATOR_PORTCULLIS_STICKLORE,
                 Message.CREATOR_PORTCULLIS_INIT);
    }

    @Override
    protected @NonNull AbstractDoorBase constructDoor()
    {
        engine = cuboid.getCenterBlock();
        return new Portcullis(constructDoorData(), blocksToMove);
    }
}
