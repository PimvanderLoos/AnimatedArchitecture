package nl.pim16aap2.bigdoors.util.pair;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a name-value pair.
 *
 * @author Pim
 */
@EqualsAndHashCode
@AllArgsConstructor
@ToString
@SuppressWarnings("squid:S1104")
public final class PairNullable<T1, T2>
{
    public @Nullable T1 first;
    public @Nullable T2 second;
}
