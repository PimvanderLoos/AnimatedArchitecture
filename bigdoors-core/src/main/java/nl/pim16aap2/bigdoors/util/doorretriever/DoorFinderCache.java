package nl.pim16aap2.bigdoors.util.doorretriever;

import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.cache.TimedCache;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.Duration;

@Singleton final class DoorFinderCache implements IRestartable
{
    private final Provider<DoorRetrieverFactory> doorRetrieverFactoryProvider;
    private final DatabaseManager databaseManager;
    private TimedCache<ICommandSender, DoorFinder> cache = TimedCache.emptyCache();

    @Inject DoorFinderCache(
        Provider<DoorRetrieverFactory> doorRetrieverFactoryProvider, RestartableHolder restartableHolder,
        DatabaseManager databaseManager)
    {
        this.doorRetrieverFactoryProvider = doorRetrieverFactoryProvider;
        this.databaseManager = databaseManager;
        restartableHolder.registerRestartable(this);
    }

    /**
     * Gets the {@link DoorFinder} for the given {@link ICommandSender} and the given input.
     * <p>
     * If no door finder exists yet, a new one will be instantiated. If one does exist for the given {@link
     * ICommandSender}, the existing one will be updated to process
     *
     * @param commandSender
     *     The {@link ICommandSender} for which to retrieve or instantiate a {@link DoorFinder}.
     * @param input
     *     The input search query to process.
     * @return The {@link DoorFinder} mapped for the provided {@link ICommandSender}.
     */
    public DoorFinder getDoorFinder(ICommandSender commandSender, String input)
    {
        return cache.compute(commandSender, (sender, finder) ->
            finder == null ? newInstance(sender, input) : finder.processInput(input));
    }

    private DoorFinder newInstance(ICommandSender commandSender, String input)
    {
        return new DoorFinder(doorRetrieverFactoryProvider.get(), databaseManager, commandSender, input);
    }

    @Override
    public synchronized void initialize()
    {
        cache = TimedCache.<ICommandSender, DoorFinder>builder()
                          .duration(Duration.ofMinutes(2))
                          .cleanup(Duration.ofMinutes(5))
                          .softReference(false)
                          .refresh(true)
                          .build();
    }

    @Override
    public synchronized void shutDown()
    {
        cache.shutDown();
    }
}
