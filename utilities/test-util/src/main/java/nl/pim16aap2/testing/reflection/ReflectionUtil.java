package nl.pim16aap2.testing.reflection;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

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
        try
        {
            ReflectionCache.get().getField(obj.getClass(), fieldName).set(obj, value);
        }
        catch (Exception e)
        {
            throw new RuntimeException(
                "Failed to set value of field '" + fieldName +
                    "' in class '" + obj.getClass().getTypeName() +
                    "' to '" + value + "'", e);
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
}
