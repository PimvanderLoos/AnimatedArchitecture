package nl.pim16aap2.bigdoors.tooluser;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorPLocation;
import nl.pim16aap2.bigdoors.util.messages.Message;

import java.util.Collections;
import java.util.List;

/**
 * Represents a type of {@link ToolUser} that tries to find powerblocks based on the locations provided by the user.
 *
 * @author Pim
 */
@ToString
public class PowerBlockInspector extends ToolUser
{
    /**
     * Whether or not this user has the bypass permission.
     * <p>
     * When this is true, the user does not have to be an owner of the door to retrieve its location.
     */
    private final boolean bypassPermission;

    public PowerBlockInspector(final @NonNull IPPlayer player, final boolean bypassPermission)
    {
        super(player);
        this.bypassPermission = bypassPermission;
    }

    public PowerBlockInspector(final @NonNull IPPlayer player)
    {
        this(player, false);
    }

    @Override
    protected void init()
    {
        giveTool(Message.CREATOR_GENERAL_STICKNAME, Message.CREATOR_PBINSPECTOR_STICKLORE,
                 Message.CREATOR_PBINSPECTOR_INIT);
    }

    protected boolean inspectLoc(final @NonNull IPLocationConst loc)
    {
        throw new UnsupportedOperationException("This action has not been implemented yet!");
    }

    @Override
    protected @NonNull List<IStep> generateSteps()
        throws InstantiationException
    {
        Step stepBlocksToMove = new Step.Factory("INSPECT_POWER_BLOCK")
            .message(Message.CREATOR_PBINSPECTOR_INIT)
            .stepExecutor(new StepExecutorPLocation(this::inspectLoc))
            .waitForUserInput(true).construct();
        return Collections.singletonList(stepBlocksToMove);
    }
}
