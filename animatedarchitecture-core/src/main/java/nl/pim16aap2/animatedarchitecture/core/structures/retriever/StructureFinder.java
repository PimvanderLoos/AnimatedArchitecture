package nl.pim16aap2.animatedarchitecture.core.structures.retriever;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Locked;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.data.cache.RollingCache;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.util.CollectionsUtil;
import nl.pim16aap2.animatedarchitecture.core.util.FutureUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.Util;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Represents a class that can be used to retrieve structures when only partial data is provided.
 * <p>
 * For example, given a set of structures with the names "myDoor", "myPortcullis", "flagThatIsMine", this can be used to
 * retrieve "myDoor" and "myPortcullis" from a search input of "my" (provided both structures are owned by the
 * {@link ICommandSender} responsible for the search request.)
 * <p>
 * This class uses a {@link nl.pim16aap2.animatedarchitecture.core.data.cache.RollingCache} to keep track of provided
 * inputs. This allows both narrowing the search results from new input and rolling back to previous states (e.g. when
 * fixing typos) without causing new database requests. For example, assume we have an input of "My" and some returned
 * results "MyPortcullis" and "MyWindmill". If the next input is then "MyW", the finder will return only "MyWindmill"
 * without querying the database again.
 * <p>
 * The finder uses the command sender to determine which structures are visible to the user if the command sender is a
 * player. If the command sender is the console, all structures are visible.
 */
@Flogger
@ThreadSafe
public final class StructureFinder
{
    /**
     * The default value for the full match parameter.
     * <p>
     * This is used when retrieving the results of the search. When true, only the entries that have a complete match
     * are returned. E.g. for an input of "door", "door" would be returned, but "door1" would not.
     * <p>
     * When false, all entries that have the input as a prefix are returned. E.g. for an input of "door", both "door"
     * and "door1" would be returned.
     */
    private static final boolean DEFAULT_FULL_MATCH = false;

    /**
     * The default timeout (in seconds) when waiting for the cache to become available.
     * <p>
     * The cache will be unavailable until the database has returned the required values.
     */
    static final long DEFAULT_TIMEOUT = 2;

    /**
     * The lock used to synchronize access to the cache and other fields.
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * The factory used to create structure retrievers when converting the results of the search to structures.
     */
    private final StructureRetrieverFactory structureRetrieverFactory;

    /**
     * The manager used to interact with the database.
     * <p>
     * This is used to retrieve the structure identifiers from the database.
     */
    private final DatabaseManager databaseManager;

    /**
     * The command sender that is used to determine which structures are visible to the user.
     * <p>
     * If the command sender is a player, only structures that are visible to the player are returned. If the command
     * sender is the console, all structures are returned.
     */
    private final ICommandSender commandSender;

    /**
     * The maximum permission level the player can have to see the structures.
     */
    private final PermissionLevel maxPermission;

    /**
     * The object used to notify threads waiting for the cache to become available.
     */
    private final Object msg = new Object();

    /**
     * The history of inputs and their associated items.
     * <p>
     * It is used to roll back to previous states when the user wants to fix a typo. E.g. if the user searches for
     * "MyE", a structure named "MyWindmill" will be removed from the cache. If the user then reverts the input to "My",
     * "MyWindmill" will be added back to the cache.
     */
    @GuardedBy("lock")
    private final RollingCache<HistoryItem> history = new RollingCache<>(3);

    /**
     * The postponed inputs that have not yet been applied to the cache.
     * <p>
     * This is used to store inputs that have been provided before the cache has been seeded. This can happen if the
     * user provides multiple inputs before the cache has been seeded with results from the database.
     */
    @GuardedBy("lock")
    private final Deque<String> postponedInputs = new ArrayDeque<>();

    /**
     * The cache of structure descriptions.
     * <p>
     * The cache requires an initial set of structure descriptions to be seeded. This is done by querying the database
     * with the initial input. Once the cache has been seeded, it can be used to filter the results based on new input.
     * <p>
     * As such, the cache will not be available immediately after creating an instance of this class. User input will be
     * appended to {@link #postponedInputs} to be applied once the cache has been seeded.
     */
    @GuardedBy("lock")
    private @Nullable List<MinimalStructureDescription> cache;

    /**
     * The last input that was used to search for structures.
     * <p>
     * This is the input that was used to create the current cache.
     * <p>
     * This is used to determine if the new input is a continuation of the last input or if it is a new search query.
     */
    @GuardedBy("lock")
    private String lastInput;

    /**
     * The future that is used to retrieve the structure identifiers.
     * <p>
     * This is used to cancel the search if a new search is started before the current search has finished.
     */
    @GuardedBy("lock")
    private @Nullable CompletableFuture<List<MinimalStructureDescription>> searcher = null;

    StructureFinder(
        StructureRetrieverFactory structureRetrieverFactory,
        DatabaseManager databaseManager,
        ICommandSender commandSender,
        String input,
        PermissionLevel maxPermission)
    {
        this.structureRetrieverFactory = structureRetrieverFactory;
        this.databaseManager = databaseManager;
        this.commandSender = commandSender;
        lastInput = input;
        this.maxPermission = maxPermission;
        restartSearch(input);
    }

    StructureFinder(
        StructureRetrieverFactory structureRetrieverFactory,
        DatabaseManager databaseManager,
        ICommandSender commandSender,
        String input)
    {
        this(structureRetrieverFactory, databaseManager, commandSender, input, PermissionLevel.CREATOR);
    }

    /**
     * Processes the given input.
     * <p>
     * If the new input starts with the last input, the new input is used to narrow down the search results.
     * <p>
     * If the new input does not start with the last input, an attempt is made to roll back the search results to a
     * previous state where the last input was a prefix of the new input. If this is not possible, the search is
     * restarted.
     *
     * @param input
     *     The input to process.
     * @return The current instance of this class.
     */
    @Locked.Write("lock")
    public StructureFinder processInput(String input)
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
     * Tries to get all identifiers of structures that have been found, if any.
     * <p>
     * This method will return an empty optional if the search is still active on another thread.
     * <p>
     * This method uses the default value for fullMatch. See {@link #DEFAULT_FULL_MATCH}.
     * <p>
     * This is a shortcut for {@link #getStructureIdentifiersIfAvailable(boolean)}.
     *
     * @return A list of all structure identifiers (i.e. names or UIDs) of structures that have been found if the search
     * has returned any results. If the search is still active on another thread, this will return an empty optional.
     */
    public Optional<Set<String>> getStructureIdentifiersIfAvailable()
    {
        return getStructureIdentifiersIfAvailable(DEFAULT_FULL_MATCH);
    }

    /**
     * Tries to get all identifiers of structures that have been found, if any.
     * <p>
     * This method will return an empty optional if the search is still active on another thread.
     *
     * @param fullMatch
     *     When true, only the entries that have a complete match are returned. E.g. for an input of "door", "door"
     *     would be returned, but "door1" would not. Gets the UIDs of all the structures that have been found so far.
     *     Defaults to false.
     * @return A list of all structure identifiers (i.e. names or UIDs) of structures that have been found if the search
     * has returned any results. If the search is still active on another thread, this will return an empty optional.
     */
    @Locked.Read("lock")
    public Optional<Set<String>> getStructureIdentifiersIfAvailable(boolean fullMatch)
    {
        if (cache == null)
            return Optional.empty();
        return Optional.of(getIdentifiers(cache, fullMatch ? lastInput : null));
    }

    /**
     * Gets the identifiers of all the structures that have been found using the given search parameters.
     * <p>
     * This method uses the default value for fullMatch. See {@link #DEFAULT_FULL_MATCH}.
     * <p>
     * This is a shortcut for {@link #getStructureIdentifiers(boolean)}.
     *
     * @return A list of all identifiers that match the given search parameters. An identifier here can refer to both
     */
    public CompletableFuture<Set<String>> getStructureIdentifiers()
    {
        return getStructureIdentifiers(DEFAULT_FULL_MATCH);
    }

    /**
     * Gets the identifiers of all the structures that have been found using the given search parameters.
     *
     * @param fullMatch
     *     When true, only the entries that have a complete match are returned. E.g. for an input of "door", "door"
     *     would be returned, but "door1" would not. Gets the UIDs of all the structures that have been found so far.
     *     Defaults to false.
     * @return A list of all identifiers that match the given search parameters. An identifier here can refer to both
     * the UIDs or the names of the structures that match the query.
     */
    @Locked.Read("lock")
    public CompletableFuture<Set<String>> getStructureIdentifiers(boolean fullMatch)
    {
        final String lastInput0 = lastInput;
        return waitForDescriptions()
            .thenApply(descriptions -> getIdentifiers(descriptions, fullMatch ? lastInput0 : null));
    }

    /**
     * Gets the UIDs of all the structures that have been found using the given search parameters.
     * <p>
     * This method uses the default value for fullMatch. See {@link #DEFAULT_FULL_MATCH}.
     * <p>
     * This is a shortcut for {@link #getStructureUIDs(boolean)}.
     *
     * @return All structure UIDs that have been found if any have been found.
     */
    public Optional<LongSet> getStructureUIDs()
    {
        return getStructureUIDs(DEFAULT_FULL_MATCH);
    }

    /**
     * Gets the UIDs of all the structures that have been found using the given search parameters.
     *
     * @param fullMatch
     *     When true, only the entries that have a complete match are returned. E.g. for an input of "door", "door"
     *     would be returned, but "door1" would not. Gets the UIDs of all the structures that have been found so far.
     *     Defaults to false.
     * @return All structure UIDs that have been found if any have been found.
     */
    @Locked.Read("lock")
    public Optional<LongSet> getStructureUIDs(boolean fullMatch)
    {
        if (cache == null)
            return Optional.empty();
        return Optional.of(getUIDs(cache, fullMatch ? lastInput : null));
    }

    /**
     * Gets all structures that have been found using the given search parameters.
     * <p>
     * This method uses the default value for fullMatch. See {@link #DEFAULT_FULL_MATCH}.
     * <p>
     * This is a shortcut for {@link #getStructures(boolean)}.
     *
     * @return A list of all structures that match the given search parameters.
     */
    public CompletableFuture<List<AbstractStructure>> getStructures()
    {
        return getStructures(DEFAULT_FULL_MATCH);
    }

    /**
     * Gets all structures that have been found using the given search parameters as a {@link StructureRetriever}.
     *
     * @param fullMatch
     *     When true, only the entries that have a complete match are returned. E.g. for an input of "door", "door"
     *     would be returned, but "door1" would not. Gets the UIDs of all the structures that have been found so far.
     *     Defaults to false.
     * @return The result as a {@link StructureRetriever}.
     */
    public StructureRetriever asRetriever(boolean fullMatch)
    {
        return structureRetrieverFactory.ofStructures(getStructures(fullMatch));
    }

    /**
     * Gets all structures that have been found using the given search parameters as a {@link StructureRetriever}.
     * <p>
     * This method uses the default value for fullMatch. See {@link #DEFAULT_FULL_MATCH}.
     * <p>
     * This is a shortcut for {@link #asRetriever(boolean)}.
     *
     * @return The result as a {@link StructureRetriever}.
     */
    public StructureRetriever asRetriever()
    {
        return asRetriever(DEFAULT_FULL_MATCH);
    }

    /**
     * Gets all structures that have been found using the given search parameters as a list of
     * {@link AbstractStructure}s.
     *
     * @param fullMatch
     *     When true, only the entries that have a complete match are returned. E.g. for an input of "door", "door"
     *     would be returned, but "door1" would not. Gets the UIDs of all the structures that have been found so far.
     *     Defaults to false.
     * @return A list of all structures that match the given search parameters.
     */
    @Locked.Read("lock")
    public CompletableFuture<List<AbstractStructure>> getStructures(boolean fullMatch)
    {
        final String lastInput0 = lastInput;
        return waitForDescriptions().thenCompose(descriptions ->
        {
            final List<MinimalStructureDescription> targetList = filterIfNeeded(descriptions, lastInput0, fullMatch);

            @SuppressWarnings("unchecked") final CompletableFuture<Optional<AbstractStructure>>[] retrieved =
                (CompletableFuture<Optional<AbstractStructure>>[]) new CompletableFuture[targetList.size()];

            for (int idx = 0; idx < targetList.size(); ++idx)
                retrieved[idx] = structureRetrieverFactory
                    .of(targetList.get(idx).uid)
                    .getStructure(commandSender, maxPermission);

            return FutureUtil
                .getAllCompletableFutureResults(retrieved)
                .thenApply(lst -> lst.stream().flatMap(Optional::stream).toList());
        });
    }

    private static List<MinimalStructureDescription> filterIfNeeded(
        List<MinimalStructureDescription> lst,
        String lastInput,
        boolean fullMatch)
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
    @Locked.Write("lock")
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

        // Remove all items that are newer than the item from the history.
        for (int idx = history.size() - 1; idx >= index; --idx)
        {
            final HistoryItem removed = history.removeLast();
            cache = Collections.unmodifiableList(CollectionsUtil.concat(cache, removed.getItems()));
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
    @Locked.Write("lock")
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
     * Checks if a base String starts with a specific test String, ignoring case.
     *
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
    @Locked.Read("lock")
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
    @Locked.Write("lock")
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
    @Locked.Write("lock")
    private void applyFilter(String input)
    {
        final Map<Boolean, List<MinimalStructureDescription>> filtered =
            Util.requireNonNull(cache, "Cache")
                .stream()
                .collect(
                    Collectors.partitioningBy(
                        desc -> startsWith(input, desc.id),
                        Collectors.toUnmodifiableList())
                );

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
    private void setCache(List<MinimalStructureDescription> lst, String input)
    {
        // We do not want to have the lock on this object when notifying the object, as that may result
        // in a deadlock when the notified object tries to obtain the same lock when using #isCacheAvailable().
        lock.writeLock().lock();
        try
        {
            this.cache = lst;
            history.add(new HistoryItem(input, Collections.emptyList()));
            while (!postponedInputs.isEmpty())
                applyFilter(postponedInputs.removeFirst());
        }
        finally
        {
            lock.writeLock().unlock();
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
    @Locked.Write("lock")
    private void restartSearch(String input)
    {
        if (searcher != null && !searcher.isDone())
            searcher.cancel(true);
        postponedInputs.clear();
        this.cache = null;
        this.history.clear();
        this.lastInput = input;

        this.searcher = getNewStructureIdentifiers(input);
        this.searcher
            .thenAccept(lst -> this.setCache(lst, input))
            .exceptionally(t ->
            {
                // It can happen that the searcher was cancelled because it received incompatible
                // input before finishing. This isn't important.
                final Level lvl = t.getCause() instanceof CancellationException ? Level.FINEST : Level.SEVERE;
                log.at(lvl).withCause(t).log();
                return null;
            });
    }

    /**
     * Retrieves a new set of structure identifiers from the database.
     *
     * @param input
     *     The input to use as search query in the database. This can be its (partial) name or its (partial) UID.
     * @return The new set of structure identifiers.
     */
    private CompletableFuture<List<MinimalStructureDescription>> getNewStructureIdentifiers(String input)
    {
        final @Nullable IPlayer player = commandSender.getPlayer().orElse(null);
        return databaseManager
            .getIdentifiersFromPartial(input, player, maxPermission)
            .thenApply(ids ->
            {
                final List<MinimalStructureDescription> descriptions = new ArrayList<>(ids.size());
                for (final var id : ids)
                {
                    final String targetId = MathUtil.isNumerical(input) ? String.valueOf(id.uid()) : id.name();
                    descriptions.add(new MinimalStructureDescription(id.uid(), targetId));
                }
                return descriptions;
            });
    }

    /**
     * Extracts a set of identifiers from descriptions. Only the identifiers being used in the finder are used, so these
     * can refer either to the UIDs or the names of the structures (though never a mix).
     *
     * @param descriptions
     *     The descriptions from which to extract the identifiers.
     * @param lastInput
     *     The last available input to compare the identifiers to. If this value is provided, only identifiers that
     *     fully match this value are included.
     * @return The identifiers as extracted from the provided descriptions.
     */
    private static Set<String> getIdentifiers(
        Collection<MinimalStructureDescription> descriptions,
        @Nullable String lastInput)
    {
        if (lastInput != null)
            return new LinkedHashSet<>(getFullMatches(descriptions, lastInput))
                .stream()
                .map(MinimalStructureDescription::id)
                .collect(Collectors.toSet());

        final LinkedHashSet<String> ids = new LinkedHashSet<>(MathUtil.ceil(1.25 * descriptions.size()));
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
    private static LongSet getUIDs(Collection<MinimalStructureDescription> descriptions, @Nullable String lastInput)
    {
        if (lastInput != null)
            return LongLinkedOpenHashSet.toSet(getFullMatches(descriptions, lastInput)
                .stream()
                .mapToLong(MinimalStructureDescription::uid));

        final LongSet ids = new LongLinkedOpenHashSet();
        descriptions.forEach(desc -> ids.add(desc.uid));
        return ids;
    }

    /**
     * Gets the entries that fully match the target String.
     * <p>
     * This method uses {@link String#equalsIgnoreCase(String)} to compare the identifiers of the entries to the target
     * String.
     *
     * @param descriptions
     *     The descriptions for which to get the entries that fully match the target.
     * @param matchTo
     *     The target String to compare the identifiers of the entries to.
     * @return A list of minimal descriptions whose entries match the target String.
     */
    private static List<MinimalStructureDescription> getFullMatches(
        Collection<MinimalStructureDescription> descriptions,
        String matchTo)
    {
        return descriptions.stream().filter(desc -> desc.id.equalsIgnoreCase(matchTo)).toList();
    }

    /**
     * Gets the cache regardless of whether it is available or not.
     *
     * @return The cache if it is available, or null if it is not.
     */
    @Locked.Read("lock")
    private @Nullable List<MinimalStructureDescription> getCache()
    {
        return cache;
    }

    /**
     * Waits for the cache to become available.
     *
     * @return A completable future that will be completed with the cache
     */
    @Locked.Read("lock")
    private CompletableFuture<List<MinimalStructureDescription>> waitForDescriptions()
    {
        if (cache != null)
            return CompletableFuture.completedFuture(cache);

        final CompletableFuture<List<MinimalStructureDescription>> result = new CompletableFuture<>();
        CompletableFuture.runAsync(() ->
        {
            try
            {
                @Nullable List<MinimalStructureDescription> newCache = null;
                synchronized (msg)
                {
                    final long deadline = System.nanoTime() + Duration.ofSeconds(DEFAULT_TIMEOUT).toNanos();

                    while (System.nanoTime() < deadline && (newCache = getCache()) == null)
                    {
                        final long waitTime = Duration.ofNanos(deadline - System.nanoTime()).toMillis();
                        if (waitTime > 0)
                            msg.wait(waitTime);
                    }
                    if (newCache == null || System.nanoTime() > deadline)
                        throw new TimeoutException("Timed out waiting for list of structure descriptions.");
                }

                result.complete(Util.requireNonNull(newCache, "Cache"));
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
        }).exceptionally(FutureUtil::exceptionally);
        return result.exceptionally(t -> FutureUtil.exceptionally(t, Collections.emptyList()));
    }

    @Locked.Read("lock")
    List<String> getPostponedInputs()
    {
        return new ArrayList<>(postponedInputs);
    }

    @Getter
    @AllArgsConstructor
    @ToString
    static final class HistoryItem
    {
        private final String input;
        private final List<MinimalStructureDescription> items;
    }

    /**
     * @param uid
     *     The UID of the structure.
     * @param id
     *     The target identifier of the structure that we're looking for. This may either be its UID or its name.
     */
    public record MinimalStructureDescription(long uid, String id) {}
}
