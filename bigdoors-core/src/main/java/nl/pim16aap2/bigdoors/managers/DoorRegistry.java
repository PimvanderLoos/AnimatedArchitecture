package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.debugging.IDebuggable;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.data.cache.timed.TimedCache;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.IDoorConst;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Optional;

/**
 * Represents a registry of doors.
 *
 * @author Pim
 * @see <a href="https://en.wikipedia.org/wiki/Multiton_pattern">Wikipedia: Multiton</a>
 */
@Singleton
public final class DoorRegistry extends Restartable implements IDebuggable
{
    public static final int CONCURRENCY_LEVEL = 4;
    public static final int INITIAL_CAPACITY = 100;
    public static final Duration CACHE_EXPIRY = Duration.ofMinutes(15);

    // It's not final, so we make it volatile to ensure it's always visible.
    // SonarLint likes to complain about making it volatile, as this doesn't
    // mean access to the object is thread-safe. However, we know that the
    // type is thread-safe; we just want to ensure visibility across threads.
    @SuppressWarnings("squid:S3077")
    private volatile TimedCache<Long, AbstractDoor> doorCache;

    /**
     * Keeps track of whether to allow new entries to be added to the cache.
     */
    private volatile boolean acceptNewEntries = true;

    // TODO: Implement the use of these parameters. Once implemented, this should be public.
    private final int concurrencyLevel;
    private final int initialCapacity;
    private final Duration cacheExpiry;

    /**
     * Constructs a new {@link #DoorRegistry}.
     *
     * @param concurrencyLevel
     *     The concurrency level (see Guava docs) of the cache.
     * @param initialCapacity
     *     The initial size of the cache to reserve.
     * @param cacheExpiry
     *     How long to keep stuff in the cache.
     */
//    @IBuilder // These parameters aren't implemented atm, so there's no point in having this ctor/builder.
    private DoorRegistry(
        RestartableHolder restartableHolder, DebuggableRegistry debuggableRegistry,
        int concurrencyLevel, int initialCapacity, Duration cacheExpiry)
    {
        super(restartableHolder);
        this.concurrencyLevel = concurrencyLevel;
        this.initialCapacity = initialCapacity;
        this.cacheExpiry = cacheExpiry;
        init();
        debuggableRegistry.registerDebuggable(this);
    }

    /**
     * Constructs a new {@link #DoorRegistry} using the default values.
     * <p>
     * See {@link #CONCURRENCY_LEVEL}, {@link #INITIAL_CAPACITY}.
     */
    @Inject
    public DoorRegistry(RestartableHolder restartableHolder, DebuggableRegistry debuggableRegistry)
    {
        this(restartableHolder, debuggableRegistry, CONCURRENCY_LEVEL, INITIAL_CAPACITY, CACHE_EXPIRY);
    }

    /**
     * Creates a new {@link DoorRegistry} without any caching.
     *
     * @return The new {@link DoorRegistry}.
     */
    public static DoorRegistry unCached(RestartableHolder restartableHolder, DebuggableRegistry debuggableRegistry)
    {
        final DoorRegistry doorRegistry =
            new DoorRegistry(restartableHolder, debuggableRegistry, -1, -1, Duration.ofMillis(-1));
        doorRegistry.acceptNewEntries = false;
        return doorRegistry;
    }

    /**
     * Attempts to get the {@link DoorBase} associated the given UID. It will only search
     *
     * @param doorUID
     *     The UID of the door.
     * @return The {@link DoorBase} if it has been retrieved from the database.
     */
    public Optional<AbstractDoor> getRegisteredDoor(long doorUID)
    {
        return doorCache.get(doorUID);
    }

    /**
     * Deletes an {@link DoorBase} from the registry.
     *
     * @param doorUID
     *     The UID of the {@link DoorBase} to delete.
     */
    void deregisterDoor(long doorUID)
    {
        doorCache.remove(doorUID);
    }

    /**
     * Checks if a {@link DoorBase} associated with a given UID has been registered.
     *
     * @param doorUID
     *     The UID of the door.
     * @return True if an entry exists for the {@link DoorBase} with the given UID.
     */
    @SuppressWarnings("unused")
    public boolean isRegistered(long doorUID)
    {
        return doorCache.containsKey(doorUID);
    }

    /**
     * Checks if the exact instance of the provided {@link DoorBase} has been registered. (i.e. it uses '==' to check if
     * the cached entry is the same).
     *
     * @param doorBase
     *     The door.
     * @return True if an entry exists for the exact instance of the provided {@link DoorBase}.
     */
    public boolean isRegistered(IDoorConst doorBase)
    {
        return doorCache.get(doorBase.getDoorUID()).map(found -> found == doorBase).orElse(false);
    }

    /**
     * Registers an {@link DoorBase} if it hasn't been registered yet.
     *
     * @param registrable
     *     The {@link AbstractDoor.Registrable} that belongs to the {@link DoorBase} that is to be registered.
     * @return True if the door was added successfully (and didn't exist yet).
     */
    public boolean registerDoor(AbstractDoor.Registrable registrable)
    {
        if (!acceptNewEntries)
            return true;
        final AbstractDoor door = registrable.getAbstractDoorBase();
        return doorCache.putIfAbsent(door.getDoorUID(), door).isEmpty();
    }

    @Override
    public void shutDown()
    {
        doorCache.clear();
    }

    /**
     * (Re)initializes the {@link #doorCache}.
     *
     * @return This {@link DoorRegistry}.
     */
    @Initializer
    private void init()
    {
        if (cacheExpiry.isNegative())
            doorCache = TimedCache.emptyCache();
        else
            doorCache = TimedCache.<Long, AbstractDoor>builder()
                                  .cleanup(Duration.ofMinutes(5))
                                  .softReference(true)
                                  .keepAfterTimeOut(true)
                                  .duration(cacheExpiry)
                                  .build();
    }

    @Override
    public String getDebugInformation()
    {
        return "Accepting new entries: " + acceptNewEntries +
            "\nconcurrencyLevel: " + concurrencyLevel +
            "\ninitialCapacity: " + initialCapacity +
            "\ncacheExpiry: " + cacheExpiry +
            "\ncacheSize: " + doorCache.getSize();
    }
}
