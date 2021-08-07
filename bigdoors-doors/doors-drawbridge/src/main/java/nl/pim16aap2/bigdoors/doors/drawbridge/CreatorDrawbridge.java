package nl.pim16aap2.bigdoors.doors.drawbridge;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CreatorDrawbridge extends Creator
{
    @Getter
    private final @NotNull DoorType doorType = DoorTypeDrawbridge.get();

    public CreatorDrawbridge(final @NotNull IPPlayer player, final @Nullable String name)
    {
        super(player, name);
    }

    public CreatorDrawbridge(final @NotNull IPPlayer player)
    {
        this(player, null);
    }

    @Override
    protected @NotNull List<IStep> generateSteps()
        throws InstantiationException
    {
        return Arrays.asList(factorySetName.messageKey("creator.base.give_name")
                                           .messageVariableRetriever(
                                               () -> DoorTypeDrawbridge.get().getLocalizationKey())
                                           .construct(),
                             factorySetFirstPos.messageKey("creator.draw_bridge.step_1").construct(),
                             factorySetSecondPos.messageKey("creator.draw_bridge.step_2").construct(),
                             factorySetEnginePos.messageKey("creator.draw_bridge.step_3").construct(),
                             factorySetPowerBlockPos.messageKey("creator.base.set_power_block").construct(),
                             factorySetOpenDir.messageKey("creator.base.set_open_dir").construct(),
                             factoryConfirmPrice.construct(),
                             factoryCompleteProcess.messageKey("creator.draw_bridge.success").construct());
    }

    @Override
    protected void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.draw_bridge.stick_lore", "creator.draw_bridge.init");
    }

    @Override
    protected @NotNull AbstractDoor constructDoor()
    {
        return new Drawbridge(constructDoorData(), true);
    }
}
