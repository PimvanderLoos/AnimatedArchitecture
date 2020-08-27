package nl.pim16aap2.bigdoors.tooluser;

import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorElevator;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorPLocation;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PowerBlockInspector extends ToolUser
{
    protected PowerBlockInspector(final @NotNull IPPlayer player)
    {
        super(player);
    }

    @Override
    protected void init()
    {
        giveTool(Message.CREATOR_GENERAL_STICKNAME, Message.CREATOR_PBINSPECTOR_STICKLORE,
                 Message.CREATOR_PBINSPECTOR_INIT);
    }

    protected boolean inspectLoc(final @NotNull IPLocationConst loc)
    {
        // TODO: Implement this.
        // Just return true, otherwise it gets very spammy.
        return true;
    }

    @Override
    protected List<IStep> generateSteps()
        throws InstantiationException
    {
        Step<CreatorElevator> stepBlocksToMove = new Step.Factory<CreatorElevator>("INSPECT_POWER_BLOCK")
            .message(Message.CREATOR_PBINSPECTOR_INIT)
            .stepExecutor(new StepExecutorPLocation(this::inspectLoc))
            .waitForUserInput(true).construct();
        return Collections.singletonList(stepBlocksToMove);
    }
}
