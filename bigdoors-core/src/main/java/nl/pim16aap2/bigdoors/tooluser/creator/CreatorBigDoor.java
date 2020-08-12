package nl.pim16aap2.bigdoors.tooluser.creator;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.BigDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeBigDoor;
import nl.pim16aap2.bigdoors.tooluser.Procedure;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CreatorBigDoor extends Creator
{
    @Getter(onMethod = @__({@Override}))
    @NotNull
    private static final DoorType doorType = DoorTypeBigDoor.get();

    public CreatorBigDoor(final @NotNull IPPlayer player, final @Nullable String name)
    {
        super(player);
        if (name != null)
            completeNamingStep(name);
        prepareCurrentStep();
    }

    public CreatorBigDoor(final @NotNull IPPlayer player)
    {
        this(player, null);
    }

    private List<IStep> generateSteps()
        throws InstantiationException
    {
        return Arrays.asList(factorySetName.construct(),
                             factorySetFirstPos.construct(),
                             factorySetSecondPos.construct(),
                             factorySetEnginePos.construct(),
                             factorySetPowerBlockPos.construct(),
                             factorySetOpenDir.construct(),
                             factoryConfirmPrice.construct(),
                             factoryCompleteProcess.construct());
    }

    @Override
    @NotNull
    protected Procedure<?> getProcedure()
        throws InstantiationException
    {
        return new Procedure<>(this, generateSteps());
    }

    @Override
    protected void giveTool()
    {
        giveTool(Message.CREATOR_GENERAL_STICKNAME, Message.CREATOR_BIGDOOR_STICKLORE, Message.CREATOR_BIGDOOR_INIT);
    }

    @Override
    @NotNull
    protected AbstractDoorBase constructDoor()
    {
        return new BigDoor(constructDoorData());
    }
}
