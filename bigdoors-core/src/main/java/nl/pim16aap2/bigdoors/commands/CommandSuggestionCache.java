package nl.pim16aap2.bigdoors.commands;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.cache.TimedCache;
import nl.pim16aap2.bigdoors.util.functional.CheckedSupplier;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Represents a cache for tab completion suggestions. Once a list of suggestions is created for an {@link
 * ICommandSender}, this list will be used for future lookups, if possible. Once a list of String suggestions is
 * constructed, we don't have to recalculate all the options if the partial match increased in size, but still starts
 * with the same characters as used for the last lookup. If this is the case, we can just remove all entries from the
 * list that do not start with the new partial match.
 * <p>
 * This is especially useful when suggesting items from a list obtained via an expensive operation.
 * <p>
 * For example, a list of values from a database. On the Spigot platform, the suggestions are recalculated every time
 * the user, so caching them means that getting a name of 10 characters from it only requires a single lookup instead of
 * 10.
 * <p>
 * The suggestions are cached for 2 minutes using a {@link TimedCache}.
 *
 * @author Pim
 */
@SuppressWarnings("unused") @Flogger
public class CommandSuggestionCache
{
    private final TimedCache<ICommandSender, CacheEntry> tabCompletionCache =
        TimedCache.<ICommandSender, CacheEntry>builder()
                  .duration(Duration.ofMinutes(2))
                  .cleanup(Duration.ofMinutes(5))
                  .softReference(true)
                  .refresh(true)
                  .build();

    /**
     * Gets the list of suggested tab complete options for an {@link ICommandSender} base on the current arguments.
     *
     * @param commandSender
     *     The {@link ICommandSender} for which to get the list of suggested tab completions.
     * @param input
     *     The input to use for retrieving the suggestions.
     * @param fun
     *     The function to retrieve the list of arguments if they cannot be retrieved from cache.
     * @return The list of suggested tab completions.
     */
    public List<String> getTabCompleteOptions(ICommandSender commandSender, String input,
                                              CheckedSupplier<List<String>, Exception> fun)
    {
        final CacheEntry cacheEntry =
            tabCompletionCache.computeIfAbsent(commandSender, k -> new CacheEntry())
                              .orElseThrow(IllegalStateException::new);

        final Optional<List<String>> suggestions =
            cacheEntry.suggestionsSubSelection(input);

        if (suggestions.isPresent())
            return suggestions.get();

        try
        {
            final List<String> newSuggestions = fun.get();
            cacheEntry.reset(newSuggestions, input);
            return newSuggestions;
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e)
               .log("Failed to get command completion suggestions for command sender '%s' with input '%s'",
                    commandSender, input);
            return Collections.emptyList();
        }
    }

    /**
     * Gets the list of suggested tab complete options for an {@link ICommandSender} base on the current arguments.
     * <p>
     * If the results are not cached, the results will be obtained using an asynchronous method.
     *
     * @param commandSender
     *     The {@link ICommandSender} for which to get the list of suggested tab completions.
     * @param input
     *     The last argument in the command. This may or may not be the last entry in the list of arguments, but the
     *     parser can figure that out.
     * @param fun
     *     The function to retrieve the {@link CompletableFuture} list of arguments if they cannot be retrieved from
     *     cache.
     * @return The {@link CompletableFuture} of the list of suggested tab completions.
     */
    public CompletableFuture<List<String>> getTabCompleteOptionsAsync(
        ICommandSender commandSender, String input, CheckedSupplier<List<String>, Exception> fun)
    {
        final Triple<List<String>, CompletableFuture<List<String>>, AsyncCacheEntry> result =
            getAsyncCachedEntrySuggestions(commandSender, input, fun);

        if (result.first != null)
            return CompletableFuture.completedFuture(result.first);
        return Util.requireNonNull(result.second, "Async result");
    }

    /**
     * Gets the list of suggested tab complete options for an {@link ICommandSender} base on the current arguments.
     * <p>
     * If the results are not cached, the results will be put in the cache using an asynchronous method.
     * <p>
     * Unlike {@link #getTabCompleteOptionsAsync(ICommandSender, String, CheckedSupplier)}, this method will not return
     * a {@link CompletableFuture} with the results if they had to be retrieved async, but instead, it will return an
     * empty list.
     * <p>
     * Successive calls will keep returning empty Optionals until the async supplier has supplied the cache with a
     * result. From then on, it will retrieve any values it can find in the cache.
     *
     * @param commandSender
     *     The {@link ICommandSender} for which to get the list of suggested tab completions.
     * @param input
     *     The input to use for retrieving the suggestions.
     * @param fun
     *     The function to retrieve the {@link CompletableFuture} list of arguments if they cannot be retrieved from
     *     cache.
     * @return The list of suggested tab completions if one could be found. If no results are in the cache yet an empty
     * optional is returned.
     */
    public Optional<List<String>> getDelayedTabCompleteOptions(
        ICommandSender commandSender, String input, CheckedSupplier<List<String>, Exception> fun)
    {
        final Triple<List<String>, CompletableFuture<List<String>>, AsyncCacheEntry> result =
            getAsyncCachedEntrySuggestions(commandSender, input, fun);

        // Only return the list if the result
        if (result.first != null && result.third.entryStatus == ENTRY_STATUS.AVAILABLE)
            return Optional.of(result.first);
        return Optional.empty();
    }

    /**
     * Gets the list of tab completion suggestions from an async supplier.
     * <p>
     * If no entry exists in the cache for the provided {@link ICommandSender}, a new entry will be created with a new
     * {@link AsyncCacheEntry} as its value.
     * <p>
     * If the entry does exist, it will be used to narrow down the previously-obtained suggestions using the provided
     * lastArg.
     *
     * @param commandSender
     *     The {@link ICommandSender} for which to get the list of suggested tab completions.
     * @param input
     *     The input to use for retrieving the suggestions.
     * @param fun
     *     The function to retrieve the {@link CompletableFuture} list of arguments if they cannot be retrieved from
     *     cache.
     * @return Either a list of suggestions or a {@link CompletableFuture} with the suggestions. Only a single value is
     * returned and the other value is always null. So if a list of suggestions could be found, those will be returned
     * and null for the future one. If no list of suggestions could be found, the future suggestions will be returned
     * and the list will be null.
     */
    private Triple<List<String>, CompletableFuture<List<String>>, AsyncCacheEntry> getAsyncCachedEntrySuggestions(
        ICommandSender commandSender, String input, CheckedSupplier<List<String>, Exception> fun)
    {
        final AsyncCacheEntry cacheEntry =
            (AsyncCacheEntry) tabCompletionCache.compute(commandSender, (key, entry) ->
            {
                if (!(entry instanceof AsyncCacheEntry))
                    return new AsyncCacheEntry();
                return entry;
            });

        final Optional<List<String>> suggestions = cacheEntry.suggestionsSubSelection(input);

        if (suggestions.isPresent())
            return new Triple<>(suggestions.get(), null, cacheEntry);

        final CompletableFuture<List<String>> newSuggestions = CompletableFuture.supplyAsync(
            () ->
            {
                try
                {
                    return fun.get();
                }
                catch (InterruptedException e)
                {
                    log.at(Level.WARNING).withCause(e).log(
                        "Interrupted trying to get command completion suggestions for command sender '%s' with input '%s'",
                        commandSender, input);
                    Thread.currentThread().interrupt();
                }
                catch (Exception e)
                {
                    log.at(Level.SEVERE).withCause(e)
                       .log("Failed to get command completion suggestions for command sender '%s' with input '%s'",
                            commandSender, input);
                }
                return Collections.emptyList();
            }
        );
        cacheEntry.prepare(newSuggestions, input);

        return new Triple<>(null, newSuggestions, cacheEntry);
    }

    /**
     * Represents a cached list of tab completion options for an {@link ICommandSender}.
     *
     * @author Pim
     */
    private static class CacheEntry
    {
        /**
         * The number of characters to keep in the cache behind the new one.
         * <p>
         * For example, when set to a value of two and a "lastarg" of "pim", the suggestions list will contain all
         * suggestions for "pi" and "pim", but not "p".
         */
        protected static final int CUTOFF_DELTA = 2;

        /**
         * The cached list of suggestions.
         */
        protected @Nullable List<String> suggestions = null;

        /**
         * The cached last argument.
         */
        protected String previousArg = "";

        /**
         * Updates the current suggestions data.
         *
         * @param suggestions
         *     The updated list of suggestions.
         * @param input
         *     The input to use for retrieving the suggestions.
         */
        public void reset(final List<String> suggestions, final String input)
        {
            this.suggestions = new ArrayList<>(suggestions);
            previousArg = input;
        }

        /**
         * Gets all the cached suggestions
         *
         * @param lastArg
         *     The value of the last argument.
         * @return The list of the narrowed-down suggestions list.
         */
        public Optional<List<String>> suggestionsSubSelection(final String lastArg)
        {
            if (suggestions == null)
                return Optional.empty();

            // Get the cutoff for the old argument. This is the base string for every entry in the cached list.
            // So, if the provided lastArg does not start with that, we know that we don't have its results cached.
            // Given a CUTOFF_DELTA of 2, we'd get an empty string if there are only 2 characters. Therefore, we
            // try to get the first character in that case (if long enough).
            final int previousCutoff = Math.min(previousArg.length(),
                                                Math.max(1, previousArg.length() - CUTOFF_DELTA));
            final String basePreviousArg =
                previousArg.substring(0, Math.min(previousArg.length(), previousCutoff));

            // If the basePrevious arg is empty we don't have any data about what substring the argument starts with.
            // So we treat it as an invalid start.
            if (basePreviousArg.isEmpty() || !lastArg.startsWith(basePreviousArg))
                return Optional.empty();

            // Get rid of all entries that do not meet the cutoff.
            final int newCutoff = Math.max(0, lastArg.length() - CUTOFF_DELTA);
            suggestions.removeIf(val -> val.length() < newCutoff);

            final ArrayList<String> newSuggestions;

            newSuggestions = new ArrayList<>(suggestions.size());

            suggestions.forEach(
                val ->
                {
                    if (val.startsWith(lastArg))
                        newSuggestions.add(val);
                });

            newSuggestions.trimToSize();
            previousArg = lastArg;

            return Optional.of(newSuggestions);
        }
    }

    /**
     * Represents a specialization of the {@link CacheEntry} to use values that may or may not be immediately
     * available.
     *
     * @author Pim
     */
    private static class AsyncCacheEntry extends CacheEntry
    {
        protected volatile ENTRY_STATUS entryStatus = ENTRY_STATUS.NULL;

        public synchronized void prepare(final CompletableFuture<List<String>> newSuggestions, final String input)
        {
            entryStatus = ENTRY_STATUS.PENDING;
            newSuggestions.whenComplete((suggestions, throwable) -> reset(suggestions, input));
        }

        @Override
        public synchronized void reset(final List<String> suggestions, final String input)
        {
            entryStatus = ENTRY_STATUS.AVAILABLE;
            super.reset(suggestions, input);
        }

        @Override
        public synchronized Optional<List<String>> suggestionsSubSelection(final String input)
        {
            // If the data isn't available yet, return an empty list (not an empty optional).
            if (entryStatus == ENTRY_STATUS.PENDING)
                return Optional.of(new ArrayList<>(0));

            return super.suggestionsSubSelection(input);
        }
    }

    private enum ENTRY_STATUS
    {
        NULL,
        PENDING,
        AVAILABLE
    }

    record Triple<T, U, V>(@Nullable T first, @Nullable U second, V third) {}
}
