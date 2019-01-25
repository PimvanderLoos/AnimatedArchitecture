package nl.pim16aap2.bigDoors.util;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import nl.pim16aap2.bigDoors.BigDoors;

public class TimedCache<K, V>
{
    private final BigDoors plugin;
    private int timeout = -1;
    private Hashtable<K, Value<V>> hashtable;
    private BukkitTask verifyTask;

    public TimedCache(BigDoors plugin, int time)
    {
        hashtable = new Hashtable<K, Value<V>>();
        this.plugin = plugin;
        reinit(time);
    }

    public void reinit(int time)
    {
        if (timeout != time)
        {
            destructor();
            timeout = time;
            startTask();
        }
        hashtable.clear();
    }

    private void startTask()
    {
        if (timeout > 0)
            // Verify cache every minute.
            verifyTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> verifyCache(), 1200, 1200);
    }

    // Take care of killing the async task (if needed).
    public void destructor()
    {
        if (timeout > 0)
            verifyTask.cancel();
    }

    public void put(K key, V value)
    {
        hashtable.put(key, new Value<V>(value));
    }

    public V get(K key)
    {
        if (hashtable.containsKey(key))
            return hashtable.get(key).value;
        return null;
    }

    public void invalidate(K key)
    {
        hashtable.remove(key);
    }

    // Loop over all cache entries to verify they haven't timed out yet.
    void verifyCache()
    {
        Iterator<Entry<K, TimedCache<K, V>.Value<V>>> it = hashtable.entrySet().iterator();
        while (it.hasNext())
        {
            Entry<K, TimedCache<K, V>.Value<V>> entry = it.next();
            if (entry.getValue().timedOut())
                hashtable.remove(entry.getKey());
        }

    }

    private final class Value<T>
    {
        public final long insertTime;
        public final T value;

        public Value(T val)
        {
            value = val;
            insertTime = System.currentTimeMillis();
        }

        public boolean timedOut()
        {
            return TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - insertTime) > timeout;
        }
    }
}
