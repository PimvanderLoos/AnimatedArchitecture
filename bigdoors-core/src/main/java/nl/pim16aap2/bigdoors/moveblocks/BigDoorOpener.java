package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents an opener for {@link DoorType#BIGDOOR}s.
 *
 * @author Pim
 */
public class BigDoorOpener extends Opener
{
    public BigDoorOpener(final @NotNull BigDoors plugin)
    {
        super(plugin);
    }

    /**
     * Checks if the block on the north/east/south/west side of the location is free.
     *
     * @param playerUUID The {@link UUID} of the player initiating the toggle or the original creator if the {@link
     *                   DoorBase} was not toggled by a {@link org.bukkit.entity.Player}.
     * @param door       The {@link DoorBase}.
     * @param direction  The rotation direction of the {@link DoorBase}.
     * @param newMin     The new minimum location of the {@link DoorBase} when it opens in the returned direction. It is
     *                   modified in the method.
     * @param newMax     The new maximum location of the {@link DoorBase} when it opens in the returned direction. It is
     *                   modified in the method.
     * @return True if the position is unobstructed.
     */
    private boolean isPosFree(final @Nullable UUID playerUUID, final @NotNull DoorBase door,
                              final @NotNull PBlockFace direction, final @NotNull Location newMin,
                              final @NotNull Location newMax)
    {
        Location engLoc = door.getEngine();
        int endX = 0, endY = 0, endZ = 0;
        int startX = 0, startY = 0, startZ = 0;
        int xLen = door.getMaximum().getBlockX() - door.getMinimum().getBlockX();
        int zLen = door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ();

        switch (direction)
        {
            case NORTH:
                startX = engLoc.getBlockX();
                startY = engLoc.getBlockY();
                startZ = engLoc.getBlockZ() - xLen;
                endX = engLoc.getBlockX();
                endY = door.getMaximum().getBlockY();
                endZ = engLoc.getBlockZ() - 1;
                break;
            case EAST:
                startX = engLoc.getBlockX() + 1;
                startY = engLoc.getBlockY();
                startZ = engLoc.getBlockZ();
                endX = engLoc.getBlockX() + zLen;
                endY = door.getMaximum().getBlockY();
                endZ = engLoc.getBlockZ();
                break;
            case SOUTH:
                startX = engLoc.getBlockX();
                startY = engLoc.getBlockY();
                startZ = engLoc.getBlockZ() + 1;
                endX = engLoc.getBlockX();
                endY = door.getMaximum().getBlockY();
                endZ = engLoc.getBlockZ() + xLen;
                break;
            case WEST:
                startX = engLoc.getBlockX() - zLen;
                startY = engLoc.getBlockY();
                startZ = engLoc.getBlockZ();
                endX = engLoc.getBlockX() - 1;
                endY = door.getMaximum().getBlockY();
                endZ = engLoc.getBlockZ();
                break;
            default:
                plugin.getPLogger().dumpStackTrace("Invalid direction for door opener: " + direction.toString());
                break;
        }

        newMin.setX(startX);
        newMin.setY(startY);
        newMin.setZ(startZ);
        newMax.setX(endX);
        newMax.setY(endY);
        newMax.setZ(endZ);

        return super.isLocationEmpty(newMin, newMax, playerUUID);
    }

    /**
     * Determines which direction the {@link DoorBase} is going to rotate. Either Clockwise or counterclockwise.
     *
     * @param playerUUID The {@link UUID} of the player initiating the toggle or the original creator if the {@link
     *                   DoorBase} was not toggled by a {@link org.bukkit.entity.Player}.
     * @param door       The {@link DoorBase}.
     * @param currentDir The current direction.
     * @param newMin     The new minimum location of the {@link DoorBase} when it opens in the returned direction. It is
     *                   modified in the method.
     * @param newMax     The new maximum location of the {@link DoorBase} when it opens in the returned direction. It is
     *                   modified in the method.
     * @return The rotation direction of this door, if one was found.
     */
    private Optional<RotateDirection> getRotationDirection(final @Nullable UUID playerUUID,
                                                           final @NotNull DoorBase door,
                                                           final @NotNull PBlockFace currentDir,
                                                           final @NotNull Location newMin,
                                                           final @NotNull Location newMax)
    {
        RotateDirection openDir = door.isOpen() ? RotateDirection.getOpposite(door.getOpenDir()) : door.getOpenDir();
        if (openDir == null)
        {
            plugin.getPLogger().logException(new NullPointerException(
                String.format("Could not determine RotateDirection of door %d", door.getDoorUID())));
            return Optional.empty();
        }

        RotateDirection ret = null;
        switch (currentDir)
        {
            case NORTH:
                if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE) &&
                    isPosFree(playerUUID, door, PBlockFace.EAST, newMin, newMax))
                    ret = RotateDirection.CLOCKWISE;
                else if (!openDir.equals(RotateDirection.CLOCKWISE) &&
                    isPosFree(playerUUID, door, PBlockFace.WEST, newMin, newMax))
                    ret = RotateDirection.COUNTERCLOCKWISE;
                break;

            case EAST:
                if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE) &&
                    isPosFree(playerUUID, door, PBlockFace.SOUTH, newMin, newMax))
                    ret = RotateDirection.CLOCKWISE;
                else if (!openDir.equals(RotateDirection.CLOCKWISE) &&
                    isPosFree(playerUUID, door, PBlockFace.NORTH, newMin, newMax))
                    ret = RotateDirection.COUNTERCLOCKWISE;
                break;

            case SOUTH:
                if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE) &&
                    isPosFree(playerUUID, door, PBlockFace.WEST, newMin, newMax))
                    ret = RotateDirection.CLOCKWISE;
                else if (!openDir.equals(RotateDirection.CLOCKWISE) &&
                    isPosFree(playerUUID, door, PBlockFace.EAST, newMin, newMax))
                    ret = RotateDirection.COUNTERCLOCKWISE;
                break;

            case WEST:
                if (!openDir.equals(RotateDirection.COUNTERCLOCKWISE) &&
                    isPosFree(playerUUID, door, PBlockFace.NORTH, newMin, newMax))
                    ret = RotateDirection.CLOCKWISE;
                else if (!openDir.equals(RotateDirection.CLOCKWISE) &&
                    isPosFree(playerUUID, door, PBlockFace.SOUTH, newMin, newMax))
                    ret = RotateDirection.COUNTERCLOCKWISE;
                break;
            default:
                plugin.getPLogger().dumpStackTrace("Invalid currentDir for door opener: " + currentDir.toString());
                break;
        }
        return Optional.ofNullable(ret);
    }

    /**
     * Gets the current direction of a {@link DoorBase}.
     *
     * @param door The {@link DoorBase}.
     * @return The current direction of a {@link DoorBase} if possible.
     */
    @NotNull
    private Optional<PBlockFace> getCurrentDirection(final @NotNull DoorBase door)
    {
        // MinZ != EngineZ => Pointing North
        // MaxX != EngineX => Pointing East
        // MaxZ != EngineZ => Pointing South
        // MinX != EngineX => Pointing West
        return Optional.ofNullable(door.getEngine().getBlockZ() != door.getMinimum().getBlockZ() ? PBlockFace.NORTH :
                                   door.getEngine().getBlockX() != door.getMaximum().getBlockX() ? PBlockFace.EAST :
                                   door.getEngine().getBlockZ() != door.getMaximum().getBlockZ() ? PBlockFace.SOUTH :
                                   door.getEngine().getBlockX() != door.getMinimum().getBlockX() ? PBlockFace.WEST :
                                   null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public DoorToggleResult toggleDoor(final @NotNull UUID playerUUID, final @NotNull DoorBase door,
                                       final double time, boolean instantOpen, final boolean playerToggle)
    {
        DoorToggleResult isOpenable = super.canBeToggled(door, playerToggle);
        if (isOpenable != DoorToggleResult.SUCCESS)
            return abort(door, isOpenable);

        if (super.isTooBig(door))
            instantOpen = true;

        Optional<PBlockFace> currentDirection = getCurrentDirection(door);
        if (!currentDirection.isPresent())
        {
            plugin.getPLogger()
                  .warn("Current direction is null for door " + door.getName() + " (" + door.getDoorUID() + ")!");
            return abort(door, DoorToggleResult.ERROR);
        }
        Location newMin = new Location(door.getWorld(), 0, 0, 0);
        Location newMax = new Location(door.getWorld(), 0, 0, 0);

        Optional<RotateDirection> rotDirection = getRotationDirection(playerUUID, door, currentDirection.get(), newMin,
                                                                      newMax);
        if (!rotDirection.isPresent())
        {
            plugin.getPLogger()
                  .warn("Rotation direction is null for door " + door.getName() + " (" + door.getDoorUID() + ")!");
            return abort(door, DoorToggleResult.NODIRECTION);
        }

        // Check if the owner of the door has permission to edit blocks in the new area of the door.
        if (!super.canBreakBlocksBetweenLocs(door, newMin, newMax))
            return abort(door, DoorToggleResult.NOPERMISSION);

        plugin.getDatabaseManager()
              .addBlockMover(
                  new CylindricalMover(plugin, door.getWorld(), rotDirection.get(), time, currentDirection.get(), door,
                                       instantOpen, plugin.getConfigLoader().getMultiplier(DoorType.BIGDOOR),
                                       playerUUID));
        return DoorToggleResult.SUCCESS;
    }
}
