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

package nl.pim16aap2.animatedarchitecture.core.data.cache.timed;

import com.google.common.flogger.StackSize;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
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
 * @param <K>
 *     Type of the Key of the map.
 * @param <V>
 *     Type of the value of the map.
 */
@Flogger
public sealed class TimedCache<K, V>
{
    private static final Clock DEFAULT_CLOCK = Clock.systemUTC();

    /**
     * The actual data structure all values are cached in.
     */
    private final ConcurrentHashMap<K, AbstractTimedValue<V>> cache = new ConcurrentHashMap<>();

    /**
     * The amount of time a variable will be available measured in milliseconds for positive non-zero values.
     * <p>
     * 0 means values are kept forever,
     * <p>
     * < 0 values mean nothing ever gets added in the first place.
     */
    private final long timeOut;

    private final Timer taskTimer = new Timer(true);

    /**
     * Function that creates the specific type of {@link AbstractTimedValue} that is required according to the
     * configuration.
     */
    private final Function<V, AbstractTimedValue<V>> timedValueCreator;

    /**
     * Whether to refresh entries whenever they are accessed.
     * <p>
     * When set to false, entries will expire after the configured amount of time after their insertion time.
     * <p>
     * When set to true, entries will expire  after the configured amount of time after they were last retrieved.
     */
    private final boolean refresh;

    /**
     * Whether to keep values after their timeOut value is exceeded.
     * <p>
     * This is used when using either soft or weak references and results in values remaining in the cache until they
     * are removed by the garbage collector.
     */
    private final boolean keepAfterTimeOut;

    /**
     * The clock to use to determine the insertion/cleanup times.
     */
    private final Clock clock;

    /**
     * The flag that keeps track of whether this cache is alive. This will be false after using {@link #shutDown()}.
     * <p>
     * When the cache is not alive, you cannot interact with it anymore.
     */
    private volatile boolean alive = true;

    private TimedCache(Clock clock, long timeOut, boolean softReference, boolean refresh, boolean keepAfterTimeOut)
    {
        this.clock = clock;
        this.refresh = refresh;
        this.keepAfterTimeOut = keepAfterTimeOut;
        this.timeOut = timeOut;
        timedValueCreator = softReference ? this::createTimedSoftValue : this::createTimedValue;
    }

    @Builder(
        // This builder is used for testing purposes only.
        access = AccessLevel.PACKAGE,
        builderClassName = "TimedCacheBuilderWithClock",
        builderMethodName = "builderWithClock"
    )
    private TimedCache(
        Clock clock,
        @Nullable Duration timeOut,
        @Nullable Duration cleanup,
        boolean softReference,
        boolean refresh,
        boolean keepAfterTimeOut)
    {
        this(
            clock,
            Objects.requireNonNullElse(timeOut, Duration.ZERO).toMillis(),
            softReference,
            refresh,
            keepAfterTimeOut
        );

        final long cleanupMillis = cleanup == null ? 0 : cleanup.toMillis();

        if (this.timeOut == 0 && (!softReference || cleanupMillis < 1))
            throw new IllegalArgumentException(
                "A timeOut of zero is only allowed in combination with soft " +
                    "references and a non-zero positive cleanup timeOut!"
            );
        setupCleanupTask(cleanupMillis);
    }

    /**
     * Ensures that this cache is still {@link #alive}.
     *
     * @throws IllegalStateException
     *     When this cache is not alive.
     */
    protected void validateState()
    {
        if (!alive)
            throw new IllegalStateException("Trying to interact with TimedCache object that has been shut down!");
    }

    /**
     * Constructor of {@link TimedCache}
     *
     * @param timeOut
     *     The amount of time a cached entry remains valid.
     *     <p>
     *     Note that this value is used for millisecond precision. Anything smaller than that will be ignored.
     *     <p>
     *     Setting this value to 0 means that values will never be evicted from the cache based on their age. This is a
     *     valid configuration in combination with the softReference option and a non-zero cleanup timeOut. This will
     *     cause all entries to exist in the cache until they are no longer referenced.
     *     <p>
     *     When null, this value defaults to {@link Duration#ZERO}.
     * @param cleanup
     *     The timeOut between each cleanup cycle. During cleanup, all expired entries will be removed from the cache.
     *     When null (default) or 0, entries are evicted from the cache whenever they are accessed after they have
     *     expired. This value also uses millisecond precision.
     * @param softReference
     *     Whether to wrap values in {@link SoftReference}s or not. This allows the garbage collector to clear up any
     *     values as it sees fit.
     * @param refresh
     *     Whether to refresh entries whenever they are accessed.
     *     <p>
     *     When set to false, entries will expire after the configured amount of time after their insertion time.
     *     <p>
     *     When set to true, entries will expire  after the configured amount of time after they were last retrieved.
     * @param keepAfterTimeOut
     *     Whether to keep values after their timeOut value is exceeded.
     *     <p>
     *     This is used when using either soft or weak references and results in values remaining in the cache until
     *     they are removed by the garbage collector.
     *     <p>
     *     When this is true, values in the cache
     */
    @Builder
    protected TimedCache(
        @Nullable Duration timeOut,
        @Nullable Duration cleanup,
        boolean softReference,
        boolean refresh,
        boolean keepAfterTimeOut)
    {
        this(DEFAULT_CLOCK, timeOut, cleanup, softReference, refresh, keepAfterTimeOut);
    }

    /**
     * Returns a new 'cache' that does not cache any values.
     * <p>
     * Any calls to getter methods will return null or empty optionals and any put methods will not do anything.
     *
     * @param <K>
     *     The type of the keys of the values.
     * @param <V>
     *     The type of the values.
     * @return The new not-cache.
     */
    public static <K, V> TimedCache<K, V> emptyCache()
    {
        return new EmptyCache<>();
    }

    /**
     * Puts a new key/value pair in the cache.
     *
     * @param key
     *     The key of the pair to add to the cache.
     * @param value
     *     The value of the pair to add to the cache.
     * @return The value that was just added to the cache.
     */
    public V put(K key, V value)
    {
        validateState();
        cache.put(key, timedValueCreator.apply(value));
        return value;
    }

    /**
     * Updates a value if it currently exists in the cache.
     *
     * @param key
     *     The key with which the specified value is to be associated.
     * @param value
     *     The value to be associated with the specified key.
     * @return The updated value. If no value was updated, an empty Optional.
     */
    public Optional<V> putIfPresent(K key, V value)
    {
        validateState();
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
     * @return An empty {@link Optional} if no mapping existed for the key. If a mapping did exist, the value that was
     * associated with the key is returned instead.
     */
    public Optional<V> putIfAbsent(K key, V value)
    {
        validateState();
        final AtomicReference<@Nullable V> returnValue = new AtomicReference<>();
        cache.compute(key, (k, tValue) ->
        {
            if (tValue == null || tValue.timedOut())
                return timedValueCreator.apply(value);

            returnValue.set(tValue.getValue(refresh));
            return tValue;
        });

        return Optional.ofNullable(returnValue.get());
    }

    /**
     * See {@link ConcurrentHashMap#computeIfAbsent(Object, Function)}.
     */
    public V computeIfAbsent(K key, Function<K, V> mappingFunction)
    {
        validateState();
        final AtomicReference<V> returnValue = new AtomicReference<>();
        cache.compute(key, (k, tValue) ->
        {
            @Nullable V innerValue;
            if (tValue == null || tValue.timedOut() || (innerValue = tValue.getValue(refresh)) == null)
            {
                innerValue = Util.requireNonNull(mappingFunction.apply(k),
                    "Computed TimedCache value for key: \"" + key + "\"");
                returnValue.set(innerValue);
                return timedValueCreator.apply(innerValue);
            }

            returnValue.set(innerValue);
            return tValue;
        });
        return Util.requireNonNull(returnValue.get(), "Computed TimedCache value for key: \"" + key + "\"");
    }

    /**
     * See {@link ConcurrentHashMap#computeIfPresent(Object, BiFunction)}.
     */
    public Optional<V> computeIfPresent(K key, BiFunction<K, @Nullable V, V> remappingFunction)
    {
        validateState();
        return Optional.ofNullable(cache.compute(key, (k, timedValue) ->
        {
            if (timedValue == null || timedValue.timedOut())
                return null;
            final @Nullable var value = timedValue.getValue(refresh);
            return createTimedValue(remappingFunction, k, value);
        })).map(entry -> entry.getValue(refresh));
    }

    private AbstractTimedValue<V> createTimedValue(BiFunction<K, @Nullable V, V> function, K key, @Nullable V val)
    {
        return timedValueCreator.apply(function.apply(key, val));
    }

    /**
     * See {@link ConcurrentHashMap#compute(Object, BiFunction)}.
     */
    public V compute(K key, BiFunction<K, @Nullable V, V> mappingFunction)
    {
        validateState();
        return Util.requireNonNull(cache.compute(key, (k, timedValue)
            ->
        {
            final @Nullable V value;
            if (timedValue == null || timedValue.timedOut())
                value = null;
            else
                value = timedValue.getValue(refresh);
            return createTimedValue(mappingFunction, k, value);
        }).getValue(refresh), "Computed cache value for key: \"" + key + "\"");
    }

    /**
     * See {@link ConcurrentHashMap#remove(Object)}.
     */
    public Optional<V> remove(K key)
    {
        validateState();
        return getValue(cache.remove(key));
    }

    /**
     * Gets the value associated with the provided key.
     * <p>
     * If the value has expired but still exists in the map, it will be evicted and treated as if it did not exist at
     * all.
     *
     * @param key
     *     The key of the value to look up.
     * @return The value associated with the provided key if it is available.
     */
    public Optional<V> get(K key)
    {
        validateState();
        final @Nullable AbstractTimedValue<V> entry = cache.get(key);
        if (entry == null)
            return Optional.empty();

        final @Nullable var value = entry.getValue(refresh);
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
     * @param key
     *     The key to check.
     * @return True if the key exists and has not timed out.
     */
    public boolean containsKey(K key)
    {
        validateState();
        return get(key).isPresent();
    }

    /**
     * Wraps a value in an {@link Optional}. If the provided entry is not null, it will retrieve the value wrapped
     * inside it.
     * <p>
     * See {@link AbstractTimedValue#getValue(boolean)}.
     *
     * @param entry
     *     The entry to wrap.
     * @return The value stored in the entry, if any.
     */
    Optional<V> getValue(@Nullable AbstractTimedValue<V> entry)
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
        validateState();
        return cache.size();
    }

    /**
     * Removes all entries from the cache.
     */
    public void clear()
    {
        validateState();
        cache.clear();
    }

    /**
     * Gets the raw {@link AbstractTimedValue} from the cache, if it exists.
     * <p>
     * This does not respect any kind of timeouts.
     *
     * @param key
     *     The key associated with the value to retrieve.
     * @return The value associated with the key, if it exists.
     */
    @Nullable AbstractTimedValue<V> getRaw(K key)
    {
        return cache.get(key);
    }

    /**
     * Creates a new {@link TimedValue}. This method should not be called directly. Instead, use to
     * {@link #timedValueCreator}.
     *
     * @param val
     *     The value to wrap in an {@link AbstractTimedValue}.
     * @return The newly created {@link TimedValue}.
     */
    private AbstractTimedValue<V> createTimedValue(V val)
    {
        return new TimedValue<>(clock, val, timeOut);
    }

    /**
     * Creates a new {@link TimedSoftValue}. This method should not be called directly. Instead, use to
     * {@link #timedValueCreator}.
     *
     * @param val
     *     The value to wrap in an {@link AbstractTimedValue}.
     * @return The newly created {@link TimedSoftValue}.
     */
    private AbstractTimedValue<V> createTimedSoftValue(V val)
    {
        return new TimedSoftValue<>(clock, val, timeOut, keepAfterTimeOut);
    }

    /**
     * Removes any entries that have expired from the map.
     * <p>
     * An entry counts as expired if {@link AbstractTimedValue#getValue(boolean)} returns null.
     */
    protected void cleanupCache()
    {
        for (final Map.Entry<K, AbstractTimedValue<V>> entry : cache.entrySet())
            if (entry.getValue().canBeEvicted())
                cache.remove(entry.getKey());
    }

    /**
     * Creates the cleanup task that will clean up the cache every 'period' milliseconds.
     * <p>
     * See {@link #cleanupCache()}.
     *
     * @param period
     *     The amount of time (in milliseconds) between each cleanup run. If this value is less than 1, nothing
     *     happens.
     */
    private void setupCleanupTask(long period)
    {
        if (period < 1)
            return;

        final var verifyTask = new TimerTask()
        {
            @Override
            public void run()
            {
                cleanupCache();
            }
        };
        taskTimer.scheduleAtFixedRate(verifyTask, period, period);
    }

    /**
     * Shuts this cache down.
     * <p>
     * The cache will be cleared and any running processes will be stopped.
     * <p>
     * After shutting the cache down, it is not possible to interact with it anymore. Trying to do so will result in
     * exceptions.
     */
    public void shutDown()
    {
        this.alive = false;
        log.atFinest().withStackTrace(StackSize.FULL).log("Shutting down TimedCache normally!");
        cache.clear();
        taskTimer.cancel();
    }

    /**
     * Represents a special case of the timed cache that does not cache any values.
     *
     * @param <K>
     *     The type of the keys (that are not cached)
     * @param <V>
     *     The type of the values (that are not cached).
     */
    static final class EmptyCache<K, V> extends TimedCache<K, V>
    {
        EmptyCache()
        {
            super(DEFAULT_CLOCK, 0, false, false, false);
        }

        @Override
        public V put(K key, V value)
        {
            validateState();
            return value;
        }

        @Override
        public Optional<V> putIfPresent(K key, V value)
        {
            validateState();
            return Optional.empty();
        }

        @Override
        public Optional<V> putIfAbsent(K key, V value)
        {
            validateState();
            return Optional.empty();
        }

        @Override
        public V computeIfAbsent(K key, Function<K, V> mappingFunction)
        {
            validateState();
            return mappingFunction.apply(key);
        }

        @Override
        public Optional<V> computeIfPresent(K key, BiFunction<K, @Nullable V, V> remappingFunction)
        {
            validateState();
            return Optional.empty();
        }

        @Override
        public V compute(K key, BiFunction<K, @Nullable V, V> mappingFunction)
        {
            validateState();
            return mappingFunction.apply(key, null);
        }

        @Override
        public Optional<V> remove(K key)
        {
            validateState();
            return Optional.empty();
        }

        @Override
        public Optional<V> get(K key)
        {
            validateState();
            return Optional.empty();
        }

        @Override
        public boolean containsKey(K key)
        {
            validateState();
            return false;
        }

        @Override
        protected Optional<V> getValue(@Nullable AbstractTimedValue<V> entry)
        {
            validateState();
            return Optional.empty();
        }

        @Override
        public void clear()
        {
            validateState();
        }
    }
}
