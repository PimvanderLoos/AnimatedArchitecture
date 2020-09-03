package nl.pim16aap2.bigdoors.doors.garagedoor;

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreatorGarageDoor extends Creator
{
    @Getter(onMethod = @__({@Override}))
    @NotNull
    private final DoorType doorType = DoorTypeGarageDoor.get();

    /**
     * The valid open directions when the door is positioned along the north/south axis.
     */
    @NotNull
    private static final List<RotateDirection> northSouthAxisOpenDirs = new ArrayList<>(
        Arrays.asList(RotateDirection.EAST, RotateDirection.WEST));

    /**
     * The valid open directions when the door is positioned along the east/west axis.
     */
    @NotNull
    private static final List<RotateDirection> eastWestAxisOpenDirs = new ArrayList<>(
        Arrays.asList(RotateDirection.NORTH, RotateDirection.SOUTH));

    private boolean northSouthAligned;

    public CreatorGarageDoor(final @NotNull IPPlayer player, final @Nullable String name)
    {
        super(player);
        if (name != null)
            completeNamingStep(name);
        prepareCurrentStep();
    }

    public CreatorGarageDoor(final @NotNull IPPlayer player)
    {
        this(player, null);
    }

    @Override
    protected @NotNull List<IStep> generateSteps()
        throws InstantiationException
    {
        return Arrays.asList(factorySetName.message(Message.CREATOR_GENERAL_GIVENAME).construct(),
                             factorySetFirstPos.message(Message.CREATOR_GARAGEDOOR_STEP1).construct(),
                             factorySetSecondPos.message(Message.CREATOR_GARAGEDOOR_STEP2).construct(),
                             factorySetPowerBlockPos.message(Message.CREATOR_GENERAL_SETPOWERBLOCK).construct(),
                             factorySetOpenDir.message(Message.CREATOR_GENERAL_SETOPENDIR).construct(),
                             factoryConfirmPrice.message(Message.CREATOR_GENERAL_CONFIRMPRICE).construct(),
                             factoryCompleteProcess.message(Message.CREATOR_GARAGEDOOR_SUCCESS).construct());
    }

    @Override
    protected boolean setSecondPos(final @NotNull IPLocationConst loc)
    {
        if (!verifyWorldMatch(loc))
            return false;

        final Vector3Di cuboidDims = new Cuboid(new Vector3Di(firstPos),
                                                new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()))
            .getDimensions();

        // Check if there's exactly 1 dimension that is 1 block deep.
        if ((cuboidDims.getX() == 1) ^ (cuboidDims.getY() == 1) ^ (cuboidDims.getZ() == 1))
        {
            northSouthAligned = cuboidDims.getX() == 1;
            isOpen = cuboidDims.getY() == 1;
            return super.setSecondPos(loc);
        }

        player.sendMessage(messages.getString(Message.CREATOR_GENERAL_2NDPOSNOT2D));
        return false;
    }

    @Override
    protected @NotNull List<RotateDirection> getValidOpenDirections()
    {
        if (isOpen)
            return getDoorType().getValidOpenDirections();
        // When the garage door is not open (i.e. vertical), it can only be opened along one axis.
        return northSouthAligned ? northSouthAxisOpenDirs : eastWestAxisOpenDirs;
    }

    @Override
    protected void giveTool()
    {
        giveTool(Message.CREATOR_GENERAL_STICKNAME, Message.CREATOR_GARAGEDOOR_STICKLORE,
                 Message.CREATOR_GARAGEDOOR_INIT);
    }

    @Override
    protected boolean completeSetOpenDirStep(final @NotNull String str)
    {
        if (super.completeSetOpenDirStep(str))
        {
            // This may seem counter-intuitive, but if it's positioned along the north/south axis,
            // then it can only open in east/west direction, because there isn't any space in the other
            // directions.
            if (opendir == RotateDirection.NORTH || opendir == RotateDirection.SOUTH)
                northSouthAligned = false;
            return true;
        }
        return false;
    }

    /**
     * Calculates the position of the engine. This should be called at the end of the process, as not all variables may
     * be set at an earlier stage.
     */
    protected void setEngine()
    {
        if (cuboid == null)
            return;

        if (!isOpen)
        {
            engine = cuboid.getMin();
            return;
        }

        // The engine should be located at the bottom of the garage door.
        // An additional 1 is subtracted because garage doors in the 'up' position
        // are 1 block above the highest point.
        final int moveDistance = northSouthAligned ? cuboid.getDimensions().getX() : cuboid.getDimensions().getZ();
        final int engineY = cuboid.getMin().getY() - moveDistance - 1;
        final @NotNull Vector3Di engineTmp = cuboid.getCenterBlock();
        engineTmp.setY(engineY);

        if (opendir == RotateDirection.NORTH)
            engineTmp.setZ(cuboid.getMax().getZ() + 1);
        else if (opendir == RotateDirection.EAST)
            engineTmp.setX(cuboid.getMin().getX() - 1);
        else if (opendir == RotateDirection.SOUTH)
            engineTmp.setZ(cuboid.getMin().getZ() - 1);
        else if (opendir == RotateDirection.WEST)
            engineTmp.setX(cuboid.getMax().getX() + 1);
        engine = engineTmp;
    }

    @Override
    protected @NotNull AbstractDoorBase constructDoor()
    {
        setEngine();
        return new GarageDoor(constructDoorData(), northSouthAligned);
    }
}
