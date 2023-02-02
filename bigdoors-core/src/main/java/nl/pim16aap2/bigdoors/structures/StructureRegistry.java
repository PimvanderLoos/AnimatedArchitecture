package nl.pim16aap2.bigdoors.structures;

import com.google.common.flogger.StackSize;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.debugging.IDebuggable;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.data.cache.timed.TimedCache;
import nl.pim16aap2.bigdoors.managers.StructureDeletionManager;
import nl.pim16aap2.bigdoors.util.Util;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Represents a registry of structures.
 *
 * @author Pim
 * @see <a href="https://en.wikipedia.org/wiki/Multiton_pattern">Wikipedia: Multiton</a>
 */
@Singleton
@Flogger
public final class StructureRegistry extends Restartable
    implements IDebuggable, StructureDeletionManager.IDeletionListener
{
    public static final int CONCURRENCY_LEVEL = 4;
    public static final int INITIAL_CAPACITY = 100;
    public static final Duration CACHE_EXPIRY = Duration.ofMinutes(15);

    // It's not final, so we make it volatile to ensure it's always visible.
    // SonarLint likes to complain about making it volatile, as this doesn't
    // mean access to the object is thread-safe. However, we know that the
    // type is thread-safe; we just want to ensure visibility across threads.
    @SuppressWarnings("squid:S3077")
    private volatile TimedCache<Long, AbstractStructure> structureCache;

    /**
     * Keeps track of whether to allow new entries to be added to the cache.
     */
    private volatile boolean acceptNewEntries = true;

    // TODO: Implement the use of these parameters. Once implemented, this should be public.
    private final int concurrencyLevel;
    private final int initialCapacity;
    private final Duration cacheExpiry;

    /**
     * Constructs a new {@link #StructureRegistry}.
     *
     * @param concurrencyLevel
     *     The concurrency level (see Guava docs) of the cache.
     * @param initialCapacity
     *     The initial size of the cache to reserve.
     * @param cacheExpiry
     *     How long to keep stuff in the cache.
     */
//    @IBuilder // These parameters aren't implemented atm, so there's no point in having this ctor/builder.
    private StructureRegistry(
        RestartableHolder restartableHolder, DebuggableRegistry debuggableRegistry,
        int concurrencyLevel, int initialCapacity, Duration cacheExpiry,
        StructureDeletionManager structureDeletionManager)
    {
        super(restartableHolder);
        this.concurrencyLevel = concurrencyLevel;
        this.initialCapacity = initialCapacity;
        this.cacheExpiry = cacheExpiry;

        init();

        debuggableRegistry.registerDebuggable(this);
        structureDeletionManager.registerDeletionListener(this);
    }

    /**
     * Constructs a new {@link #StructureRegistry} using the default values.
     * <p>
     * See {@link #CONCURRENCY_LEVEL}, {@link #INITIAL_CAPACITY}.
     */
    @Inject StructureRegistry(
        RestartableHolder restartableHolder, DebuggableRegistry debuggableRegistry,
        StructureDeletionManager structureDeletionManager)
    {
        this(restartableHolder, debuggableRegistry, CONCURRENCY_LEVEL, INITIAL_CAPACITY, CACHE_EXPIRY,
             structureDeletionManager);
    }

    /**
     * Creates a new {@link StructureRegistry} without any caching.
     *
     * @return The new {@link StructureRegistry}.
     */
    public static StructureRegistry unCached(
        RestartableHolder restartableHolder, DebuggableRegistry debuggableRegistry,
        StructureDeletionManager structureDeletionManager)
    {
        final StructureRegistry structureRegistry = new StructureRegistry(
            restartableHolder, debuggableRegistry, -1, -1, Duration.ofMillis(-1), structureDeletionManager);

        structureRegistry.acceptNewEntries = false;
        return structureRegistry;
    }

    @Override
    public void onStructureDeletion(IStructureConst structure)
    {
        structureCache.remove(structure.getUid());
    }

    /**
     * Attempts to get the {@link AbstractStructure} associated the given UID. It can only retrieve doors that still
     * exist in the cache.
     *
     * @param structureUID
     *     The UID of the structure.
     * @return The {@link AbstractStructure} if it has been retrieved from the database.
     */
    public Optional<AbstractStructure> getRegisteredStructure(long structureUID)
    {
        return structureCache.get(structureUID);
    }

    /**
     * Checks if a {@link AbstractStructure} associated with a given UID has been registered.
     *
     * @param structureUID
     *     The UID of the structure.
     * @return True if an entry exists for the {@link AbstractStructure} with the given UID.
     */
    @SuppressWarnings("unused")
    public boolean isRegistered(long structureUID)
    {
        return structureCache.containsKey(structureUID);
    }

    /**
     * Checks if the exact instance of the provided {@link AbstractStructure} has been registered. (i.e. it uses '==' to
     * check if the cached entry is the same).
     *
     * @param structure
     *     The structure.
     * @return True if an entry exists for the exact instance of the provided {@link AbstractStructure}.
     */
    public boolean isRegistered(AbstractStructure structure)
    {
        return structureCache.get(structure.getUid()).map(found -> found == structure).orElse(false);
    }

    /**
     * Puts a new {@link AbstractStructure} in the cache if it does not exist yet.
     *
     * @param uid
     *     The UID of the structure.
     * @param supplier
     *     The supplier that will create a new structure if no mapping exists yet for the provided UID.
     * @return The {@link AbstractStructure} that ends up being in the cache. If a mapping already existed, this will be
     * the old structure. If not, the newly created one will be returned instead.
     */
    AbstractStructure computeIfAbsent(long uid, Supplier<AbstractStructure> supplier)
    {
        if (uid <= 0)
            throw new IllegalArgumentException("Trying to register structure with UID " + uid);

        return structureCache.compute(uid, (key, value) ->
        {
            if (value == null)
                return Util.requireNonNull(supplier.get(), "Supplied Structure");
            log.atFine().withStackTrace(StackSize.FULL).log("Caught attempted double registering of structure %d", uid);
            return value;
        });
    }

    @Override
    public void shutDown()
    {
        structureCache.clear();
    }

    /**
     * (Re)initializes the {@link #structureCache}.
     *
     * @return This {@link StructureRegistry}.
     */
    @Initializer
    private void init()
    {
        if (cacheExpiry.isNegative())
            structureCache = TimedCache.emptyCache();
        else
            structureCache = TimedCache.<Long, AbstractStructure>builder()
                                       .cleanup(Duration.ofMinutes(15))
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
            "\ncacheSize: " + structureCache.getSize();
    }
}
