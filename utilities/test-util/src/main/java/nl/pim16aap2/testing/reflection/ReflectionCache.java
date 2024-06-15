package nl.pim16aap2.testing.reflection;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to cache reflection results during testing.
 * <p>
 * This class is a thread-safe singleton. To get the singleton instance, use {@link #get()}.
 */
@ThreadSafe
final class ReflectionCache
{
    private static final ReflectionCache INSTANCE = new ReflectionCache();

    private final Map<String, Field> fieldCache = new ConcurrentHashMap<>();

    private final Map<String, Method> methodCache = new ConcurrentHashMap<>();

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
     * Gets an accessible object from a class. This method caches the result.
     *
     * @param cache
     *     The cache to use.
     * @param clz
     *     The class to get the object from.
     * @param objectName
     *     The name of the object to get.
     * @param lookupFunction
     *     The function to use to get the object.
     * @param <T>
     *     The type of the object to get.
     * @return The object. The object is set to accessible.
     */
    private static <T extends AccessibleObject> T getAccessibleObject(
        Map<String, T> cache,
        Class<?> clz,
        String objectName,
        AccessibleObjectLookupFunction<T> lookupFunction)
    {
        final String key = clz.getName() + "#" + objectName;
        return cache.computeIfAbsent(key, k ->
        {
            try
            {
                final T object = lookupFunction.apply(objectName);
                object.setAccessible(true);
                return object;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
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
        return getAccessibleObject(fieldCache, clazz, fieldName, clazz::getDeclaredField);
    }

    /**
     * Gets a method from a class. This method caches the result.
     *
     * @param clazz
     *     The class to get the method from.
     * @param methodName
     *     The name of the method to get.
     * @return The method. The method is set to accessible.
     *
     * @throws RuntimeException
     *     If the method could not be found or accessed.
     */
    public Method getMethod(Class<?> clazz, String methodName)
    {
        return getAccessibleObject(methodCache, clazz, methodName, clazz::getDeclaredMethod);
    }

    @FunctionalInterface
    private interface AccessibleObjectLookupFunction<T>
    {
        T apply(String objectName)
            throws Exception;
    }
}
