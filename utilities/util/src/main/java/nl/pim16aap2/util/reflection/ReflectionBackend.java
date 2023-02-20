package nl.pim16aap2.util.reflection;

import lombok.extern.flogger.Flogger;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Represents the reflection backend for the {@link ReflectionFinder} classes.
 *
 * @author Pim
 */
@Flogger //
final class ReflectionBackend
{
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
        if (!source.isEnum())
            throw new IllegalArgumentException("Class " + source.getName() + " is not an enum!");
        return Objects.requireNonNull(source.getEnumConstants());
    }

    /**
     * Retrieves an enum constant from its name.
     *
     * @param source
     *     The class from which to retrieve the enum constants. This should be an enum.
     * @param name
     *     The name of the enum constant to look for.
     * @return The enum constant with the given name or null when it could not be found and null is allowed.
     */
    public static @Nullable Object getNamedEnumConstant(Class<?> source, String name)
    {
        if (!source.isEnum())
            throw new IllegalArgumentException("Class " + source.getName() + " is not an enum!");

        try
        {
            //noinspection unchecked,rawtypes
            return Enum.valueOf((Class) source, name);
        }
        catch (Exception e)
        {
            return null;
        }
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

    private static List<Field> getFields(Class<?> clz, boolean checkSuperClasses)
    {
        final List<Field> ret = new ArrayList<>(List.of(clz.getDeclaredFields()));
        if (!checkSuperClasses)
            return ret;

        @Nullable Class<?> check = clz.getSuperclass();
        while (check != null)
        {
            ret.addAll(List.of(check.getDeclaredFields()));
            check = check.getSuperclass();
        }
        return ret;
    }

    /**
     * Attempts to find a field in a class based on the input configuration. If all fields matching the specification
     * are desired, use {@link #getFields(Class, int, Class, boolean, boolean, Class[])} instead.
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
     * @param setAccessible
     *     True to use {@link AccessibleObject#setAccessible(boolean)}.
     * @return The first field that matches the specification.
     */
    @SafeVarargs
    public static @Nullable Field getField(
        Class<?> source, String name, int modifiers, @Nullable Class<?> type, boolean setAccessible,
        boolean checkSuperClasses, Class<? extends Annotation>... annotations)
    {
        for (final Field field : getFields(source, checkSuperClasses))
        {
            if (type != null && !field.getType().equals(type))
                continue;
            if (modifiers != 0 && field.getModifiers() != modifiers)
                continue;
            if (!field.getName().equals(name))
                continue;
            if (!containsAnnotations(field, annotations))
                continue;
            return setAccessibleIfNeeded(field, setAccessible);
        }
        return null;
    }

    /**
     * Attempts to find a field in a class. Only the first field that matches the desired specification is returned. If
     * all fields matching the specification are desired, use
     * {@link #getFields(Class, int, Class, boolean, boolean, Class[])} instead.
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
     * @param setAccessible
     *     True to use {@link AccessibleObject#setAccessible(boolean)}.
     * @return The first field that matches the specification.
     */
    @SafeVarargs
    public static @Nullable Field getField(
        Class<?> source, int modifiers, Class<?> type, boolean setAccessible, boolean checkSuperClasses,
        Class<? extends Annotation>... annotations)
    {
        for (final Field field : getFields(source, checkSuperClasses))
        {
            if (modifiers != 0 && field.getModifiers() != modifiers)
                continue;
            if (!field.getType().equals(type))
                continue;
            if (!containsAnnotations(field, annotations))
                continue;
            return setAccessibleIfNeeded(field, setAccessible);
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
     * @param setAccessible
     *     True to use {@link AccessibleObject#setAccessible(boolean)}.
     * @return All fields in the source class that match the input configuration.
     */
    @SafeVarargs
    public static List<Field> getFields(
        Class<?> source, int modifiers, @Nullable Class<?> type, boolean setAccessible, boolean checkSuperClasses,
        Class<? extends Annotation>... annotations)
    {
        final List<Field> ret = new ArrayList<>();
        for (final Field field : getFields(source, checkSuperClasses))
        {
            if (modifiers != 0 && field.getModifiers() != modifiers)
                continue;
            if (type != null && !field.getType().equals(type))
                continue;
            if (!containsAnnotations(field, annotations))
                continue;
            ret.add(setAccessibleIfNeeded(field, setAccessible));
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
    private static @Nullable Method findMethod(
        Class<?> source, @Nullable String name, int modifiers, @Nullable ParameterGroup parameters,
        @Nullable Class<?> returnType)
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

    private static <T extends AccessibleObject> T setAccessibleIfNeeded(T obj, boolean setAccessible)
    {
        if (setAccessible)
            obj.setAccessible(true);
        return obj;
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
     * @param setAccessible
     *     True to use {@link AccessibleObject#setAccessible(boolean)}.
     * @return The method matching the specified description.
     */
    public static @Nullable Method findMethod(
        final boolean checkSuperClasses, final boolean checkInterfaces,
        Class<?> source, @Nullable String name, int modifiers,
        @Nullable ParameterGroup parameters, @Nullable Class<?> returnType, boolean setAccessible)
    {
        @Nullable Method m = findMethod(source, name, modifiers, parameters, returnType);
        if (m != null)
            return setAccessibleIfNeeded(m, setAccessible);

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

                m = findMethod(true, continueInterfaceChecking, superClass, name, modifiers, parameters, returnType,
                               setAccessible);
                if (m != null)
                    return setAccessibleIfNeeded(m, setAccessible);
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
                    m = findMethod(false, true, superInterface, name, modifiers, parameters, returnType, setAccessible);
                    if (m != null)
                        return setAccessibleIfNeeded(m, setAccessible);
                }
            }
        }
        return null;
    }

    /**
     * Checks if an object is annotated with the given annotations.
     * <p>
     * Any annotations on the object not in the provided array are ignored.
     *
     * @param obj
     *     The object whose annotations to verify.
     * @param annotations
     *     The annotations that should be preset on the object.
     * @return True if all provided annotations are present on the object. If at least one annotation is missing, the
     * method will return false.
     */
    @SafeVarargs
    private static boolean containsAnnotations(
        AccessibleObject obj, Class<? extends Annotation>... annotations)
    {
        for (final Class<? extends Annotation> annotation : annotations)
            if (!obj.isAnnotationPresent(annotation))
                return false;
        return true;
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
     * @param setAccessible
     *     True to use {@link AccessibleObject#setAccessible(boolean)}.
     * @param maxCount
     *     The maximum number of constructors to return.
     * @return All constructors matching the specified description.
     */
    @SafeVarargs
    public static List<Constructor<?>> findCTor(
        Class<?> source, int modifiers, @Nullable ParameterGroup parameters, boolean setAccessible, int maxCount,
        Class<? extends Annotation>... annotations)
    {
        final List<Constructor<?>> ret = new ArrayList<>();
        for (final Constructor<?> ctor : source.getDeclaredConstructors())
        {
            if (modifiers != 0 && ctor.getModifiers() != modifiers)
                continue;
            if (parameters != null && !parameters.matches(ctor.getParameterTypes()))
                continue;
            if (!containsAnnotations(ctor, annotations))
                continue;
            ret.add(setAccessibleIfNeeded(ctor, setAccessible));
            if (ret.size() >= maxCount)
                break;
        }
        return ret;
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
     *     The modifiers to convert into a String. This is a flag value composed of flags such as
     *     {@link Modifier#PUBLIC}. See {@link #getModifiers(int...)}.
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

    /**
     * Formats an array of annotations to a single String.
     *
     * @param annotations
     *     The annotations to format.
     * @return The string representing the annotations. If the array is empty, the resulting String will also be empty.
     * When one or more annotations are provided, the output will end with a blank space.
     */
    @SafeVarargs
    public static String formatAnnotations(Class<? extends Annotation>... annotations)
    {
        if (annotations.length == 0)
            return "";
        final StringBuilder sb = new StringBuilder();
        for (final var annotation : annotations)
            sb.append('@').append(annotation.getName()).append(' ');
        return sb.toString();
    }
}
