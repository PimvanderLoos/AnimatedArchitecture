package nl.pim16aap2.bigdoors.doors.windmill;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CreatorWindMill extends Creator
{
    @Getter
    private final @NotNull DoorType doorType = DoorTypeWindmill.get();

    public CreatorWindMill(final @NotNull IPPlayer player, final @Nullable String name)
    {
        super(player, name);
    }

    public CreatorWindMill(final @NotNull IPPlayer player)
    {
        this(player, null);
    }

    @Override
    protected @NotNull List<IStep> generateSteps()
        throws InstantiationException
    {
        return Arrays.asList(factorySetName.messageKey(Message.CREATOR_GENERAL_GIVENAME).construct(),
                             factorySetFirstPos.messageKey(Message.CREATOR_WINDMILL_STEP1).construct(),
                             factorySetSecondPos.messageKey(Message.CREATOR_WINDMILL_STEP2).construct(),
                             factorySetEnginePos.messageKey(Message.CREATOR_WINDMILL_STEP3).construct(),
                             factorySetPowerBlockPos.messageKey(Message.CREATOR_GENERAL_SETPOWERBLOCK).construct(),
                             factorySetOpenDir.messageKey(Message.CREATOR_GENERAL_SETOPENDIR).construct(),
                             factoryConfirmPrice.messageKey(Message.CREATOR_GENERAL_CONFIRMPRICE).construct(),
                             factoryCompleteProcess.messageKey(Message.CREATOR_WINDMILL_SUCCESS).construct());
    }

    @Override
    protected void giveTool()
    {
        giveTool(Message.CREATOR_GENERAL_STICKNAME, Message.CREATOR_WINDMILL_STICKLORE, Message.CREATOR_WINDMILL_INIT);
    }

    @Override
    protected @NotNull AbstractDoor constructDoor()
    {
        return new Windmill(constructDoorData());
    }
}
