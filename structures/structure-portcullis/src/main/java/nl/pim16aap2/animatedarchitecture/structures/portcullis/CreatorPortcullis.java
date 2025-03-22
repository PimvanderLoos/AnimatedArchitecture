package nl.pim16aap2.animatedarchitecture.structures.portcullis;


import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Step;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorInteger;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.core.util.Limit;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

/**
 * Implementation of the {@link Creator} class for the {@link Portcullis} structure.
 */
@ToString(callSuper = true)
@Flogger
@ExtensionMethod(CompletableFutureExtensions.class)
public class CreatorPortcullis extends Creator
{
    private static final StructureType STRUCTURE_TYPE = StructureTypePortcullis.get();

    protected CreatorPortcullis(
        ToolUser.Context context,
        StructureType structureType,
        IPlayer player,
        @Nullable String name)
    {
        super(context, structureType, player, name);
        init();
    }

    public CreatorPortcullis(ToolUser.Context context, IPlayer player, @Nullable String name)
    {
        this(context, STRUCTURE_TYPE, player, name);
    }

    @Override
    protected synchronized List<Step> generateSteps()
        throws InstantiationException
    {
        final Step stepBlocksToMove = stepFactory
            .stepName("SET_BLOCKS_TO_MOVE")
            .textSupplier(text -> text.append(
                localizer.getMessage("creator.portcullis.set_blocks_to_move"),
                TextType.INFO,
                getStructureArg()))
            .propertyName(localizer.getMessage("creator.base.property.blocks_to_move"))
            .propertyValueSupplier(() -> getProperty(Property.BLOCKS_TO_MOVE))
            .updatable(true)
            .stepExecutor(new StepExecutorInteger(this::provideBlocksToMove))
            .stepPreparation(this::prepareSetBlocksToMove)
            .waitForUserInput(true)
            .construct();

        return Arrays.asList(
            factoryProvideName.construct(),
            factoryProvideFirstPos
                .textSupplier(text -> text.append(
                    localizer.getMessage("creator.portcullis.step_1"),
                    TextType.INFO,
                    getStructureArg()))
                .construct(),
            factoryProvideSecondPos
                .textSupplier(text -> text.append(
                    localizer.getMessage("creator.portcullis.step_2"),
                    TextType.INFO,
                    getStructureArg()))
                .construct(),
            factoryProvidePowerBlockPos.construct(),
            factoryProvideOpenStatus.construct(),
            factoryProvideOpenDir.construct(),
            stepBlocksToMove,
            factoryReviewResult.construct(),
            factoryConfirmPrice.construct(),
            factoryCompleteProcess.construct());
    }

    /**
     * Prepares the step that sets the number of blocks to move.
     */
    protected synchronized void prepareSetBlocksToMove()
    {
        commandFactory
            .getSetBlocksToMoveDelayed()
            .runDelayed(getPlayer(), this, this::handleInput, null)
            .handleExceptional(ex -> handleExceptional(ex, "portcullis_prepare_blocks_to_move"));
    }

    protected synchronized boolean provideBlocksToMove(int blocksToMove)
    {
        if (blocksToMove < 1)
        {
            log.atFiner().log("Blocks to move must be at least 1. Got: %d", blocksToMove);
            return false;
        }

        final OptionalInt blocksToMoveLimit = limitsManager.getLimit(getPlayer(), Limit.BLOCKS_TO_MOVE);
        if (blocksToMoveLimit.isPresent() && blocksToMove > blocksToMoveLimit.getAsInt())
        {
            getPlayer().sendMessage(textFactory.newText().append(
                localizer.getMessage("creator.base.error.blocks_to_move_too_far"),
                TextType.ERROR,
                getStructureArg(),
                arg -> arg.highlight(blocksToMove),
                arg -> arg.highlight(blocksToMoveLimit.getAsInt()))
            );
            log.atFiner().log(
                "Blocks to move is too far. Got: %d, Limit: %d", blocksToMove, blocksToMoveLimit.getAsInt());
            return false;
        }

        setProperty(Property.BLOCKS_TO_MOVE, blocksToMove);
        return true;
    }

    @Override
    protected synchronized void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.portcullis.stick_lore");
    }
}
