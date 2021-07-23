package nl.pim16aap2.bigDoors.reflection;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused") final class ReflectionUtils
{
    private static final @Nullable Method ENUM_VALUE_NAME;

    static
    {
        Method m = null;
        try
        {
            m = Enum.class.getMethod("name");
        }
        catch (NoSuchMethodException e)
        {
            e.printStackTrace();
        }
        ENUM_VALUE_NAME = m;
    }

    private ReflectionUtils()
    {
        // utility class
    }

    public static @NotNull Object[] getEnumValues(@NotNull Class<?> source)
    {
        if (ENUM_VALUE_NAME == null)
            throw new IllegalStateException("Failed to find name method for enums!");

        final Object[] values = source.getEnumConstants();
        if (values == null)
            throw new IllegalStateException("Class " + source + " is not an enum!");
        return values;
    }

    public static @NotNull Object getEnumConstant(@NotNull Class<?> source, @NotNull String name)
    {
        return getEnumConstant(true, source, name);
    }

    @Contract("true, _, _ -> !null")
    public static Object getEnumConstant(boolean nonNull, @NotNull Class<?> source, @NotNull String name)
    {
        return getEnumConstant(nonNull, source, null, name);
    }

    public static @NotNull Object getEnumConstant(@NotNull Class<?> source, int index)
    {
        return getEnumConstant(true, source, index);
    }

    @Contract("true, _, _ -> !null")
    public static Object getEnumConstant(boolean nonNull, @NotNull Class<?> source, int index)
    {
        return getEnumConstant(nonNull, source, index, null);
    }

    @Contract("true, _, _, _ -> !null")
    private static Object getEnumConstant(boolean nonNull, @NotNull Class<?> source, @Nullable Integer index,
                                          @Nullable String name)
    {
        if (index == null && name == null)
            throw new IllegalArgumentException(
                "Both index and name are null! Exactly one of them should be specified!");
        if (index != null && name != null)
            throw new IllegalArgumentException(
                "Both index and name are not null! Exactly one of them should be specified!");

        Object[] values = getEnumValues(source);
        for (int idx = 0; idx < values.length; ++idx)
        {
            try
            {
                final Object obj = values[idx];
                if (index != null && idx == index)
                    return obj;
                //noinspection ConstantConditions
                if (name != null && name.equals(ENUM_VALUE_NAME.invoke(obj)))
                    return obj;
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                break;
            }
        }
        if (nonNull)
            throw new NullPointerException(
                "Failed to find enum value with identifier \"" + (index == null ? name : index) + "\" in class: " +
                    source);
        return null;
    }

    /**
     * Finds a class from a set of names.
     *
     * @param nonNull Whether to allow returning a null value when no classes could be found. When this is true, and no
     *                classes could be found for any of the provided names, an exception will be thrown.
     * @param names   The fully qualified name(s) of the class to find.
     * @return The first class from the list that could be found or, if no classes could be found, null.
     */
    @Contract("true, _, _ -> !null")
    public static Class<?> findFirstClass(boolean nonNull, int modifiers, @NotNull String... names)
    {
        for (String name : names)
        {
            try
            {
                final Class<?> clz = Class.forName(name);
                if (modifiers != 0 && clz.getModifiers() != modifiers)
                    continue;
                return clz;
            }
            catch (ClassNotFoundException e)
            {
                // ignored
            }
        }
        if (nonNull)
            throw new NullPointerException(
                "Failed to find " + (names.length > 1 ? "any of the classes: " : "the class: ") +
                    Arrays.toString(names));
        return null;
    }

    public static @NotNull Field getField(@NotNull Class<?> source, @NotNull String name, int modifiers)
    {
        return getField(true, source, name, modifiers, null);
    }

    @Contract("true, _, _, _ -> !null")
    public static Field getField(boolean nonNull, @NotNull Class<?> source, @NotNull String name, int modifiers)
    {
        return getField(nonNull, source, name, modifiers, null);
    }

    public static @NotNull Field getField(@NotNull Class<?> source, @NotNull String name, int modifiers,
                                          @Nullable Class<?> type)
    {
        return getField(true, source, name, modifiers, type);
    }

    @Contract("true, _, _, _, _ -> !null")
    public static Field getField(boolean nonNull, @NotNull Class<?> source, @NotNull String name,
                                 int modifiers, @Nullable Class<?> type)
    {
        for (Field field : source.getDeclaredFields())
        {
            if (type != null && !field.getType().equals(type))
                continue;
            if (modifiers != 0 && field.getModifiers() != modifiers)
                continue;
            if (!field.getName().equals(name))
                continue;
            return field;
        }
        if (nonNull)
            throw new NullPointerException(
                "Failed to find field \"" + name + "\" in class \"" + source);
        return null;
    }

    public static @NotNull Class<?> getReturnType(@NotNull Class<?> source, @NotNull String methodName)
    {
        return getReturnType(true, source, methodName);
    }

    @Contract("true, _, _ -> !null")
    public static Class<?> getReturnType(boolean nonNull, @NotNull Class<?> source, @NotNull String methodName)
    {
        for (Method method : source.getDeclaredMethods())
            if (method.getName().equals(methodName))
                return method.getReturnType();
        if (nonNull)
            throw new NullPointerException(
                "Failed to find method \"" + methodName + "\" in class: \"" + source + "\"!");
        return null;
    }

    private static @Nullable Method findMethod(@NotNull Class<?> source, @Nullable String name, int modifiers,
                                               @Nullable ParameterGroup parameters, @Nullable Class<?> returnType)
    {
        for (Method method : source.getDeclaredMethods())
        {
            if (modifiers != 0 && method.getModifiers() != modifiers)
                continue;
            if (returnType != null && !method.getReturnType().equals(returnType))
                continue;
            if (name != null && !method.getName().equals(name))
                continue;
            if (parameters != null && !parameters.matches(method.getParameterTypes()))
                continue;
            return method;
        }
        return null;
    }

    @Contract("true, _, _, _, _, _, _ -> !null")
    public static Method findMethod(boolean nonNull, boolean checkSuperClasses, @NotNull Class<?> source,
                                    @Nullable String name, int modifiers, @Nullable ParameterGroup parameters,
                                    @Nullable Class<?> returnType)
    {
        Class<?> check = source;
        do
        {
            Method m = findMethod(check, name, modifiers, parameters, returnType);
            if (m != null)
                return m;
            check = source.getSuperclass();
        }
        while (checkSuperClasses && check != Object.class);
        if (nonNull)
            throw new NullPointerException(
                "Failed to find method in class \"" + source + "\" with modifiers " + modifiersToString(modifiers) +
                    ", return type \"" + returnType + "\", and arguments: " + parameters);
        return null;
    }

    /**
     * See {@link #getField(boolean, Class, int, Class)}, with nonNull enabled.
     */
    public static @NotNull Field getField(@NotNull Class<?> source, int modifiers, @NotNull Class<?> type)
    {
        return getField(true, source, modifiers, type);
    }

    /**
     * Attempts to find a field in a class. Only the first field that matches the desired specification is returned. If
     * all fields matching the specification are desired, use {@link #getFields(Class, int, Class)} instead.
     *
     * @param nonNull   Whether to allow returning a null value when no classes could be found. When this is true, and
     *                  no classes could be found for any of the provided names, an exception will be thrown.
     * @param source    The class in which to look for the field.
     * @param modifiers The {@link Modifier}s that the field should match. E.g. {@link Modifier#PUBLIC}. When this is
     *                  null, no modifier constraints will be applied during the search.
     *                  <p>
     *                  When a mix of two or more modifiers is needed, {@link #getModifiers(int...)} can be used.
     * @param type      The type of the field to search for.
     * @return The first field that matches the specification.
     */
    @Contract("true, _, _, _ -> !null")
    public static Field getField(boolean nonNull, @NotNull Class<?> source, int modifiers,
                                 @NotNull Class<?> type)
    {
        for (Field field : source.getDeclaredFields())
        {
            if (modifiers != 0 && field.getModifiers() != modifiers)
                continue;

            if (!field.getType().equals(type))
                continue;

            return field;
        }

        if (nonNull)
            throw new NullPointerException("Failed to find field in class \"" + source + "\" with modifiers " +
                                               modifiersToString(modifiers) + " of type \"" + type + "\"");
        return null;
    }

    public static @NotNull List<Field> getFields(int expected, @NotNull Class<?> source, int modifiers,
                                                 @NotNull Class<?> type)
        throws IllegalStateException
    {
        List<Field> ret = getFields(source, modifiers, type);
        if (ret.size() != expected)
            throw new IllegalStateException(
                String.format("Expected %d fields of type %s with modifiers %s in class %s, but found %d!",
                              expected, type, modifiersToString(modifiers), source,
                              ret.size()));
        return ret;
    }

    public static @NotNull List<Field> getFields(@NotNull Class<?> source, int modifiers,
                                                 @NotNull Class<?> type)
    {
        List<Field> ret = new ArrayList<>();
        for (Field field : source.getDeclaredFields())
        {
            if (modifiers != 0 && field.getModifiers() != modifiers)
                continue;

            if (!field.getType().equals(type))
                continue;

            ret.add(field);
        }
        return ret;
    }

    public static @NotNull Constructor<?> findCTor(@NotNull Class<?> source, @NotNull Class<?>... args)
    {
        return findCTor(true, source, args);
    }

    @Contract("true, _, _ -> !null")
    public static Constructor<?> findCTor(boolean nonNull, @NotNull Class<?> source, @NotNull Class<?>... args)
    {
        try
        {
            return source.getDeclaredConstructor(args);
        }
        catch (NoSuchMethodException e)
        {
            if (nonNull)
                throw new NullPointerException(
                    "Failed to find constructor of class \"" + source + "\" with arguments: " + Arrays.toString(args));
            return null;
        }
    }

    /**
     * Gets the final modifier from the provided integer flags.
     * <p>
     * Example usage: <code>ReflectionUtils.getModifier(java.lang.reflect.Modifier.PRIVATE,
     * java.lang.reflect.Modifier.STATIC</code>
     *
     * @param mods The modifiers to apply.
     * @return The final value with all the modifiers applied.
     */
    public static int getModifiers(int... mods)
    {
        int ret = 0;
        for (int mod : mods)
            ret |= mod;
        return ret;
    }

    public static @NotNull Constructor<?> findCTor(@NotNull Class<?> source, int modifiers,
                                                   @Nullable ParameterGroup parameters)
    {
        return findCTor(true, source, modifiers, parameters);
    }

    @Contract("true, _, _, _ -> !null")
    public static Constructor<?> findCTor(boolean nonnull, @NotNull Class<?> source, int modifiers,
                                          @Nullable ParameterGroup parameters)
    {
        for (Constructor<?> ctor : source.getDeclaredConstructors())
        {
            if (modifiers != 0 && ctor.getModifiers() != modifiers)
                continue;
            if (parameters != null && !parameters.matches(ctor.getParameterTypes()))
                continue;
            return ctor;
        }
        if (nonnull)
            throw new NullPointerException(
                "Failed to find constructor in class " + source + " with modifiers " + modifiersToString(modifiers) +
                    " and parameters: " + parameters);
        return null;
    }

    private static @NotNull String modifiersToString(int modifiers)
    {
        return modifiers == 0 ? "0" : Modifier.toString(modifiers);
    }
}
