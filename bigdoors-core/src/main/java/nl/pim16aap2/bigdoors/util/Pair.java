package nl.pim16aap2.bigdoors.util;

import java.util.Objects;

/**
 * Represents a name-value pair.
 *
 * @author Pim
 */
public final class Pair<T1, T2>
{
    public T1 first;
    public T2 second;

    public Pair(final T1 first, final T2 second)
    {
        this.first = first;
        this.second = second;
    }

    /**
     * Returns the hashCode of {@link #first}.
     */
    @Override
    public int hashCode()
    {
        return first == null ? 0 : first.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        Pair<?, ?> other = (Pair<?, ?>) o;
        return Objects.equals(first, other.first) && Objects.equals(second, other.second);
    }
}
