package nl.pim16aap2.bigdoors.core.api.factories;

import nl.pim16aap2.bigdoors.core.api.IPLocation;
import nl.pim16aap2.bigdoors.core.api.IPWorld;
import nl.pim16aap2.bigdoors.core.util.vector.IVector3D;

/**
 * Represents a factory for {@link IPLocation} objects.
 *
 * @author Pim
 */
public interface IPLocationFactory
{
    /**
     * Creates a new IPLocation.
     *
     * @param world
     *     The world.
     * @param x
     *     The x coordinate.
     * @param y
     *     The y coordinate.
     * @param z
     *     The z coordinate.
     * @return A new IPLocation object.
     */
    IPLocation create(IPWorld world, double x, double y, double z);

    /**
     * Creates a new IPLocation.
     *
     * @param world
     *     The world.
     * @param position
     *     The position in the world
     * @return A new IPLocation object.
     */
    IPLocation create(IPWorld world, IVector3D position);

    /**
     * Creates a new IPLocation.
     *
     * @param worldName
     *     The name of the world.
     * @param x
     *     The x coordinate.
     * @param y
     *     The y coordinate.
     * @param z
     *     The z coordinate.
     * @return A new IPLocation object.
     */
    IPLocation create(String worldName, double x, double y, double z);

    /**
     * Creates a new IPLocation.
     *
     * @param worldName
     *     The name of the world.
     * @param position
     *     The position in the world
     * @return A new IPLocation object.
     */
    IPLocation create(String worldName, IVector3D position);
}
