package nl.pim16aap2.bigdoors.structures.clock;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.ILocation;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.StructureType;
import nl.pim16aap2.bigdoors.core.tooluser.Step;
import nl.pim16aap2.bigdoors.core.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.core.tooluser.stepexecutor.StepExecutorLocation;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.PBlockFace;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Flogger
public class CreatorClock extends Creator
{
    private static final StructureType STRUCTURE_TYPE = StructureTypeClock.get();

    protected @Nullable PBlockFace hourArmSide;

    /**
     * The valid open directions when the structure is positioned along the north/south axis.
     */
    private static final Set<MovementDirection> NORTH_SOUTH_AXIS_OPEN_DIRS =
        EnumSet.of(MovementDirection.EAST, MovementDirection.WEST);

    /**
     * The valid open directions when the structure is positioned along the east/west axis.
     */
    private static final Set<MovementDirection> EAST_WEST_AXIS_OPEN_DIRS =
        EnumSet.of(MovementDirection.NORTH, MovementDirection.SOUTH);

    private boolean northSouthAligned;

    public CreatorClock(Creator.Context context, IPlayer player, @Nullable String name)
    {
        super(context, player, name);
    }

    @SuppressWarnings("unused")
    public CreatorClock(Creator.Context context, IPlayer player)
    {
        this(context, player, null);
    }

    @Override
    protected List<Step> generateSteps()
        throws InstantiationException
    {
        final Step stepSelectHourArm = stepFactory
            .stepName("SELECT_HOUR_ARM")
            .messageKey("creator.clock.step_3")
            .stepExecutor(new StepExecutorLocation(this::completeSelectHourArmStep))
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
    protected boolean completeSelectHourArmStep(ILocation loc)
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
    protected boolean setSecondPos(ILocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        Util.requireNonNull(firstPos, "firstPos");
        final Vector3Di cuboidDims = new Cuboid(firstPos, new Vector3Di(loc.getBlockX(), loc.getBlockY(),
                                                                        loc.getBlockZ())).getDimensions();

        // The clock has to be an odd number of blocks tall.
        if (cuboidDims.y() % 2 == 0)
        {
            log.atFine().log("ClockCreator: %s: The height of the selected clock area (%d) is not an odd value!",
                             getPlayer(), cuboidDims.y());
            return false;
        }

        if (cuboidDims.x() % 2 == 0)
        {
            // It has to be a square.
            if (cuboidDims.y() != cuboidDims.z())
            {
                log.atFine()
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
                log.atFine()
                   .log("ClockCreator: %s: The selected Clock area (%s) is not square! The z-axis is valid.",
                        getPlayer(), cuboidDims);
                return false;
            }
            northSouthAligned = true;
        }
        else
        {
            log.atFine()
               .log("ClockCreator: %s: The selected Clock area (%s) is not valid!", getPlayer(), cuboidDims);
            return false;
        }

        return super.setSecondPos(loc);
    }

    @Override
    public Set<MovementDirection> getValidOpenDirections()
    {
        if (isOpen)
            return getStructureType().getValidOpenDirections();
        // When the garage structure is not open (i.e. vertical), it can only be opened along one axis.
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
            openDir = hourArmSide == PBlockFace.NORTH ? MovementDirection.WEST : MovementDirection.EAST;
        else
            openDir = hourArmSide == PBlockFace.EAST ? MovementDirection.NORTH : MovementDirection.SOUTH;
    }

    @Override
    protected AbstractStructure constructStructure()
    {
        setRotationPoint();
        setOpenDirection();
        Util.requireNonNull(hourArmSide, "hourArmSide");
        return new Clock(constructStructureData(), northSouthAligned, hourArmSide);
    }

    @Override
    protected StructureType getStructureType()
    {
        return STRUCTURE_TYPE;
    }
}
