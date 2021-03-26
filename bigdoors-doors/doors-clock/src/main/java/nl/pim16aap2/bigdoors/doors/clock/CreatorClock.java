package nl.pim16aap2.bigdoors.doors.clock;

import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorPLocation;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreatorClock extends Creator
{
    @Getter(onMethod = @__({@Override}))
    @NotNull
    private final DoorType doorType = DoorTypeClock.get();

    protected PBlockFace hourArmSide;

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

    public CreatorClock(final @NotNull IPPlayer player, final @Nullable String name)
    {
        super(player);
        if (name != null)
            completeNamingStep(name);
        prepareCurrentStep();
    }

    public CreatorClock(final @NotNull IPPlayer player)
    {
        this(player, null);
    }

    @Override
    protected @NotNull
    List<IStep> generateSteps()
        throws InstantiationException
    {
        Step<CreatorClock> stepSelectHourArm = new Step.Factory<CreatorClock>("SELECT_HOUR_ARM")
            .message(Message.CREATOR_CLOCK_SELECTHOURARMSIDE)
            .stepExecutor(new StepExecutorPLocation(this::completeSelectHourArmStep))
            .waitForUserInput(true).construct();

        return Arrays.asList(factorySetName.message(Message.CREATOR_GENERAL_GIVENAME).construct(),
                             factorySetFirstPos.message(Message.CREATOR_CLOCK_STEP1).construct(),
                             factorySetSecondPos.message(Message.CREATOR_CLOCK_STEP2).construct(),
                             stepSelectHourArm,
                             factorySetPowerBlockPos.message(Message.CREATOR_GENERAL_SETPOWERBLOCK).construct(),
                             factoryConfirmPrice.message(Message.CREATOR_GENERAL_CONFIRMPRICE).construct(),
                             factoryCompleteProcess.message(Message.CREATOR_CLOCK_SUCCESS).construct());
    }

    /**
     * Selects the side of the hour arm that will be the hour arm of the clock.
     *
     * @param loc The selected location.
     * @return True if step finished successfully.
     */
    protected boolean completeSelectHourArmStep(final @NotNull IPLocationConst loc)
    {
        if (!verifyWorldMatch(loc))
            return false;

        if (northSouthAligned)
            hourArmSide = loc.getBlockZ() == cuboid.getMin().getZ() ? PBlockFace.NORTH :
                          loc.getBlockZ() == cuboid.getMax().getZ() ? PBlockFace.SOUTH : null;
        else
            hourArmSide = loc.getBlockX() == cuboid.getMin().getX() ? PBlockFace.WEST :
                          loc.getBlockX() == cuboid.getMax().getX() ? PBlockFace.EAST : null;

        boolean result = hourArmSide != null;
        if (result)
            procedure.goToNextStep();
        return result;
    }

    @Override
    protected boolean setSecondPos(final @NotNull IPLocationConst loc)
    {
        if (!verifyWorldMatch(loc))
            return false;

        final @NotNull Vector3DiConst cuboidDims = new Cuboid(new Vector3Di(firstPos),
                                                              new Vector3Di(loc.getBlockX(), loc.getBlockY(),
                                                                            loc.getBlockZ())).getDimensions();

        // The clock has to be an odd number of blocks tall.
        if (cuboidDims.getY() % 2 == 0)
        {
            BigDoors.get().getPLogger()
                    .debug("ClockCreator: " + player.asString() +
                               ": The height of the selected area for the clock is not odd!");
            return false;
        }

        if (cuboidDims.getX() % 2 == 0)
        {
            // It has to be a square.
            if (cuboidDims.getY() != cuboidDims.getZ())
            {
                BigDoors.get().getPLogger().debug("ClockCreator: " + player.asString() +
                                                      ": The selected Clock area is not square! The x-axis is valid.");
                return false;
            }
            northSouthAligned = false;
        }
        else if (cuboidDims.getZ() % 2 == 0)
        {
            // It has to be a square.
            if (cuboidDims.getY() != cuboidDims.getX())
            {
                BigDoors.get().getPLogger().debug("ClockCreator: " + player.asString() +
                                                      ": The selected Clock area is not square! The z-axis is valid.");
                return false;
            }
            northSouthAligned = true;
        }
        else
        {
            BigDoors.get().getPLogger()
                    .debug("ClockCreator: " + player.asString() + ": Selected Clock area is not valid!");
            return false;
        }

        return super.setSecondPos(loc);
    }

    @Override
    protected @NotNull
    List<RotateDirection> getValidOpenDirections()
    {
        if (isOpen)
            return getDoorType().getValidOpenDirections();
        // When the garage door is not open (i.e. vertical), it can only be opened along one axis.
        return northSouthAligned ? northSouthAxisOpenDirs : eastWestAxisOpenDirs;
    }

    @Override
    protected void giveTool()
    {
        giveTool(Message.CREATOR_GENERAL_STICKNAME, Message.CREATOR_CLOCK_STICKLORE,
                 Message.CREATOR_CLOCK_INIT);
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
            opendir = hourArmSide == PBlockFace.NORTH ? RotateDirection.WEST : RotateDirection.EAST;
        else
            opendir = hourArmSide == PBlockFace.EAST ? RotateDirection.NORTH : RotateDirection.SOUTH;
    }

    @Override
    protected @NotNull
    AbstractDoorBase constructDoor()
    {
        setEngine();
        setOpenDirection();
        return new Clock(constructDoorData(), northSouthAligned, hourArmSide);
    }
}
