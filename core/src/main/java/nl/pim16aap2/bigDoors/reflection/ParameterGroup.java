package nl.pim16aap2.bigDoors.reflection;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a group of parameters that describes, for example, the input arguments of a {@link Constructor}.
 * <p>
 * The advantage over using an array of raw {@link Class} objects is that these parameters can be marked optional,
 * allowing for greater freedom when searching for a method with a set of parameters.
 * <p>
 * For example, when providing the types {@code {int.class, boolean.class, float.class}} and mark {@code float.class} as
 * optional, this set of parameters can be used to match a method {@code void fun(int, boolean)} as well as a method
 * {@code void fun(int, boolean, float}.
 * <p>
 * Use {@link ParameterGroup.Builder} to create a new instance.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public class ParameterGroup
{
    private final @NotNull List<Parameter> parameters;
    private final int requiredCount;

    private ParameterGroup(@NotNull List<Parameter> parameters, int requiredCount)
    {
        this.parameters = Collections.unmodifiableList(parameters);
        this.requiredCount = requiredCount;
    }

    // Copy constructor
    public ParameterGroup(@NotNull ParameterGroup other)
    {
        Objects.requireNonNull(other, "Copy constructor cannot copy from null!");

        final List<Parameter> tmpList = new ArrayList<>(other.parameters.size());
        other.parameters.forEach(parameter -> tmpList.add(new Parameter(parameter)));

        this.parameters = Collections.unmodifiableList(tmpList);
        this.requiredCount = other.requiredCount;
    }


    /**
     * Gets the currently-configured list of {@link Parameter}s.
     * <p>
     * Note that this is an unmodifiable List.
     *
     * @return The currently-configured list of {@link Parameter}s.
     */
    public @NotNull List<Parameter> getParameters()
    {
        return parameters;
    }

    /**
     * Finds the number of steps to the next parameter in {@link #parameters} of a given type.
     * <p>
     * Only optional parameters (See {@link Parameter#isOptional()}) can be skipped. If a required parameter is
     * encountered before a parameter of the desired type is found, -1 is returned.
     *
     * @param target   The target type to search for in the {@link #parameters}.
     * @param startIdx The starting index to start searching from.
     * @return The number of steps (indices) to the next parameter of the provided type or -1 if no valid type can be
     * found.
     */
    private int findStepsToNextParameterOfType(@NotNull Class<?> target, int startIdx)
    {
        int skipped = 0;
        for (int idx = startIdx; idx < parameters.size(); ++idx)
        {
            final Parameter parameter = parameters.get(idx);
            // If it's a match, return the number of skipped parameters.
            if (target.equals(parameter.getType()))
                return skipped;
            // If it's not a match and the current one is required, return -1 (i.e. invalid).
            if (parameter.isRequired())
                return -1;
            skipped += 1;
        }
        return -1;
    }

    /**
     * Checks if a set of types matches the current set of parameters.
     * <p>
     * Any optional parameters may be skipped if that helps to achieve a match.
     *
     * @param types The types to compare the parameters against.
     * @return True if the provided types match the defined parameters.
     */
    public boolean matches(@NotNull Class<?>[] types)
    {
        if (types.length > parameters.size())
            return false;

        if (requiredCount > types.length)
            return false;

        int skipped = 0;
        for (int idx = 0; idx < types.length; ++idx)
        {
            final Class<?> type = types[idx];
            final int skip = findStepsToNextParameterOfType(type, idx + skipped);
            if (skip == -1)
                return false;
            skipped += skip;
        }

        // Ensure there are no trailing required parameters
        for (int idx = types.length + skipped; idx < parameters.size(); ++idx)
            if (parameters.get(idx).isRequired())
                return false;
        return true;
    }

    @Override
    public String toString()
    {
        final int lastIdx = parameters.size() - 1;
        final StringBuilder sb = new StringBuilder();
        for (int idx = 0; idx < parameters.size(); ++idx)
        {
            sb.append(parameters.get(idx));
            if (idx != lastIdx)
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * Represents a builder for {@link ParameterGroup}s.
     */
    public static class Builder
    {
        private final @NotNull List<Parameter> parameters = new ArrayList<>();
        private int requiredCount = 0;

        public Builder()
        {
        }

        public Builder(@NotNull ParameterGroup group)
        {
            group.getParameters().forEach(parameter -> parameters.add(new Parameter(parameter)));
            requiredCount = group.requiredCount;
        }

        /**
         * Appends a set of types as required parameters to the current parameter group.
         *
         * @param types The types to add as required parameters.
         * @return The instance of the current parameter group.
         */
        @Contract("_ -> this")
        public ParameterGroup.Builder withRequiredParameters(@NotNull Class<?>... types)
        {
            for (@NotNull Class<?> type : types)
                parameters.add(new Parameter(type, false));
            requiredCount += types.length;
            return this;
        }

        /**
         * Appends a set of types as optional parameter to the current parameter group.
         * <p>
         * Optional types can be skipped when comparing against a set of arguments.
         *
         * @param types The types to add as optional parameters.
         * @return The instance of the current parameter group.
         */
        @Contract("_ -> this")
        public ParameterGroup.Builder withOptionalParameters(@NotNull Class<?>... types)
        {
            for (Class<?> type : types)
                parameters.add(new Parameter(type, true));
            return this;
        }

        /**
         * Constructs a new {@link ParameterGroup} from this builder.
         *
         * @return The newly created {@link ParameterGroup}.
         */
        public ParameterGroup construct()
        {
            return new ParameterGroup(parameters, requiredCount);
        }
    }

    private static final class Parameter
    {
        private final @NotNull Class<?> type;
        private final boolean optional;

        private Parameter(@NotNull Class<?> type, boolean optional)
        {
            this.type = type;
            this.optional = optional;
        }

        // Copy constructor
        private Parameter(@NotNull Parameter other)
        {
            this.type = Objects.requireNonNull(other, "Copy constructor cannot copy from null!").type;
            this.optional = other.optional;
        }

        private @NotNull Class<?> getType()
        {
            return type;
        }

        private boolean isRequired()
        {
            return !optional;
        }

        private boolean isOptional()
        {
            return optional;
        }

        @Override
        public String toString()
        {
            return optional ? ("[" + type.getName() + "]") : type.getName();
        }
    }
}
