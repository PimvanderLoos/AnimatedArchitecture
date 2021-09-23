package nl.pim16aap2.reflection;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Represents the reflection backend for the {@link ReflectionFinder} classes.
 *
 * @author Pim
 */
final class ReflectionBackend
{
    private static final @Nullable Method ENUM_VALUE_NAME;

    static
    {
        @Nullable Method m = null;
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

    private ReflectionBackend()
    {
        // utility class
    }

    /**
     * Retrieves all enum constants in a class.
     *
     * @param source
     *     The class from which to retrieve the enum constants. This should be an enum.
     * @return All enum constants in a class.
     *
     * @throws IllegalStateException
     *     When the provided class is not an enum or when enum processing is currently disabled.
     */
    public static Object[] getEnumValues(Class<?> source)
    {
        if (ENUM_VALUE_NAME == null)
            throw new IllegalStateException("Failed to find name method for enums!");

        final Object[] values = source.getEnumConstants();
        if (values == null)
            throw new IllegalStateException("Class " + source.getName() + " is not an enum!");
        return values;
    }

    /**
     * Retrieves an enum constant from its name.
     *
     * @param source
     *     The class from which to retrieve the enum constants. This should be an enum.
     * @param name
     *     The name of the enum constant to look for.
     * @return The enum constant with the given name or null when it could not be found and null is allowed.
     *
     * @throws IllegalStateException
     *     When the provided class is not an enum or when enum processing is currently disabled.
     */
    public static @Nullable Object getNamedEnumConstant(Class<?> source, String name)
    {
        if (ENUM_VALUE_NAME == null)
            throw new IllegalStateException("Failed to find name method for enums!");

        final Object[] values = getEnumValues(source);
        for (final Object value : values)
        {
            try
            {
                if (name.equals(ENUM_VALUE_NAME.invoke(value)))
                    return value;
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                break;
            }
        }
        return null;
    }

    /**
     * Finds a class from a set of names.
     *
     * @param names
     *     The fully qualified name(s) of the class to find.
     * @return The first class from the list that could be found or, if no classes could be found, null.
     */
    public static @Nullable Class<?> findFirstClass(int modifiers, String... names)
    {
        for (final String name : names)
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
        return null;
    }

    /**
     * Attempts to find a field in a class based on the input configuration. If all fields matching the specification
     * are desired, use {@link #getFields(Class, int, Class)} instead.
     *
     * @param source
     *     The class in which to look for the field.
     * @param name
     *     The name of the field to look for.
     * @param modifiers
     *     The {@link Modifier}s that the field should match. E.g. {@link Modifier#PUBLIC}. When this is 0, no modifier
     *     constraints will be applied during the search.
     *     <p>
     *     When a mix of two or more modifiers is needed, {@link #getModifiers(int...)} can be used.
     * @param type
     *     The type of the field to search for.
     * @return The first field that matches the specification.
     */
    public static @Nullable Field getField(Class<?> source, String name, int modifiers,
                                           @Nullable Class<?> type)
    {
        for (final Field field : source.getDeclaredFields())
        {
            if (type != null && !field.getType().equals(type))
                continue;
            if (modifiers != 0 && field.getModifiers() != modifiers)
                continue;
            if (!field.getName().equals(name))
                continue;
            return field;
        }
        return null;
    }

    /**
     * Attempts to find a field in a class. Only the first field that matches the desired specification is returned. If
     * all fields matching the specification are desired, use {@link #getFields(Class, int, Class)} instead.
     *
     * @param source
     *     The class in which to look for the field.
     * @param modifiers
     *     The {@link Modifier}s that the field should match. E.g. {@link Modifier#PUBLIC}. When this is 0, no modifier
     *     constraints will be applied during the search.
     *     <p>
     *     When a mix of two or more modifiers is needed, {@link #getModifiers(int...)} can be used.
     * @param type
     *     The type of the field to search for.
     * @return The first field that matches the specification.
     */
    public static @Nullable Field getField(Class<?> source, int modifiers, Class<?> type)
    {
        for (final Field field : source.getDeclaredFields())
        {
            if (modifiers != 0 && field.getModifiers() != modifiers)
                continue;

            if (!field.getType().equals(type))
                continue;

            return field;
        }
        return null;
    }

    /**
     * Attempts to find all fields in a class that match a specific set of modifiers and type.
     *
     * @param source
     *     The class in which to look for the fields.
     * @param modifiers
     *     The {@link Modifier}s that the fields should match. E.g. {@link Modifier#PUBLIC}. When this is 0, no modifier
     *     constraints will be applied during the search.
     *     <p>
     *     When a mix of two or more modifiers is needed, {@link #getModifiers(int...)} can be used.
     * @param type
     *     The type of the field to search for.
     * @return All fields in the source class that match the input configuration.
     */
    public static List<Field> getFields(Class<?> source, int modifiers, Class<?> type)
    {
        final List<Field> ret = new ArrayList<>();
        for (final Field field : source.getDeclaredFields())
        {
            if (modifiers != 0 && field.getModifiers() != modifiers)
                continue;

            if (!field.getType().equals(type))
                continue;

            ret.add(field);
        }
        return ret;
    }

    /**
     * Finds a method in a class.
     *
     * @param source
     *     The class in which to look for the method.
     * @param name
     *     The name of the method. When this is null, the name of the method is ignored.
     * @param modifiers
     *     The {@link Modifier}s that the method should match. E.g. {@link Modifier#PUBLIC}. When this is 0, no modifier
     *     constraints will be applied during the search.
     *     <p>
     *     When a mix of two or more modifiers is needed, {@link #getModifiers(int...)} can be used.
     * @param parameters
     *     The parameters of the method. When this is null, the method's parameters are ignored.
     * @param returnType
     *     The return type the method should have. When this is null, the return type will be ignored.
     * @return The method matching the specified description.
     */
    private static @Nullable Method findMethod(Class<?> source, @Nullable String name, int modifiers,
                                               @Nullable ParameterGroup parameters, @Nullable Class<?> returnType)
    {
        for (final Method method : source.getDeclaredMethods())
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

    /**
     * Finds a method in a class.
     * <p>
     * When both `checkSuperClasses` and `checkInterfaces` are allowed, the superclass will be evaluated first, followed
     * by the interfaces of the superclasses.
     *
     * @param checkSuperClasses
     *     Whether to include methods from superclasses of the source class in the search.
     * @param checkInterfaces
     *     Whether to include methods from super interfaces of the source class.
     * @param source
     *     The class in which to look for the method.
     * @param name
     *     The name of the method. When this is null, the name of the method is ignored.
     * @param modifiers
     *     The {@link Modifier}s that the method should match. E.g. {@link Modifier#PUBLIC}. When this is 0, no modifier
     *     constraints will be applied during the search.
     *     <p>
     *     When a mix of two or more modifiers is needed, {@link #getModifiers(int...)} can be used.
     * @param parameters
     *     The parameters of the method. When this is null, the method's parameters are ignored.
     * @param returnType
     *     The return type the method should have. When this is null, the return type will be ignored.
     * @return The method matching the specified description.
     */
    public static @Nullable Method findMethod(final boolean checkSuperClasses, final boolean checkInterfaces,
                                              Class<?> source, @Nullable String name, int modifiers,
                                              @Nullable ParameterGroup parameters, @Nullable Class<?> returnType)
    {
        @Nullable Method m = findMethod(source, name, modifiers, parameters, returnType);
        if (m != null)
            return m;

        boolean continueSuperClassChecking = checkSuperClasses;
        boolean continueInterfaceChecking = checkInterfaces;
        while (continueSuperClassChecking || continueInterfaceChecking)
        {
            // Superclasses take precedence, so evaluate them first.
            if (continueSuperClassChecking)
            {
                // Recursion base case for superclasses.
                final Class<?> superClass = source.getSuperclass();
                if (superClass == null)
                {
                    continueSuperClassChecking = false;
                    continue;
                }

                m = findMethod(true, continueInterfaceChecking, superClass, name, modifiers, parameters, returnType);
                if (m != null)
                    return m;
            }

            if (continueInterfaceChecking)
            {
                final Class<?>[] superInterfaces = source.getInterfaces();
                // Recursion base case for interfaces.
                if (superInterfaces.length == 0)
                {
                    continueInterfaceChecking = false;
                    continue;
                }

                for (final Class<?> superInterface : superInterfaces)
                {
                    m = findMethod(false, true, superInterface, name, modifiers, parameters, returnType);
                    if (m != null)
                        return m;
                }
            }
        }
        return null;
    }

    /**
     * Finds a constructor in a class.
     *
     * @param source
     *     The class in which to look for the constructor.
     * @param modifiers
     *     The {@link Modifier}s that the constructor should match. E.g. {@link Modifier#PUBLIC}. When this is 0, no
     *     modifier constraints will be applied during the search.
     *     <p>
     *     When a mix of two or more modifiers is needed, {@link #getModifiers(int...)} can be used.
     * @param parameters
     *     The parameters of the constructor. When this is null, the constructor's parameters are ignored.
     * @return The constructor matching the specified description.
     */
    public static @Nullable Constructor<?> findCTor(Class<?> source, int modifiers, @Nullable ParameterGroup parameters)
    {
        for (final Constructor<?> ctor : source.getDeclaredConstructors())
        {
            if (modifiers != 0 && ctor.getModifiers() != modifiers)
                continue;
            if (parameters != null && !parameters.matches(ctor.getParameterTypes()))
                continue;
            return ctor;
        }
        return null;
    }

    /**
     * Gets the final modifier from the provided integer flags.
     * <p>
     * Example usage: <code>ReflectionBackend.getModifier(java.lang.reflect.Modifier.PRIVATE,
     * java.lang.reflect.Modifier.STATIC</code>
     *
     * @param mods
     *     The modifiers to apply.
     * @return The final value with all the modifiers applied.
     */
    public static int getModifiers(int... mods)
    {
        int ret = 0;
        for (final int mod : mods)
            ret |= mod;
        return ret;
    }

    /**
     * Converts a set of modifiers to a String.
     * <p>
     * This is different from using {@link Modifier#toString(int)}, because that returns an empty String when the
     * modifiers equal to '0', while this method returns "[*]".
     *
     * @param modifiers
     *     The modifiers to convert into a String. This is a flag value composed of flags such as {@link
     *     Modifier#PUBLIC}. See {@link #getModifiers(int...)}.
     * @return A String representing the modifiers.
     */
    public static String optionalModifiersToString(int modifiers)
    {
        return modifiers == 0 ? "[*]" : Modifier.toString(modifiers);
    }

    /**
     * Gets the String representation of an optional value.
     * <p>
     * If the value is not null, it will be mapped to a String using the mapper function. If it is null, it will be
     * represented by "[*]" to indicate a wildcard.
     *
     * @param obj
     *     The object to get as String.
     * @param mapper
     *     The mapping function used to map the input object to a String.
     * @param <T>
     *     The type of the object.
     * @return The String representation of the object.
     */
    public static <T> String formatOptionalValue(@Nullable T obj, Function<T, String> mapper)
    {
        return obj == null ? "[*]" : mapper.apply(obj);
    }

    /**
     * See {@link #formatOptionalValue(Object, Function)} using {@link Object#toString()} as mapping function.
     */
    public static <T> String formatOptionalValue(@Nullable T obj)
    {
        return formatOptionalValue(obj, Object::toString);
    }
}
