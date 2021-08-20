package nl.pim16aap2.bigdoors.doors.garagedoor;

import nl.pim16aap2.bigdoors.BigDoors;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreatorGarageDoor extends Creator
{
    private static final DoorType DOOR_TYPE = DoorTypeGarageDoor.get();

    /**
     * The valid open directions when the door is positioned along the north/south axis.
     */
    private static final List<RotateDirection> northSouthAxisOpenDirs = new ArrayList<>(
        Arrays.asList(RotateDirection.EAST, RotateDirection.WEST));

    /**
     * The valid open directions when the door is positioned along the east/west axis.
     */
    private static final List<RotateDirection> eastWestAxisOpenDirs = new ArrayList<>(
        Arrays.asList(RotateDirection.NORTH, RotateDirection.SOUTH));

    private boolean northSouthAligned;

    public CreatorGarageDoor(IPPlayer player, @Nullable String name)
    {
        super(player, name);
    }

    public CreatorGarageDoor(IPPlayer player)
    {
        this(player, null);
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

        getPlayer().sendMessage(BigDoors.get().getLocalizer().getMessage("creator.base.second_pos_not_2d"));
        return false;
    }

    @Override
    protected List<RotateDirection> getValidOpenDirections()
    {
        if (isOpen)
            return getDoorType().getValidOpenDirections();
        // When the garage door is not open (i.e. vertical), it can only be opened along one axis.
        return northSouthAligned ? northSouthAxisOpenDirs : eastWestAxisOpenDirs;
    }

    @Override
    protected void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.garage_door.stick_lore", "creator.garage_door.init");
    }

    @Override
    protected boolean completeSetOpenDirStep(String str)
    {
        if (super.completeSetOpenDirStep(str))
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
        final int moveDistance = northSouthAligned ? cuboid.getDimensions().x() : cuboid.getDimensions().z();
        final int engineY = cuboid.getMin().y() - moveDistance - 1;
        final Vector3Di engineTmp = cuboid.getCenterBlock();

        int newX = engineTmp.x();
        int newZ = engineTmp.z();

        if (openDir == RotateDirection.NORTH)
            newZ = cuboid.getMax().z() + 1;
        else if (openDir == RotateDirection.EAST)
            newX = cuboid.getMin().x() - 1;
        else if (openDir == RotateDirection.SOUTH)
            newZ = cuboid.getMin().z() - 1;
        else if (openDir == RotateDirection.WEST)
            newX = cuboid.getMax().x() + 1;

        engine = new Vector3Di(newX, engineY, newZ);
    }

    @Override
    protected AbstractDoor constructDoor()
    {
        setEngine();
        return new GarageDoor(constructDoorData(), northSouthAligned);
    }

    @Override
    protected DoorType getDoorType()
    {
        return DOOR_TYPE;
    }
}
