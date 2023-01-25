package nl.pim16aap2.bigdoors.managers;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.debugging.IDebuggable;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.data.cache.timed.TimedCache;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.IMovableConst;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Optional;

/**
 * Represents a registry of movables.
 *
 * @author Pim
 * @see <a href="https://en.wikipedia.org/wiki/Multiton_pattern">Wikipedia: Multiton</a>
 */
@Singleton
@Flogger
public final class MovableRegistry extends Restartable implements IDebuggable, MovableDeletionManager.IDeletionListener
{
    public static final int CONCURRENCY_LEVEL = 4;
    public static final int INITIAL_CAPACITY = 100;
    public static final Duration CACHE_EXPIRY = Duration.ofMinutes(15);

    // It's not final, so we make it volatile to ensure it's always visible.
    // SonarLint likes to complain about making it volatile, as this doesn't
    // mean access to the object is thread-safe. However, we know that the
    // type is thread-safe; we just want to ensure visibility across threads.
    @SuppressWarnings("squid:S3077")
    private volatile TimedCache<Long, AbstractMovable> movableCache;

    /**
     * Keeps track of whether to allow new entries to be added to the cache.
     */
    private volatile boolean acceptNewEntries = true;

    // TODO: Implement the use of these parameters. Once implemented, this should be public.
    private final int concurrencyLevel;
    private final int initialCapacity;
    private final Duration cacheExpiry;

    /**
     * Constructs a new {@link #MovableRegistry}.
     *
     * @param concurrencyLevel
     *     The concurrency level (see Guava docs) of the cache.
     * @param initialCapacity
     *     The initial size of the cache to reserve.
     * @param cacheExpiry
     *     How long to keep stuff in the cache.
     */
//    @IBuilder // These parameters aren't implemented atm, so there's no point in having this ctor/builder.
    private MovableRegistry(
        RestartableHolder restartableHolder, DebuggableRegistry debuggableRegistry,
        int concurrencyLevel, int initialCapacity, Duration cacheExpiry, MovableDeletionManager movableDeletionManager)
    {
        super(restartableHolder);
        this.concurrencyLevel = concurrencyLevel;
        this.initialCapacity = initialCapacity;
        this.cacheExpiry = cacheExpiry;

        init();

        debuggableRegistry.registerDebuggable(this);
        movableDeletionManager.registerDeletionListener(this);
    }

    /**
     * Constructs a new {@link #MovableRegistry} using the default values.
     * <p>
     * See {@link #CONCURRENCY_LEVEL}, {@link #INITIAL_CAPACITY}.
     */
    @Inject MovableRegistry(
        RestartableHolder restartableHolder, DebuggableRegistry debuggableRegistry,
        MovableDeletionManager movableDeletionManager)
    {
        this(restartableHolder, debuggableRegistry, CONCURRENCY_LEVEL, INITIAL_CAPACITY, CACHE_EXPIRY,
             movableDeletionManager);
    }

    /**
     * Creates a new {@link MovableRegistry} without any caching.
     *
     * @return The new {@link MovableRegistry}.
     */
    public static MovableRegistry unCached(
        RestartableHolder restartableHolder, DebuggableRegistry debuggableRegistry,
        MovableDeletionManager movableDeletionManager)
    {
        final MovableRegistry movableRegistry = new MovableRegistry(
            restartableHolder, debuggableRegistry, -1, -1, Duration.ofMillis(-1), movableDeletionManager);

        movableRegistry.acceptNewEntries = false;
        return movableRegistry;
    }

    @Override
    public void onMovableDeletion(IMovableConst movable)
    {
        movableCache.remove(movable.getUid());
    }

    /**
     * Attempts to get the {@link AbstractMovable} associated the given UID. It can only retrieve doors that still exist
     * in the cache.
     *
     * @param movableUID
     *     The UID of the movable.
     * @return The {@link AbstractMovable} if it has been retrieved from the database.
     */
    public Optional<AbstractMovable> getRegisteredMovable(long movableUID)
    {
        return movableCache.get(movableUID);
    }

    /**
     * Checks if a {@link AbstractMovable} associated with a given UID has been registered.
     *
     * @param movableUID
     *     The UID of the movable.
     * @return True if an entry exists for the {@link AbstractMovable} with the given UID.
     */
    @SuppressWarnings("unused")
    public boolean isRegistered(long movableUID)
    {
        return movableCache.containsKey(movableUID);
    }

    /**
     * Checks if the exact instance of the provided {@link AbstractMovable} has been registered. (i.e. it uses '==' to
     * check if the cached entry is the same).
     *
     * @param movable
     *     The movable.
     * @return True if an entry exists for the exact instance of the provided {@link AbstractMovable}.
     */
    public boolean isRegistered(AbstractMovable movable)
    {
        return movableCache.get(movable.getUid()).map(found -> found == movable).orElse(false);
    }

    /**
     * Registers an {@link AbstractMovable} if it hasn't been registered yet.
     *
     * @param registrable
     *     The {@link AbstractMovable.Registrable} that belongs to the {@link AbstractMovable} that is to be
     *     registered.
     * @return True if the movable was added successfully (and didn't exist yet).
     */
    public boolean registerMovable(AbstractMovable.Registrable registrable)
    {
        if (!acceptNewEntries)
            return true;
        final AbstractMovable movable = registrable.getAbstractMovableBase();
        return movableCache.putIfAbsent(movable.getUid(), movable).isEmpty();
    }

    @Override
    public void shutDown()
    {
        movableCache.clear();
    }

    /**
     * (Re)initializes the {@link #movableCache}.
     *
     * @return This {@link MovableRegistry}.
     */
    @Initializer
    private void init()
    {
        if (cacheExpiry.isNegative())
            movableCache = TimedCache.emptyCache();
        else
            movableCache = TimedCache.<Long, AbstractMovable>builder()
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
            "\ncacheSize: " + movableCache.getSize();
    }
}
