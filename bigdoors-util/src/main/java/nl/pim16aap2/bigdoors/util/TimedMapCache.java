package nl.pim16aap2.bigdoors.util;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Represents a cached map. Entries can not be accessed after the provided time
 * anymore and they are removed at certain intervals.
 *
 * @param <K> Type of the Key of the map.
 * @param <V> Type of the value of the map.
 *
 * @author Pim
 */
public class TimedMapCache<K, V> extends Restartable implements Map<K, V>
{
    /**
     * The amount of time a variable will be available measured in milliseconds for
     * positive non-zero values.
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
    private TimerTask verifyTask;
    /**
     * Periodically run the {@link TimedMapCache#verifyTask}.
     */
    private Timer taskTimer;
    private final Supplier<? extends Map> ctor;
    private final Class<? extends Map> mapType;

    /**
     * Constructor of {@link TimedMapCache}
     *
     * @param holder   The {@link RestartableHolder} that manages this object.
     * @param ctor     The type of the map to be used. For example, to use a
     *                 {@link java.util.HashMap}, use "HashMap::new"
     * @param time     The {@link TimedMapCache#timeOut}.
     * @param timeUnit The {@link java.util.concurrent.TimeUnit} of the
     *                 {@link TimedMapCache#timeOut}.
     */
    public TimedMapCache(final RestartableHolder holder, Supplier<? extends Map> ctor, long time, TimeUnit timeUnit)
    {
        super(holder);

        this.ctor = Objects.requireNonNull(ctor);
        map = ctor.get();
        mapType = map.getClass();

        verifyTask = new TimerTask()
        {
            @Override
            public void run()
            {
                verifyCache();
            }
        };
        reInit(time, timeUnit);
    }

    /**
     * Constructor of {@link TimedMapCache}
     *
     * @param holder The {@link RestartableHolder} that manages this object.
     * @param ctor   The type of the map to be used. For example, to use a
     *               {@link java.util.HashMap}, use "HashMap::new"
     * @param time   The {@link TimedMapCache#timeOut}.
     */
    public TimedMapCache(final RestartableHolder holder, Supplier<? extends Map> ctor, long time)
    {
        this(holder, ctor, time, TimeUnit.MINUTES);
    }

    /**
     * Reinitialize the map. If the {@link TimedMapCache#timeOut} has changed,
     * reinitialize the {@link TimedMapCache#taskTimer} as well.
     *
     * @param time     The {@link TimedMapCache#timeOut}.
     * @param timeUnit The {@link java.util.concurrent.TimeUnit} of the
     *                 {@link TimedMapCache#timeOut}
     */
    private void reInit(long time, TimeUnit timeUnit)
    {
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
     * Initiate the {@link TimedMapCache#taskTimer}
     */
    private void startTask()
    {
        if (timeOut > 0)
        {
            // Verify cache 1/2 the timeOut time. Timeout is in minutes, task timer in
            // ticks, so 1200 * timeOut = timeOut in ticks.
            taskTimer = new Timer();
            taskTimer.scheduleAtFixedRate(verifyTask, 60000 * timeOut, 60000 * timeOut / 2);
        }
    }

    /**
     * Kill the {@link TimedMapCache#taskTimer}
     */
    private void destructor()
    {
        if (timeOut > 0)
            taskTimer.cancel();
    }

    /**
     * Get the object from the map if it exists and if caching is enabled (See
     * {@link TimedMapCache#timeOut}).
     * 
     * @param key The Key of the entry that is to be retrieved.
     * @return The value associated with the key if it exists and hasn't exceeded
     *         the {@link TimedMapCache#timeOut} and if caching is enabled,
     *         otherwise null.
     */
    public V get(Object key)
    {
        if (timeOut < 0)
            return null;
        if (map.containsKey(key))
            return map.get(key).timedOut() ? null : (V) map.get(key).value;
        return null;
    }

    /**
     * Check if any entries have overstayed their welcome (exceeded
     * {@link TimedMapCache#timeOut}) and remove them.
     */
    private void verifyCache()
    {
        Iterator<Entry<K, TimedValue<V>>> it = map.entrySet().iterator();
        while (it.hasNext())
        {
            Entry<K, TimedValue<V>> entry = it.next();
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
     * @return True if this map contains a mapping for the specified key and if it
     *         hasn't exceeded the {@link TimedMapCache#timeOut} yet.
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
     * If caching is disabled (see {@link TimedMapCache#timeOut}), this method does
     * nothing!
     * <p>
     * {@inheritDoc}
     */
    @Override
    public V put(K key, V value)
    {
        if (timeOut < 0)
            return null;
        TimedValue<V> newVal = new TimedValue<V>(value);
        map.put(key, newVal);
        return newVal.value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V remove(Object key)
    {
        return map.remove(key).value;
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
     * Represents a value in a {@link TimedMapCache}. It holds the value and the
     * time of insertion.
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
         * Check if this {@link TimedValue} was inserted more than
         * {@link TimedMapCache#timeOut} milliseconds ago. If so, it's considered "timed
         * out".
         * 
         * @return True if the value has timed out.
         */
        public boolean timedOut()
        {
            return (System.currentTimeMillis() - insertTime) > timeOut;
        }

        /**
         * Check if an object equals this {@link TimedValue#value}. Note that it only
         * checks the {@link TimedValue#value}; The {@link TimedValue#insertTime} is
         * ignored!
         * 
         * @param o The object to compare to this {@link TimedValue}.
         * @return True if the {@link TimedValue#value} of the object equals the
         *         {@link TimedValue#value} of this object, otherwise false.
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
         * Check if an object equals the {@link TimedValue#value} of this
         * {@link TimedValue}. Note that a {@link TimedValue} object would return false!
         *
         * @param o The object to compare to the {@link TimedValue#value} of this
         *          {@link TimedValue}.
         * @return True if the {@link TimedValue#value} of the object equals the
         *         {@link TimedValue#value} of this {@link TimedValue}, otherwise false.
         */
        public final boolean equalsValue(Object o)
        {
            return o.equals(value);
        }
    }

    /**
     * Reinitialize this object. See {@link TimedMapCache#reInit(long, TimeUnit)}.
     */
    @Override
    public void restart()
    {
        this.reInit(this.timeOut, TimeUnit.MILLISECONDS);
    }
}
