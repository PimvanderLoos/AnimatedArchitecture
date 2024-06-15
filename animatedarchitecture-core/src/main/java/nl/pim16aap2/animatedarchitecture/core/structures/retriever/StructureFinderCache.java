package nl.pim16aap2.animatedarchitecture.core.structures.retriever;

import nl.pim16aap2.animatedarchitecture.core.api.restartable.IRestartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.data.cache.timed.TimedCache;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.Duration;

/**
 * Represents a cache of {@link StructureFinder}s.
 * <p>
 * This cache is used to keep track of the {@link StructureFinder}s that are currently being used by players. This
 * allows us to keep track of the current state of the {@link StructureFinder} and to update it when new input is
 * provided.
 */
@Singleton
final class StructureFinderCache implements IRestartable
{
    private final Provider<StructureRetrieverFactory> structureRetrieverFactoryProvider;
    private final DatabaseManager databaseManager;
    private volatile TimedCache<ICommandSender, StructureFinder> cache = TimedCache.emptyCache();

    @Inject
    StructureFinderCache(
        Provider<StructureRetrieverFactory> structureRetrieverFactoryProvider,
        RestartableHolder restartableHolder,
        DatabaseManager databaseManager)
    {
        this.structureRetrieverFactoryProvider = structureRetrieverFactoryProvider;
        this.databaseManager = databaseManager;
        restartableHolder.registerRestartable(this);
    }

    /**
     * Gets the {@link StructureFinder} for the given {@link ICommandSender} and the given input.
     * <p>
     * If no structure finder exists yet, a new one will be instantiated. If one does exist for the given
     * {@link ICommandSender}, the existing one will be updated to process
     *
     * @param commandSender
     *     The {@link ICommandSender} for which to retrieve or instantiate a {@link StructureFinder}.
     * @param input
     *     The input search query to process.
     * @param maxPermission
     *     The maximum permission (inclusive) of the structure owner of the structures to find. Does not apply if the
     *     command sender is not a player.
     * @return The {@link StructureFinder} mapped for the provided {@link ICommandSender}.
     */
    StructureFinder getStructureFinder(ICommandSender commandSender, String input, PermissionLevel maxPermission)
    {
        return cache.compute(
            commandSender,
            (sender, finder) ->
                finder == null ?
                    newInstance(sender, input, maxPermission) :
                    finder.processInput(input)
        );
    }

    private StructureFinder newInstance(ICommandSender commandSender, String input, PermissionLevel maxPermission)
    {
        return new StructureFinder(
            structureRetrieverFactoryProvider.get(),
            databaseManager,
            commandSender,
            input,
            maxPermission
        );
    }

    @Override
    public void initialize()
    {
        cache = TimedCache.<ICommandSender, StructureFinder>builder()
            .timeOut(Duration.ofMinutes(2))
            .cleanup(Duration.ofMinutes(5))
            .softReference(false)
            .refresh(true)
            .build();
    }

    @Override
    public void shutDown()
    {
        cache.shutDown();
    }
}
