package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.api.IRestartableHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Represents a cached map. Entries can not be accessed after the provided time anymore and they are removed at certain
 * intervals.
 * <p>
 * Usage example: new TimedMapCache<>(holder, HashMap::new, 10, TimeUnit.MINUTES);
 *
 * @param <K> Type of the Key of the map.
 * @param <V> Type of the value of the map.
 * @author Pim
 */
public class TimedMapCache<K, V> extends Restartable implements Map<K, V>
{
    private static final TimeUnit DEFAULTTIMEUNIT = TimeUnit.MINUTES;
    private static final TimeUnit SMALLESTTIMEUNIT = TimeUnit.MILLISECONDS;
    private final Supplier<? extends Map> ctor;
    private final Class<? extends Map> mapType;
    /**
     * The amount of time a variable will be available measured in milliseconds for positive non-zero values.
     * <p>
     * 0 means values are kept forever,
     * <p>
     * < 0 values mean nothing ever gets added in the first place.
     */
    private long timeOut = -1L;

    private Map<K, TimedValue<V>> map;
    /**
     * {@link TimerTask} of {@link TimedMapCache#verifyCache}
     */
    private final TimerTask verifyTask = new TimerTask()
    {
        @Override
        public void run()
        {
            verifyCache();
        }
    };

    /**
     * Periodically run the {@link TimedMapCache#verifyTask}.
     */
    private Timer taskTimer;

    /**
     * Constructor of {@link TimedMapCache}
     *
     * @param holder   The {@link IRestartableHolder} that manages this object.
     * @param ctor     The type of the map to be used. For example, to use a {@link java.util.HashMap}, use
     *                 "HashMap::new"
     * @param time     The {@link TimedMapCache#timeOut}.
     * @param timeUnit The {@link java.util.concurrent.TimeUnit} of the {@link TimedMapCache#timeOut}. Cannot be smaller
     *                 than {@link TimeUnit#MILLISECONDS}
     */
    public TimedMapCache(final @NotNull IRestartableHolder holder, final @NotNull Supplier<? extends Map> ctor,
                         final long time, final @NotNull TimeUnit timeUnit)
    {
        super(holder);

        this.ctor = Objects.requireNonNull(ctor);
        map = ctor.get();
        mapType = map.getClass();
        reInit(time, timeUnit);
    }

    /**
     * Constructor of {@link TimedMapCache}
     *
     * @param holder The {@link IRestartableHolder} that manages this object.
     * @param ctor   The type of the map to be used. For example, to use a {@link java.util.HashMap}, use
     *               "HashMap::new"
     * @param time   The {@link TimedMapCache#timeOut}.
     */
    public TimedMapCache(final @NotNull IRestartableHolder holder, Supplier<? extends Map> ctor, final long time)
    {
        this(holder, ctor, time, DEFAULTTIMEUNIT);
    }

    /**
     * Reinitialize the map. If the {@link TimedMapCache#timeOut} has changed, reinitialize the {@link
     * TimedMapCache#taskTimer} as well.
     *
     * @param time     The {@link TimedMapCache#timeOut}.
     * @param timeUnit The {@link java.util.concurrent.TimeUnit} of the {@link TimedMapCache#timeOut}
     */
    public void reInit(long time, final @NotNull TimeUnit timeUnit)
    {
        if (time > 0 && timeUnit.toNanos(time) < SMALLESTTIMEUNIT.toNanos(1))
            throw new IllegalArgumentException(
                "Resolution of TimeUnit " + timeUnit.name() + " is too low! Only MILLISECONDS and up are allowed.");
        time = timeUnit.toMillis(time);
        if (timeOut != time)
        {
            destructor();
            timeOut = time;
            startTask();
        }
        map.clear();
    }

    /**
     * Reinitialize the map. If the {@link TimedMapCache#timeOut} has changed, reinitialize the {@link
     * TimedMapCache#taskTimer} as well.
     *
     * @param time The {@link TimedMapCache#timeOut}.
     */
    public void reInit(final long time)
    {
        reInit(time, DEFAULTTIMEUNIT);
    }

    /**
     * Initiate the {@link TimedMapCache#taskTimer}
     */
    private void startTask()
    {
        if (timeOut > 0)
        {
            // Verify cache 1/2 the timeOut time.
            taskTimer = new Timer();
            // TODO: This looks too manual. Use proper conversion.
            // TODO: Also, this is a pretty blunt solution. Try to find something with a bit more elegance.
            //       If something is to be cached for 1 year, don't store the data for 1.5 years...
            taskTimer.scheduleAtFixedRate(verifyTask, timeOut, timeOut / 2);
        }
    }

    /**
     * Kill the {@link TimedMapCache#taskTimer}
     */
    private void destructor()
    {
        if (timeOut > 0)
            taskTimer.cancel();
        map.clear();
    }

    /**
     * Get the object from the map if it exists and if caching is enabled (See {@link TimedMapCache#timeOut}).
     *
     * @param key The Key of the entry that is to be retrieved.
     * @return The value associated with the key if it exists and hasn't exceeded the {@link TimedMapCache#timeOut} and
     * if caching is enabled, otherwise null.
     */
    @Override
    public V get(Object key)
    {
        if (timeOut < 0 || key == null)
            return null;
        if (map.containsKey(key))
        {
            // Return the value or if the value has timed out, remove it and return null instead.
            TimedValue<V> value = map.get(key);
            if (!value.timedOut())
                return value.value;
            remove(key);
        }
        return null;
    }

    /**
     * Check if any entries have overstayed their welcome (exceeded {@link TimedMapCache#timeOut}) and remove them.
     */
    private void verifyCache()
    {
        for (Entry<K, TimedValue<V>> entry : map.entrySet())
        {
            if (entry.getValue().timedOut())
                map.remove(entry.getKey());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return map.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @return True if this map contains a mapping for the specified key and if it hasn't exceeded the {@link
     * TimedMapCache#timeOut} yet.
     */
    @Override
    public boolean containsKey(Object key)
    {
        TimedValue<V> var = map.get(key);
        if (var == null || var.timedOut())
            return false;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsValue(Object value)
    {
        return map.containsValue(value);
    }

    /**
     * If caching is disabled (see {@link TimedMapCache#timeOut}), this method does nothing!
     * <p>
     * {@inheritDoc}
     */
    @Override
    public V put(K key, V value)
    {
        if (timeOut < 0)
            return null;
        TimedValue<V> newVal = new TimedValue<>(value);
        map.put(key, newVal);
        return newVal.value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V remove(Object key)
    {
        TimedValue<V> timedValue = map.remove(key);
        return timedValue == null ? null : timedValue.value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(Map m)
    {
        map.putAll(m);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear()
    {
        map.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set keySet()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection values()
    {
        return map.values();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.util.Set entrySet()
    {
        return mapType.cast(map).entrySet();
    }

    /**
     * Reinitialize this object. See {@link TimedMapCache#reInit(long, TimeUnit)}.
     */
    @Override
    public void restart()
    {
        reInit(timeOut, SMALLESTTIMEUNIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown()
    {
        destructor();
    }

    /**
     * Represents a value in a {@link TimedMapCache}. It holds the value and the time of insertion.
     *
     * @param <V> Type of the value.
     */
    private final class TimedValue<V>
    {
        public final long insertTime;
        public final V value;

        /**
         * Constructor of {@link TimedValue}.
         *
         * @param val The value of this {@link TimedValue}.
         */
        public TimedValue(V val)
        {
            value = val;
            insertTime = System.currentTimeMillis();
        }

        /**
         * Check if this {@link TimedValue} was inserted more than {@link TimedMapCache#timeOut} milliseconds ago. If
         * so, it's considered "timed out".
         *
         * @return True if the value has timed out.
         */
        public boolean timedOut()
        {
            if (timeOut == 0)
                return false;
            return (System.currentTimeMillis() - insertTime) > timeOut;
        }

        /**
         * Check if an object equals this {@link TimedValue#value}. Note that it only checks the {@link
         * TimedValue#value}; The {@link TimedValue#insertTime} is ignored!
         *
         * @param o The object to compare to this {@link TimedValue}.
         * @return True if the {@link TimedValue#value} of the object equals the {@link TimedValue#value} of this
         * object, otherwise false.
         */
        @Override
        public final boolean equals(Object o)
        {
            if (o == this)
                return true;
            if (o instanceof TimedMapCache.TimedValue)
                return ((TimedValue) o).value.equals(value);
            return false;
        }

        /**
         * Check if an object equals the {@link TimedValue#value} of this {@link TimedValue}. Note that a {@link
         * TimedValue} object would return false!
         *
         * @param o The object to compare to the {@link TimedValue#value} of this {@link TimedValue}.
         * @return True if the {@link TimedValue#value} of the object equals the {@link TimedValue#value} of this {@link
         * TimedValue}, otherwise false.
         */
        public final boolean equalsValue(Object o)
        {
            return o.equals(value);
        }
    }
}
