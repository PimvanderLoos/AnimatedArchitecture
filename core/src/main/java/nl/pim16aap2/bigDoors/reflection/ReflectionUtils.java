package nl.pim16aap2.bigDoors.reflection;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ReflectionUtils
{
    public static final String NMS_BASE =
        "net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + ".";
    public static final String CRAFT_BASE =
        "org.bukkit.craftbukkit." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + ".";

    private ReflectionUtils()
    {
        // utility class
    }

    public static @NotNull Class<?> findClass(@NotNull String name)
    {
        return findFirstClass(name);
    }

    @Contract("true, _ -> !null")
    public static @NotNull Class<?> findClass(boolean nonNull, @NotNull String name)
    {
        return findFirstClass(nonNull, name);
    }

    /**
     * Finds a class from a set of names.
     * <p>
     * See {@link #findFirstClass(boolean, String...)} where null values are allowed.
     *
     * @param names The fully qualified name(s) of the class to find.
     * @return The first class from the list that could be found or, if no classes could be found, null.
     */
    public static @NotNull Class<?> findFirstClass(@NotNull String... names)
    {
        return findFirstClass(true, names);
    }

    /**
     * Finds a class from a set of names.
     *
     * @param nonNull Whether to allow returning a null value when no classes could be found. When this is true, and no
     *                classes could be found for any of the provided names, an exception will be thrown.
     * @param names   The fully qualified name(s) of the class to find.
     * @return The first class from the list that could be found or, if no classes could be found, null.
     */
    @Contract("true, _ -> !null")
    public static Class<?> findFirstClass(boolean nonNull, @NotNull String... names)
    {
        for (String name : names)
        {
            try
            {
                return Class.forName(name);
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

    public static @NotNull Method getMethod(@NotNull Class<?> source, @NotNull String name, @NotNull Class<?>... args)
    {
        return getMethod(true, source, name, args);
    }

    @Contract("true, _, _, _ -> !null")
    public static Method getMethod(boolean nonNull, @NotNull Class<?> source, @NotNull String name,
                                   @NotNull Class<?>... args)
    {
        try
        {
            return source.getDeclaredMethod(name, args);
        }
        catch (NoSuchMethodException e)
        {
            if (nonNull)
                throw new NullPointerException(
                    "Failed to find method \"" + name + "\" in class \"" + source + "\" with args: " +
                        Arrays.toString(args));
            return null;
        }
    }

    public static @NotNull Field getField(@NotNull Class<?> source, @NotNull String name)
    {
        return getField(true, source, name, null);
    }

    public static @NotNull Field getField(@NotNull Class<?> source, @NotNull String name, @Nullable Class<?> type)
    {
        return getField(true, source, name, type);
    }

    @Contract("true, _, _ -> !null")
    public static Field getField(boolean nonNull, @NotNull Class<?> source, @NotNull String name)
    {
        return getField(nonNull, source, name, null);
    }

    @Contract("true, _, _, _ -> !null")
    public static Field getField(boolean nonNull, @NotNull Class<?> source, @NotNull String name,
                                 @Nullable Class<?> type)
    {
        try
        {
            Field f = source.getDeclaredField(name);
            if (type == null || f.getType().equals(type))
                return f;
        }
        catch (NoSuchFieldException e)
        {
            // Ignored
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

    public static @NotNull Method findMethodFromProfile(@NotNull Class<?> source, @Nullable Class<?> returnType,
                                                        @Nullable Integer modifiers, @Nullable Class<?>... args)
    {
        return findMethodFromProfile(true, source, returnType, modifiers, args);
    }

    @Contract("true, _, _, _, _ -> !null")
    public static Method findMethodFromProfile(boolean nonNull, @NotNull Class<?> source, @Nullable Class<?> returnType,
                                               @Nullable Integer modifiers, @Nullable Class<?>... args)
    {
        final Method[] methods = modifiers != null && Modifier.isPublic(modifiers) ?
                                 source.getMethods() : source.getDeclaredMethods();

        for (Method method : methods)
        {
            if (modifiers != null && method.getModifiers() != modifiers)
                continue;

            if (returnType != null && !method.getReturnType().equals(returnType))
                continue;

            if (!testArgsSimilarity(args, method.getParameterTypes()))
                continue;

            return method;
        }

        if (nonNull)
            throw new NullPointerException(
                "Failed to find method in class \"" + source + "\" with modifiers " +
                    (modifiers == null ? "NULL" : Modifier.toString(modifiers)) +
                    ", return type \"" + returnType + "\", and arguments: " + Arrays.toString(args));
        return null;
    }

    /**
     * Checks if two sets of arguments are similar in the sense that all non-null values of the desired arguments match
     * the found arguments. Trailing null values in the desired arguments are not considered. Additionally, if the
     * desired args array itself is null, this method will also consider the arrays similar.
     * <p>
     * Note that while all situations where two arrays A and B return true for {@code Arrays.equals(A, B)} will also
     * return true for this method, but the reverse is not true!
     * <p>
     * For example, given {@code found = {int.class, double.class}} all of the following inputs will return true:
     * <p>
     * - {@code desired = {int.class, double.class, null}}}
     * <p>
     * - {@code desired = {int.class, null, null}}}
     * <p>
     * - {@code desired = {null, null, null}}}
     * <p>
     * - {@code desired = null}}
     * <p>
     * However, {@code desired = {int.class, double.class, String.class}} would return false.
     *
     * @param desiredArgs The desired argument types.
     * @param foundArgs   The found argument types.
     * @return True if all non-null values in the desired args array equal the value in the found args array at the same
     * index.
     */
    private static boolean testArgsSimilarity(@Nullable Class<?>[] desiredArgs, @NotNull Class<?>[] foundArgs)
    {
        if (desiredArgs == null)
            return true;

        // Trailing null values are ignored, so it doesn't
        // matter if there are more found args than desired args.
        // However, the reverse is not true.
        if (desiredArgs.length < foundArgs.length)
            return false;

        for (int idx = 0; idx < desiredArgs.length; ++idx)
        {
            // We do not look for exact equality;
            // Any null-values of the desired args are skipped.
            if (desiredArgs[idx] == null)
                continue;
            // All trailing null values in the desired args are ignored.
            // However, if a non-null value is found out of range for the
            // found args, the similarity fails.
            if (idx >= foundArgs.length)
                return false;
            if (!foundArgs[idx].equals(desiredArgs[idx]))
                return false;
        }
        return true;
    }

    /**
     * See {@link #getField(boolean, Class, Integer, Class)}, with nonNull enabled.
     */
    public static @NotNull Field getField(@NotNull Class<?> source, @Nullable Integer modifiers, @NotNull Class<?> type)
    {
        return getField(true, source, modifiers, type);
    }

    /**
     * Attempts to find a field in a class. Only the first field that matches the desired specification is returned. If
     * all fields matching the specification are desired, use {@link #getFields(Class, Integer, Class)} instead.
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
    public static Field getField(boolean nonNull, @NotNull Class<?> source, @Nullable Integer modifiers,
                                 @NotNull Class<?> type)
    {
        final Field[] fields = modifiers != null && Modifier.isPublic(modifiers) ?
                               source.getFields() : source.getDeclaredFields();
        for (Field field : fields)
        {
            if (modifiers != null && field.getModifiers() != modifiers)
                continue;

            if (!field.getType().equals(type))
                continue;

            return field;
        }

        if (nonNull)
            throw new NullPointerException(
                "Failed to find field in class \"" + source + "\" with modifiers " +
                    (modifiers == null ? "NULL" : Modifier.toString(modifiers)) +
                    " of type \"" + type + "\"");
        return null;
    }

    public static @NotNull List<Field> getFields(@NotNull Class<?> source, @Nullable Integer modifiers,
                                                 @NotNull Class<?> type)
    {
        List<Field> ret = new ArrayList<>();
        final Field[] fields = modifiers != null && Modifier.isPublic(modifiers) ?
                               source.getFields() : source.getDeclaredFields();
        for (Field field : fields)
        {
            if (modifiers != null && field.getModifiers() != modifiers)
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
}
