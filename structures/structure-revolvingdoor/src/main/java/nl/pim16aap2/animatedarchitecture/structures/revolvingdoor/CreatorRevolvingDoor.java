package nl.pim16aap2.animatedarchitecture.structures.revolvingdoor;

import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Step;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.structures.bigdoor.CreatorBigDoor;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

@ToString(callSuper = true)
public class CreatorRevolvingDoor extends CreatorBigDoor
{
    private static final StructureType STRUCTURE_TYPE = StructureTypeRevolvingDoor.get();

    protected CreatorRevolvingDoor(
        ToolUser.Context context,
        StructureType structureType,
        IPlayer player,
        @Nullable String name)
    {
        super(context, structureType, player, name);
    }

    public CreatorRevolvingDoor(ToolUser.Context context, IPlayer player, @Nullable String name)
    {
        this(context, STRUCTURE_TYPE, player, name);
    }

    @Override
    protected synchronized List<Step> generateSteps()
        throws InstantiationException
    {
        return Arrays.asList(
            factoryProvideName.construct(),
            factoryProvideFirstPos
                .textSupplier(text -> text.append(
                    localizer.getMessage("creator.revolving_door.step_1"),
                    TextType.INFO,
                    getStructureArg()))
                .construct(),
            factoryProvideSecondPos
                .textSupplier(text -> text.append(
                    localizer.getMessage("creator.revolving_door.step_2"),
                    TextType.INFO,
                    getStructureArg()))
                .construct(),
            factoryProvideRotationPointPos
                .textSupplier(text -> text.append(
                    localizer.getMessage("creator.revolving_door.step_3"),
                    TextType.INFO,
                    getStructureArg()))
                .construct(),
            factoryProvidePowerBlockPos.construct(),
            factoryProvideOpenDir.construct(),
            factoryReviewResult.construct(),
            factoryConfirmPrice.construct(),
            factoryCompleteProcess.construct());
    }

    @Override
    protected synchronized void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.revolving_door.stick_lore");
    }
}
