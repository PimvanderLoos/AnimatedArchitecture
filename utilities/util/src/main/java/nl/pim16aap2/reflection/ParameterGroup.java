package nl.pim16aap2.reflection;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;

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
 * Use {@link Builder} to create a new instance.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public class ParameterGroup
{
    private final List<Parameter> parameters;
    private final int requiredCount;

    private ParameterGroup(List<Parameter> parameters, int requiredCount)
    {
        this.parameters = Collections.unmodifiableList(parameters);
        this.requiredCount = requiredCount;
    }

    // Copy constructor
    public ParameterGroup(ParameterGroup other)
    {
        final List<Parameter> tmpList = new ArrayList<>(other.parameters.size());
        Objects.requireNonNull(other, "Copy constructor cannot copy from null!").parameters
            .forEach(parameter -> tmpList.add(new Parameter(parameter)));

        parameters = Collections.unmodifiableList(tmpList);
        requiredCount = other.requiredCount;
    }


    /**
     * Gets the currently-configured list of {@link Parameter}s.
     * <p>
     * Note that this is an unmodifiable List.
     *
     * @return The currently-configured list of {@link Parameter}s.
     */
    public List<Parameter> getParameters()
    {
        return parameters;
    }

    /**
     * Finds the number of steps to the next parameter in {@link #parameters} of a given type.
     * <p>
     * Only optional parameters (See {@link Parameter#optional()}) can be skipped. If a required parameter is
     * encountered before a parameter of the desired type is found, -1 is returned.
     *
     * @param target
     *     The target type to search for in the {@link #parameters}.
     * @param startIdx
     *     The starting index to start searching from.
     * @return The number of steps (indices) to the next parameter of the provided type or -1 if no valid type can be
     * found.
     */
    private int findStepsToNextParameterOfType(Class<?> target, int startIdx)
    {
        int skipped = 0;
        for (int idx = startIdx; idx < parameters.size(); ++idx)
        {
            final Parameter parameter = parameters.get(idx);
            // If it's a match, return the number of skipped parameters.
            if (target.equals(parameter.type))
                return skipped;
            // If it's not a match and the current one is required, return -1 (i.e. invalid).
            if (parameter.required())
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
     * @param types
     *     The types to compare the parameters against.
     * @return True if the provided types match the defined parameters.
     */
    @CheckReturnValue @Contract(pure = true)
    public boolean matches(Class<?>... types)
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
            if (parameters.get(idx).required())
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
        private final List<Parameter> parameters = new ArrayList<>();
        private int requiredCount = 0;

        public Builder()
        {
        }

        public Builder(ParameterGroup group)
        {
            group.getParameters().forEach(parameter -> parameters.add(new Parameter(parameter)));
            requiredCount = group.requiredCount;
        }

        /**
         * Appends a set of types as required parameters to the current parameter group.
         *
         * @param types
         *     The types to add as required parameters.
         * @return The instance of the current parameter group.
         */
        public Builder withRequiredParameters(Class<?>... types)
        {
            for (final Class<?> type : types)
                parameters.add(new Parameter(type, false));
            requiredCount += types.length;
            return this;
        }

        /**
         * Appends a set of types as optional parameter to the current parameter group.
         * <p>
         * Optional types can be skipped when comparing against a set of arguments.
         *
         * @param types
         *     The types to add as optional parameters.
         * @return The instance of the current parameter group.
         */
        public Builder withOptionalParameters(Class<?>... types)
        {
            for (final Class<?> type : types)
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

    private record Parameter(Class<?> type, boolean optional)
    {
        // Copy constructor
        private Parameter(Parameter other)
        {
            this(other.type, other.optional);
        }

        private boolean required()
        {
            return !optional;
        }

        @Override
        public String toString()
        {
            return optional ? ("[" + type.getName() + "]") : type.getName();
        }
    }
}
