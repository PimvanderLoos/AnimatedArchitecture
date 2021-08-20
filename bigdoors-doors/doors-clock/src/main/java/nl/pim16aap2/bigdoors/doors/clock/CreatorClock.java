package nl.pim16aap2.bigdoors.doors.clock;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorPLocation;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreatorClock extends Creator
{
    private static final DoorType DOOR_TYPE = DoorTypeClock.get();

    protected @Nullable PBlockFace hourArmSide;

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

    public CreatorClock(IPPlayer player, @Nullable String name)
    {
        super(player, name);
    }

    public CreatorClock(IPPlayer player)
    {
        this(player, null);
    }

    @Override
    protected List<IStep> generateSteps()
        throws InstantiationException
    {
        Step stepSelectHourArm = new Step.Factory("SELECT_HOUR_ARM")
            .messageKey("creator.clock.step_3")
            .stepExecutor(new StepExecutorPLocation(this::completeSelectHourArmStep))
            .waitForUserInput(true).construct();

        return Arrays.asList(factorySetName.construct(),
                             factorySetFirstPos.messageKey("creator.clock.step_1").construct(),
                             factorySetSecondPos.messageKey("creator.clock.step_2").construct(),
                             stepSelectHourArm,
                             factorySetPowerBlockPos.construct(),
                             factoryConfirmPrice.construct(),
                             factoryCompleteProcess.messageKey("creator.clock.success").construct());
    }

    /**
     * Selects the side of the hour arm that will be the hour arm of the clock.
     *
     * @param loc The selected location.
     * @return True if step finished successfully.
     */
    protected boolean completeSelectHourArmStep(IPLocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        Util.requireNonNull(cuboid, "cuboid");
        if (northSouthAligned)
            hourArmSide = loc.getBlockZ() == cuboid.getMin().z() ? PBlockFace.NORTH :
                          loc.getBlockZ() == cuboid.getMax().z() ? PBlockFace.SOUTH : null;
        else
            hourArmSide = loc.getBlockX() == cuboid.getMin().x() ? PBlockFace.WEST :
                          loc.getBlockX() == cuboid.getMax().x() ? PBlockFace.EAST : null;

        return hourArmSide != null;
    }

    @Override
    protected boolean setSecondPos(IPLocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        Util.requireNonNull(firstPos, "firstPos");
        final Vector3Di cuboidDims = new Cuboid(firstPos, new Vector3Di(loc.getBlockX(), loc.getBlockY(),
                                                                        loc.getBlockZ())).getDimensions();

        // The clock has to be an odd number of blocks tall.
        if (cuboidDims.y() % 2 == 0)
        {
            BigDoors.get().getPLogger()
                    .debug("ClockCreator: " + getPlayer().asString() +
                               ": The height of the selected area for the clock is not odd!");
            return false;
        }

        if (cuboidDims.x() % 2 == 0)
        {
            // It has to be a square.
            if (cuboidDims.y() != cuboidDims.z())
            {
                BigDoors.get().getPLogger().debug("ClockCreator: " + getPlayer().asString() +
                                                      ": The selected Clock area is not square! The x-axis is valid.");
                return false;
            }
            northSouthAligned = false;
        }
        else if (cuboidDims.z() % 2 == 0)
        {
            // It has to be a square.
            if (cuboidDims.y() != cuboidDims.x())
            {
                BigDoors.get().getPLogger().debug("ClockCreator: " + getPlayer().asString() +
                                                      ": The selected Clock area is not square! The z-axis is valid.");
                return false;
            }
            northSouthAligned = true;
        }
        else
        {
            BigDoors.get().getPLogger()
                    .debug("ClockCreator: " + getPlayer().asString() + ": Selected Clock area is not valid!");
            return false;
        }

        return super.setSecondPos(loc);
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
        giveTool("tool_user.base.stick_name", "creator.clock.stick_lore", "creator.clock.init");
    }

    /**
     * Calculates the position of the engine. This should be called at the end of the process, as not all variables may
     * be set at an earlier stage.
     */
    protected void setEngine()
    {
        if (cuboid == null)
            return;
        engine = cuboid.getCenterBlock();
    }

    /**
     * Calculates the open direction from the current physical aspects of this clock.
     */
    protected void setOpenDirection()
    {
        if (northSouthAligned)
            openDir = hourArmSide == PBlockFace.NORTH ? RotateDirection.WEST : RotateDirection.EAST;
        else
            openDir = hourArmSide == PBlockFace.EAST ? RotateDirection.NORTH : RotateDirection.SOUTH;
    }

    @Override
    protected AbstractDoor constructDoor()
    {
        setEngine();
        setOpenDirection();
        Util.requireNonNull(hourArmSide, "hourArmSide");
        return new Clock(constructDoorData(), northSouthAligned, hourArmSide);
    }

    @Override
    protected DoorType getDoorType()
    {
        return DOOR_TYPE;
    }
}
