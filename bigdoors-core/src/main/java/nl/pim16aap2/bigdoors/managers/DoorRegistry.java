package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.cache.TimedCache;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;

/**
 * Represents a registry of doors.
 *
 * @author Pim
 * @see <a href="https://en.wikipedia.org/wiki/Multiton_pattern">Wikipedia: Multiton</a>
 */
public final class DoorRegistry extends Restartable
{
    // TODO: Figure out how much space a door takes up in memory, roughly, and figure out what sane values to use.
    // TODO: Make these configurable.
    public static final int MAX_REGISTRY_SIZE = 1000;
    public static final int CONCURRENCY_LEVEL = 4;
    public static final int INITIAL_CAPACITY = 100;
    public static final @NotNull Duration CACHE_EXPIRY = Duration.ofMinutes(5);

    private TimedCache<Long, AbstractDoorBase> doorCache;

    /**
     * Constructs a new {@link #DoorRegistry}.
     *
     * @param maxRegistrySize  The maximum number of entries in the cache.
     * @param concurrencyLevel The concurrency level (see Guava docs) of the cache.
     * @param initialCapacity  The initial size of the cache to reserve.
     * @param cacheExpiry      How long to keep stuff in the cache.
     */
//    @Builder // These parameters aren't implemented atm, so there's no point in having this ctor/builder.
    private DoorRegistry(int maxRegistrySize, int concurrencyLevel, int initialCapacity, @NotNull Duration cacheExpiry)
    {
        super(BigDoors.get());
        init(maxRegistrySize, concurrencyLevel, initialCapacity, cacheExpiry);
    }

    /**
     * Constructs a new {@link #DoorRegistry} using the default values.
     * <p>
     * See {@link #MAX_REGISTRY_SIZE}, {@link #CONCURRENCY_LEVEL}, {@link #INITIAL_CAPACITY}, {@link #CACHE_EXPIRY}.
     */
    public DoorRegistry()
    {
        this(MAX_REGISTRY_SIZE, CONCURRENCY_LEVEL, INITIAL_CAPACITY, CACHE_EXPIRY);
    }

    /**
     * Creates a new {@link DoorRegistry} without any caching.
     *
     * @return The new {@link DoorRegistry}.
     */
    public static @NotNull DoorRegistry uncached()
    {
        return new DoorRegistry(-1, -1, -1, Duration.ofMillis(-1));
    }

    /**
     * Attempts to get the {@link AbstractDoorBase} associated the given UID. It will only search
     *
     * @param doorUID The UID of the door.
     * @return The {@link AbstractDoorBase} if it has been retrieved from the database.
     */
    public @NotNull Optional<AbstractDoorBase> getRegisteredDoor(final long doorUID)
    {
        return doorCache.get(doorUID);
    }

    /**
     * Deletes an {@link AbstractDoorBase} from the registry.
     *
     * @param doorUID The UID of the {@link AbstractDoorBase} to delete.
     */
    void deregisterDoor(final long doorUID)
    {
        doorCache.remove(doorUID);
    }

    /**
     * Checks if a {@link AbstractDoorBase} associated with a given UID has been registered.
     *
     * @param doorUID The UID of the door.
     * @return True if an entry exists for the {@link AbstractDoorBase} with the given UID.
     */
    public boolean isRegistered(final long doorUID)
    {
        return doorCache.containsKey(doorUID);
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
        return doorCache.get(doorBase.getDoorUID()).map(found -> found == doorBase).orElse(false);
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
        return doorCache.putIfAbsent(doorBase.getDoorUID(), doorBase).isEmpty();
    }

    @Override
    public void restart()
    {
        shutdown();
    }

    @Override
    public void shutdown()
    {
        doorCache.clear();
    }

    /**
     * (Re)initializes the {@link #doorCache}.
     *
     * @return This {@link DoorRegistry}.
     */
    public @NotNull DoorRegistry init(final @NotNull Duration duration)
    {
        return init(MAX_REGISTRY_SIZE, CONCURRENCY_LEVEL, INITIAL_CAPACITY, duration);
    }

    /**
     * (Re)initializes the {@link #doorCache}.
     *
     * @param maxRegistrySize  The maximum number of entries in the cache.
     * @param concurrencyLevel The concurrency level (see Guava docs) of the cache.
     * @param initialCapacity  The initial size of the cache to reserve.
     * @param cacheExpiry      How long to keep the doors in the cache.
     * @return This {@link DoorRegistry}.
     */
    // TODO: Implement these parameters. Once implemented, this should be public.
    @Initializer
    private @NotNull DoorRegistry init(final int maxRegistrySize, final int concurrencyLevel, final int initialCapacity,
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
     * @param cacheExpiry      How long to keep the doors in the cache.
     * @return This {@link DoorRegistry}.
     */
    // TODO: Implement these parameters. Once implemented, this should be public.
    @Initializer
    private @NotNull DoorRegistry init(final int maxRegistrySize, final int concurrencyLevel, final int initialCapacity,
                                       final @NotNull Duration cacheExpiry, final boolean removalListener)
    {
        if (doorCache != null)
            doorCache.clear();

        doorCache = TimedCache.<Long, AbstractDoorBase>builder()
                              .softReference(true)
                              .duration(cacheExpiry)
                              .build();

        return this;
    }
}
