package nl.pim16aap2.bigDoors.moveBlocks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.events.DoorEventToggle.ToggleType;
import nl.pim16aap2.bigDoors.events.DoorEventTogglePrepare;
import nl.pim16aap2.bigDoors.events.DoorEventToggleStart;
import nl.pim16aap2.bigDoors.util.ChunkUtils;
import nl.pim16aap2.bigDoors.util.DoorOpenResult;
import nl.pim16aap2.bigDoors.util.Pair;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;
import nl.pim16aap2.bigDoors.util.Vector2D;

public interface Opener
{
    default boolean hasValidCoordinates(Door door)
    {
        if (door.getWorld() == null)
        {
            BigDoors.get().getMyLogger().logMessage("World is null for door \"" + door.getName().toString() + "\"",
                                                    true, false);
            return false;
        }
        if (door.getWorld().getChunkAt(door.getMaximum()) == null)
        {
            BigDoors.get().getMyLogger()
                .logMessage("Chunk at maximum for door \"" + door.getName().toString() + "\" is null!", true, false);
            return false;
        }
        if (door.getWorld().getChunkAt(door.getMinimum()) == null)
        {
            BigDoors.get().getMyLogger()
                .logMessage("Chunk at minimum for door \"" + door.getName().toString() + "\" is null!", true, false);
            return false;
        }
        return true;
    }

    /**
     * Gets the minimum and maximum chunk coordinates this door may visit. Note that
     * the output may not be 100% correct. The accuracy depends on the type and how
     * much is known about the door.
     *
     * @param door The door for which to check it.
     * @return A pair of coordinates in chunk-space (hence 2d) containing the
     *         lower-bound coordinates first and the upper bound second.
     */
    public Pair<Vector2D, Vector2D> getChunkRange(Door door);

    /**
     * Gets the minimum and maximum chunk coordinates of the door according to its
     * current location.
     *
     * @param door The door for which to check it.
     * @return A pair of coordinates in chunk-space (hence 2d) containing the
     *         lower-bound coordinates first and the upper bound second.
     */
    public default Pair<Vector2D, Vector2D> getCurrentChunkRange(Door door)
    {
        return ChunkUtils.getChunkRangeBetweenCoords(door.getMinimum(), door.getMaximum());
    }

    public DoorOpenResult openDoor(Door door, double time);

    default DoorOpenResult openDoor(Door door, double time, boolean instantOpen)
    {
        return openDoor(door, time, instantOpen, false);
    }

    public DoorOpenResult openDoor(Door door, double time, boolean instantOpen, boolean silent);

    public DoorOpenResult shadowToggle(Door door);

    default DoorOpenResult abort(DoorOpenResult reason, long doorUID)
    {
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
