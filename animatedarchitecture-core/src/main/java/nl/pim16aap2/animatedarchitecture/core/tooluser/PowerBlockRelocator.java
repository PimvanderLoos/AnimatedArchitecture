package nl.pim16aap2.animatedarchitecture.core.tooluser;

import com.google.common.flogger.StackSize;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.AsyncStepExecutor;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorVoid;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a tool user that relocates a powerblock to a new position.
 */
@Flogger
@ToString(callSuper = true)
@ExtensionMethod(CompletableFutureExtensions.class)
public class PowerBlockRelocator extends ToolUser
{
    private final Structure structure;

    @GuardedBy("this")
    private @Nullable ILocation newLoc;

    @AssistedInject
    public PowerBlockRelocator(
        ToolUser.Context context,
        @Assisted IPlayer player,
        @Assisted Structure structure)
    {
        super(context, player);
        this.structure = structure;

        giveTool(
            "tool_user.base.stick_name",
            "tool_user.powerblock_relocator.stick_lore",
            player.newText().append(localizer.getMessage("tool_user.powerblock_relocator.init"), TextType.INFO));

        init();
    }

    protected CompletableFuture<Boolean> moveToLoc(ILocation loc)
    {
        if (!loc.getWorld().equals(structure.getWorld()))
        {
            getPlayer().sendError(
                "tool_user.powerblock_relocator.error.world_mismatch",
                arg -> arg.localizedHighlight(structure.getType())
            );
            return CompletableFuture.completedFuture(false);
        }

        if (loc.getPosition().equals(structure.getPowerBlock()))
        {
            synchronized (this)
            {
                newLoc = loc;
            }
            return CompletableFuture.completedFuture(true);
        }

        return playerHasAccessToLocation(loc)
            .thenApply(hasAccess ->
            {
                if (!hasAccess)
                    return false;

                synchronized (this)
                {
                    newLoc = loc;
                }
                return true;
            });
    }

    private synchronized boolean completeProcess()
    {
        if (newLoc == null)
        {
            log.atSevere().withStackTrace(StackSize.FULL).log(
                "newLoc is null, which should not be possible at this point!");
            getPlayer().sendError("constants.error.generic");
        }
        else if (structure.getPowerBlock().equals(newLoc.getPosition()))
            getPlayer().sendError("tool_user.powerblock_relocator.error.location_unchanged");
        else
        {
            structure.setPowerBlock(newLoc.getPosition());
            structure
                .syncData()
                .thenRun(() ->
                    getPlayer().sendSuccess("tool_user.powerblock_relocator.success"))
                .handleExceptional(ex ->
                {
                    getPlayer().sendError("constants.error.generic");
                    log.atSevere().withCause(ex).log("Failed to sync structure data after powerblock relocation.");
                });
        }
        return true;
    }

    @Override
    protected synchronized List<Step> generateSteps()
        throws InstantiationException
    {
        final Step stepPowerblockRelocatorInit = stepFactory
            .stepName(localizer, "RELOCATE_POWER_BLOCK_INIT")
            .messageKey("tool_user.powerblock_relocator.init")
            .stepExecutor(new AsyncStepExecutor<>(ILocation.class, this::moveToLoc))
            .waitForUserInput(true).construct();

        final Step stepPowerblockRelocatorCompleted = stepFactory
            .stepName(localizer, "RELOCATE_POWER_BLOCK_COMPLETED")
            .messageKey("tool_user.powerblock_relocator.success")
            .stepExecutor(new StepExecutorVoid(this::completeProcess))
            .waitForUserInput(false).construct();

        return Arrays.asList(stepPowerblockRelocatorInit, stepPowerblockRelocatorCompleted);
    }

    /**
     * Gets the new location of the powerblock.
     *
     * @return The new location of the powerblock. May be {@code null} if the powerblock has not been relocated yet.
     */
    @SuppressWarnings("unused") // It is used by the generated toString method.
    protected final synchronized @Nullable ILocation getNewLoc()
    {
        return newLoc;
    }

    @AssistedFactory
    public interface IFactory
    {
        PowerBlockRelocator create(IPlayer player, Structure structure);
    }
}
