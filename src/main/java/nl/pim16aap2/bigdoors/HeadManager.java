package nl.pim16aap2.bigdoors;

import java.lang.reflect.Field;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonObject;

import nl.pim16aap2.bigdoors.util.TimedCache;

public abstract class HeadManager
{
    protected final BigDoors plugin;
    protected TimedCache<UUID, ItemStack> headMap;
    protected TimedCache<String, JsonObject> map;

    public HeadManager(BigDoors plugin)
    {
        this.plugin = plugin;
    }

    public void init()
    {
        map = new TimedCache<String, JsonObject> (plugin, plugin.getConfigLoader().headCacheTimeout());
        headMap = new TimedCache<UUID, ItemStack>(plugin, plugin.getConfigLoader().headCacheTimeout());
    }

    public void reload()
    {
        headMap.reinit(plugin.getConfigLoader().headCacheTimeout());
        map.reinit(plugin.getConfigLoader().headCacheTimeout());
    }

    public ItemStack getPlayerHead(UUID playerUUID, String playerName, int x, int y, int z, Player player)
    {
        if (headMap.contains(playerUUID))
            return headMap.get(playerUUID);

        createSkull(x, y, z, playerName, playerUUID, player);
        return headMap.get(playerUUID);
    }

    protected abstract String[] getFromPlayer(Player playerBukkit);
    protected abstract void createSkull(int x, int y, int z, String name, UUID playerUUID, Player p);
    protected abstract String[] getFromName(String name, Player p);

    private long start;
    private long end;

    protected void ExecutionTimer()
    {
        reset();
        start = System.currentTimeMillis();
    }

    protected void end()
    {
        end = System.currentTimeMillis();
    }

    protected long duration()
    {
        return (end - start);
    }

    protected void reset()
    {
        start = 0;
        end = 0;
    }




    public static <T> FieldAccessor<T> getField(Class<?> target, String name, Class<T> fieldType)
    {
        return getField(target, name, fieldType, 0);
    }

    public static <T> FieldAccessor<T> getField(Class<?> target, Class<T> fieldType, int index)
    {
        return getField(target, null, fieldType, index);
    }

    private static <T> FieldAccessor<T> getField(Class<?> target, String name, Class<T> fieldType, int index)
    {
        for (final Field field : target.getDeclaredFields())
            if ((name == null || field.getName().equals(name)) && fieldType.isAssignableFrom(field.getType())
                && index-- <= 0)
            {
                field.setAccessible(true);

                // A function for retrieving a specific field value
                return new FieldAccessor<T>()
                {
                    @SuppressWarnings("unchecked")
                    @Override
                    public T get(Object target)
                    {
                        try
                        {
                            return (T) field.get(target);
                        }
                        catch (IllegalAccessException e)
                        {
                            throw new RuntimeException("Cannot access reflection.", e);
                        }
                    }

                    @Override
                    public void set(Object target, Object value)
                    {
                        try
                        {
                            field.set(target, value);
                        }
                        catch (IllegalAccessException e)
                        {
                            throw new RuntimeException("Cannot access reflection.", e);
                        }
                    }

                    @Override
                    public boolean hasField(Object target)
                    {
                        // target instanceof DeclaringClass
                        return field.getDeclaringClass().isAssignableFrom(target.getClass());
                    }
                };
            }

        // Search in parent classes
        if (target.getSuperclass() != null)
            return getField(target.getSuperclass(), name, fieldType, index);
        throw new IllegalArgumentException("Cannot find field with type " + fieldType);
    }

    public interface FieldAccessor<T>
    {
        /**
         * Retrieve the content of a field.
         *
         * @param target the target object, or NULL for a static field
         * @return the value of the field
         */
        public T get(Object target);

        /**
         * Set the content of a field.
         *
         * @param target the target object, or NULL for a static field
         * @param value  the new value of the field
         */
        public void set(Object target, Object value);

        /**
         * Determine if the given object has this field.
         *
         * @param target the object to test
         * @return TRUE if it does, FALSE otherwise
         */
        public boolean hasField(Object target);
    }
}
