package nl.pim16aap2.bigdoors.doors.portcullis;

import lombok.Getter;
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
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

public class CreatorPortcullis extends Creator
{
    @Getter
    private final @NotNull DoorType doorType = DoorTypePortcullis.get();

    protected int blocksToMove;

    public CreatorPortcullis(final @NotNull IPPlayer player, final @Nullable String name)
    {
        super(player, name);
    }

    public CreatorPortcullis(final @NotNull IPPlayer player)
    {
        this(player, null);
    }

    @Override
    protected @NotNull List<IStep> generateSteps()
        throws InstantiationException
    {
        Step stepBlocksToMove = new Step.Factory("SET_BLOCKS_TO_MOVE")
            .messageKey(Message.CREATOR_PORTCULLIS_BLOCKSTOMOVE)
            .stepExecutor(new StepExecutorInteger(this::setBlocksToMove))
            .waitForUserInput(true).construct();

        return Arrays.asList(factorySetName.messageKey(Message.CREATOR_GENERAL_GIVENAME).construct(),
                             factorySetFirstPos.messageKey(Message.CREATOR_PORTCULLIS_STEP1).construct(),
                             factorySetSecondPos.messageKey(Message.CREATOR_PORTCULLIS_STEP2).construct(),
                             factorySetPowerBlockPos.messageKey(Message.CREATOR_GENERAL_SETPOWERBLOCK).construct(),
                             factorySetOpenDir.messageKey(Message.CREATOR_GENERAL_SETOPENDIR).construct(),
                             stepBlocksToMove,
                             factoryConfirmPrice.messageKey(Message.CREATOR_GENERAL_CONFIRMPRICE).construct(),
                             factoryCompleteProcess.messageKey(Message.CREATOR_PORTCULLIS_SUCCESS).construct());
    }

    protected boolean setBlocksToMove(final int blocksToMove)
    {
        if (blocksToMove < 1)
            return false;

        final @NotNull OptionalInt blocksToMoveLimit = BigDoors.get().getLimitsManager()
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
    protected @NotNull AbstractDoor constructDoor()
    {
        Util.requireNonNull(cuboid, "cuboid");
        engine = cuboid.getCenterBlock();
        return new Portcullis(constructDoorData(), blocksToMove);
    }
}
