package nl.pim16aap2.bigDoors.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class TimedCache<K, V>
{
    private final JavaPlugin plugin;
    private int timeout = -1;
    private Map<K, Value<V>> cache;
    private BukkitTask verifyTask;

    public TimedCache(JavaPlugin plugin, int time)
    {
        cache = new ConcurrentHashMap<>();
        this.plugin = plugin;
        reinit(time);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        cache.forEach((K, V) -> sb.append(K.toString()).append("=").append(V.toString()).append(", "));
//        if (sb.length() > 3)
//            sb.delete(sb.length() - 1, sb.length() - 3);
        sb.append("}");
        return sb.toString();
    }

    public void reinit(int time)
    {
        if (timeout != time)
        {
            destructor();
            timeout = time;
            startTask();
        }
        cache.clear();
    }

    private void startTask()
    {
        if (timeout > 0)
            // Verify cache 1/2 the timeout time. Timeout is in minutes, task timer in ticks, so 1200 * timeout = timeout in ticks.
            verifyTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> verifyCache(), 600 * timeout, 1200 * timeout);
    }

    // Take care of killing the async task (if needed).
    public void destructor()
    {
        if (timeout > 0)
            verifyTask.cancel();
    }

    public void put(K key, V value)
    {
        if (timeout < 0)
            return;
        cache.put(key, new Value<>(value));
    }

    public V get(K key)
    {
        if (timeout < 0)
            return null;
        if (cache.containsKey(key))
            return cache.get(key).timedOut() ? null : cache.get(key).value;
        return null;
    }

    public boolean contains(K key)
    {
        return get(key) != null;
    }

    public void invalidate(K key)
    {
        cache.remove(key);
    }

    // Loop over all cache entries to verify they haven't timed out yet.
    private void verifyCache()
    {
        Iterator<Entry<K, TimedCache<K, V>.Value<V>>> it = cache.entrySet().iterator();
        while (it.hasNext())
        {
            Entry<K, TimedCache<K, V>.Value<V>> entry = it.next();
            if (entry.getValue().timedOut())
                it.remove();
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

    public ArrayList<K> getKeys()
    {
        if (timeout < 0)
            return null;
        ArrayList<K> keys = new ArrayList<>();
        Iterator<Entry<K, TimedCache<K, V>.Value<V>>> it = cache.entrySet().iterator();
        while (it.hasNext())
            keys.add(it.next().getKey());
        return keys;
    }

    public int getChunkCount()
    {
        return cache.size();
    }
}
