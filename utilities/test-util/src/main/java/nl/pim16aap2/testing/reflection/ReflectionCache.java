package nl.pim16aap2.testing.reflection;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to cache reflection results during testing.
 * <p>
 * This class is a thread-safe singleton. To get the singleton instance, use {@link #get()}.
 */
@ThreadSafe final class ReflectionCache
{
    private static final ReflectionCache INSTANCE = new ReflectionCache();

    private final Map<String, Field> fieldCache = new ConcurrentHashMap<>();

    private ReflectionCache()
    {
        // Singleton
    }

    /**
     * Gets the singleton instance of this class.
     *
     * @return The singleton instance of this class.
     */
    public static ReflectionCache get()
    {
        return INSTANCE;
    }

    /**
     * Gets a field from a class. This method caches the result.
     *
     * @param clazz
     *     The class to get the field from.
     * @param fieldName
     *     The name of the field to get.
     * @return The field. The field is set to accessible.
     *
     * @throws RuntimeException
     *     If the field could not be found or accessed.
     */
    public Field getField(Class<?> clazz, String fieldName)
    {
        final String key = clazz.getName() + "#" + fieldName;
        return fieldCache.computeIfAbsent(key, k ->
        {
            try
            {
                final Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            }
            catch (NoSuchFieldException e)
            {
                throw new RuntimeException(e);
            }
        });
    }
}
