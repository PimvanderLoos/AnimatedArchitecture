package nl.pim16aap2.bigdoors.tooluser;

import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorPLocation;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorVoid;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a tool user that relocates a powerblock to a new position.
 *
 * @author Pim
 */
@ToString
public class PowerBlockRelocator extends ToolUser
{
    private final @NotNull AbstractDoor door;
    private @Nullable IPLocation newLoc;

    public PowerBlockRelocator(final @NotNull IPPlayer player, final @NotNull AbstractDoor door)
    {
        super(player);
        this.door = door;
    }

    @Override
    protected void init()
    {
        giveTool(Message.CREATOR_GENERAL_STICKNAME, Message.CREATOR_PBRELOCATOR_STICKLORE,
                 Message.CREATOR_PBRELOCATOR_INIT);
    }

    protected boolean moveToLoc(final @NotNull IPLocation loc)
    {
        if (!loc.getWorld().equals(door.getWorld()))
        {
            getPlayer().sendMessage(BigDoors.get().getPlatform().getMessages()
                                            .getString(Message.CREATOR_PBRELOCATOR_LOCATIONNOTINSAMEWORLD));
            return false;
        }

        if (loc.getPosition().equals(door.getPowerBlock()))
        {
            newLoc = loc;
            return true;
        }

        if (!playerHasAccessToLocation(loc))
            return false;

        newLoc = loc;
        return true;
    }

    private boolean completeProcess()
    {
        if (newLoc == null)
        {
            BigDoors.get().getPLogger().logThrowable(
                new NullPointerException("newLoc is null, which should not be possible at this point!"));
            // TODO: Localization
            getPlayer().sendMessage("An error occurred! Please contact a server admin!");
        }
        else if (door.getPowerBlock().equals(newLoc.getPosition()))
            // TODO: Localization
            getPlayer().sendMessage("New location is the same as the old position! Nothing changed!");
        else
        {
            door.setPowerBlockPosition(newLoc.getPosition());
            door.syncData();
            getPlayer().sendMessage(BigDoors.get().getPlatform().getMessages()
                                            .getString(Message.CREATOR_PBRELOCATOR_SUCCESS));
        }
        return true;
    }

    @Override
    protected @NotNull List<IStep> generateSteps()
        throws InstantiationException
    {
        Step stepPowerblockRelocatorInit = new Step.Factory("RELOCATE_POWER_BLOCK_INIT")
            .message(Message.CREATOR_PBRELOCATOR_INIT)
            .stepExecutor(new StepExecutorPLocation(this::moveToLoc))
            .waitForUserInput(true).construct();

        Step stepPowerblockRelocatorCompleted = new Step.Factory("RELOCATE_POWER_BLOCK_COMPLETED")
            .message(Message.CREATOR_PBRELOCATOR_SUCCESS)
            .stepExecutor(new StepExecutorVoid(this::completeProcess))
            .waitForUserInput(false).construct();

        return Arrays.asList(stepPowerblockRelocatorInit, stepPowerblockRelocatorCompleted);
    }
}
