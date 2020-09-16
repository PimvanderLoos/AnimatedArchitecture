package nl.pim16aap2.bigdoors.managers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.Restartable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Represents a registry of doors.
 *
 * @author Pim
 * @see <a href="https://en.wikipedia.org/wiki/Multiton_pattern">Wikipedia: Multiton</a>
 */
// TODO: Guava performs some cache cleanup on writes if possible, but it will resort to performing it on reads,
//       if necessary. Because this cache is probably not written to too often, this can cause some read operations
//       to get slowed down. Consider regularly calling Cache#cleanup from a separate thread.
// TODO: Make sure that entries aren't removed from the cache if a reference is still available.
//       Perhaps setting the expiry to an insanely high value? 24h?
// TODO: Allow enabling statistics for debugging purposes.
public final class DoorRegistry extends Restartable
{
    // TODO: Figure out how much space a door takes up in memory, roughly, and figure out what sane values to use.
    // TODO: Make these configurable.
    public static final int MAX_REGISTRY_SIZE = 1000;
    public static final int CONCURRENCY_LEVEL = 4;
    public static final int INITIAL_CAPACITY = 100;
    public static final @NotNull Duration CACHE_EXPIRY = Duration.ofMinutes(5);

    private static final @NotNull DoorRegistry INSTANCE = new DoorRegistry();

    private Cache<Long, AbstractDoorBase> doorCache;

    private DoorRegistry()
    {
        super(BigDoors.get());
        if (INSTANCE != null)
        {
            IllegalAccessError e = new IllegalAccessError("Illegally trying to instantiate DoorManager!");
            PLogger.get().logThrowableSilently(e);
            throw e;
        }

        init(MAX_REGISTRY_SIZE, CONCURRENCY_LEVEL, INITIAL_CAPACITY, CACHE_EXPIRY);
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
     *
     * @param doorUID The UID of the door.
     * @return True if an entry exists for the {@link AbstractDoorBase} with the given UID.
     */
    public boolean isRegistered(final long doorUID)
    {
        return doorCache.getIfPresent(doorUID) != null;
    }

    /**
     * Checks if the exact instance of the provided {@link AbstractDoorBase} has been registered. (i.e. it uses '==' to
     * check if the cached entry is the same).
     *
     * @param doorBase The door.
     * @return True if an entry exists for the exact instance of the provided {@link AbstractDoorBase}.
     */
    public boolean isRegistered(final @NotNull AbstractDoorBase doorBase)
    {
        return doorCache.getIfPresent(doorBase.getDoorUID()) == doorBase;
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

    /**
     * (Re)initializes the {@link #doorCache}.
     *
     * @param maxRegistrySize  The maximum number of entries in the cache.
     * @param concurrencyLevel The concurrency level (see Guava docs) of the cache.
     * @param initialCapacity  The initial size of the cache to reserve.
     * @param cacheExpiry      How long to keep stuff in the cache.
     * @return This {@link DoorRegistry}.
     */
    public @NotNull DoorRegistry init(final int maxRegistrySize, final int concurrencyLevel, final int initialCapacity,
                                      final @NotNull Duration cacheExpiry)
    {
        return init(maxRegistrySize, concurrencyLevel, initialCapacity, cacheExpiry, true);
    }

    /**
     * (Re)initializes the {@link #doorCache}.
     *
     * @param maxRegistrySize  The maximum number of entries in the cache.
     * @param concurrencyLevel The concurrency level (see Guava docs) of the cache.
     * @param initialCapacity  The initial size of the cache to reserve.
     * @param cacheExpiry      How long to keep stuff in the cache.
     * @param removalListener  Whether to enable the RemovalListener that syncs the door with the database when that
     *                         door is evicted from the cache. This is mostly useful for unit tests.
     * @return This {@link DoorRegistry}.
     */
    public @NotNull DoorRegistry init(final int maxRegistrySize, final int concurrencyLevel, final int initialCapacity,
                                      final @NotNull Duration cacheExpiry, final boolean removalListener)
    {
        if (doorCache != null)
            doorCache.invalidateAll();

        @NotNull CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
                                                                         .softValues()
                                                                         .maximumSize(maxRegistrySize)
                                                                         .expireAfterAccess(cacheExpiry)
                                                                         .initialCapacity(initialCapacity)
                                                                         .concurrencyLevel(concurrencyLevel);
        if (removalListener)
            cacheBuilder = cacheBuilder.removalListener(
                notification ->
                {
                    PLogger.get().logMessage(Level.FINEST, "Removed door " +
                        notification.getKey().toString() + " from the registry! Reason: " +
                        notification.getCause().name());

                    if (notification.getCause() == RemovalCause.COLLECTED ||
                        notification.getCause() == RemovalCause.EXPIRED ||
                        notification.getCause() == RemovalCause.SIZE)
                        ((AbstractDoorBase) notification.getValue()).syncAllData();
                });

        doorCache = cacheBuilder.build();
        return this;
    }
}
