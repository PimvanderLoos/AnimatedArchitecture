package nl.pim16aap2.bigdoors.doors.garagedoor;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class CreatorGarageDoor extends Creator
{
    private static final DoorType DOOR_TYPE = DoorTypeGarageDoor.get();

    /**
     * The valid open directions when the door is positioned along the north/south axis.
     */
    private static final Set<RotateDirection> NORTH_SOUTH_AXIS_OPEN_DIRS =
        EnumSet.of(RotateDirection.EAST, RotateDirection.WEST);

    /**
     * The valid open directions when the door is positioned along the east/west axis.
     */
    private static final Set<RotateDirection> EAST_WEST_AXIS_OPEN_DIRS =
        EnumSet.of(RotateDirection.NORTH, RotateDirection.SOUTH);

    private boolean northSouthAligned;

    public CreatorGarageDoor(Creator.Context context, IPPlayer player, @Nullable String name)
    {
        super(context, player, name);
    }

    public CreatorGarageDoor(Creator.Context context, IPPlayer player)
    {
        this(context, player, null);
    }

    @Override
    protected List<IStep> generateSteps()
        throws InstantiationException
    {
        return Arrays.asList(factorySetName.construct(),
                             factorySetFirstPos.messageKey("creator.garage_door.step_1").construct(),
                             factorySetSecondPos.messageKey("creator.garage_door.step_2").construct(),
                             factorySetPowerBlockPos.construct(),
                             factorySetOpenDir.construct(),
                             factoryConfirmPrice.construct(),
                             factoryCompleteProcess.messageKey("creator.garage_door.success").construct());
    }

    @Override
    protected boolean setSecondPos(IPLocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        Util.requireNonNull(firstPos, "firstPos");
        final Vector3Di cuboidDims = new Cuboid(firstPos, new Vector3Di(loc.getBlockX(), loc.getBlockY(),
                                                                        loc.getBlockZ())).getDimensions();

        // Check if there's exactly 1 dimension that is 1 block deep.
        if ((cuboidDims.x() == 1) ^ (cuboidDims.y() == 1) ^ (cuboidDims.z() == 1))
        {
            northSouthAligned = cuboidDims.x() == 1;
            isOpen = cuboidDims.y() == 1;
            return super.setSecondPos(loc);
        }

        getPlayer().sendMessage(localizer.getMessage("creator.base.second_pos_not_2d"));
        return false;
    }

    @Override
    public Set<RotateDirection> getValidOpenDirections()
    {
        if (isOpen)
            return getDoorType().getValidOpenDirections();
        // When the garage door is not open (i.e. vertical), it can only be opened along one axis.
        return northSouthAligned ? NORTH_SOUTH_AXIS_OPEN_DIRS : EAST_WEST_AXIS_OPEN_DIRS;
    }

    @Override
    protected void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.garage_door.stick_lore", "creator.garage_door.init");
    }

    @Override
    protected boolean completeSetOpenDirStep(RotateDirection direction)
    {
        if (super.completeSetOpenDirStep(direction))
        {
            // This may seem counter-intuitive, but if it's positioned along the north/south axis,
            // then it can only open in east/west direction, because there isn't any space in the other
            // directions.
            if (openDir == RotateDirection.NORTH || openDir == RotateDirection.SOUTH)
                northSouthAligned = false;
            return true;
        }
        return false;
    }

    /**
     * Calculates the position of the rotation point. This should be called at the end of the process, as not all
     * variables may be set at an earlier stage.
     */
    protected void setRotationPoint()
    {
        if (cuboid == null)
            return;

        if (!isOpen)
        {
            rotationPoint = cuboid.getMin();
            return;
        }

        // The rotation point should be located at the bottom of the garage door.
        // An additional 1 is subtracted because garage doors in the 'up' position
        // are 1 block above the highest point.
        final int moveDistance = northSouthAligned ? cuboid.getDimensions().x() : cuboid.getDimensions().z();
        final int rotationPointY = cuboid.getMin().y() - moveDistance - 1;
        final Vector3Di rotationPointTmp = cuboid.getCenterBlock();

        int newX = rotationPointTmp.x();
        int newZ = rotationPointTmp.z();

        if (openDir == RotateDirection.NORTH)
            newZ = cuboid.getMax().z() + 1;
        else if (openDir == RotateDirection.EAST)
            newX = cuboid.getMin().x() - 1;
        else if (openDir == RotateDirection.SOUTH)
            newZ = cuboid.getMin().z() - 1;
        else if (openDir == RotateDirection.WEST)
            newX = cuboid.getMax().x() + 1;

        rotationPoint = new Vector3Di(newX, rotationPointY, newZ);
    }

    @Override
    protected AbstractDoor constructDoor()
    {
        setRotationPoint();
        return new GarageDoor(constructDoorData(), northSouthAligned);
    }

    @Override
    protected DoorType getDoorType()
    {
        return DOOR_TYPE;
    }
}
