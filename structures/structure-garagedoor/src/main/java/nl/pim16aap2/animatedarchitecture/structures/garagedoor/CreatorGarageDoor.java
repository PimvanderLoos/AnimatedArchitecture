package nl.pim16aap2.animatedarchitecture.structures.garagedoor;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Step;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@ToString(callSuper = true)
public class CreatorGarageDoor extends Creator
{
    private static final StructureType STRUCTURE_TYPE = StructureTypeGarageDoor.get();

    /**
     * The valid open directions when the structure is animated along the north/south axis.
     */
    private static final Set<MovementDirection> NORTH_SOUTH_AXIS_OPEN_DIRS =
        EnumSet.of(MovementDirection.NORTH, MovementDirection.SOUTH);

    /**
     * The valid open directions when the structure is animated along the east/west axis.
     */
    private static final Set<MovementDirection> EAST_WEST_AXIS_OPEN_DIRS =
        EnumSet.of(MovementDirection.EAST, MovementDirection.WEST);

    @GuardedBy("this")
    private boolean northSouthAnimated;

    public CreatorGarageDoor(ToolUser.Context context, IPlayer player, @Nullable String name)
    {
        super(context, player, name);
        init();
    }

    @Override
    protected synchronized List<Step> generateSteps()
        throws InstantiationException
    {
        return Arrays.asList(
            factoryProvideName.construct(),
            factoryProvideFirstPos
                .textSupplier(text -> text.append(
                    localizer.getMessage("creator.garage_door.step_1"), TextType.INFO, getStructureArg()))
                .construct(),
            factoryProvideSecondPos
                .textSupplier(text -> text.append(
                    localizer.getMessage("creator.garage_door.step_2"), TextType.INFO, getStructureArg()))
                .construct(),
            factoryProvidePowerBlockPos.construct(),
            factoryProvideOpenStatus.construct(),
            factoryProvideOpenDir.construct(),
            factoryReviewResult.construct(),
            factoryConfirmPrice.construct(),
            factoryCompleteProcess.construct());
    }

    @Override
    protected synchronized boolean provideSecondPos(ILocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        final Vector3Di firstPos = Util.requireNonNull(getFirstPos(), "firstPos");
        final Vector3Di cuboidDims = new Cuboid(firstPos, new Vector3Di(loc.getBlockX(), loc.getBlockY(),
                                                                        loc.getBlockZ())).getDimensions();

        // Check if there's exactly 1 dimension that is 1 block deep.
        if ((cuboidDims.x() == 1) ^ (cuboidDims.y() == 1) ^ (cuboidDims.z() == 1))
        {
            northSouthAnimated = cuboidDims.z() == 1;
            setOpen(cuboidDims.y() == 1);
            return super.provideSecondPos(loc);
        }

        getPlayer().sendError(textFactory, localizer.getMessage("creator.base.second_pos_not_2d"));
        return false;
    }

    @Override
    public synchronized Set<MovementDirection> getValidOpenDirections()
    {
        if (isOpen())
            return getStructureType().getValidMovementDirections();
        // When the garage structure is not open (i.e. vertical), it can only be opened along one axis.
        return northSouthAnimated ? NORTH_SOUTH_AXIS_OPEN_DIRS : EAST_WEST_AXIS_OPEN_DIRS;
    }

    @Override
    protected synchronized void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.garage_door.stick_lore");
    }

    @Override
    protected synchronized boolean completeSetOpenDirStep(MovementDirection direction)
    {
        if (super.completeSetOpenDirStep(direction))
        {
            final MovementDirection movementDirection =
                Util.requireNonNull(getMovementDirection(), "movementDirection");

            if (movementDirection == MovementDirection.NORTH || movementDirection == MovementDirection.SOUTH)
                northSouthAnimated = true;
            return true;
        }
        return false;
    }

    /**
     * Calculates the position of the rotation point. This should be called at the end of the process, as not all
     * variables may be set at an earlier stage.
     */
    protected synchronized void setRotationPoint()
    {
        final @Nullable Cuboid cuboid = getCuboid();
        if (cuboid == null)
            return;

        final MovementDirection movementDirection = Util.requireNonNull(getMovementDirection(), "movementDirection");

        final Vector3Di center = cuboid.getCenterBlock();
        final boolean isVertical = cuboid.getDimensions().y() > 1;

        int newX = center.x();
        int newY = center.y();
        int newZ = center.z();

        if (isVertical)
            newY = cuboid.getMax().y() + 1;
        else if (movementDirection == MovementDirection.NORTH)
            newZ = cuboid.getMax().z() + 1;
        else if (movementDirection == MovementDirection.EAST)
            newX = cuboid.getMin().x() - 1;
        else if (movementDirection == MovementDirection.SOUTH)
            newZ = cuboid.getMin().z() - 1;
        else if (movementDirection == MovementDirection.WEST)
            newX = cuboid.getMax().x() + 1;

        setRotationPoint(new Vector3Di(newX, newY, newZ));
    }

    @Override
    protected synchronized AbstractStructure constructStructure()
    {
        setRotationPoint();
        return new GarageDoor(constructStructureData(), northSouthAnimated);
    }

    @Override
    protected StructureType getStructureType()
    {
        return STRUCTURE_TYPE;
    }

    @SuppressWarnings("unused") // It is used by the generated toString method.
    protected final synchronized boolean isNorthSouthAnimated()
    {
        return northSouthAnimated;
    }
}
