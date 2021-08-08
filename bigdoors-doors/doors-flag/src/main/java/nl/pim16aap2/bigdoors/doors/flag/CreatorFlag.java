package nl.pim16aap2.bigdoors.doors.flag;

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CreatorFlag extends Creator
{
    private static final @NotNull DoorType DOOR_TYPE = DoorTypeFlag.get();
    protected boolean northSouthAligned;

    public CreatorFlag(final @NotNull IPPlayer player, final @Nullable String name)
    {
        super(player, name);
    }

    public CreatorFlag(final @NotNull IPPlayer player)
    {
        this(player, null);
    }

    @Override
    protected @NotNull List<IStep> generateSteps()
        throws InstantiationException
    {
        return Arrays.asList(factorySetName.construct(),
                             factorySetFirstPos.messageKey("creator.flag.step_1").construct(),
                             factorySetSecondPos.messageKey("creator.flag.step_2").construct(),
                             factorySetEnginePos.messageKey("creator.flag.step_3").construct(),
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
    protected boolean setSecondPos(final @NotNull IPLocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        Util.requireNonNull(firstPos, "firstPos");
        final @NotNull Vector3Di cuboidDims = new Cuboid(firstPos, new Vector3Di(loc.getBlockX(), loc.getBlockY(),
                                                                                 loc.getBlockZ())).getDimensions();

        // Flags must have a dimension of 1 along either the x or z axis, as it's a `2d` shape.
        if ((cuboidDims.x() == 1) ^ (cuboidDims.z() == 1))
        {
            northSouthAligned = cuboidDims.x() == 1;
            return super.setSecondPos(loc);
        }

        getPlayer().sendMessage(BigDoors.get().getLocalizer().getMessage("creator.base.second_pos_not_2d"));
        return false;
    }

    @Override
    protected boolean completeSetEngineStep(final @NotNull IPLocation loc)
    {
        Util.requireNonNull(cuboid, "cuboid");
        // For flags, the rotation point has to be a corner of the total area.
        // It doesn't make sense to have it in the middle or something; that's now how flags work.
        if ((loc.getBlockX() == cuboid.getMin().x() || loc.getBlockX() == cuboid.getMax().x()) &&
            (loc.getBlockZ() == cuboid.getMin().z() || loc.getBlockZ() == cuboid.getMax().z()))
            return super.completeSetEngineStep(loc);

        getPlayer().sendMessage(BigDoors.get().getPlatform().getLocalizer()
                                        .getMessage("creator.base.position_not_in_corner"));
        return false;
    }

    @Override
    protected @NotNull AbstractDoor constructDoor()
    {
        Util.requireNonNull(cuboid, "cuboid");
        Util.requireNonNull(engine, "engine");
        if (northSouthAligned)
            openDir = engine.z() == cuboid.getMin().z() ? RotateDirection.SOUTH : RotateDirection.NORTH;
        else
            openDir = engine.x() == cuboid.getMin().x() ? RotateDirection.EAST : RotateDirection.WEST;

        return new Flag(constructDoorData(), northSouthAligned);
    }

    @Override
    protected @NotNull DoorType getDoorType()
    {
        return DOOR_TYPE;
    }
}
