package nl.pim16aap2.bigdoors.storage;

import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.TooManyDoorsException;
import nl.pim16aap2.bigdoors.spigotutil.DoorOwner;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents storage of all door related stuff.
 *
 * @author Pim
 */
public interface IStorage
{
    /**
     * Gets the level of ownership a given player has over a given door.
     *
     * @param playerUUID The player to check.
     * @param doorUID    The door to retrieve this player's level of ownership of.
     * @return The level of ownership this player has over this door or -1 for no ownership at all.
     */
    int getPermission(@NotNull final String playerUUID, final long doorUID);

    /**
     * Delete the door with the given doorUID from the database.
     *
     * @param doorUID The UID of the door to delete.
     * @return True if at least 1 door was successfully removed.
     */
    boolean removeDoor(final long doorUID);

    /**
     * Delete all doors owned by the given player with the given name.
     *
     * @param playerUUID The player whose doors to delete.
     * @param doorName   The name of the doors to delete.
     * @return True if at least 1 door was successfully removed.
     */
    boolean removeDoors(@NotNull final String playerUUID, @NotNull final String doorName);

    /**
     * Gets the total number of doors own by the given player.
     *
     * @param playerUUID The uuid of the player whose doors to count.
     * @return The total number of doors own by the given player.
     */
    int getDoorCountForPlayer(@NotNull UUID playerUUID);

    /**
     * Gets the number of doors own by the given player with the given name.
     *
     * @param playerUUID The uuid of the player whose doors to count.
     * @param doorName   The name of the door to search for.
     * @return The number of doors own by the given player with the given name.
     */
    int getDoorCountForPlayer(@NotNull UUID playerUUID, @NotNull String doorName);

    /**
     * Gets the total number of doors with the given name regardless of who owns them.
     *
     * @param doorName The name of the doors to search for.
     * @return The total number of doors with the given name.
     */
    int getDoorCountByName(@NotNull String doorName);

    /**
     * Gets the door with the given UID for the given player at the level of ownership this player has over this door,
     * if any.
     *
     * @param playerUUID The UUID of the player.
     * @param doorUID    The UID of the door to retrieve.
     * @return The door if it exists and if the player is an owner of it.
     */
    Optional<DoorBase> getDoor(@NotNull UUID playerUUID, final long doorUID);


    Optional<DoorBase> getDoor(final long doorUID);

    /**
     * Gets all the doors owned by the the given player with the given name.
     *
     * @param playerUUID The UUID of the player to search for.
     * @param name       The name of the doors to search for.
     * @return All doors owned by the given player with the given name.
     */
    Optional<ArrayList<DoorBase>> getDoors(@NotNull final UUID playerUUID, @NotNull final String name);

    /**
     * Gets all the doors owned by the the given player.
     *
     * @param playerUUID The UUID of the player to search for.
     * @return All doors owned by the given player.
     */
    Optional<ArrayList<DoorBase>> getDoors(@NotNull final UUID playerUUID);

    /**
     * Gets all the doors with the given name, regardless of who owns them.
     *
     * @param name The name of the doors to search for.
     * @return All doors with the given name.
     */
    Optional<ArrayList<DoorBase>> getDoors(@NotNull final String name);

    Optional<ArrayList<DoorBase>> getDoors(@NotNull final String playerUUID, @NotNull final String doorName,
                                           int maxPermission);

    Optional<ArrayList<DoorBase>> getDoors(@NotNull final String playerUUID, int maxPermission);

    /**
     * Gets the {@link DoorBase} with the given name owned by the given player. If the player owns 0 or more than 1
     * doors with the given name, no doors are returned at all!
     *
     * @param playerUUID The UUID of the player whose {@link DoorBase} will be obtained.
     * @param doorName   The name of the {@link DoorBase} to look for.
     * @return The {@link DoorBase} with the given name owned by this player if exactly 1 such DoorBase exists.
     */
    Optional<DoorBase> getDoor(@NotNull final String playerUUID, @NotNull final String doorName)
            throws TooManyDoorsException;

    /**
     * Gets the {@link DoorBase} with the given name owned by the given player. If the player owns 0 or more than 1
     * doors with the given name, no doors are returned at all!
     *
     * @param playerUUID    The UUID of the player whose {@link DoorBase} will be obtained.
     * @param doorName      The name of the {@link DoorBase} to look for.
     * @param maxPermission The highest level of ownership this player can have over the door being looked for. If set
     *                      to 0, for example, only door doors where the given player has ownership level 0 (= creator)
     *                      are included in the search.
     * @return The {@link DoorBase} with the given name owned by this player if exactly 1 such DoorBase exists.
     */
    Optional<DoorBase> getDoor(@NotNull final String playerUUID, @NotNull final String doorName, int maxPermission)
            throws TooManyDoorsException;

    boolean updatePlayerName(@NotNull final String playerUUID, @NotNull final String playerName);

    UUID getPlayerUUID(@NotNull final String playerName);

    String getPlayerName(@NotNull final String playerUUID);

    DoorOwner getOwnerOfDoor(final long doorUID);

    HashMap<Long, Long> getPowerBlockData(final long chunkHash);

    void updateDoorBlocksToMove(final long doorUID, final int blocksToMove);

    // Update the door at doorUID with the provided coordinates and open status.
    void updateDoorCoords(final long doorUID, final boolean isOpen, final int xMin, final int yMin,
                          final int zMin, final int xMax, final int yMax, final int zMax,
                          @Nullable final PBlockFace engSide);

    // Update the door with UID doorUID's Power Block Location with the provided
    // coordinates and open status.
    void updateDoorAutoClose(final long doorUID, final int autoClose);

    // Update the door with UID doorUID's Power Block Location with the provided
    // coordinates and open status.
    void updateDoorOpenDirection(final long doorUID, @NotNull final RotateDirection openDir);

    // Update the door with UID doorUID's Power Block Location with the provided
    // coordinates and open status.
    void updateDoorPowerBlockLoc(final long doorUID, final int xPos, final int yPos, final int zPos,
                                 @NotNull final UUID worldUUID);

    // Check if a given location already contains a power block or not.
    // Returns false if it's already occupied.
    boolean isPowerBlockLocationEmpty(@NotNull final Location loc);

    // Update the door at doorUID with the provided new lockstatus.
    void setLock(final long doorUID, final boolean newLockStatus);

    // Insert a new door in the db.
    void insert(@NotNull final DoorBase door);

    // Insert a new door in the db.
    boolean removeOwner(final long doorUID, @NotNull final String playerUUID);

    ArrayList<DoorOwner> getOwnersOfDoor(final long doorUID);

    // Insert a new door in the db.
    void addOwner(final long doorUID, @NotNull final UUID playerUUID, final int permission);
}
