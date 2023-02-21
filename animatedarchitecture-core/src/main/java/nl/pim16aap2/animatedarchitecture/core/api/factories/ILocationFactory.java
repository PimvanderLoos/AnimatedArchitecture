package nl.pim16aap2.animatedarchitecture.core.api.factories;

import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;

/**
 * Represents a factory for {@link ILocation} objects.
 *
 * @author Pim
 */
public interface ILocationFactory
{
    /**
     * Creates a new ILocation.
     *
     * @param world
     *     The world.
     * @param x
     *     The x coordinate.
     * @param y
     *     The y coordinate.
     * @param z
     *     The z coordinate.
     * @return A new ILocation object.
     */
    ILocation create(IWorld world, double x, double y, double z);

    /**
     * Creates a new ILocation.
     *
     * @param world
     *     The world.
     * @param position
     *     The position in the world
     * @return A new ILocation object.
     */
    ILocation create(IWorld world, IVector3D position);

    /**
     * Creates a new ILocation.
     *
     * @param worldName
     *     The name of the world.
     * @param x
     *     The x coordinate.
     * @param y
     *     The y coordinate.
     * @param z
     *     The z coordinate.
     * @return A new ILocation object.
     */
    ILocation create(String worldName, double x, double y, double z);

    /**
     * Creates a new ILocation.
     *
     * @param worldName
     *     The name of the world.
     * @param position
     *     The position in the world
     * @return A new ILocation object.
     */
    ILocation create(String worldName, IVector3D position);
}
