package nl.pim16aap2.bigdoors.doors.flag;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CreatorFlag extends Creator
{
    @Getter(onMethod = @__({@Override}))
    @NotNull
    private final DoorType doorType = DoorTypeFlag.get();
    protected boolean northSouthAligned;

    public CreatorFlag(final @NotNull IPPlayer player, final @Nullable String name)
    {
        super(player);
        if (name != null)
            completeNamingStep(name);
        prepareCurrentStep();
    }

    public CreatorFlag(final @NotNull IPPlayer player)
    {
        this(player, null);
    }

    @Override
    protected @NotNull List<IStep> generateSteps()
        throws InstantiationException
    {
        return Arrays.asList(factorySetName.message(Message.CREATOR_GENERAL_GIVENAME).construct(),
                             factorySetFirstPos.message(Message.CREATOR_FLAG_STEP1).construct(),
                             factorySetSecondPos.message(Message.CREATOR_FLAG_STEP2).construct(),
                             factorySetEnginePos.message(Message.CREATOR_FLAG_STEP3).construct(),
                             factorySetPowerBlockPos.message(Message.CREATOR_GENERAL_SETPOWERBLOCK).construct(),
                             factoryConfirmPrice.message(Message.CREATOR_GENERAL_CONFIRMPRICE).construct(),
                             factoryCompleteProcess.message(Message.CREATOR_FLAG_SUCCESS).construct());
    }

    @Override
    protected void giveTool()
    {
        giveTool(Message.CREATOR_GENERAL_STICKNAME, Message.CREATOR_FLAG_STICKLORE, Message.CREATOR_FLAG_INIT);
    }

    @Override
    protected boolean setSecondPos(final @NotNull IPLocationConst loc)
    {
        if (!verifyWorldMatch(loc))
            return false;

        final @NotNull Vector3DiConst cuboidDims = new Cuboid(new Vector3Di(firstPos),
                                                              new Vector3Di(loc.getBlockX(), loc.getBlockY(),
                                                                            loc.getBlockZ())).getDimensions();

        // Flags must have a dimension of 1 along either the x or z axis, as it's a `2d` shape.
        if ((cuboidDims.getX() == 1) ^ (cuboidDims.getZ() == 1))
        {
            northSouthAligned = cuboidDims.getX() == 1;
            return super.setSecondPos(loc);
        }

        player.sendMessage(messages.getString(Message.CREATOR_GENERAL_2NDPOSNOT2D));
        return false;
    }

    @Override
    protected boolean completeSetEngineStep(final @NotNull IPLocationConst loc)
    {
        // For flags, the rotation point has to be a corner of the total area.
        // It doesn't make sense to have it in the middle or something; that's now how flags work.
        if ((loc.getBlockX() == cuboid.getMin().getX() || loc.getBlockX() == cuboid.getMax().getX()) &&
            (loc.getBlockZ() == cuboid.getMin().getZ() || loc.getBlockZ() == cuboid.getMax().getZ()))
            return super.completeSetEngineStep(loc);

        player.sendMessage(messages.getString(Message.CREATOR_GENERAL_POINTNOTACORNER));
        return false;
    }

    @Override
    protected @NotNull AbstractDoorBase constructDoor()
    {
        if (northSouthAligned)
            opendir = engine.getZ() == cuboid.getMin().getZ() ? RotateDirection.SOUTH : RotateDirection.NORTH;
        else
            opendir = engine.getX() == cuboid.getMin().getX() ? RotateDirection.EAST : RotateDirection.WEST;

        return new Flag(constructDoorData(), northSouthAligned);
    }
}
