package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

/**
 * Represents a structure that has the rotation point property. See {@link Property#ROTATION_POINT}.
 * <p>
 * All structures that implement this interface need to register the {@link Property#ROTATION_POINT} property in their
 * structure type.
 */
public interface IStructureWithRotationPoint extends IPropertyHolder
{
    /**
     * Gets the rotation point of the structure.
     *
     * @return The rotation point of the structure.
     */
    default Vector3Di getRotationPoint()
    {
        return getRawPropertyValue(Property.ROTATION_POINT);
    }

    /**
     * Sets the rotation point of the structure.
     *
     * @param rotationPoint
     *     The new rotation point of the structure.
     */
    default void setRotationPoint(Vector3Di rotationPoint)
    {
        setPropertyValue(Property.ROTATION_POINT, rotationPoint);
    }
}
