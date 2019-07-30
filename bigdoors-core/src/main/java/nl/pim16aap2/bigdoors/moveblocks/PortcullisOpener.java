package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PortcullisOpener extends Opener
{
    public PortcullisOpener(final @NotNull BigDoors plugin)
    {
        super(plugin);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public DoorToggleResult toggleDoor(final @Nullable UUID playerUUID, final @NotNull DoorBase door,
                                       final double time, boolean instantOpen, final boolean playerToggle)
    {
        DoorToggleResult isOpenable = super.canBeToggled(door, playerToggle);
        if (isOpenable != DoorToggleResult.SUCCESS)
            return abort(door, isOpenable);

        if (super.isTooBig(door))
            instantOpen = true;

        Location newMin = door.getMinimum().clone();
        Location newMax = door.getMaximum().clone();
        int blocksToMove = getBlocksToMove(door, newMin, newMax);

        // Check if the owner of the door has permission to edit blocks in the new area of the door.
        if (!super.canBreakBlocksBetweenLocs(door, newMin, newMax))
            return abort(door, DoorToggleResult.NOPERMISSION);

        if (blocksToMove != 0)
            plugin.getDatabaseManager()
                  .addBlockMover(new VerticalMover(plugin, door.getWorld(), time, door, instantOpen, blocksToMove,
                                                   plugin.getConfigLoader().getMultiplier(DoorType.PORTCULLIS),
                                                   playerUUID));
        else
            return abort(door, DoorToggleResult.NODIRECTION);
        return DoorToggleResult.SUCCESS;
    }

    private int getBlocksInDir(final @NotNull DoorBase door, final @NotNull RotateDirection upDown)
    {
        int xMin, xMax, zMin, zMax, yMin, yMax, yLen, blocksUp = 0, delta;
        xMin = door.getMinimum().getBlockX();
        yMin = door.getMinimum().getBlockY();
        zMin = door.getMinimum().getBlockZ();
        xMax = door.getMaximum().getBlockX();
        yMax = door.getMaximum().getBlockY();
        zMax = door.getMaximum().getBlockZ();
        yLen = yMax - yMin + 1;

        int distanceToCheck = door.getBlocksToMove() < 1 ? yLen : door.getBlocksToMove();

        int xAxis, yAxis, zAxis, yGoal;
        World world = door.getWorld();
        delta = upDown == RotateDirection.DOWN ? -1 : 1;
        yAxis = upDown == RotateDirection.DOWN ? yMin - 1 : yMax + 1;
        yGoal = upDown == RotateDirection.DOWN ? yMin - distanceToCheck - 1 : yMax + distanceToCheck + 1;

        while (yAxis != yGoal)
        {
            for (xAxis = xMin; xAxis <= xMax; ++xAxis)
                for (zAxis = zMin; zAxis <= zMax; ++zAxis)
                    if (!SpigotUtil.isAirOrLiquid(world.getBlockAt(xAxis, yAxis, zAxis)))
                        return blocksUp;
            yAxis += delta;
            blocksUp += delta;
        }
        return blocksUp;
    }

    private int getBlocksToMove(final @NotNull DoorBase door, final @NotNull Location newMin,
                                final @NotNull Location newMax)
    {
        int blocksUp = getBlocksInDir(door, RotateDirection.UP);
        int blocksDown = getBlocksInDir(door, RotateDirection.DOWN);
        int blocksToMove = blocksUp > -1 * blocksDown ? blocksUp : blocksDown;

        newMin.add(0, blocksToMove, 0);
        newMax.add(0, blocksToMove, 0);

        return blocksToMove;
    }
}
