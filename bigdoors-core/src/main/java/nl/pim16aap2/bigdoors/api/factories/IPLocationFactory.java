package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

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
    IPLocation create(IPWorld world, Vector3Di position);

    /**
     * Creates a new IPLocation.
     *
     * @param world
     *     The world.
     * @param position
     *     The position in the world
     * @return A new IPLocation object.
     */
    IPLocation create(IPWorld world, Vector3Dd position);

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
    IPLocation create(String worldName, Vector3Di position);

    /**
     * Creates a new IPLocation.
     *
     * @param worldName
     *     The name of the world.
     * @param position
     *     The position in the world
     * @return A new IPLocation object.
     */
    IPLocation create(String worldName, Vector3Dd position);
}
