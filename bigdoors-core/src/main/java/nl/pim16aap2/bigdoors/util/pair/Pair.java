package nl.pim16aap2.bigdoors.util.pair;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Represents a name-value pair.
 *
 * @author Pim
 */
@EqualsAndHashCode
@AllArgsConstructor
@ToString
@SuppressWarnings("NullAway")
public final class Pair<T1, T2>
{
    public T1 first;
    public T2 second;
}
