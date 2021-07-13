package nl.pim16aap2.bigDoors.moveBlocks;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.events.DoorEventToggle.ToggleType;
import nl.pim16aap2.bigDoors.events.DoorEventTogglePrepare;
import nl.pim16aap2.bigDoors.events.DoorEventToggleStart;
import nl.pim16aap2.bigDoors.util.ChunkUtils;
import nl.pim16aap2.bigDoors.util.ChunkUtils.ChunkLoadMode;
import nl.pim16aap2.bigDoors.util.ChunkUtils.ChunkLoadResult;
import nl.pim16aap2.bigDoors.util.ConfigLoader;
import nl.pim16aap2.bigDoors.util.DoorOpenResult;
import nl.pim16aap2.bigDoors.util.Pair;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;
import nl.pim16aap2.bigDoors.util.Vector2D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Opener
{
    default boolean hasValidCoordinates(Door door)
    {
        if (door.getWorld() != null)
            return true;

        BigDoors.get().getMyLogger().logMessage("World is null for door " + door.toSimpleString() + "!", true, false);
        return false;
    }

    /**
     * Gets the minimum and maximum chunk coordinates this door may visit. Note that the output may not be 100% correct.
     * The accuracy depends on the type and how much is known about the door.
     *
     * @param door The door for which to check it.
     * @return A pair of coordinates in chunk-space (hence 2d) containing the lower-bound coordinates first and the
     * upper bound second.
     */
    Pair<Vector2D, Vector2D> getChunkRange(Door door);

    /**
     * Gets the minimum and maximum chunk coordinates of the door according to its current location.
     *
     * @param door The door for which to check it.
     * @return A pair of coordinates in chunk-space (hence 2d) containing the lower-bound coordinates first and the
     * upper bound second.
     */
    default Pair<Vector2D, Vector2D> getCurrentChunkRange(Door door)
    {
        return ChunkUtils.getChunkRangeBetweenCoords(door.getMinimum(), door.getMaximum());
    }

    default ChunkLoadResult chunksLoaded(Door door, ChunkLoadMode mode)
    {
        if (!hasValidCoordinates(door))
            return ChunkLoadResult.FAIL;

        return ChunkUtils.checkChunks(door.getWorld(), getCurrentChunkRange(door), mode);
    }

    DoorOpenResult openDoor(Door door, double time);

    default DoorOpenResult openDoor(Door door, double time, boolean instantOpen)
    {
        return openDoor(door, time, instantOpen, false);
    }

    default DoorOpenResult openDoor(Door door, double time, boolean instantOpen, boolean silent)
    {
        ChunkLoadMode mode = BigDoors.get().getConfigLoader().loadChunksForToggle() ? ChunkLoadMode.ATTEMPT_LOAD :
                             ChunkLoadMode.VERIFY_LOADED;

        // When "skipUnloadedAutoCloseToggle" is enabled, doors will not try to load
        // chunks if they have an autoCloseTimer. This only affects closed doors,
        // because opened doors cannot initiate an autoCloseTimer.
        if (mode == ChunkLoadMode.ATTEMPT_LOAD && (!door.isOpen()) &&
            BigDoors.get().getConfigLoader().skipUnloadedAutoCloseToggle() && door.getAutoClose() > 0)
            mode = ChunkLoadMode.VERIFY_LOADED;

        return openDoor(door, time, instantOpen, silent, mode);
    }

    public DoorOpenResult openDoor(Door door, double time, boolean instantOpen, boolean silent, ChunkLoadMode mode);

    default DoorOpenResult abort(DoorOpenResult reason, long doorUID)
    {
        if (ConfigLoader.DEBUG)
            BigDoors.get().getMyLogger().info("Aborted toggle for door " + doorUID + ". Reason: " + reason.name());
        // If the door was busy, this new attempt should leave that alone.
        if (reason != DoorOpenResult.BUSY)
            BigDoors.get().getCommander().setDoorAvailable(doorUID);
        return reason;
    }

    RotateDirection getRotateDirection(Door door);

    default boolean isValidOpenDirection(@Nullable RotateDirection rotateDirection)
    {
        if (rotateDirection == null)
            return false;
        return getValidRotateDirections().contains(rotateDirection);
    }

    default boolean isRotateDirectionValid(@Nonnull Door door)
    {
        return isValidOpenDirection(door.getOpenDir());
    }

    /**
     * Retrieves the coordinate pair describing the region the door would move into if it were toggled right now.
     * <p>
     * Note that this will check the blocks in the world. If the server hasn't loaded the required chunk(s), the
     * chunk(s) will be loaded!
     * <p>
     * This method may not always be able to find the future coordinates for the following reasons:
     * <p>
     * 1) The opening direction is specified, but it is obstructed. So there are no new coordinates, because it could
     * not go anywhere.
     * <p>
     * 2) The opening direction is not specified and all possible directions (exact number depends on the type, e.g. 2
     * for doors, 4 for sliding doors) are obstructed. In this case there are no new coordinates, because it cannot go
     * anywhere and because even if it wanted to, it wouldn't know which direction to go.
     * <p>
     * 3) The door's size ({@link #getSizeLimit(Door)}) exceeds {@link Door#getBlockCount()}.
     * <p>
     * 4) The door's {@link Door#getBlocksToMove()} exceeds {@link ConfigLoader#getMaxBlocksToMove()}. This only applies
     * to doors which actually use this (i.e. portcullis, sliding door).
     *
     * @param door The door for which to find the new coordinates.
     * @return The new minimum and maximum coordinates the door would take up if it were toggled now.
     */
    @Nonnull Optional<Pair<Location, Location>> getNewCoordinates(@Nonnull Door door);

    default int getSizeLimit(final Door door)
    {
        int globalLimit = BigDoors.get().getConfigLoader().maxDoorSize();
        Player player = Bukkit.getPlayer(door.getPlayerUUID());
        int personalLimit = player == null ? -1 : Util.getMaxDoorSizeForPlayer(player);

        return Util.getLowestPositiveNumber(personalLimit, globalLimit);
    }

    /**
     * Checks if there aren't any obstructions between two positions.
     *
     * @param world The world to check in.
     * @param min   The minimum coordinates.
     * @param max   The maximum coordinates.
     * @return True if all blocks in the region defined by the min/max coordinates do not obstruct doors (e.g. water or
     * air).
     */
    default boolean isPosFree(@Nonnull World world, @Nonnull Location min, @Nonnull Location max)
    {
        for (int xAxis = min.getBlockX(); xAxis <= max.getBlockX(); ++xAxis)
            for (int yAxis = min.getBlockY(); yAxis <= max.getBlockY(); ++yAxis)
                for (int zAxis = min.getBlockZ(); zAxis <= max.getBlockZ(); ++zAxis)
                    if (!Util.isAirOrWater(world.getBlockAt(xAxis, yAxis, zAxis).getType()))
                        return false;
        return true;
    }

    /**
     * See {@link #isPosFree(World, Location, Location)}.
     *
     * @param locations The min and max locations respectively.
     */
    default boolean isPosFree(@Nonnull World world, @Nonnull Pair<Location, Location> locations)
    {
        return isPosFree(world, locations.first, locations.second);
    }

    /**
     * Checks if a player has access to both the new and the old positions of a door.
     * <p>
     * See {@link BigDoors#canBreakBlocksBetweenLocs(UUID, String, World, Location, Location)}.
     *
     * @param door   The door which data to use for check the owner's access to the new and the old locations.
     * @param newMin The new minimum coordinates.
     * @param newMax The new maximum coordinates.
     * @return True if the owner of the door has access to both the current area of the door as well as the new area of
     * the door.
     */
    default boolean hasAccessToLocations(@Nonnull Door door, @Nonnull Location newMin, @Nonnull Location newMax)
    {
        String protectionBlocker = BigDoors.get().canBreakBlocksBetweenLocs(door.getPlayerUUID(), door.getPlayerName(),
                                                                            door.getWorld(), newMin, newMax);
        if (protectionBlocker != null)
        {
            BigDoors.get().getMyLogger().logMessageToLogFile(
                "Toggle denied because access to new location was prevented by " + protectionBlocker + " for door: " +
                    door);
            return false;
        }

        protectionBlocker = BigDoors.get().canBreakBlocksBetweenLocs(door.getPlayerUUID(), door.getPlayerName(),
                                                                     door.getWorld(), door.getMinimum(),
                                                                     door.getMinimum());
        if (protectionBlocker != null)
        {
            BigDoors.get().getMyLogger().logMessageToLogFile(
                "Toggle denied because access to old location was prevented by " + protectionBlocker + " for door: " +
                    door);
            return false;
        }
        return true;
    }

    /**
     * Fires a {@link DoorEventTogglePrepare} for the given door.
     *
     * @param door The door that will be toggled.
     * @return True if the event has been cancelled.
     */
    default boolean fireDoorEventTogglePrepare(final Door door, final boolean instantOpen)
    {
        final ToggleType toggleType = door.isOpen() ? ToggleType.CLOSE : ToggleType.OPEN;
        DoorEventTogglePrepare preparationEvent = new DoorEventTogglePrepare(door, toggleType, instantOpen);
        Bukkit.getPluginManager().callEvent(preparationEvent);
        return preparationEvent.isCancelled();
    }

    /**
     * Fires a {@link DoorEventToggleStart} for the given door.
     *
     * @param door The door that is being toggled.
     */
    default void fireDoorEventToggleStart(final Door door, final boolean instantOpen)
    {
        final ToggleType toggleType = door.isOpen() ? ToggleType.CLOSE : ToggleType.OPEN;
        DoorEventToggleStart startEvent = new DoorEventToggleStart(door, toggleType, instantOpen);
        Bukkit.getPluginManager().callEvent(startEvent);
    }

    /**
     * Retrieves all valid {@link RotateDirection}s for this opener.
     *
     * @return A list with all valid {@link RotateDirection}s for this opener.
     */
    @Nonnull List<RotateDirection> getValidRotateDirections();
}
