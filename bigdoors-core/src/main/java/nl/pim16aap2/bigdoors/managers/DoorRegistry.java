package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.Restartable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a registry of doors.
 *
 * @author Pim
 * @see <a href="https://en.wikipedia.org/wiki/Multiton_pattern">Wikipedia: Multiton</a>
 */
// TODO: ConcurrentHashMaps are pretty slow at resizing, so try to estimate the size (based on the config options)
//       on init. Also, see the note at #getDoor(long) regarding the use of ConcurrentHashmaps in general.
// TODO: Perhaps this class should be a private member for the DatabaseManager?
public final class DoorRegistry extends Restartable
{
    @NotNull
    private static final DoorRegistry INSTANCE = new DoorRegistry();

    /*
     * The idea here is that the doors map contains the completed queries. Which should not be stored as
     * CompletableFutures, as they have been completed by definition. Only the AbstractDoorBase class is allowed
     * to add actual AbstractDoorBases to it. Not even this class may do so.
     *
     * The futureDoors map contains the active queries. I.e. doors that have been requested but haven't been
     * found/constructed yet.
     *
     * When requesting a door, this class will first attempt to retrieve it from the doors map.
     * If it isn't in the doors map, it will request a new instance and add the request to the futureDoors map.
     * Note that the futureDoors entries need a .onComplete to remove themselves from this map and to add an empty
     * optional to the doors map if no existing entry exists for it (i.e. could not be found).
     *
     * The AbstractDoorBase class will register itself in the doors map on instantiation.
     */
    @NotNull
    private final Map<Long, Optional<AbstractDoorBase>> doors = new ConcurrentHashMap<>();

    @NotNull
    private final Map<Long, CompletableFuture<Optional<AbstractDoorBase>>> futureDoors = new ConcurrentHashMap<>();

    private DoorRegistry()
    {
        super(BigDoors.get());
        if (INSTANCE != null)
        {
            IllegalAccessError e = new IllegalAccessError("Illegally trying to instantiate DoorManager!");
            PLogger.get().logThrowableSilently(e);
            throw e;
        }
    }

    public static @NotNull DoorRegistry get()
    {
        return INSTANCE;
    }

    /**
     * Attempts to get the {@link AbstractDoorBase} associated the given UID. If the door does not exist in the
     * registry, the database will be queried to find it.
     *
     * @param doorUID The UID of the door.
     * @return The {@link AbstractDoorBase} once the database has been queried if it could be found. Otherwise, the
     * {@link Optional} will be empty.
     */
    // TODO: Synchronized? Perhaps the maps shouldn't be concurrent ones?
    public @NotNull CompletableFuture<Optional<AbstractDoorBase>> getDoor(final long doorUID)
    {
        final @Nullable CompletableFuture<Optional<AbstractDoorBase>> futureDoor = futureDoors.get(doorUID);
        if (futureDoor != null)
            return futureDoor;

        final @Nullable Optional<AbstractDoorBase> currentDoor = doors.get(doorUID);
        if (currentDoor != null)
            return CompletableFuture.completedFuture(currentDoor);

        final @NotNull CompletableFuture<Optional<AbstractDoorBase>> newDoor = DatabaseManager.get().getDoor(doorUID);
        newDoor.whenComplete(
            (door, throwable) ->
            {
                if (!door.isPresent())
                    doors.put(doorUID, Optional.empty());
            });
        futureDoors.put(doorUID, newDoor);
        return newDoor;
    }

    /**
     * Attempts to get the {@link AbstractDoorBase} associated the given UID. It will only search
     *
     * @param doorUID The UID of the door.
     * @return The {@link AbstractDoorBase} if it has been retrieved from the database.
     */
    public @NotNull Optional<AbstractDoorBase> getDoorResult(final long doorUID)
    {
        return doors.getOrDefault(doorUID, Optional.empty());
    }

    /**
     * Deletes an {@link AbstractDoorBase} from both the database and the registry.
     *
     * @param door The {@link AbstractDoorBase} to delete.
     */
    public void deleteDoor(final @NotNull AbstractDoorBase door)
    {
        doors.computeIfPresent(door.getDoorUID(), (key, val)
            ->
        {
            DatabaseManager.get().removeDoor(door).whenComplete(
                (result, throwable) ->
                {
                    if (!result)
                        PLogger.get()
                               .logThrowable(new IllegalStateException("Failed to delete door: " + door.getDoorUID()));
                });
            return Optional.empty();
        });
    }

    /**
     * Inserts a {@link AbstractDoorBase} into the database.
     *
     * @param newDoor The new {@link AbstractDoorBase}.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public @NotNull CompletableFuture<Optional<AbstractDoorBase>> addDoorBase(final @NotNull AbstractDoorBase newDoor)
    {
        if (newDoor.getDoorUID() > 0)
        {
            PLogger.get().logThrowable(new IllegalArgumentException(
                "Tried to insert a door with doorUID \"" + newDoor.getDoorUID() +
                    "\"! Only the database is allowed to assign doorUIDs!"));
            doors.remove(newDoor.getDoorUID());
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return DatabaseManager.get().addDoorBase(newDoor);
    }

    /**
     * Checks if a {@link AbstractDoorBase} associated with a given UID has been registered.
     * <p>
     * Note that this does not mean that this {@link AbstractDoorBase} actually exists. Merely that a mapping to a
     * potentially missing {@link AbstractDoorBase} exists.
     *
     * @param doorUID The UID of the door.
     * @return True if an entry exists for the {@link AbstractDoorBase} with the given UID.
     */
    public boolean isRegistered(final long doorUID)
    {
        return doors.containsKey(doorUID);
    }

    /**
     * Registers an {@link AbstractDoorBase} if it hasn't been registered yet.
     *
     * @param registerable The {@link AbstractDoorBase.Registerable} that belongs to the {@link AbstractDoorBase} that
     *                     is to be registered.
     * @return True if the door was added successfully (and didn't exist yet).
     */
    public boolean registerDoor(final @NotNull AbstractDoorBase.Registerable registerable)
    {
        final @NotNull AbstractDoorBase doorBase = registerable.getAbstractDoorBase();
        return doors.putIfAbsent(doorBase.getDoorUID(), Optional.of(doorBase)) == null;
    }

    @Override
    public void restart()
    {
        doors.clear();
    }

    @Override
    public void shutdown()
    {
        doors.clear();
    }
}
