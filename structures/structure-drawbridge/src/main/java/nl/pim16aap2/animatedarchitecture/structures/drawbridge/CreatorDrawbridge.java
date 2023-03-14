package nl.pim16aap2.animatedarchitecture.structures.drawbridge;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Step;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CreatorDrawbridge extends Creator
{
    private static final StructureType STRUCTURE_TYPE = StructureTypeDrawbridge.get();

    public CreatorDrawbridge(ToolUser.Context context, IPlayer player, @Nullable String name)
    {
        super(context, player, name);
    }

    @Override
    protected List<Step> generateSteps()
        throws InstantiationException
    {
        return Arrays.asList(
            factorySetName.construct(),
            factorySetFirstPos
                .textSupplier(text -> text.append(
                    localizer.getMessage("creator.draw_bridge.step_1"), TextType.INFO, getStructureArg()))
                .construct(),
            factorySetSecondPos
                .textSupplier(text -> text.append(
                    localizer.getMessage("creator.draw_bridge.step_2"), TextType.INFO, getStructureArg()))
                .construct(),
            factorySetRotationPointPos
                .textSupplier(text -> text.append(
                    localizer.getMessage("creator.draw_bridge.step_3"), TextType.INFO, getStructureArg()))
                .construct(),
            factorySetPowerBlockPos.construct(),
            factorySetOpenStatus.construct(),
            factorySetOpenDir.construct(),
            factoryReviewResult.construct(),
            factoryConfirmPrice.construct(),
            factoryCompleteProcess.construct());
    }

    @Override
    protected void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.draw_bridge.stick_lore");
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
