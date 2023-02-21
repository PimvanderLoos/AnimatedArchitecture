package nl.pim16aap2.animatedarchitecture.structures.drawbridge;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Step;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CreatorDrawbridge extends Creator
{
    private static final StructureType STRUCTURE_TYPE = StructureTypeDrawbridge.get();

    public CreatorDrawbridge(Creator.Context context, IPlayer player, @Nullable String name)
    {
        super(context, player, name);
    }

    @Override
    protected List<Step> generateSteps()
        throws InstantiationException
    {
        return Arrays.asList(factorySetName.construct(),
                             factorySetFirstPos.messageKey("creator.draw_bridge.step_1").construct(),
                             factorySetSecondPos.messageKey("creator.draw_bridge.step_2").construct(),
                             factorySetRotationPointPos.messageKey("creator.draw_bridge.step_3").construct(),
                             factorySetPowerBlockPos.construct(),
                             factorySetOpenStatus.construct(),
                             factorySetOpenDir.construct(),
                             factoryConfirmPrice.construct(),
                             factoryCompleteProcess.messageKey("creator.draw_bridge.success").construct());
    }

    @Override
    protected void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.draw_bridge.stick_lore", "creator.draw_bridge.init");
    }

    @Override
    protected AbstractStructure constructStructure()
    {
        return new Drawbridge(constructStructureData());
    }

    @Override
    protected StructureType getStructureType()
    {
        return STRUCTURE_TYPE;
    }
}
