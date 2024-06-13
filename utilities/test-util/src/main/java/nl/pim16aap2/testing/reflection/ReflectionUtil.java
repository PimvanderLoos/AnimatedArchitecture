package nl.pim16aap2.testing.reflection;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility class for reflection.
 */
public final class ReflectionUtil
{
    private ReflectionUtil()
    {
        // Utility class
    }

    /**
     * Sets the value of a field in an object.
     *
     * @param obj
     *     The object to set the field in.
     * @param fieldName
     *     The name of the field to set.
     * @param value
     *     The value to set the field to.
     * @throws RuntimeException
     *     If the field could not be found or accessed.
     */
    public static void setField(Object obj, String fieldName, @Nullable Object value)
    {
        setField(obj.getClass(), obj, fieldName, value);
    }

    /**
     * Sets the value of a field in an object.
     *
     * @param source
     *     The class to get the field from.
     * @param obj
     *     The object to set the field in. This should be an instance of {@code source} or a subclass of it.
     * @param fieldName
     *     The name of the field to set.
     * @param value
     *     The value to set the field to.
     * @throws RuntimeException
     *     If the field could not be found or accessed.
     */
    public static void setField(Class<?> source, Object obj, String fieldName, @Nullable Object value)
    {
        try
        {
            ReflectionCache.get().getField(source, fieldName).set(obj, value);
        }
        catch (Exception exception)
        {
            throw new RuntimeException(
                String.format(
                    "Failed to set value of field '%s' in class '%s' to '%s'",
                    fieldName,
                    obj.getClass().getTypeName(),
                    value),
                exception
            );
        }
    }

    /**
     * Gets a named field from a class.
     *
     * @param clazz
     *     The class to get the field from.
     * @param fieldName
     *     The name of the field to get.
     * @return The field. The field is set to accessible.
     */
    public static Field getField(Class<?> clazz, String fieldName)
    {
        return ReflectionCache.get().getField(clazz, fieldName);
    }

    /**
     * Gets a named field from an object.
     *
     * @param obj
     *     The object to get the field from.
     * @param fieldName
     *     The name of the field to get.
     * @return The field. The field is set to accessible.
     */
    public static Field getField(Object obj, String fieldName)
    {
        return getField(obj.getClass(), fieldName);
    }

    /**
     * Gets a named method from a class.
     *
     * @param clazz
     *     The class to get the method from.
     * @param methodName
     *     The name of the method to get.
     * @return The method. The method is set to accessible.
     */
    public static Method getMethod(Class<?> clazz, String methodName)
    {
        return ReflectionCache.get().getMethod(clazz, methodName);
    }

    /**
     * Gets a named method from an object.
     *
     * @param obj
     *     The object to get the method from.
     * @param methodName
     *     The name of the method to get.
     * @return The method. The method is set to accessible.
     */
    public static Method getMethod(Object obj, String methodName)
    {
        return getMethod(obj.getClass(), methodName);
    }
}
