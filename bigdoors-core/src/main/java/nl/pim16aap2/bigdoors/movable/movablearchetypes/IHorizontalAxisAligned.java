package nl.pim16aap2.bigdoors.movable.movablearchetypes;

import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;

/**
 * Represents all {@link MovableType}s that are aligned on the North/South or East/West axis. e.g. a sliding movable.
 * <p>
 * Only movables with a depth of 1 block can be extended.
 *
 * @author Pim
 * @see AbstractMovable
 */
public interface IHorizontalAxisAligned
{
    /**
     * Checks if the {@link AbstractMovable} is aligned with the z-axis (North/South).
     */
    boolean isNorthSouthAligned();
}
