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
        giveTool("tool_user.base.stick_name", "tool_user.powerblock_relocator.stick_lore",
                 "tool_user.powerblock_relocator.init");
    }

    protected boolean moveToLoc(final @NotNull IPLocation loc)
    {
        if (!loc.getWorld().equals(door.getWorld()))
        {
            getPlayer().sendMessage(BigDoors.get().getLocalizer()
                                            .getMessage("tool_user.powerblock_relocator.error.world_mismatch"));
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
            getPlayer().sendMessage(BigDoors.get().getLocalizer().getMessage("constants.error.generic"));
        }
        else if (door.getPowerBlock().equals(newLoc.getPosition()))
            getPlayer().sendMessage(BigDoors.get().getLocalizer()
                                            .getMessage("tool_user.powerblock_relocator.error.location_unchanged"));
        else
        {
            door.setPowerBlockPosition(newLoc.getPosition());
            door.syncData();
            getPlayer().sendMessage(BigDoors.get().getLocalizer()
                                            .getMessage("tool_user.powerblock_relocator.success"));
        }
        return true;
    }

    @Override
    protected @NotNull List<IStep> generateSteps()
        throws InstantiationException
    {
        Step stepPowerblockRelocatorInit = new Step.Factory("RELOCATE_POWER_BLOCK_INIT")
            .message("tool_user.powerblock_relocator.init")
            .stepExecutor(new StepExecutorPLocation(this::moveToLoc))
            .waitForUserInput(true).construct();

        Step stepPowerblockRelocatorCompleted = new Step.Factory("RELOCATE_POWER_BLOCK_COMPLETED")
            .message("tool_user.powerblock_relocator.success")
            .stepExecutor(new StepExecutorVoid(this::completeProcess))
            .waitForUserInput(false).construct();

        return Arrays.asList(stepPowerblockRelocatorInit, stepPowerblockRelocatorCompleted);
    }
}
