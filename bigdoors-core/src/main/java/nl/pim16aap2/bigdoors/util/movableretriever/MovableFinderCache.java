package nl.pim16aap2.bigdoors.util.movableretriever;

import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.data.cache.timed.TimedCache;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.movable.PermissionLevel;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.Duration;

@Singleton final class MovableFinderCache implements IRestartable
{
    private final Provider<MovableRetrieverFactory> movableRetrieverFactoryProvider;
    private final DatabaseManager databaseManager;
    private TimedCache<ICommandSender, MovableFinder> cache = TimedCache.emptyCache();

    @Inject MovableFinderCache(
        Provider<MovableRetrieverFactory> movableRetrieverFactoryProvider, RestartableHolder restartableHolder,
        DatabaseManager databaseManager)
    {
        this.movableRetrieverFactoryProvider = movableRetrieverFactoryProvider;
        this.databaseManager = databaseManager;
        restartableHolder.registerRestartable(this);
    }

    /**
     * Gets the {@link MovableFinder} for the given {@link ICommandSender} and the given input.
     * <p>
     * If no movable finder exists yet, a new one will be instantiated. If one does exist for the given
     * {@link ICommandSender}, the existing one will be updated to process
     *
     * @param commandSender
     *     The {@link ICommandSender} for which to retrieve or instantiate a {@link MovableFinder}.
     * @param input
     *     The input search query to process.
     * @param maxPermission
     *     The maximum permission (inclusive) of the movable owner of the movables to find. Does not apply if the
     *     command sender is not a player.
     * @return The {@link MovableFinder} mapped for the provided {@link ICommandSender}.
     */
    MovableFinder getMovableFinder(ICommandSender commandSender, String input, PermissionLevel maxPermission)
    {
        return cache.compute(commandSender, (sender, finder) ->
            finder == null ? newInstance(sender, input, maxPermission) : finder.processInput(input));
    }

    private MovableFinder newInstance(ICommandSender commandSender, String input, PermissionLevel maxPermission)
    {
        return new MovableFinder(movableRetrieverFactoryProvider.get(), databaseManager, commandSender, input,
                                 maxPermission);
    }

    @Override
    public synchronized void initialize()
    {
        cache = TimedCache.<ICommandSender, MovableFinder>builder()
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
