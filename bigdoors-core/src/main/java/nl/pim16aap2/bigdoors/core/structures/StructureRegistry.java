package nl.pim16aap2.bigdoors.core.structures;

import com.google.common.flogger.StackSize;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.core.api.debugging.IDebuggable;
import nl.pim16aap2.bigdoors.core.data.cache.timed.TimedCache;
import nl.pim16aap2.bigdoors.core.managers.StructureDeletionManager;
import nl.pim16aap2.bigdoors.core.util.Util;

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
public final class StructureRegistry implements IDebuggable, StructureDeletionManager.IDeletionListener
{
    public static final Duration CACHE_EXPIRY = Duration.ofMinutes(15);

    private final TimedCache<Long, AbstractStructure> structureCache;

    /**
     * Keeps track of whether to allow new entries to be added to the cache.
     */
    private volatile boolean acceptNewEntries = true;

    private final Duration cacheExpiry;

    private StructureRegistry(
        DebuggableRegistry debuggableRegistry, Duration cacheExpiry, StructureDeletionManager structureDeletionManager)
    {
        this.cacheExpiry = cacheExpiry;

        if (cacheExpiry.isNegative())
            structureCache = TimedCache.emptyCache();
        else
            structureCache = TimedCache.<Long, AbstractStructure>builder()
                                       .cleanup(Duration.ofMinutes(15))
                                       .softReference(true)
                                       .keepAfterTimeOut(true)
                                       .duration(cacheExpiry)
                                       .build();

        debuggableRegistry.registerDebuggable(this);
        structureDeletionManager.registerDeletionListener(this);
    }

    /**
     * Constructs a new {@link #StructureRegistry} using the default cache expiry value: {@link #CACHE_EXPIRY}.
     */
    @Inject StructureRegistry(
        DebuggableRegistry debuggableRegistry, StructureDeletionManager structureDeletionManager)
    {
        this(debuggableRegistry, CACHE_EXPIRY, structureDeletionManager);
    }

    /**
     * Creates a new {@link StructureRegistry} without any caching.
     *
     * @return The new {@link StructureRegistry}.
     */
    public static StructureRegistry unCached(
        DebuggableRegistry debuggableRegistry, StructureDeletionManager structureDeletionManager)
    {
        final StructureRegistry structureRegistry = new StructureRegistry(
            debuggableRegistry, Duration.ofMillis(-1), structureDeletionManager);

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
    public String getDebugInformation()
    {
        return "Accepting new entries: " + acceptNewEntries +
            "\ncacheExpiry: " + cacheExpiry +
            "\ncacheSize: " + structureCache.getSize();
    }
}
