package nl.pim16aap2.animatedarchitecture.structures.garagedoor;

import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Step;
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

    private boolean northSouthAnimated;

    public CreatorGarageDoor(Creator.Context context, IPlayer player, @Nullable String name)
    {
        super(context, player, name);
    }

    @Override
    protected List<Step> generateSteps()
        throws InstantiationException
    {
        return Arrays.asList(factorySetName.construct(),
                             factorySetFirstPos.messageKey("creator.garage_door.step_1").construct(),
                             factorySetSecondPos.messageKey("creator.garage_door.step_2").construct(),
                             factorySetPowerBlockPos.construct(),
                             factorySetOpenStatus.construct(),
                             factorySetOpenDir.construct(),
                             factoryReviewResult.construct(),
                             factoryConfirmPrice.construct(),
                             factoryCompleteProcess.messageKey("creator.garage_door.success").construct());
    }

    @Override
    protected boolean setSecondPos(ILocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        Util.requireNonNull(firstPos, "firstPos");
        final Vector3Di cuboidDims = new Cuboid(firstPos, new Vector3Di(loc.getBlockX(), loc.getBlockY(),
                                                                        loc.getBlockZ())).getDimensions();

        // Check if there's exactly 1 dimension that is 1 block deep.
        if ((cuboidDims.x() == 1) ^ (cuboidDims.y() == 1) ^ (cuboidDims.z() == 1))
        {
            northSouthAnimated = cuboidDims.z() == 1;
            isOpen = cuboidDims.y() == 1;
            return super.setSecondPos(loc);
        }

        getPlayer().sendError(textFactory, localizer.getMessage("creator.base.second_pos_not_2d"));
        return false;
    }

    @Override
    public Set<MovementDirection> getValidOpenDirections()
    {
        if (isOpen)
            return getStructureType().getValidOpenDirections();
        // When the garage structure is not open (i.e. vertical), it can only be opened along one axis.
        return northSouthAnimated ? NORTH_SOUTH_AXIS_OPEN_DIRS : EAST_WEST_AXIS_OPEN_DIRS;
    }

    @Override
    protected void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.garage_door.stick_lore", "creator.garage_door.init");
    }

    @Override
    protected boolean completeSetOpenDirStep(MovementDirection direction)
    {
        if (super.completeSetOpenDirStep(direction))
        {
            if (openDir == MovementDirection.NORTH || openDir == MovementDirection.SOUTH)
                northSouthAnimated = true;
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

        final Vector3Di center = cuboid.getCenterBlock();
        final boolean isVertical = cuboid.getDimensions().y() > 1;

        int newX = center.x();
        int newY = center.y();
        int newZ = center.z();

        if (isVertical)
            newY = cuboid.getMax().y() + 1;
        else if (openDir == MovementDirection.NORTH)
            newZ = cuboid.getMax().z() + 1;
        else if (openDir == MovementDirection.EAST)
            newX = cuboid.getMin().x() - 1;
        else if (openDir == MovementDirection.SOUTH)
            newZ = cuboid.getMin().z() - 1;
        else if (openDir == MovementDirection.WEST)
            newX = cuboid.getMax().x() + 1;

        rotationPoint = new Vector3Di(newX, newY, newZ);
    }

    @Override
    protected AbstractStructure constructStructure()
    {
        setRotationPoint();
        return new GarageDoor(constructStructureData(), northSouthAnimated);
    }

    @Override
    protected StructureType getStructureType()
    {
        return STRUCTURE_TYPE;
    }
}
