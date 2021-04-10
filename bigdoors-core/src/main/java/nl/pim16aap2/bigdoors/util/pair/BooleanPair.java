package nl.pim16aap2.bigdoors.util.pair;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/**
 * Represents a pair of booleans. This is a specialized version of {@link Pair} that avoids boxing.
 *
 * @author Pim
 */
@EqualsAndHashCode
@AllArgsConstructor
public class BooleanPair
{
    public boolean first;
    public boolean second;
}
