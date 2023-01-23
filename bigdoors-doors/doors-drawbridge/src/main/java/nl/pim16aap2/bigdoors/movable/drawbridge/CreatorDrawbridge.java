package nl.pim16aap2.bigdoors.movable.drawbridge;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CreatorDrawbridge extends Creator
{
    private static final MovableType MOVABLE_TYPE = MovableTypeDrawbridge.get();

    public CreatorDrawbridge(Creator.Context context, IPPlayer player, @Nullable String name)
    {
        super(context, player, name);
    }

    public CreatorDrawbridge(Creator.Context context, IPPlayer player)
    {
        this(context, player, null);
    }

    @Override
    protected List<IStep> generateSteps()
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
    protected AbstractMovable constructMovable()
    {
        return new Drawbridge(constructMovableData(), true);
    }

    @Override
    protected MovableType getMovableType()
    {
        return MOVABLE_TYPE;
    }
}
