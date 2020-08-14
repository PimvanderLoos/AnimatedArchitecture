package nl.pim16aap2.bigdoors.tooluser.creator;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Portcullis;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypePortcullis;
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorInteger;
import nl.pim16aap2.bigdoors.util.Limit;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

public class CreatorPortcullis extends Creator
{
    @Getter(onMethod = @__({@Override}))
    @NotNull
    private final DoorType doorType = DoorTypePortcullis.get();

    protected int blocksToMove;

    public CreatorPortcullis(final @NotNull IPPlayer player, final @Nullable String name)
    {
        super(player);
        if (name != null)
            completeNamingStep(name);
        prepareCurrentStep();
    }

    public CreatorPortcullis(final @NotNull IPPlayer player)
    {
        this(player, null);
    }

    @Override
    @NotNull
    protected List<IStep> generateSteps()
        throws InstantiationException
    {
        Step<CreatorPortcullis> stepBlocksToMove = new Step.Factory<CreatorPortcullis>("SET_BLOCKS_TO_MOVE")
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
        final @NotNull OptionalInt blocksToMoveLimit = LimitsManager.getLimit(player, Limit.BLOCKS_TO_MOVE);
        if (blocksToMoveLimit.isPresent() && blocksToMove > blocksToMoveLimit.getAsInt())
        {
            player.sendMessage(messages.getString(Message.CREATOR_GENERAL_BLOCKSTOMOVETOOFAR,
                                                  Integer.toString(blocksToMove),
                                                  Integer.toString(blocksToMoveLimit.getAsInt())));
            return false;
        }

        this.blocksToMove = blocksToMove;
        procedure.goToNextStep();
        return true;
    }

    @Override
    protected void giveTool()
    {
        giveTool(Message.CREATOR_GENERAL_STICKNAME, Message.CREATOR_PORTCULLIS_STICKLORE,
                 Message.CREATOR_PORTCULLIS_INIT);
    }

    @Override
    @NotNull
    protected AbstractDoorBase constructDoor()
    {
        engine = cuboid.getCenterBlock();
        return new Portcullis(constructDoorData(), blocksToMove);
    }
}
