package nl.pim16aap2.bigdoors.tooluser;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorPLocation;
import nl.pim16aap2.bigdoors.util.messages.Message;

import java.util.Collections;
import java.util.List;

public class PowerBlockInspector extends ToolUser
{
    protected PowerBlockInspector(final @NonNull IPPlayer player)
    {
        super(player);
    }

    @Override
    protected void init()
    {
        giveTool(Message.CREATOR_GENERAL_STICKNAME, Message.CREATOR_PBINSPECTOR_STICKLORE,
                 Message.CREATOR_PBINSPECTOR_INIT);
    }

    protected boolean inspectLoc(final @NonNull IPLocationConst loc)
    {
        // TODO: Implement this.
        // Just return true, otherwise it gets very spammy.
        return true;
    }

    @Override
    protected @NonNull List<IStep> generateSteps()
        throws InstantiationException
    {
        Step<ToolUser> stepBlocksToMove = new Step.Factory<>("INSPECT_POWER_BLOCK")
            .message(Message.CREATOR_PBINSPECTOR_INIT)
            .stepExecutor(new StepExecutorPLocation(this::inspectLoc))
            .waitForUserInput(true).construct();
        return Collections.singletonList(stepBlocksToMove);
    }
}
