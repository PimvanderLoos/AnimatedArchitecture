package nl.pim16aap2.bigdoors.structures.revolvingdoor;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.structures.bigdoor.CreatorBigDoor;
import nl.pim16aap2.bigdoors.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CreatorRevolvingDoor extends CreatorBigDoor
{
    private static final StructureType STRUCTURE_TYPE = StructureTypeRevolvingDoor.get();

    public CreatorRevolvingDoor(Creator.Context context, IPPlayer player, @Nullable String name)
    {
        super(context, player);
        if (name != null)
            completeNamingStep(name);
        prepareCurrentStep();
    }

    public CreatorRevolvingDoor(Creator.Context context, IPPlayer player)
    {
        this(context, player, null);
    }

    @Override
    protected List<IStep> generateSteps()
        throws InstantiationException
    {
        return Arrays.asList(factorySetName.construct(),
                             factorySetFirstPos.messageKey("creator.revolving_door.step_1").construct(),
                             factorySetSecondPos.messageKey("creator.revolving_door.step_2").construct(),
                             factorySetRotationPointPos.messageKey("creator.revolving_door.step_3").construct(),
                             factorySetPowerBlockPos.construct(),
                             factorySetOpenStatus.construct(),
                             factorySetOpenDir.construct(),
                             factoryConfirmPrice.construct(),
                             factoryCompleteProcess.messageKey("creator.revolving_door.success").construct());
    }

    @Override
    protected void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.revolving_door.stick_lore", "creator.revolving_door.init");
    }

    @Override
    protected AbstractStructure constructStructure()
    {
        return new RevolvingDoor(constructStructureData());
    }

    @Override
    protected StructureType getStructureType()
    {
        return STRUCTURE_TYPE;
    }
}
