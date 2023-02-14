package nl.pim16aap2.bigdoors.structures.elevator;

import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.StructureType;
import nl.pim16aap2.bigdoors.core.tooluser.Step;
import nl.pim16aap2.bigdoors.core.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.core.tooluser.stepexecutor.StepExecutorInteger;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.structures.portcullis.CreatorPortcullis;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CreatorElevator extends CreatorPortcullis
{
    private static final StructureType STRUCTURE_TYPE = StructureTypeElevator.get();

    public CreatorElevator(Creator.Context context, IPlayer player, @Nullable String name)
    {
        super(context, player, name);
    }

    public CreatorElevator(Creator.Context context, IPlayer player)
    {
        this(context, player, null);
    }

    @Override
    protected List<Step> generateSteps()
        throws InstantiationException
    {
        final Step stepBlocksToMove = stepFactory
            .stepName("SET_BLOCKS_TO_MOVE")
            .messageKey("creator.elevator.set_blocks_to_move")
            .stepExecutor(new StepExecutorInteger(this::setBlocksToMove))
            .stepPreparation(this::prepareSetBlocksToMove)
            .waitForUserInput(true).construct();

        return Arrays.asList(factorySetName.construct(),
                             factorySetFirstPos.messageKey("creator.elevator.step_1").construct(),
                             factorySetSecondPos.messageKey("creator.elevator.step_2").construct(),
                             factorySetPowerBlockPos.construct(),
                             factorySetOpenStatus.construct(),
                             factorySetOpenDir.construct(),
                             stepBlocksToMove,
                             factoryConfirmPrice.construct(),
                             factoryCompleteProcess.messageKey("creator.elevator.success").construct());
    }

    @Override
    protected void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.elevator.stick_lore", "creator.elevator.init");
    }

    @Override
    protected AbstractStructure constructStructure()
    {
        Util.requireNonNull(cuboid, "cuboid");
        rotationPoint = cuboid.getCenterBlock();
        return new Elevator(constructStructureData(), blocksToMove);
    }

    @Override
    protected StructureType getStructureType()
    {
        return STRUCTURE_TYPE;
    }
}
