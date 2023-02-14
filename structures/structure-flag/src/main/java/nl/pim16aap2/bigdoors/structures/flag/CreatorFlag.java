package nl.pim16aap2.bigdoors.structures.flag;

import nl.pim16aap2.bigdoors.core.api.ILocation;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.StructureType;
import nl.pim16aap2.bigdoors.core.tooluser.Step;
import nl.pim16aap2.bigdoors.core.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CreatorFlag extends Creator
{
    private static final StructureType STRUCTURE_TYPE = StructureTypeFlag.get();
    protected boolean northSouthAligned;

    public CreatorFlag(Creator.Context context, IPlayer player, @Nullable String name)
    {
        super(context, player, name);
    }

    public CreatorFlag(Creator.Context context, IPlayer player)
    {
        this(context, player, null);
    }

    @Override
    protected List<Step> generateSteps()
        throws InstantiationException
    {
        return Arrays.asList(factorySetName.construct(),
                             factorySetFirstPos.messageKey("creator.flag.step_1").construct(),
                             factorySetSecondPos.messageKey("creator.flag.step_2").construct(),
                             factorySetRotationPointPos.messageKey("creator.flag.step_3").construct(),
                             factorySetPowerBlockPos.construct(),
                             factoryConfirmPrice.construct(),
                             factoryCompleteProcess.messageKey("creator.flag.success").construct());
    }

    @Override
    protected void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.flag.stick_lore", "creator.flag.init");
    }

    @Override
    protected boolean setSecondPos(ILocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        Util.requireNonNull(firstPos, "firstPos");
        final Vector3Di cuboidDims = new Cuboid(firstPos, new Vector3Di(loc.getBlockX(), loc.getBlockY(),
                                                                        loc.getBlockZ())).getDimensions();

        // Flags must have a dimension of 1 along either the x or z axis, as it's a `2d` shape.
        if ((cuboidDims.x() == 1) ^ (cuboidDims.z() == 1))
        {
            northSouthAligned = cuboidDims.x() == 1;
            return super.setSecondPos(loc);
        }

        getPlayer().sendError(textFactory, localizer.getMessage("creator.base.second_pos_not_2d"));
        return false;
    }

    @Override
    protected boolean completeSetRotationPointStep(ILocation loc)
    {
        Util.requireNonNull(cuboid, "cuboid");
        // For flags, the rotation point has to be a corner of the total area.
        // It doesn't make sense to have it in the middle or something; that's now how flags work.
        if ((loc.getBlockX() == cuboid.getMin().x() || loc.getBlockX() == cuboid.getMax().x()) &&
            (loc.getBlockZ() == cuboid.getMin().z() || loc.getBlockZ() == cuboid.getMax().z()))
            return super.completeSetRotationPointStep(loc);

        getPlayer().sendError(textFactory, localizer.getMessage("creator.base.position_not_in_corner"));
        return false;
    }

    @Override
    protected AbstractStructure constructStructure()
    {
        Util.requireNonNull(cuboid, "cuboid");
        Util.requireNonNull(rotationPoint, "rotationPoint");
        if (northSouthAligned)
            openDir = rotationPoint.z() == cuboid.getMin().z() ? MovementDirection.SOUTH : MovementDirection.NORTH;
        else
            openDir = rotationPoint.x() == cuboid.getMin().x() ? MovementDirection.EAST : MovementDirection.WEST;

        return new Flag(constructStructureData(), northSouthAligned);
    }

    @Override
    protected StructureType getStructureType()
    {
        return STRUCTURE_TYPE;
    }
}
