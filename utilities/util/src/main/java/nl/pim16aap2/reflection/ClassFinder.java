package nl.pim16aap2.reflection;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents an implementation of {@link ReflectionFinder} for {@link Class} objects.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public class ClassFinder extends ReflectionFinder<Class<?>, ClassFinder>
{
    private String[] names = new String[0];

    /**
     * Configures the set of names to look up.
     * <p>
     * When more than 1 name is provided, the first class that can be found is returned.
     *
     * @param names
     *     The list of names the class might have.
     * @return The current {@link ClassFinder} instance.
     */
    public ClassFinder withNames(String... names)
    {
        this.names = Objects.requireNonNull(names, "List of lookup names cannot be null!");
        if (names.length == 0)
            throw new IllegalArgumentException("At least 1 class name must be provided!");
        return this;
    }

    @Override
    public Class<?> get()
    {
        return Objects.requireNonNull(getNullable(),
                                      String.format("Failed to find %s %s.",
                                                    (names.length > 1 ? "any of the classes:" : "the class:"),
                                                    Arrays.toString(names)));
    }

    @Override
    public @Nullable Class<?> getNullable()
    {
        return ReflectionBackend.findFirstClass(modifiers, names);
    }
}
