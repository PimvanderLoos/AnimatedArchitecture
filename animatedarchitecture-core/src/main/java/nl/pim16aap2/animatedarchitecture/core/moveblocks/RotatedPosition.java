package nl.pim16aap2.animatedarchitecture.core.moveblocks;

import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;

/**
 * Represents a position with a rotation.
 *
 * @param position
 *     The x-y-z coordinates of the position.
 * @param rotation
 *     The roll-pitch-yaw values of the rotation.
 */
public record RotatedPosition(Vector3Dd position, Vector3Dd rotation)
{
    private static final Vector3Dd NULL_VEC = new Vector3Dd(0, 0, 0);

    public RotatedPosition(Vector3Dd position)
    {
        this(position, NULL_VEC);
    }
}
