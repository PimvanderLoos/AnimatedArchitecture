package nl.pim16aap2.animatedarchitecture.structures.clock;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Step;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorLocation;
import nl.pim16aap2.animatedarchitecture.core.util.BlockFace;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Flogger
public class CreatorClock extends Creator
{
    private static final StructureType STRUCTURE_TYPE = StructureTypeClock.get();

    protected @Nullable BlockFace hourArmSide;

    /**
     * The valid open directions when the structure is positioned along the north/south axis.
     */
    private static final Set<MovementDirection> NORTH_SOUTH_AXIS_OPEN_DIRS =
        EnumSet.of(MovementDirection.NORTH, MovementDirection.SOUTH);

    /**
     * The valid open directions when the structure is positioned along the east/west axis.
     */
    private static final Set<MovementDirection> EAST_WEST_AXIS_OPEN_DIRS =
        EnumSet.of(MovementDirection.EAST, MovementDirection.WEST);

    private boolean northSouthAligned;

    public CreatorClock(Creator.Context context, IPlayer player, @Nullable String name)
    {
        super(context, player, name);
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
                             factoryReviewResult.construct(),
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
            hourArmSide = loc.getBlockX() == cuboid.getMin().x() ? BlockFace.WEST :
                          loc.getBlockX() == cuboid.getMax().x() ? BlockFace.EAST : null;
        else
            hourArmSide = loc.getBlockZ() == cuboid.getMin().z() ? BlockFace.NORTH :
                          loc.getBlockZ() == cuboid.getMax().z() ? BlockFace.SOUTH : null;

        if (hourArmSide == null)
            getPlayer().sendError(textFactory, localizer.getMessage("creator.clock.error.invalid_hour_arm_side"));

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

        final int height = cuboidDims.y();
        final int maxHorizontalDim = Math.max(cuboidDims.x(), cuboidDims.z());
        if (height < 3 || maxHorizontalDim < 3)
        {
            getPlayer().sendMessage(textFactory.newText().append(
                localizer.getMessage("creator.clock.error.too_small"), TextType.ERROR,
                arg -> arg.highlight(maxHorizontalDim),
                arg -> arg.highlight(height)));
            return false;
        }

        if (height != maxHorizontalDim)
        {
            getPlayer().sendMessage(textFactory.newText().append(
                localizer.getMessage("creator.clock.error.not_square"), TextType.ERROR,
                arg -> arg.highlight(maxHorizontalDim),
                arg -> arg.highlight(height)));
            return false;
        }

        // The clock has to be an odd number of blocks tall.
        if (height % 2 == 0)
        {
            getPlayer().sendMessage(textFactory.newText().append(
                localizer.getMessage("creator.clock.error.not_odd"), TextType.ERROR,
                arg -> arg.highlight(maxHorizontalDim),
                arg -> arg.highlight(height)));
            return false;
        }

        final int depth = Math.min(cuboidDims.x(), cuboidDims.z());
        if (depth != 2)
        {
            getPlayer().sendMessage(textFactory.newText().append(
                localizer.getMessage("creator.clock.error.not_2_deep"), TextType.ERROR,
                arg -> arg.highlight(depth)));
            return false;
        }

        northSouthAligned = cuboidDims.x() == 2;
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
            openDir = hourArmSide == BlockFace.NORTH ? MovementDirection.WEST : MovementDirection.EAST;
        else
            openDir = hourArmSide == BlockFace.EAST ? MovementDirection.NORTH : MovementDirection.SOUTH;
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
