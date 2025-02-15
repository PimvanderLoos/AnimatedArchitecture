package nl.pim16aap2.animatedarchitecture.core.structures;

import com.google.common.flogger.StackSize;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.data.cache.timed.TimedCache;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureDeletionManager;
import nl.pim16aap2.animatedarchitecture.core.util.Util;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Represents a registry of structures.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Multiton_pattern">Wikipedia: Multiton</a>
 */
@Singleton
@Flogger
public final class StructureRegistry implements IDebuggable, StructureDeletionManager.IDeletionListener
{
    public static final Duration CACHE_EXPIRY = Duration.ofMinutes(15);

    private final TimedCache<Long, Structure> structureCache;

    /**
     * Keeps track of whether to allow new entries to be added to the cache.
     */
    private volatile boolean acceptNewEntries = true;

    private final Duration cacheExpiry;

    private StructureRegistry(
        DebuggableRegistry debuggableRegistry,
        Duration cacheExpiry,
        StructureDeletionManager structureDeletionManager)
    {
        this.cacheExpiry = cacheExpiry;

        if (cacheExpiry.isNegative())
            structureCache = TimedCache.emptyCache();
        else
            structureCache = TimedCache.<Long, Structure>builder()
                .cleanup(Duration.ofMinutes(15))
                .softReference(true)
                .keepAfterTimeOut(true)
                .timeOut(cacheExpiry)
                .build();

        debuggableRegistry.registerDebuggable(this);
        structureDeletionManager.registerDeletionListener(this);
    }

    /**
     * Constructs a new {@link #StructureRegistry} using the default cache expiry value: {@link #CACHE_EXPIRY}.
     */
    @Inject
    StructureRegistry(
        DebuggableRegistry debuggableRegistry,
        StructureDeletionManager structureDeletionManager)
    {
        this(debuggableRegistry, CACHE_EXPIRY, structureDeletionManager);
    }

    /**
     * Creates a new {@link StructureRegistry} without any caching.
     *
     * @return The new {@link StructureRegistry}.
     */
    public static StructureRegistry unCached(
        DebuggableRegistry debuggableRegistry,
        StructureDeletionManager structureDeletionManager)
    {
        final StructureRegistry structureRegistry = new StructureRegistry(
            debuggableRegistry,
            Duration.ofMillis(-1),
            structureDeletionManager
        );

        structureRegistry.acceptNewEntries = false;
        return structureRegistry;
    }

    @Override
    public void onStructureDeletion(IStructureConst structure)
    {
        if (structure.getUid() > 0)
            structureCache.remove(structure.getUid());
    }

    /**
     * Attempts to get the {@link Structure} associated the given UID. It can only retrieve doors that still exist in
     * the cache.
     *
     * @param structureUID
     *     The UID of the structure.
     * @return The {@link Structure} if it has been retrieved from the database.
     */
    public Optional<Structure> getRegisteredStructure(long structureUID)
    {
        return structureUID > 0 ? structureCache.get(structureUID) : Optional.empty();
    }

    /**
     * Checks if a {@link Structure} associated with a given UID has been registered.
     *
     * @param structureUID
     *     The UID of the structure.
     * @return True if an entry exists for the {@link Structure} with the given UID.
     */
    @SuppressWarnings("unused")
    public boolean isRegistered(long structureUID)
    {
        return structureUID > 0 && structureCache.containsKey(structureUID);
    }

    /**
     * Checks if the exact instance of the provided {@link Structure} has been registered. (i.e. it uses '==' to check
     * if the cached entry is the same).
     *
     * @param structure
     *     The structure.
     * @return True if an entry exists for the exact instance of the provided {@link Structure}.
     */
    public boolean isRegistered(Structure structure)
    {
        return structure.getUid() > 0 &&
            structureCache.get(structure.getUid()).map(found -> found == structure).orElse(false);
    }

    /**
     * Puts a new {@link Structure} in the cache if it does not exist yet.
     *
     * @param structure
     *     The structure to put in the cache.
     * @return An empty {@link Optional} if the structure was successfully put in the cache. If a mapping already
     * existed, the old structure will be returned instead.
     */
    public Optional<Structure> putIfAbsent(Structure structure)
    {
        return structureCache.putIfAbsent(structure.getUid(), structure);
    }

    /**
     * Puts a new {@link Structure} in the cache if it does not exist yet.
     *
     * @param uid
     *     The UID of the structure.
     * @param supplier
     *     The supplier that will create a new structure if no mapping exists yet for the provided UID.
     * @return The {@link Structure} that ends up being in the cache. If a mapping already existed, this will be the old
     * structure. If not, the newly created one will be returned instead.
     */
    Structure computeIfAbsent(long uid, Supplier<Structure> supplier)
    {
        if (uid < 1)
            throw new IllegalArgumentException("Trying to register structure with UID " + uid);

        return structureCache.compute(
            uid, (key, value) ->
            {
                if (value == null)
                    return Util.requireNonNull(supplier.get(), "Supplied Structure");

                log.atFine().withStackTrace(StackSize.FULL).log(
                    "Caught attempted double registering of structure %d! Existing = %s",
                    uid, value
                );
                return value;
            });
    }

    @Override
    public String getDebugInformation()
    {
        return "Accepting new entries: " + acceptNewEntries +
            "\ncacheExpiry: " + cacheExpiry +
            "\ncacheSize: " + structureCache.getSize();
    }
}
