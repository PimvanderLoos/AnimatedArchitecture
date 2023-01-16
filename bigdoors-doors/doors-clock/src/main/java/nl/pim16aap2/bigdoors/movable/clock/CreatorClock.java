package nl.pim16aap2.bigdoors.movable.clock;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

@Flogger
public class CreatorClock extends Creator
{
    private static final MovableType MOVABLE_TYPE = MovableTypeClock.get();

    protected @Nullable PBlockFace hourArmSide;

    /**
     * The valid open directions when the movable is positioned along the north/south axis.
     */
    private static final Set<RotateDirection> NORTH_SOUTH_AXIS_OPEN_DIRS =
        EnumSet.of(RotateDirection.EAST, RotateDirection.WEST);

    /**
     * The valid open directions when the movable is positioned along the east/west axis.
     */
    private static final Set<RotateDirection> EAST_WEST_AXIS_OPEN_DIRS =
        EnumSet.of(RotateDirection.NORTH, RotateDirection.SOUTH);

    private boolean northSouthAligned;

    public CreatorClock(Creator.Context context, IPPlayer player, @Nullable String name)
    {
        super(context, player, name);
    }

    @SuppressWarnings("unused")
    public CreatorClock(Creator.Context context, IPPlayer player)
    {
        this(context, player, null);
    }

    @Override
    protected List<IStep> generateSteps()
        throws InstantiationException
    {
        final Step stepSelectHourArm = stepFactory
            .stepName("SELECT_HOUR_ARM")
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
     * @param loc
     *     The selected location.
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
            log.at(Level.FINE).log("ClockCreator: %s: The height of the selected clock area (%d) is not an odd value!",
                                   getPlayer(), cuboidDims.y());
            return false;
        }

        if (cuboidDims.x() % 2 == 0)
        {
            // It has to be a square.
            if (cuboidDims.y() != cuboidDims.z())
            {
                log.at(Level.FINE)
                   .log("ClockCreator: %s: The selected Clock area (%s) is not square! The x-axis is valid.",
                        getPlayer(), cuboidDims);
                return false;
            }
            northSouthAligned = false;
        }
        else if (cuboidDims.z() % 2 == 0)
        {
            // It has to be a square.
            if (cuboidDims.y() != cuboidDims.x())
            {
                log.at(Level.FINE)
                   .log("ClockCreator: %s: The selected Clock area (%s) is not square! The z-axis is valid.",
                        getPlayer(), cuboidDims);
                return false;
            }
            northSouthAligned = true;
        }
        else
        {
            log.at(Level.FINE)
               .log("ClockCreator: %s: The selected Clock area (%s) is not valid!", getPlayer(), cuboidDims);
            return false;
        }

        return super.setSecondPos(loc);
    }

    @Override
    public Set<RotateDirection> getValidOpenDirections()
    {
        if (isOpen)
            return getMovableType().getValidOpenDirections();
        // When the garage movable is not open (i.e. vertical), it can only be opened along one axis.
        return northSouthAligned ? NORTH_SOUTH_AXIS_OPEN_DIRS : EAST_WEST_AXIS_OPEN_DIRS;
    }

    @Override
    protected void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.clock.stick_lore", "creator.clock.init");
    }

    /**
     * Calculates the position of the rotation point. This should be called at the end of the process, as not all
     * variables may be set at an earlier stage.
     */
    protected void setRotationPoint()
    {
        if (cuboid == null)
            return;
        rotationPoint = cuboid.getCenterBlock();
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
    protected AbstractMovable constructMovable()
    {
        setRotationPoint();
        setOpenDirection();
        Util.requireNonNull(hourArmSide, "hourArmSide");
        return new Clock(constructMovableData(), northSouthAligned, hourArmSide);
    }

    @Override
    protected MovableType getMovableType()
    {
        return MOVABLE_TYPE;
    }
}
