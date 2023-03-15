package nl.pim16aap2.animatedarchitecture.core.moveblocks;

import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
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

    public RotatedPosition(IVector3D position)
    {
        this(Vector3Dd.of(position), NULL_VEC);
    }

    public double roll()
    {
        return rotation.x();
    }

    public double pitch()
    {
        return rotation.y();
    }

    public double yaw()
    {
        return rotation.z();
    }
}
