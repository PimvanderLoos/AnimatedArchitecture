package nl.pim16aap2.bigdoors.util.doorretriever;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.data.RollingCache;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Represents a class that can be used to retrieve doors when only partial data is provided.
 * <p>
 * For example, given a set of doors with the names "myDoor", "myPortcullis", "flagThatIsMine", this can be used to
 * retrieve "myDoor" and "myPortcullis" from a search input of "my" (provided both doors are owned by the {@link
 * ICommandSender} responsible for the search request.)
 *
 * @author Pim
 */
public final class DoorFinder
{
    /**
     * The default timeout (in seconds) when waiting for the cache to become available.
     * <p>
     * The cache will be unavailable until the database has returned the required values.
     */
    static final long DEFAULT_TIMEOUT = 2;

    private final DoorRetrieverFactory doorRetrieverFactory;
    private final DatabaseManager databaseManager;
    private final ICommandSender commandSender;
    private final Object msg = new Object();

    @GuardedBy("this")
    private final RollingCache<HistoryItem> history = new RollingCache<>(3);
    @GuardedBy("this")
    private final List<Runnable> delayedOperations = new ArrayList<>();
    @GuardedBy("this")
    private @Nullable List<MinimalDoorDescription> cache;
    @GuardedBy("this")
    private String lastInput;
    @GuardedBy("this")
    private @Nullable CompletableFuture<Void> searcher = null;

    DoorFinder(
        DoorRetrieverFactory doorRetrieverFactory, DatabaseManager databaseManager, ICommandSender commandSender,
        String input)
    {
        this.doorRetrieverFactory = doorRetrieverFactory;
        this.databaseManager = databaseManager;
        this.commandSender = commandSender;
        lastInput = input;
        restartSearch(input);
    }

    synchronized DoorFinder processInput(String input)
    {
        if (input.length() <= lastInput.length())
        {
            if (input.equals(lastInput))
                return this;
            rollBack(input);
        }
        else
        {
            updateCache(input);
        }

        lastInput = input;
        return this;
    }

    /**
     * See {@link #getDoorIdentifiersIfAvailable(boolean)}.
     */
    public Optional<Set<String>> getDoorIdentifiersIfAvailable()
    {
        return getDoorIdentifiersIfAvailable(false);
    }

    /**
     * @param fullMatch
     *     When true, only the entries that have a complete match are returned. E.g. for an input of "door", "door"
     *     would be returned, but "door1" would not. Gets the UIDs of all the doors that have been found so far.
     *     Defaults to false.
     * @return A list of all door identifiers (i.e. names or UIDs) of doors that have been found if the search has
     * returned any results. If the search is still active on another thread, this will return an empty optional.
     */
    public synchronized Optional<Set<String>> getDoorIdentifiersIfAvailable(boolean fullMatch)
    {
        if (cache == null)
            return Optional.empty();
        return Optional.of(getIdentifiers(cache, fullMatch ? lastInput : null));
    }

    /**
     * See {@link #getDoorIdentifiers(boolean)}.
     */
    public CompletableFuture<Set<String>> getDoorIdentifiers()
    {
        return getDoorIdentifiers(false);
    }

    /**
     * @param fullMatch
     *     When true, only the entries that have a complete match are returned. E.g. for an input of "door", "door"
     *     would be returned, but "door1" would not. Gets the UIDs of all the doors that have been found so far.
     *     Defaults to false.
     * @return A list of all identifiers that match the given search parameters. An identifier here can refer to both
     * the UIDs or the names of the doors that match the query.
     */
    public synchronized CompletableFuture<Set<String>> getDoorIdentifiers(boolean fullMatch)
    {
        final String lastInput0 = lastInput;
        return waitForDescriptions().thenApply(descs -> getIdentifiers(descs, fullMatch ? lastInput0 : null));
    }

    /**
     * See {@link #getDoorUIDs(boolean)}.
     */
    public Optional<Set<Long>> getDoorUIDs()
    {
        return getDoorUIDs(false);
    }

    /**
     * @param fullMatch
     *     When true, only the entries that have a complete match are returned. E.g. for an input of "door", "door"
     *     would be returned, but "door1" would not. Gets the UIDs of all the doors that have been found so far.
     *     Defaults to false.
     * @return All door UIDs that have been found if any have been found.
     */
    public synchronized Optional<Set<Long>> getDoorUIDs(boolean fullMatch)
    {
        if (cache == null)
            return Optional.empty();
        return Optional.of(getUIDs(cache, fullMatch ? lastInput : null));
    }

    /**
     * See {@link #getDoors(boolean)}.
     */
    public CompletableFuture<List<AbstractDoor>> getDoors()
    {
        return getDoors(false);
    }

    /**
     * @param fullMatch
     *     When true, only the entries that have a complete match are returned. E.g. for an input of "door", "door"
     *     would be returned, but "door1" would not. Gets the UIDs of all the doors that have been found so far.
     *     Defaults to false.
     * @return A list of all doors that match the given search parameters.
     */
    public synchronized CompletableFuture<List<AbstractDoor>> getDoors(boolean fullMatch)
    {
        final String lastInput0 = lastInput;
        return waitForDescriptions().thenCompose(
            descriptions ->
            {
                final List<MinimalDoorDescription> targetList = filterIfNeeded(descriptions, lastInput0, fullMatch);

                @SuppressWarnings("unchecked") final CompletableFuture<Optional<AbstractDoor>>[] retrieved =
                    (CompletableFuture<Optional<AbstractDoor>>[]) new CompletableFuture[targetList.size()];

                for (int idx = 0; idx < targetList.size(); ++idx)
                    retrieved[idx] = doorRetrieverFactory.of(targetList.get(idx).uid).getDoor(commandSender);
                return Util.getAllCompletableFutureResults(retrieved)
                           .thenApply(lst -> lst.stream().flatMap(Optional::stream).toList());
            });
    }

    private static List<MinimalDoorDescription> filterIfNeeded(
        List<MinimalDoorDescription> lst, String lastInput, boolean fullMatch)
    {
        if (fullMatch)
            return lst.stream().filter(desc -> desc.id.equals(lastInput)).toList();
        return lst;
    }

    /**
     * Tries to roll back to a previous history item that contained the given input as query.
     * <p>
     * If no history item exists for the given input, {@link #restartSearch(String)} is used to start over.
     *
     * @param input
     *     The input query to look for in the history.
     */
    @GuardedBy("this")
    void rollBack(String input)
    {
        final int index = inputHistoryIndex(input);
        if (index == -1)
        {
            restartSearch(input);
            return;
        }

        Util.requireNonNull(cache, "Cache");
        // Remove all items that are newer than the
        // item from the history.
        for (int idx = 0; idx < index; ++idx)
        {
            final HistoryItem removed = history.removeLast();
            cache.addAll(removed.getItems());
        }
        updateCache(input);
    }

    /**
     * Searches for an input query in the history.
     *
     * @param input
     *     The input query to search for in the history.
     * @return The index of the item in the history or -1 if it could not be found.
     */
    @GuardedBy("this")
    int inputHistoryIndex(String input)
    {
        for (int idx = 0; idx < history.size(); ++idx)
            if (input.startsWith(history.get(idx).getInput()))
                return idx;
        return -1;
    }

    @GuardedBy("this")
    void updateCache(String input)
    {
        if (cache == null)
            this.delayedOperations.add(() -> applyFilter(input));
        else
            applyFilter(input);
    }

    synchronized void applyFilter(String input)
    {
        final Map<Boolean, List<MinimalDoorDescription>> filtered =
            Util.requireNonNull(cache, "Cache").stream()
                .collect(Collectors.partitioningBy(desc -> desc.id.startsWith(input)));
        history.add(new HistoryItem(input, Objects.requireNonNull(filtered.get(false))));
        cache = Objects.requireNonNull(filtered.get(true));
    }

    private synchronized void setCache(List<MinimalDoorDescription> lst)
    {
        this.cache = lst;
        for (final var op : delayedOperations)
            op.run();
        delayedOperations.clear();
        this.searcher = null;
        synchronized (msg)
        {
            msg.notifyAll();
        }
    }

    /**
     * Resets everything and starts the search over from the beginning.
     *
     * @param input
     *     The search query.
     */
    private synchronized void restartSearch(String input)
    {
        if (searcher != null)
            searcher.cancel(true);
        delayedOperations.clear();
        this.cache = null;
        this.history.clear();
        this.searcher = getNewDoorIdentifiers(input).thenAccept(this::setCache);
    }

    private CompletableFuture<List<MinimalDoorDescription>> getNewDoorIdentifiers(String input)
    {
        final boolean isNumerical = Util.parseLong(input).isPresent();
        return databaseManager.getIdentifiersFromPartial(input, commandSender.getPlayer().orElse(null)).thenApply(
            ids ->
            {
                final List<MinimalDoorDescription> descriptions = new ArrayList<>(ids.size());
                for (final var id : ids)
                {
                    final String targetId = isNumerical ? String.valueOf(id.uid()) : id.name();
                    descriptions.add(new MinimalDoorDescription(id.uid(), targetId));
                }
                return descriptions;
            });
    }

    private static Set<String> getIdentifiers(
        Collection<MinimalDoorDescription> descriptions, @Nullable String lastInput)
    {
        if (lastInput != null)
            return new LinkedHashSet<>(getFullMatch(descriptions, lastInput))
                .stream().map(MinimalDoorDescription::id).collect(Collectors.toSet());
        final LinkedHashSet<String> ids = new LinkedHashSet<>();
        descriptions.forEach(desc -> ids.add(desc.id));
        return ids;
    }

    private static Set<Long> getUIDs(Collection<MinimalDoorDescription> descriptions, @Nullable String lastInput)
    {
        if (lastInput != null)
            return new LinkedHashSet<>(getFullMatch(descriptions, lastInput))
                .stream().map(MinimalDoorDescription::uid).collect(Collectors.toSet());
        final LinkedHashSet<Long> ids = new LinkedHashSet<>();
        descriptions.forEach(desc -> ids.add(desc.uid));
        return ids;
    }

    private static List<MinimalDoorDescription> getFullMatch(
        Collection<MinimalDoorDescription> descriptions, String matchTo)
    {
        return descriptions.stream().filter(desc -> desc.id.equals(matchTo)).toList();
    }

    private synchronized boolean isCacheSet()
    {
        return cache != null;
    }

    @GuardedBy("this")
    private CompletableFuture<List<MinimalDoorDescription>> waitForDescriptions()
    {
        if (cache != null)
            return CompletableFuture.completedFuture(cache);

        final CompletableFuture<List<MinimalDoorDescription>> result = new CompletableFuture<>();
        CompletableFuture.runAsync(
            () ->
            {
                try
                {
                    synchronized (msg)
                    {
                        final long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
                        while (System.nanoTime() < deadline && !isCacheSet())
                        {
                            final long waitTime = Duration.ofNanos(deadline - System.nanoTime()).toMillis();
                            if (waitTime > 0)
                                msg.wait(waitTime);
                        }
                        if (!isCacheSet() || System.nanoTime() > deadline)
                            throw new TimeoutException("Timed out waiting for list of door descriptions.");
                    }

                    synchronized (this)
                    {
                        result.complete(Util.requireNonNull(cache, "Cache"));
                    }
                }
                catch (InterruptedException e)
                {
                    result.completeExceptionally(e);
                    Thread.currentThread().interrupt();
                }
                catch (Throwable t)
                {
                    result.completeExceptionally(t);
                }
            });
        return result.exceptionally(t -> Util.exceptionally(t, Collections.emptyList()));
    }

    @Getter
    @AllArgsConstructor
    static final class HistoryItem
    {
        private final String input;
        private final List<MinimalDoorDescription> items;
    }

    /**
     * @param uid
     *     The UID of the door.
     * @param id
     *     The target identifier of the door that we're looking for. This may either be its UID or its name.
     */
    public record MinimalDoorDescription(long uid, String id) {}
}
