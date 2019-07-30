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

import java.util.UUID;

public class BridgeOpener extends Opener
{
    public BridgeOpener(final @NotNull BigDoors plugin)
    {
        super(plugin);
    }

    /**
     * Checks if the new position is free.
     *
     * @param playerUUID The {@link UUID} of the {@link org.bukkit.entity.Player} who requested the door toggle if
     *                   available or the original creator otherwise.
     * @param door       The {@link DoorBase}.
     * @param upDown     Whether to check the location when going up or down.
     * @param cardinal   The direction to check.
     * @param newMin     The new minimum location of the {@link DoorBase} when it opens in the returned direction. It is
     *                   modified in the method.
     * @param newMax     The new maximum location of the {@link DoorBase} when it opens in the returned direction. It is
     *                   modified in the method.
     * @return True if the new position is free.
     */
    private boolean isNewPosFree(final @Nullable UUID playerUUID, final @NotNull DoorBase door,
                                 final @NotNull PBlockFace upDown, final @NotNull PBlockFace cardinal,
                                 final @NotNull Location newMin, final @NotNull Location newMax)
    {
        int startX = 0, startY = 0, startZ = 0;
        int endX = 0, endY = 0, endZ = 0;

        if (upDown.equals(PBlockFace.UP))
            switch (cardinal)
            {
                // North West = Min X, Min Z
                // South West = Min X, Max Z
                // North East = Max X, Min Z
                // South East = Max X, Max X
                case NORTH:
                    startX = door.getMinimum().getBlockX();
                    endX = door.getMaximum().getBlockX();

                    startY = door.getMinimum().getBlockY() + 1;
                    endY = door.getMinimum().getBlockY() + door.getMaximum().getBlockZ() -
                        door.getMinimum().getBlockZ();

                    startZ = door.getMinimum().getBlockZ();
                    endZ = door.getMinimum().getBlockZ();
                    break;

                case SOUTH:
                    startX = door.getMinimum().getBlockX();
                    endX = door.getMaximum().getBlockX();

                    startY = door.getMinimum().getBlockY() + 1;
                    endY = door.getMinimum().getBlockY() + door.getMaximum().getBlockZ() -
                        door.getMinimum().getBlockZ();

                    startZ = door.getMaximum().getBlockZ();
                    endZ = door.getMaximum().getBlockZ();
                    break;

                case EAST:
                    startX = door.getMaximum().getBlockX();
                    endX = door.getMaximum().getBlockX();

                    startY = door.getMinimum().getBlockY() + 1;
                    endY = door.getMinimum().getBlockY() + door.getMaximum().getBlockX() -
                        door.getMinimum().getBlockX();

                    startZ = door.getMinimum().getBlockZ();
                    endZ = door.getMaximum().getBlockZ();
                    break;

                case WEST:
                    startX = door.getMinimum().getBlockX();
                    endX = door.getMinimum().getBlockX();

                    startY = door.getMinimum().getBlockY() + 1;
                    endY = door.getMinimum().getBlockY() + door.getMaximum().getBlockX() -
                        door.getMinimum().getBlockX();

                    startZ = door.getMinimum().getBlockZ();
                    endZ = door.getMaximum().getBlockZ();
                    break;
                default:
                    plugin.getPLogger().dumpStackTrace("Invalid rotation for bridge opener: " + cardinal.toString());
                    break;
            }
        else
            switch (cardinal)
            {
                // North West = Min X, Min Z
                // South West = Min X, Max Z
                // North East = Max X, Min Z
                // South East = Max X, Max X
                case NORTH:
                    startX = door.getMinimum().getBlockX();
                    endX = door.getMaximum().getBlockX();

                    startY = door.getMinimum().getBlockY();
                    endY = door.getMinimum().getBlockY();

                    startZ = door.getMinimum().getBlockZ() - door.getMaximum().getBlockY() +
                        door.getMinimum().getBlockY();
                    endZ = door.getMinimum().getBlockZ() - 1;
                    break;

                case SOUTH:
                    startX = door.getMinimum().getBlockX();
                    endX = door.getMaximum().getBlockX();

                    startY = door.getMinimum().getBlockY();
                    endY = door.getMinimum().getBlockY();

                    startZ = door.getMinimum().getBlockZ() + 1;
                    endZ = door.getMinimum().getBlockZ() + door.getMaximum().getBlockY() -
                        door.getMinimum().getBlockY();
                    break;

                case EAST:
                    startX = door.getMinimum().getBlockX() + 1;
                    endX = door.getMaximum().getBlockX() + door.getMaximum().getBlockY() -
                        door.getMinimum().getBlockY();

                    startY = door.getMinimum().getBlockY();
                    endY = door.getMinimum().getBlockY();

                    startZ = door.getMinimum().getBlockZ();
                    endZ = door.getMaximum().getBlockZ();
                    break;

                case WEST:
                    startX = door.getMinimum().getBlockX() - door.getMaximum().getBlockY() +
                        door.getMinimum().getBlockY();
                    endX = door.getMinimum().getBlockX() - 1;

                    startY = door.getMinimum().getBlockY();
                    endY = door.getMinimum().getBlockY();

                    startZ = door.getMinimum().getBlockZ();
                    endZ = door.getMaximum().getBlockZ();
                    break;
                default:
                    plugin.getPLogger().dumpStackTrace("Invalid rotation for bridge opener: " + cardinal.toString());
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

    // TODO: LSP!

    /**
     * Checks if the {@link DoorBase} should go up or down.
     *
     * @param door The {@link DoorBase}.
     * @return {@link PBlockFace#DOWN} or {@link PBlockFace#UP}.
     */
    private PBlockFace getUpDown(final @NotNull DoorBase door)
    {
        int height = Math.abs(door.getMinimum().getBlockY() - door.getMaximum().getBlockY());
        if (height > 0)
            return PBlockFace.DOWN;
        return PBlockFace.UP;
    }

    /**
     * Figures out which way the bridge should go and finds the new minimum and maximum coordinates.
     *
     * @param playerUUID The {@link UUID} of the {@link org.bukkit.entity.Player} who requested the door toggle if
     *                   available or the original creator otherwise.
     * @param door       The {@link DoorBase}.
     * @param newMin     The new minimum location of the {@link DoorBase} when it opens in the returned direction. It is
     *                   modified in the method.
     * @param newMax     The new maximum location of the {@link DoorBase} when it opens in the returned direction. It is
     *                   modified in the method.
     * @return The direction this {@link DoorType#DRAWBRIDGE} will open in.
     */
    private RotateDirection getOpenDirection(final @Nullable UUID playerUUID, final @NotNull DoorBase door,
                                             final @NotNull Location newMin, final @NotNull Location newMax)
    {
        PBlockFace upDown = getUpDown(door);
        PBlockFace cDir = getCurrentDirection(door);
        boolean NS = cDir == PBlockFace.NORTH || cDir == PBlockFace.SOUTH;

        if (upDown.equals(PBlockFace.UP))
            return isNewPosFree(playerUUID, door, upDown, door.getEngineSide(), newMin, newMax) ?
                   RotateDirection.valueOf(door.getEngineSide().toString()) : null;

        if (door.getOpenDir().equals(RotateDirection.CLOCKWISE) && !door.isOpen() ||
            door.getOpenDir().equals(RotateDirection.COUNTERCLOCKWISE) && door.isOpen())
        {
            return NS && isNewPosFree(playerUUID, door, upDown, PBlockFace.SOUTH, newMin, newMax) ?
                   RotateDirection.SOUTH :
                   !NS && isNewPosFree(playerUUID, door, upDown, PBlockFace.EAST, newMin, newMax) ?
                   RotateDirection.EAST : null;
        }
        if (door.getOpenDir().equals(RotateDirection.CLOCKWISE) && door.isOpen() ||
            door.getOpenDir().equals(RotateDirection.COUNTERCLOCKWISE) && !door.isOpen())
        {
            return NS && isNewPosFree(playerUUID, door, upDown, PBlockFace.NORTH, newMin, newMax) ?
                   RotateDirection.NORTH :
                   !NS && isNewPosFree(playerUUID, door, upDown, PBlockFace.WEST, newMin, newMax) ?
                   RotateDirection.WEST : null;
        }

        return NS && isNewPosFree(playerUUID, door, upDown, PBlockFace.NORTH, newMin, newMax) ? RotateDirection.NORTH :
               !NS && isNewPosFree(playerUUID, door, upDown, PBlockFace.EAST, newMin, newMax) ? RotateDirection.EAST :
               NS && isNewPosFree(playerUUID, door, upDown, PBlockFace.SOUTH, newMin, newMax) ? RotateDirection.SOUTH :
               !NS && isNewPosFree(playerUUID, door, upDown, PBlockFace.WEST, newMin, newMax) ? RotateDirection.WEST :
               null;
    }

    /**
     * Gets the "current direction". In this context this means on which side of the {@link DoorType#DRAWBRIDGE} the
     * engine is.
     *
     * @param door The {@link DoorBase}.
     * @return The current direction of the {@link DoorBase}.
     */
    @NotNull
    private PBlockFace getCurrentDirection(final @NotNull DoorBase door)
    {
        return door.getEngineSide();
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

        PBlockFace currentDirection = getCurrentDirection(door);
        if (currentDirection == null)
        {
            plugin.getPLogger()
                  .warn("Current direction is null for bridge " + door.getName() + " (" + door.getDoorUID() + ")!");
            return abort(door, DoorToggleResult.ERROR);
        }

        PBlockFace upDown = getUpDown(door);
        if (upDown == null)
        {
            plugin.getPLogger()
                  .warn("UpDown direction is null for bridge " + door.getName() + " (" + door.getDoorUID() + ")!");
            return abort(door, DoorToggleResult.ERROR);
        }

        Location newMin = new Location(door.getWorld(), 0, 0, 0);
        Location newMax = new Location(door.getWorld(), 0, 0, 0);
        RotateDirection openDirection = getOpenDirection(playerUUID, door, newMin, newMax);
        if (openDirection == null)
        {
            plugin.getPLogger().warn("OpenDirection direction is null for bridge " + door.getName() + " ("
                                         + door.getDoorUID() + ")!");
            return abort(door, DoorToggleResult.NODIRECTION);
        }

        // Check if the owner of the door has permission to edit blocks in the new area of the door.
        if (!super.canBreakBlocksBetweenLocs(door, newMin, newMax))
            return abort(door, DoorToggleResult.NOPERMISSION);

        plugin.getDatabaseManager()
              .addBlockMover(new BridgeMover(plugin, door.getWorld(), time, door, upDown, openDirection, instantOpen,
                                             plugin.getConfigLoader().getMultiplier(DoorType.DRAWBRIDGE), playerUUID));
        return DoorToggleResult.SUCCESS;
    }
}
