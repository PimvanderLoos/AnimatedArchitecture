package nl.pim16aap2.bigdoors.util;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class TimedMapCache<K, V> extends Restartable implements Map<K, V>
{
    private int timeout = -1;
    private Map<K, Value<V>> map;
    private TimerTask verifyTask;
    private Timer taskTimer;
    private final Supplier<? extends Map> ctor;
    private final Class<? extends Map> mapType;

    public TimedMapCache(final RestartableHolder holder, Supplier<? extends Map> ctor, int time)
    {
        super(holder);

        this.ctor = Objects.requireNonNull(ctor);
        map = ctor.get();
        mapType = map.getClass();

        if (map == null)
            System.out.println("MAP IS NULL!");
        else
            System.out.println("MAP IS NOT NULL!");
        Thread.dumpStack();

//        System.exit(0);

        verifyTask = new TimerTask() {
            @Override
            public void run()
            {
                verifyCache();
            }
        };
        reInit(time);
    }

    public void reInit(int time)
    {
        if (timeout != time)
        {
            destructor();
            timeout = time;
            startTask();
        }
        map.clear();
    }

    private void startTask()
    {
        if (timeout > 0)
        {
            // Verify cache 1/2 the timeout time. Timeout is in minutes, task timer in ticks, so 1200 * timeout = timeout in ticks.
            taskTimer = new Timer();
            taskTimer.scheduleAtFixedRate(verifyTask, 60000 * timeout, 60000 * timeout / 2);
        }
    }

    // Take care of killing the async task (if needed).
    public void destructor()
    {
        if (timeout > 0)
            taskTimer.cancel();
    }

    public V get(Object key)
    {
        if (timeout < 0)
            return null;
        if (map.containsKey(key))
            return map.get(key).timedOut() ? null : (V) map.get(key).value;
        return null;
    }

    public void invalidate(K key)
    {
        map.remove(key);
    }

    // Loop over all cache entries to verify they haven't timed out yet.
    private void verifyCache()
    {
        Iterator<Entry<K, Value<V>>> it = map.entrySet().iterator();
        while (it.hasNext())
        {
            Entry<K, Value<V>> entry = it.next();
            if (entry.getValue().timedOut())
                map.remove(entry.getKey());
        }
    }

    @Override
    public int size()
    {
        return map.size();
    }

    @Override
    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        return false;
    }

    @Override
    public V put(K key, V value)
    {
        if (timeout < 0)
            return null;
        Value<V> newVal = new Value<V>(value);
        map.put(key, newVal);
        return newVal.value;
    }

    @Override
    public V remove(Object key)
    {
        return map.remove(key).value;
    }

    @Override
    public void putAll(Map m)
    {
        map.putAll(m);
    }

    @Override
    public void clear()
    {
        map.clear();
    }

    @Override
    public Set keySet()
    {
        return null;
    }

    @Override
    public Collection values()
    {
        return map.values();
    }

    @Override
    public java.util.Set entrySet() {
        return mapType.cast(map).entrySet();
    }

    public final class Value<V>
    {
        public final long insertTime;
        public final V value;

        public Value(V val)
        {
            value = val;
            insertTime = System.currentTimeMillis();
        }

        public boolean timedOut()
        {
            return TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - insertTime) > timeout;
        }
    }

    @Override
    public void restart()
    {
        this.reInit(this.timeout);
    }
}
