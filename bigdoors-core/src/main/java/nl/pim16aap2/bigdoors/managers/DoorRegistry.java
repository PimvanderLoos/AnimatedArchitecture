package nl.pim16aap2.bigdoors.managers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.Restartable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;

/**
 * Represents a registry of doors.
 *
 * @author Pim
 * @see <a href="https://en.wikipedia.org/wiki/Multiton_pattern">Wikipedia: Multiton</a>
 */
// TODO: Guava performs some cache cleanup on writes if possible, but it will resort to performing it on reads,
//       if necessary. Because this cache is probably not written to too often, this can cause some read operations
//       to get slowed down. Consider regularly calling Cache#cleanup from a separate thread.
public final class DoorRegistry extends Restartable
{
    private static final DoorRegistry INSTANCE = new DoorRegistry();

    // TODO: Figure out how much space a door takes up in memory, roughly, and figure out what sane values to use.
    // TODO: Make these configurable.
    public static final int MAX_REGISTRY_SIZE = 1000;
    public static final int CONCURRENCY_LEVEL = 4;
    public static final int INITIAL_CAPACITY = 100;
    public static final @NotNull Duration CACHE_EXPIRY = Duration.ofMinutes(10);

    private final @NotNull Cache<Long, AbstractDoorBase> doorCache;

    private DoorRegistry()
    {
        super(BigDoors.get());
        if (INSTANCE != null)
        {
            IllegalAccessError e = new IllegalAccessError("Illegally trying to instantiate DoorManager!");
            PLogger.get().logThrowableSilently(e);
            throw e;
        }

        doorCache = CacheBuilder.newBuilder()
                                .softValues()
                                .maximumSize(MAX_REGISTRY_SIZE)
                                .expireAfterAccess(CACHE_EXPIRY)
                                .initialCapacity(INITIAL_CAPACITY)
                                .concurrencyLevel(CONCURRENCY_LEVEL)
                                .build();
    }

    public static @NotNull DoorRegistry get()
    {
        return INSTANCE;
    }

    /**
     * Attempts to get the {@link AbstractDoorBase} associated the given UID. It will only search
     *
     * @param doorUID The UID of the door.
     * @return The {@link AbstractDoorBase} if it has been retrieved from the database.
     */
    public @NotNull Optional<AbstractDoorBase> getRegisteredDoor(final long doorUID)
    {
        return Optional.ofNullable(doorCache.getIfPresent(doorUID));
    }

    /**
     * Deletes an {@link AbstractDoorBase} from the registry.
     *
     * @param doorUID The UID of the {@link AbstractDoorBase} to delete.
     */
    void deregisterDoor(final long doorUID)
    {
        doorCache.invalidate(doorUID);
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
        return doorCache.getIfPresent(doorUID) != null;
    }

    /**
     * Registers an {@link AbstractDoorBase} if it hasn't been registered yet.
     *
     * @param registerable The {@link AbstractDoorBase.Registerable} that belongs to the {@link AbstractDoorBase} that
     *                     is to be registered.
     * @return True if the door was added successfully (and didn't exist yet).
     */
    public synchronized boolean registerDoor(final @NotNull AbstractDoorBase.Registerable registerable)
    {
        final @NotNull AbstractDoorBase doorBase = registerable.getAbstractDoorBase();
        if (doorCache.getIfPresent(doorBase.getDoorUID()) != null)
            return false;
        doorCache.put(doorBase.getDoorUID(), doorBase);
        return true;
    }

    @Override
    public void restart()
    {
        shutdown();
    }

    @Override
    public void shutdown()
    {
        doorCache.invalidateAll();
    }
}
