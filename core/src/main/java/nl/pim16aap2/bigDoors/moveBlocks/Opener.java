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
import org.bukkit.entity.Player;

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
    public Pair<Vector2D, Vector2D> getChunkRange(Door door);

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

    DoorOpenResult openDoor(Door door, double time);

    default DoorOpenResult openDoor(Door door, double time, boolean instantOpen)
    {
        return openDoor(door, time, instantOpen, false);
    }

    default ChunkLoadResult chunksLoaded(Door door, ChunkLoadMode mode)
    {
        if (!hasValidCoordinates(door))
            return ChunkLoadResult.FAIL;

        return ChunkUtils.checkChunks(door.getWorld(), getCurrentChunkRange(door), mode);
    }

    public default DoorOpenResult openDoor(Door door, double time, boolean instantOpen, boolean silent)
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

    boolean isRotateDirectionValid(Door door);

    default int getSizeLimit(final Door door)
    {
        int globalLimit = BigDoors.get().getConfigLoader().maxDoorSize();
        Player player = Bukkit.getPlayer(door.getPlayerUUID());
        int personalLimit = player == null ? -1 : Util.getMaxDoorSizeForPlayer(player);

        return Util.getLowestPositiveNumber(personalLimit, globalLimit);
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
}
