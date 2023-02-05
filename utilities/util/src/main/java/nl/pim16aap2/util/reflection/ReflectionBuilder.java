package nl.pim16aap2.util.reflection;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Contains several static methods to easily create new finders for several reflection lookups.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public final class ReflectionBuilder
{
    private ReflectionBuilder()
    {
        // Static utility class
    }

    /**
     * Creates a new {@link ClassFinder}.
     *
     * @return A new {@link ClassFinder}.
     */
    @CheckReturnValue @Contract(pure = true)
    public static ClassFinder findClass()
    {
        return new ClassFinder();
    }

    /**
     * Creates a new {@link ClassFinder} and configures it with a set of class names.
     *
     * @param names
     *     A list of class names to search for. There should be at least one.
     * @return A new {@link ClassFinder}.
     */
    @CheckReturnValue @Contract(pure = true)
    public static ClassFinder findClass(String... names)
    {
        return new ClassFinder().withNames(names);
    }

    /**
     * Creates a new {@link ConstructorFinder}.
     *
     * @return A new {@link ConstructorFinder}.
     */
    @CheckReturnValue @Contract(pure = true)
    public static ConstructorFinder findConstructor()
    {
        return new ConstructorFinder();
    }

    /**
     * Creates a new {@link ConstructorFinder} and specifies the class the {@link Constructor} should exist in.
     *
     * @param source
     *     The class in which the finder will look for the {@link Constructor}.
     * @return A new {@link ConstructorFinder}.
     */
    @CheckReturnValue @Contract(pure = true)
    public static ConstructorFinder.ConstructorFinderInSource findConstructor(Class<?> source)
    {
        return new ConstructorFinder().inClass(source);
    }

    /**
     * Creates a new {@link FieldFinder}.
     *
     * @return A new {@link FieldFinder}.
     */
    @CheckReturnValue @Contract(pure = true)
    public static FieldFinder findField()
    {
        return new FieldFinder();
    }

    /**
     * Creates a new {@link FieldFinder.FieldFinderInSource} and configures the class the {@link Field} should exist
     * in.
     *
     * @param source
     *     The class in which the finder will look for the {@link Field}.
     * @return A new {@link FieldFinder.FieldFinderInSource}.
     */
    @CheckReturnValue @Contract(pure = true)
    public static FieldFinder.FieldFinderInSource findField(Class<?> source)
    {
        return new FieldFinder().inClass(source);
    }

    /**
     * Creates a new {@link MethodFinder}.
     *
     * @return A new {@link MethodFinder}.
     */
    @CheckReturnValue @Contract(pure = true)
    public static MethodFinder findMethod()
    {
        return new MethodFinder();
    }

    /**
     * Creates a new {@link MethodFinder.MethodFinderInSource} and configures the class the {@link Method} should exist
     * in.
     *
     * @param source
     *     The class in which the finder will look for the {@link Method}.
     * @return A new {@link MethodFinder.MethodFinderInSource}.
     */
    @CheckReturnValue @Contract(pure = true)
    public static MethodFinder.MethodFinderInSource findMethod(Class<?> source)
    {
        return new MethodFinder().inClass(source);
    }

    /**
     * Creates a new {@link EnumValuesFinder}.
     *
     * @return A new {@link EnumValuesFinder}.
     */
    @CheckReturnValue @Contract(pure = true)
    public static EnumValuesFinder findEnumValues()
    {
        return new EnumValuesFinder();
    }

    /**
     * Creates a new {@link EnumValuesFinder.EnumValuesFinderInSource} and configures the enum they should exist in.
     *
     * @param source
     *     The class in which the finder will look for the enum values.
     * @return A new {@link EnumValuesFinder.EnumValuesFinderInSource}.
     */
    @CheckReturnValue @Contract(pure = true)
    public static EnumValuesFinder.EnumValuesFinderInSource findEnumValues(Class<?> source)
    {
        return new EnumValuesFinder().inClass(source);
    }

    /**
     * Creates a new {@link ParameterGroup.Builder}.
     *
     * @return A new {@link ParameterGroup.Builder}.
     */
    @CheckReturnValue @Contract(pure = true)
    public static ParameterGroup.Builder parameterBuilder()
    {
        return new ParameterGroup.Builder();
    }
}
