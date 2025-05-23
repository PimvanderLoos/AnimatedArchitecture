package nl.pim16aap2.animatedarchitecture.structures.clock;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Step;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
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
import java.util.concurrent.CompletableFuture;

@Flogger
@ToString(callSuper = true)
public class CreatorClock extends Creator
{
    private static final StructureType STRUCTURE_TYPE = StructureTypeClock.get();

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

    @GuardedBy("this")
    private @Nullable BlockFace hourArmSide;

    /**
     * Whether the structure is aligned along the north/south axis.
     */
    @GuardedBy("this")
    private boolean northSouthAligned;

    protected CreatorClock(
        ToolUser.Context context,
        StructureType structureType,
        IPlayer player,
        @Nullable String name)
    {
        super(context, structureType, player, name);
        init();
    }

    public CreatorClock(ToolUser.Context context, IPlayer player, @Nullable String name)
    {
        this(context, STRUCTURE_TYPE, player, name);
    }

    @Override
    protected synchronized List<Step> generateSteps()
        throws InstantiationException
    {
        final Step stepSelectHourArm = stepFactory
            .stepName(localizer, "SELECT_HOUR_ARM")
            .textSupplier(text -> text.append(
                localizer.getMessage("creator.clock.step_3"),
                TextType.INFO,
                getStructureArg()))
            .stepExecutor(new StepExecutorLocation(this::completeSelectHourArmStep))
            .waitForUserInput(true).construct();

        return Arrays.asList(
            factoryProvideName.construct(),
            factoryProvideFirstPos
                .textSupplier(text -> text.append(
                    localizer.getMessage("creator.clock.step_1"),
                    TextType.INFO,
                    getStructureArg()))
                .construct(),
            factoryProvideSecondPos
                .textSupplier(text -> text.append(
                    localizer.getMessage("creator.clock.step_2"),
                    TextType.INFO,
                    getStructureArg()))
                .construct(),
            stepSelectHourArm,
            factoryProvidePowerBlockPos.construct(),
            factoryReviewResult.construct(),
            factoryConfirmPrice.construct(),
            factoryCompleteProcess.construct());
    }

    /**
     * Selects the side of the hour arm that will be the hour arm of the clock.
     *
     * @param loc
     *     The selected location.
     * @return True if step finished successfully.
     */
    protected synchronized boolean completeSelectHourArmStep(ILocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        final Cuboid cuboid = Util.requireNonNull(getCuboid(), "cuboid");
        if (northSouthAligned)
            hourArmSide =
                loc.getBlockX() == cuboid.getMin().x() ? BlockFace.WEST :
                    loc.getBlockX() == cuboid.getMax().x() ? BlockFace.EAST :
                        null;
        else
            hourArmSide =
                loc.getBlockZ() == cuboid.getMin().z() ? BlockFace.NORTH :
                    loc.getBlockZ() == cuboid.getMax().z() ? BlockFace.SOUTH :
                        null;

        if (hourArmSide == null)
            getPlayer().sendError("creator.clock.error.invalid_hour_arm_side");
        else
            setProperty(Property.ROTATION_POINT, calculateRotationPoint(northSouthAligned, hourArmSide, cuboid));

        return hourArmSide != null;
    }

    private static Vector3Di calculateRotationPoint(
        boolean northSouthAligned,
        BlockFace hourArmSide,
        Cuboid cuboid)
    {
        final Vector3Di center = cuboid.getCenterBlock();

        final int x;
        final int y = center.y();
        final int z;

        if (northSouthAligned)
        {
            z = center.z();
            if (hourArmSide == BlockFace.WEST)
                x = cuboid.getMin().x();
            else
                x = cuboid.getMax().x();
        }
        else
        {
            x = center.x();
            if (hourArmSide == BlockFace.NORTH)
                z = cuboid.getMin().z();
            else
                z = cuboid.getMax().z();
        }
        return new Vector3Di(x, y, z);
    }

    @Override
    protected synchronized CompletableFuture<Boolean> provideSecondPos(ILocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return CompletableFuture.completedFuture(false);

        final Vector3Di firstPos = Util.requireNonNull(getFirstPos(), "firstPos");
        final Vector3Di cuboidDims = new Cuboid(firstPos, loc.getPosition()).getDimensions();

        final int height = cuboidDims.y();
        final int maxHorizontalDim = Math.max(cuboidDims.x(), cuboidDims.z());
        if (height < 3 || maxHorizontalDim < 3)
        {
            getPlayer().sendError(
                "creator.clock.error.too_small",
                getStructureArg(),
                arg -> arg.highlight(maxHorizontalDim),
                arg -> arg.highlight(height)
            );
            return CompletableFuture.completedFuture(false);
        }

        if (height != maxHorizontalDim)
        {
            getPlayer().sendError(
                "creator.clock.error.not_square",
                arg -> arg.highlight(maxHorizontalDim),
                arg -> arg.highlight(height)
            );
            return CompletableFuture.completedFuture(false);
        }

        // The clock has to be an odd number of blocks tall.
        if (height % 2 == 0)
        {
            getPlayer().sendError(
                "creator.clock.error.not_odd",
                arg -> arg.highlight(maxHorizontalDim),
                arg -> arg.highlight(height)
            );
            return CompletableFuture.completedFuture(false);
        }

        final int depth = Math.min(cuboidDims.x(), cuboidDims.z());
        if (depth != 2)
        {
            getPlayer().sendError(
                "creator.clock.error.not_2_deep",
                getStructureArg(),
                arg -> arg.highlight(depth)
            );
            return CompletableFuture.completedFuture(false);
        }

        northSouthAligned = cuboidDims.x() == 2;
        return super.provideSecondPos(loc);
    }

    @Override
    public synchronized Set<MovementDirection> getValidOpenDirections()
    {
        return northSouthAligned ? NORTH_SOUTH_AXIS_OPEN_DIRS : EAST_WEST_AXIS_OPEN_DIRS;
    }

    @Override
    protected synchronized void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.clock.stick_lore");
    }

    /**
     * Calculates the open direction from the current physical aspects of this clock.
     */
    protected synchronized void updateOpenDirection()
    {
        if (northSouthAligned)
            setMovementDirection(hourArmSide == BlockFace.EAST ? MovementDirection.SOUTH : MovementDirection.NORTH);
        else
            setMovementDirection(hourArmSide == BlockFace.NORTH ? MovementDirection.EAST : MovementDirection.WEST);
    }

    @Override
    protected synchronized Structure constructStructure()
    {
        Util.requireNonNull(hourArmSide, "hourArmSide");
        updateOpenDirection();

        return super.constructStructure();
    }

    @SuppressWarnings("unused") // It is used by the generated toString method.
    protected final synchronized @Nullable BlockFace getHourArmSide()
    {
        return hourArmSide;
    }

    @SuppressWarnings("unused") // It is used by the generated toString method.
    protected final synchronized boolean isNorthSouthAligned()
    {
        return northSouthAligned;
    }
}
