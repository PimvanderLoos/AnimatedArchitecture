/*
 *  MIT License
 *
 * Copyright (c) 2020 Pim van der Loos
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package nl.pim16aap2.bigdoors.util.cache;

import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.SoftReference;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a timed cached map backed by a {@link ConcurrentHashMap}. Entries will expire after a configurable amount
 * of time.
 * <p>
 * Expired entries cannot be retrieved or used in any way, but they will still show up in the size arguments. If
 * configured, a separate thread may perform regular cleanup.
 *
 * @param <K> Type of the Key of the map.
 * @param <V> Type of the value of the map.
 * @author Pim
 */
public class TimedCache<K, V>
{
    /**
     * The actual data structure all values are cached in.
     */
    private final @NonNull ConcurrentHashMap<K, AbstractTimedValue<V>> cache = new ConcurrentHashMap<>();

    /**
     * The amount of time a variable will be available measured in milliseconds for positive non-zero values.
     * <p>
     * 0 means values are kept forever,
     * <p>
     * < 0 values mean nothing ever gets added in the first place.
     */
    private final long timeOut;

    /**
     * Function that creates the specific type of {@link AbstractTimedValue} that is required according to the
     * configuration.
     */
    private final @NonNull Function<V, AbstractTimedValue<V>> timedValueCreator;

    /**
     * Whether to refresh entries whenever they are accessed.
     * <p>
     * When set to false, entries will expire after the configured amount of time after their insertion time.
     * <p>
     * When set to true, entries will expire  after the configured amount of time after they were last retrieved.
     */
    private final boolean refresh;

    /**
     * The clock to use to determine the insertion/cleanup times.
     */
    private final @NonNull Clock clock;

    // For testing purposes.
    TimedCache(final @NonNull Clock clock, final @NonNull Duration duration, final @Nullable Duration cleanup,
               final boolean softReference, final boolean refresh)
    {
        this.clock = clock;
        timeOut = duration.toMillis();
        timedValueCreator = softReference ? this::createTimedSoftValue : this::createTimedValue;
        setupCleanupTask(cleanup == null ? 0 : cleanup.toMillis());
        this.refresh = refresh;
    }

    /**
     * Constructor of {@link TimedCache}
     *
     * @param duration      The amount of time a cached entry remains valid.
     *                      <p>
     *                      Note that this value is used for millisecond precision. Anything smaller than that will be
     *                      ignored.
     * @param cleanup       The duration between each cleanup cycle. During cleanup, all expired entries will be removed
     *                      from the cache. When null (default) or 0, entries are evicted from the cache whenever they
     *                      are accessed after they have expired. This value also uses millisecond precision.
     * @param softReference Whether to wrap values in {@link SoftReference}s or not. This allows the garbage collector
     *                      to clear up any values as it sees fit.
     * @param refresh       Whether to refresh entries whenever they are accessed.
     *                      <p>
     *                      When set to false, entries will expire after the configured amount of time after their
     *                      insertion time.
     *                      <p>
     *                      When set to true, entries will expire  after the configured amount of time after they were
     *                      last retrieved.
     */
    @Builder
    protected TimedCache(final @NonNull Duration duration, final @Nullable Duration cleanup,
                         final boolean softReference, final boolean refresh)
    {
        this(Clock.systemUTC(), duration, cleanup, softReference, refresh);
    }

    /**
     * Puts a new key/value pair in the cache.
     *
     * @param key   The key of the pair to add to the cache.
     * @param value The value of the pair to add to the cache.
     * @return The value that was just added to the cache.
     */
    public @NonNull V put(final @NonNull K key, final @NonNull V value)
    {
        cache.put(key, timedValueCreator.apply(value));
        return value;
    }

    /**
     * Updates a value if it currently exists in the cache.
     *
     * @param key   The key with which the specified value is to be associated.
     * @param value The value to be associated with the specified key.
     * @return The updated value. If no value was updated, an empty Optional.
     */
    public @NonNull Optional<V> putIfPresent(final @NonNull K key, final @NonNull V value)
    {
        return Optional.ofNullable(cache.compute(key, (k, tValue) ->
        {
            if (tValue == null || tValue.timedOut())
                return null;
            return timedValueCreator.apply(value);
        })).map(entry -> entry.getValue(refresh));
    }

    /**
     * See {@link ConcurrentHashMap#putIfAbsent(Object, Object)}.
     *
     * @return If no value existed in the map or the existing entry timed out, an empty optional is returned.
     * <p>
     * If a valid mapping existed for the provided key, an optional containing the mapped value is returned.
     */
    public @NonNull Optional<V> putIfAbsent(final @NonNull K key, final @NonNull V value)
    {
        final @NonNull AtomicReference<V> returnValue = new AtomicReference<>();
        cache.compute(key, (k, tValue) ->
        {
            if (tValue == null || tValue.timedOut())
                return timedValueCreator.apply(value);

            returnValue.set(tValue.getValue(refresh));
            return null;
        });

        return Optional.ofNullable(returnValue.get());
    }

    /**
     * See {@link ConcurrentHashMap#computeIfAbsent(Object, Function)}.
     *
     * @return If no value existed in the map or the existing entry timed out, an empty optional is returned.
     * <p>
     * If a valid mapping existed for the provided key, an optional containing the mapped value is returned.
     */
    public @NonNull Optional<V> computeIfAbsent(final @NonNull K key,
                                                final @NonNull Function<K, @NonNull V> mappingFunction)
    {
        final @NonNull AtomicReference<V> returnValue = new AtomicReference<>();
        Objects.requireNonNull(cache.compute(key, (k, tValue) ->
        {
            if (tValue == null || tValue.timedOut())
                return timedValueCreator.apply(mappingFunction.apply(k));

            returnValue.set(tValue.getValue(refresh));
            return tValue;
        }));
        return Optional.ofNullable(returnValue.get());
    }

    /**
     * See {@link ConcurrentHashMap#computeIfPresent(Object, BiFunction)}.
     */
    public @NonNull Optional<V> computeIfPresent(final @NonNull K key,
                                                 final @NonNull BiFunction<@NonNull K, V, @NonNull V> remappingFunction)
    {
        return Optional.ofNullable(cache.compute(key, (k, timedValue) ->
        {
            if (timedValue == null || timedValue.timedOut())
                return null;
            val value = timedValue.getValue(refresh);
            return timedValueCreator.apply(remappingFunction.apply(k, value));
        })).map(entry -> entry.getValue(refresh));
    }

    /**
     * See {@link ConcurrentHashMap#compute(Object, BiFunction)}.
     */
    public @NonNull V compute(final @NonNull K key,
                              final @NonNull BiFunction<K, V, @NonNull V> mappingFunction)
    {
        return Objects.requireNonNull(cache.compute(key, (k, timedValue)
            ->
        {
            final @Nullable V value;
            if (timedValue == null || timedValue.timedOut())
                value = null;
            else
                value = timedValue.getValue(refresh);

            return timedValueCreator.apply(mappingFunction.apply(k, value));
        }).getValue(refresh));
    }

    /**
     * See {@link ConcurrentHashMap#remove(Object)}.
     */
    public @NonNull Optional<V> remove(final @NonNull K key)
    {
        return getValue(cache.remove(key));
    }

    /**
     * Gets the value associated with the provided key.
     * <p>
     * If the value has expired but still exists in the map, it will be evicted and treated as if it did not exist at
     * all.
     *
     * @param key The key of the value to look up.
     * @return The value associated with the provided key if it is available.
     */
    public @NonNull Optional<V> get(final @NonNull K key)
    {
        final @Nullable AbstractTimedValue<V> entry = cache.get(key);
        if (entry == null)
            return Optional.empty();

        @Nullable val value = entry.getValue(refresh);
        if (value == null)
        {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.of(value);
    }

    /**
     * Checks if a provided key both exists in the map and is not expired.
     * <p>
     * If {@link #refresh} is enabled, the key will be refreshed.
     *
     * @param key The key to check.
     * @return True if the key exists and has not timed out.
     */
    public boolean containsKey(final @NonNull K key)
    {
        return get(key).isPresent();
    }

    /**
     * Wraps a value in an {@link Optional}. If the provided entry is not null, it will retrieve the value wrapped
     * inside it.
     * <p>
     * See {@link AbstractTimedValue#getValue(boolean)}.
     *
     * @param entry The entry to wrap.
     * @return The value stored in the entry, if any.
     */
    protected @NonNull Optional<V> getValue(final @Nullable AbstractTimedValue<V> entry)
    {
        return entry == null ? Optional.empty() : Optional.ofNullable(entry.getValue(refresh));
    }

    /**
     * Gets the total number cached entries.
     * <p>
     * Note that this also includes expired entries.
     *
     * @return The total number of cached entries.
     */
    public int getSize()
    {
        return cache.size();
    }

    /**
     * Removes all entries from the cache.
     */
    public void clear()
    {
        cache.clear();
    }

    /**
     * Gets the raw {@link AbstractTimedValue} from the cache, if it exists.
     * <p>
     * This does not respect any kind of timeouts.
     *
     * @param key The key associated with the value to retrieve.
     * @return The value associated with the key, if it exists.
     */
    protected @Nullable AbstractTimedValue<V> getRaw(final @NonNull K key)
    {
        return cache.get(key);
    }

    /**
     * Creates a new {@link TimedValue}. This method should not be called directly. Instead, use to {@link
     * #timedValueCreator}.
     *
     * @param val The value to wrap in an {@link AbstractTimedValue}.
     * @return The newly created {@link TimedValue}.
     */
    private @NonNull AbstractTimedValue<V> createTimedValue(final @NonNull V val)
    {
        return new TimedValue<>(clock, val, timeOut);
    }

    /**
     * Creates a new {@link TimedSoftValue}. This method should not be called directly. Instead, use to {@link
     * #timedValueCreator}.
     *
     * @param val The value to wrap in an {@link AbstractTimedValue}.
     * @return The newly created {@link TimedSoftValue}.
     */
    private @NonNull AbstractTimedValue<V> createTimedSoftValue(final @NonNull V val)
    {
        return new TimedSoftValue<>(clock, val, timeOut);
    }

    /**
     * Removes any entries that have expired from the map.
     * <p>
     * An entry counts as expired if {@link AbstractTimedValue#getValue(boolean)} returns null.
     */
    protected void cleanupCache()
    {
        for (Map.Entry<K, AbstractTimedValue<V>> entry : cache.entrySet())
        {
            if (entry.getValue().getValue(false) == null)
                cache.remove(entry.getKey());
        }
    }

    /**
     * Creates the cleanup task that will clean up the cache every 'period' milliseconds.
     * <p>
     * See {@link #cleanupCache()}.
     *
     * @param period The amount of time (in milliseconds) between each cleanup run. If this value is less than 1,
     *               nothing happens.
     */
    private void setupCleanupTask(final long period)
    {
        if (period < 1)
            return;

        @NonNull val taskTimer = new Timer(true);
        @NonNull val verifyTask = new TimerTask()
        {
            @Override
            public void run()
            {
                cleanupCache();
            }
        };
        taskTimer.scheduleAtFixedRate(verifyTask, period, period);
    }
}
