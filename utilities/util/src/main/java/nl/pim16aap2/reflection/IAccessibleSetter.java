package nl.pim16aap2.reflection;

import java.lang.reflect.AccessibleObject;

/**
 * Shared interface for any type of reflection interaction system that can be used to set a reflection object as
 * accessible.
 *
 * @param <T>
 *     The type of the class setting the object as accessible.
 */
public interface IAccessibleSetter<T>
{
    /**
     * Sets the returned reflection object(s) as accessible.
     * <p>
     * See {@link AccessibleObject#setAccessible(boolean)}.
     *
     * @return The current object.
     */
    T setAccessible();
}
