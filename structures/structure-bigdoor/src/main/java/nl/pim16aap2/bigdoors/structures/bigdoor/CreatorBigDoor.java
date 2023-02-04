package nl.pim16aap2.bigdoors.structures.bigdoor;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CreatorBigDoor extends Creator
{
    private static final StructureType STRUCTURE_TYPE = StructureTypeBigDoor.get();

    public CreatorBigDoor(Creator.Context context, IPPlayer player, @Nullable String name)
    {
        super(context, player, name);
    }

    public CreatorBigDoor(Creator.Context context, IPPlayer player)
    {
        this(context, player, null);
    }

    @Override
    protected List<IStep> generateSteps()
        throws InstantiationException
    {
        return Arrays.asList(factorySetName.construct(),
                             factorySetFirstPos.messageKey("creator.big_door.step_1").construct(),
                             factorySetSecondPos.messageKey("creator.big_door.step_2").construct(),
                             factorySetRotationPointPos.messageKey("creator.big_door.step_3").construct(),
                             factorySetPowerBlockPos.construct(),
                             factorySetOpenStatus.construct(),
                             factorySetOpenDir.construct(),
                             factoryConfirmPrice.construct(),
                             factoryCompleteProcess.messageKey("creator.big_door.success").construct());
    }

    @Override
    protected void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.big_door.stick_lore", "creator.big_door.init");
    }

    @Override
    protected AbstractStructure constructStructure()
    {
        return new BigDoor(constructStructureData());
    }

    @Override
    protected StructureType getStructureType()
    {
        return STRUCTURE_TYPE;
    }
}
