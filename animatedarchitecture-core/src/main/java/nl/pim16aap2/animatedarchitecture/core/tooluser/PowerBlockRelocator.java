package nl.pim16aap2.animatedarchitecture.core.tooluser;

import com.google.common.flogger.StackSize;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorLocation;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorVoid;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a tool user that relocates a powerblock to a new position.
 *
 * @author Pim
 */
@ToString
@Flogger
public class PowerBlockRelocator extends ToolUser
{
    private final AbstractStructure structure;
    private @Nullable ILocation newLoc;

    @AssistedInject
    public PowerBlockRelocator(
        ToolUser.Context context, @Assisted IPlayer player, @Assisted AbstractStructure structure)
    {
        super(context, player);
        this.structure = structure;
    }

    @Override
    protected void init()
    {
        giveTool("tool_user.base.stick_name", "tool_user.powerblock_relocator.stick_lore",
                 "tool_user.powerblock_relocator.init");
    }

    protected boolean moveToLoc(ILocation loc)
    {
        if (!loc.getWorld().equals(structure.getWorld()))
        {
            getPlayer().sendError(textFactory,
                                  localizer.getMessage("tool_user.powerblock_relocator.error.world_mismatch",
                                                       localizer.getStructureType(structure.getType())));
            return false;
        }

        if (loc.getPosition().equals(structure.getPowerBlock()))
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
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("newLoc is null, which should not be possible at this point!");
            getPlayer().sendError(textFactory, localizer.getMessage("constants.error.generic"));
        }
        else if (structure.getPowerBlock().equals(newLoc.getPosition()))
            getPlayer().sendError(textFactory,
                                  localizer.getMessage("tool_user.powerblock_relocator.error.location_unchanged"));
        else
        {
            structure.setPowerBlock(newLoc.getPosition());
            structure.syncData();
            getPlayer().sendSuccess(textFactory, localizer.getMessage("tool_user.powerblock_relocator.success"));
        }
        return true;
    }

    @Override
    protected List<Step> generateSteps()
        throws InstantiationException
    {
        final Step stepPowerblockRelocatorInit = stepFactory
            .stepName("RELOCATE_POWER_BLOCK_INIT")
            .messageKey("tool_user.powerblock_relocator.init")
            .stepExecutor(new StepExecutorLocation(this::moveToLoc))
            .waitForUserInput(true).construct();

        final Step stepPowerblockRelocatorCompleted = stepFactory
            .stepName("RELOCATE_POWER_BLOCK_COMPLETED")
            .messageKey("tool_user.powerblock_relocator.success")
            .stepExecutor(new StepExecutorVoid(this::completeProcess))
            .waitForUserInput(false).construct();

        return Arrays.asList(stepPowerblockRelocatorInit, stepPowerblockRelocatorCompleted);
    }

    @AssistedFactory
    public interface IFactory
    {
        PowerBlockRelocator create(IPlayer player, AbstractStructure structure);
    }
}
