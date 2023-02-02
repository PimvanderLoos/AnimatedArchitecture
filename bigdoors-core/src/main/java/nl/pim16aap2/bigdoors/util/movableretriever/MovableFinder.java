package nl.pim16aap2.bigdoors.util.movableretriever;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.data.cache.RollingCache;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.PermissionLevel;
import nl.pim16aap2.bigdoors.util.Util;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Represents a class that can be used to retrieve movables when only partial data is provided.
 * <p>
 * For example, given a set of movables with the names "myDoor", "myPortcullis", "flagThatIsMine", this can be used to
 * retrieve "myDoor" and "myPortcullis" from a search input of "my" (provided both movables are owned by the
 * {@link ICommandSender} responsible for the search request.)
 *
 * @author Pim
 */
@Flogger
public final class MovableFinder
{
    /**
     * The default timeout (in seconds) when waiting for the cache to become available.
     * <p>
     * The cache will be unavailable until the database has returned the required values.
     */
    static final long DEFAULT_TIMEOUT = 2;

    private final MovableRetrieverFactory movableRetrieverFactory;
    private final DatabaseManager databaseManager;
    private final ICommandSender commandSender;
    private final PermissionLevel maxPermission;
    private final Object msg = new Object();

    @GuardedBy("this")
    private final RollingCache<HistoryItem> history = new RollingCache<>(3);
    @GuardedBy("this")
    private final Deque<String> postponedInputs = new ArrayDeque<>();
    @GuardedBy("this")
    private @Nullable List<MinimalMovableDescription> cache;
    @GuardedBy("this")
    private String lastInput;
    @GuardedBy("this")
    private @Nullable CompletableFuture<List<MinimalMovableDescription>> searcher = null;

    MovableFinder(
        MovableRetrieverFactory movableRetrieverFactory, DatabaseManager databaseManager, ICommandSender commandSender,
        String input, PermissionLevel maxPermission)
    {
        this.movableRetrieverFactory = movableRetrieverFactory;
        this.databaseManager = databaseManager;
        this.commandSender = commandSender;
        lastInput = input;
        this.maxPermission = maxPermission;
        restartSearch(input);
    }

    MovableFinder(
        MovableRetrieverFactory movableRetrieverFactory, DatabaseManager databaseManager, ICommandSender commandSender,
        String input)
    {
        this(movableRetrieverFactory, databaseManager, commandSender, input, PermissionLevel.CREATOR);
    }

    synchronized MovableFinder processInput(String input)
    {
        if (input.length() <= lastInput.length())
        {
            if (input.equalsIgnoreCase(lastInput))
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
     * See {@link #getMovableIdentifiersIfAvailable(boolean)}.
     */
    public Optional<Set<String>> getMovableIdentifiersIfAvailable()
    {
        return getMovableIdentifiersIfAvailable(false);
    }

    /**
     * @param fullMatch
     *     When true, only the entries that have a complete match are returned. E.g. for an input of "door", "door"
     *     would be returned, but "door1" would not. Gets the UIDs of all the movables that have been found so far.
     *     Defaults to false.
     * @return A list of all movable identifiers (i.e. names or UIDs) of movables that have been found if the search has
     * returned any results. If the search is still active on another thread, this will return an empty optional.
     */
    public synchronized Optional<Set<String>> getMovableIdentifiersIfAvailable(boolean fullMatch)
    {
        if (cache == null)
            return Optional.empty();
        return Optional.of(getIdentifiers(cache, fullMatch ? lastInput : null));
    }

    /**
     * See {@link #getMovableIdentifiers(boolean)}.
     */
    public CompletableFuture<Set<String>> getMovableIdentifiers()
    {
        return getMovableIdentifiers(false);
    }

    /**
     * @param fullMatch
     *     When true, only the entries that have a complete match are returned. E.g. for an input of "door", "door"
     *     would be returned, but "door1" would not. Gets the UIDs of all the movables that have been found so far.
     *     Defaults to false.
     * @return A list of all identifiers that match the given search parameters. An identifier here can refer to both
     * the UIDs or the names of the movables that match the query.
     */
    public synchronized CompletableFuture<Set<String>> getMovableIdentifiers(boolean fullMatch)
    {
        final String lastInput0 = lastInput;
        return waitForDescriptions().thenApply(descs -> getIdentifiers(descs, fullMatch ? lastInput0 : null));
    }

    /**
     * See {@link #getMovableUIDs(boolean)}.
     */
    public Optional<LongSet> getMovableUIDs()
    {
        return getMovableUIDs(false);
    }

    /**
     * @param fullMatch
     *     When true, only the entries that have a complete match are returned. E.g. for an input of "door", "door"
     *     would be returned, but "door1" would not. Gets the UIDs of all the movables that have been found so far.
     *     Defaults to false.
     * @return All movable UIDs that have been found if any have been found.
     */
    public synchronized Optional<LongSet> getMovableUIDs(boolean fullMatch)
    {
        if (cache == null)
            return Optional.empty();
        return Optional.of(getUIDs(cache, fullMatch ? lastInput : null));
    }

    /**
     * See {@link #getMovables(boolean)}, with fullMatch set to false.
     */
    public CompletableFuture<List<AbstractMovable>> getMovables()
    {
        return getMovables(false);
    }

    /**
     * See {@link #getMovables(boolean)}.
     * <p>
     * Returns the result as a {@link MovableRetriever}.
     */
    public MovableRetriever asRetriever(boolean fullMatch)
    {
        return MovableRetrieverFactory.ofMovables(getMovables(fullMatch));
    }

    /**
     * See {@link #getMovables()}.
     * <p>
     * Returns the result as a {@link MovableRetriever}.
     */
    public MovableRetriever asRetriever()
    {
        return MovableRetrieverFactory.ofMovables(getMovables());
    }

    /**
     * @param fullMatch
     *     When true, only the entries that have a complete match are returned. E.g. for an input of "door", "door"
     *     would be returned, but "door1" would not. Gets the UIDs of all the movables that have been found so far.
     *     Defaults to false.
     * @return A list of all movables that match the given search parameters.
     */
    public synchronized CompletableFuture<List<AbstractMovable>> getMovables(boolean fullMatch)
    {
        final String lastInput0 = lastInput;
        return waitForDescriptions().thenCompose(
            descriptions ->
            {
                final List<MinimalMovableDescription> targetList = filterIfNeeded(descriptions, lastInput0, fullMatch);

                @SuppressWarnings("unchecked") final CompletableFuture<Optional<AbstractMovable>>[] retrieved =
                    (CompletableFuture<Optional<AbstractMovable>>[]) new CompletableFuture[targetList.size()];

                for (int idx = 0; idx < targetList.size(); ++idx)
                    retrieved[idx] = movableRetrieverFactory.of(targetList.get(idx).uid).getMovable(commandSender);
                return Util.getAllCompletableFutureResults(retrieved)
                           .thenApply(lst -> lst.stream().flatMap(Optional::stream).toList());
            });
    }

    private static List<MinimalMovableDescription> filterIfNeeded(
        List<MinimalMovableDescription> lst, String lastInput, boolean fullMatch)
    {
        if (fullMatch)
            return lst.stream().filter(desc -> desc.id.equalsIgnoreCase(lastInput)).toList();
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
        if (cache == null)
        {
            rollBackDelayedOperations(input);
            return;
        }

        final int index = inputHistoryIndex(input);
        if (index < 0)
        {
            restartSearch(input);
            return;
        }

        // Remove all items that are newer than the
        // item from the history.
        for (int idx = history.size() - 1; idx >= index; --idx)
        {
            final HistoryItem removed = history.removeLast();
            cache.addAll(removed.getItems());
            lastInput = removed.input;
        }
        updateCache(input);
    }

    /**
     * Tries to roll back postponed inputs (See {@link #postponedInputs}).
     * <p>
     * This can be used to roll back the inputs before they have been applied, which happens if more inputs are supplied
     * before the cache has been seeded.
     *
     * @param input
     *     The current input to try to roll back to.
     */
    @GuardedBy("this")
    void rollBackDelayedOperations(String input)
    {
        while (!postponedInputs.isEmpty())
        {
            final String postponedInput = postponedInputs.peekLast();
            if (!startsWith(postponedInput, input))
                postponedInputs.removeLast();
            else
                break;
        }

        if (postponedInputs.isEmpty())
            restartSearch(input);
        else if (!input.equalsIgnoreCase(postponedInputs.peekLast()))
            postponedInputs.addLast(input);
    }

    /**
     * @param base
     *     The base String to use.
     * @param test
     *     The String to compare against the base String.
     * @return True if the test String has the base string as its base.
     */
    static boolean startsWith(String base, String test)
    {
        return test.toLowerCase(Locale.ROOT).startsWith(base.toLowerCase(Locale.ROOT));
    }

    /**
     * Searches for an input query in the history.
     *
     * @param input
     *     The input query to search for in the history.
     * @return The index of the item in the history or -1 if it could not be found.
     */
    @GuardedBy("this")
    private int inputHistoryIndex(String input)
    {
        for (int idx = history.size() - 1; idx >= 0; --idx)
            if (startsWith(history.get(idx).getInput(), input))
                return idx;
        return -1;
    }

    /**
     * Updates the cache using the provided input.
     * <p>
     * If the cache is already available, the input will be used to immediately filter the current cache. If the cache
     * does not exist yet, the input will be stored as postponed input and applied when the cache becomes available.
     *
     * @param input
     *     The input to use for updating the cache.
     */
    @GuardedBy("this")
    private void updateCache(String input)
    {
        if (cache == null)
            this.postponedInputs.addLast(input);
        else
            applyFilter(input);
    }

    /**
     * Applies a filter to the current cache.
     * <p>
     * Items that are removed from the cache are moved into {@link #history} using the new input as its key.
     * <p>
     * Note that this method assumes that the cache already exists! If it might not, use {@link #updateCache(String)}
     * instead.
     *
     * @param input
     *     The new input. All cache entries whose identifiers do not start with this String are removed from the cache.
     */
    @GuardedBy("this")
    private void applyFilter(String input)
    {
        final Map<Boolean, List<MinimalMovableDescription>> filtered =
            Util.requireNonNull(cache, "Cache").stream()
                .collect(Collectors.partitioningBy(desc -> startsWith(input, desc.id)));
        history.add(new HistoryItem(input, Objects.requireNonNull(filtered.get(false))));
        cache = Objects.requireNonNull(filtered.get(true));
    }

    /**
     * Sets the cache to a new list.
     * <p>
     * After setting the value, a new history item is created and all postponed inputs stored in
     * {@link #postponedInputs} are applied.
     * <p>
     * Once the new cache value has been processed, all threads waiting for {@link #msg} are notified.
     *
     * @param lst
     *     The new list to use as cache.
     * @param input
     *     The input used to create the cache. This will be used for the first history item.
     */
    private void setCache(List<MinimalMovableDescription> lst, String input)
    {
        // We do not want to have the lock on this object when notifying the object, as that may result
        // in a deadlock when the notified object tries to obtain the same lock when using #isCacheAvailable().
        synchronized (this)
        {
            this.cache = lst;
            history.add(new HistoryItem(input, Collections.emptyList()));
            while (!postponedInputs.isEmpty())
                applyFilter(postponedInputs.removeFirst());
        }
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
        if (searcher != null && !searcher.isDone())
            searcher.cancel(true);
        postponedInputs.clear();
        this.cache = null;
        this.history.clear();
        this.lastInput = input;
        this.searcher = getNewMovableIdentifiers(input);
        this.searcher.thenAccept(lst -> this.setCache(lst, input))
                     .exceptionally(
                         t ->
                         {
                             // It can happen that the searcher was cancelled because it received incompatible
                             // input before finishing. This isn't important.
                             final Level lvl = t.getCause() instanceof CancellationException ?
                                               Level.FINEST : Level.SEVERE;
                             log.at(lvl).withCause(t).log();
                             return null;
                         });
    }

    /**
     * Retrieves a new set of movable identifiers from the database.
     *
     * @param input
     *     The input to use as search query in the database. This can be its (partial) name or its (partial) UID.
     * @return The new set of movable identifiers.
     */
    private CompletableFuture<List<MinimalMovableDescription>> getNewMovableIdentifiers(String input)
    {
        final @Nullable IPPlayer player = commandSender.getPlayer().orElse(null);
        return databaseManager.getIdentifiersFromPartial(input, player, maxPermission).thenApply(
            ids ->
            {
                final List<MinimalMovableDescription> descriptions = new ArrayList<>(ids.size());
                for (final var id : ids)
                {
                    final String targetId = Util.isNumerical(input) ? String.valueOf(id.uid()) : id.name();
                    descriptions.add(new MinimalMovableDescription(id.uid(), targetId));
                }
                return descriptions;
            });
    }

    /**
     * Extracts a set of identifiers from descriptions. Only the identifiers being used in the finder are used, so these
     * can refer either to the UIDs or the names of the movables (though never a mix).
     *
     * @param descriptions
     *     The descriptions from which to extract the identifiers.
     * @param lastInput
     *     The last available input to compare the identifiers to. If this value is provided, only identifiers that
     *     fully match this value are included.
     * @return The identifiers as extracted from the provided descriptions.
     */
    private static Set<String> getIdentifiers(
        Collection<MinimalMovableDescription> descriptions, @Nullable String lastInput)
    {
        if (lastInput != null)
            return new LinkedHashSet<>(getFullMatches(descriptions, lastInput))
                .stream().map(MinimalMovableDescription::id).collect(Collectors.toSet());
        final LinkedHashSet<String> ids = new LinkedHashSet<>(descriptions.size());
        descriptions.forEach(desc -> ids.add(desc.id));
        return ids;
    }

    /**
     * Extracts a set of UIDs from descriptions.
     *
     * @param descriptions
     *     The descriptions from which to extract the UIDs.
     * @param lastInput
     *     The last available input to compare the identifiers to. If this value is provided, only the UIDs of the
     *     associated identifiers that fully match this value are included.
     * @return The UIDs as extracted from the provided descriptions.
     */
    private static LongSet getUIDs(Collection<MinimalMovableDescription> descriptions, @Nullable String lastInput)
    {
        if (lastInput != null)
            return LongLinkedOpenHashSet.toSet(new LinkedHashSet<>(getFullMatches(descriptions, lastInput))
                                                   .stream().mapToLong(MinimalMovableDescription::uid));
        final LongSet ids = new LongLinkedOpenHashSet();
        descriptions.forEach(desc -> ids.add(desc.uid));
        return ids;
    }

    /**
     * @param descriptions
     *     The descriptions for which to get the entries that fully match the target.
     * @param matchTo
     *     The target String to compare the identifiers of the entries to.
     * @return A list of minimal descriptions whose entries match the target String.
     */
    private static List<MinimalMovableDescription> getFullMatches(
        Collection<MinimalMovableDescription> descriptions, String matchTo)
    {
        return descriptions.stream().filter(desc -> desc.id.equalsIgnoreCase(matchTo)).toList();
    }

    private synchronized boolean isCacheAvailable()
    {
        return cache != null;
    }

    @GuardedBy("this")
    private CompletableFuture<List<MinimalMovableDescription>> waitForDescriptions()
    {
        if (cache != null)
            return CompletableFuture.completedFuture(cache);

        final CompletableFuture<List<MinimalMovableDescription>> result = new CompletableFuture<>();
        CompletableFuture.runAsync(
            () ->
            {
                try
                {
                    synchronized (msg)
                    {
                        final long deadline = System.nanoTime() + Duration.ofSeconds(DEFAULT_TIMEOUT).toNanos();

                        while (System.nanoTime() < deadline && !isCacheAvailable())
                        {
                            final long waitTime = Duration.ofNanos(deadline - System.nanoTime()).toMillis();
                            if (waitTime > 0)
                                msg.wait(waitTime);
                        }
                        if (!isCacheAvailable() || System.nanoTime() > deadline)
                            throw new TimeoutException("Timed out waiting for list of movable descriptions.");
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

    synchronized List<String> getPostponedInputs()
    {
        return new ArrayList<>(postponedInputs);
    }

    @Getter
    @AllArgsConstructor
    @ToString
    static final class HistoryItem
    {
        private final String input;
        private final List<MinimalMovableDescription> items;
    }

    /**
     * @param uid
     *     The UID of the movable.
     * @param id
     *     The target identifier of the movable that we're looking for. This may either be its UID or its name.
     */
    public record MinimalMovableDescription(long uid, String id) {}
}