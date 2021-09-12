package nl.pim16aap2.bigdoors.testing;

import org.jetbrains.annotations.Nullable;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public final class Util
{
    private Util()
    {
        // Utility class
    }

    public static @Nullable Object newMock(Class<?> type)
    {
        return newMock(type, Mockito.withSettings());
    }

    public static @Nullable Object newMock(Class<?> type, Answer<?> defaultAnswer)
    {
        return newMock(type, Mockito.withSettings().defaultAnswer(defaultAnswer));
    }

    public static @Nullable Object newMock(Class<?> type, MockSettings mockSettings)
    {
        if (type.isPrimitive())
            return getPrimitiveDefault(type);
        final @Nullable Object defaultValue = getDefaultValue(type);
        if (defaultValue != null)
            return defaultValue;
        return tryMock(type, mockSettings);
    }

    private static @Nullable Object tryMock(Class<?> type, MockSettings mockSettings)
    {
        try
        {
            return Mockito.mock(type, mockSettings);
        }
        catch (MockitoException e)
        {
            return null;
        }
    }

    private static @Nullable Object getDefaultValue(Class<?> type)
    {
        if (Integer.class.isAssignableFrom(type))
            return 0;
        if (Double.class.isAssignableFrom(type))
            return 0;
        if (Float.class.isAssignableFrom(type))
            return 0;
        if (Long.class.isAssignableFrom(type))
            return 0;
        if (Short.class.isAssignableFrom(type))
            return 0;
        if (Character.class.isAssignableFrom(type))
            return 0;
        if (Byte.class.isAssignableFrom(type))
            return 0;
        if (Boolean.class.isAssignableFrom(type))
            return Boolean.FALSE;
        if (String.class.isAssignableFrom(type))
            return "";
        if (Map.class.isAssignableFrom(type))
            return Collections.emptyMap();
        if (Set.class.isAssignableFrom(type))
            return Collections.emptySet();
        if (List.class.isAssignableFrom(type))
            return Collections.emptyList();
        if (Collections.class.isAssignableFrom(type))
            return Collections.emptyList();
        return null;
    }

    private static Object getPrimitiveDefault(Class<?> type)
    {
        if (int.class.isAssignableFrom(type))
            return 0;
        if (double.class.isAssignableFrom(type))
            return 0;
        if (float.class.isAssignableFrom(type))
            return 0;
        if (long.class.isAssignableFrom(type))
            return 0;
        if (short.class.isAssignableFrom(type))
            return 0;
        if (char.class.isAssignableFrom(type))
            return 0;
        if (byte.class.isAssignableFrom(type))
            return 0;
        if (boolean.class.isAssignableFrom(type))
            return false;
        throw new IllegalArgumentException("Could not find default primitive value for type: " + type);
    }
}
