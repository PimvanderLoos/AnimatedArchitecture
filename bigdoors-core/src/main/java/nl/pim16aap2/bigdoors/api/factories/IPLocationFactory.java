package nl.pim16aap2.bigdoors.api.factories;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;

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
     * @param world The world.
     * @param x     The x coordinate.
     * @param y     The y coordinate.
     * @param z     The z coordinate.
     * @return A new IPLocation object.
     */
    @NonNull IPLocation create(final @NonNull IPWorld world, final double x, final double y, final double z);

    /**
     * Creates a new IPLocation.
     *
     * @param world    The world.
     * @param position The position in the world
     * @return A new IPLocation object.
     */
    @NonNull IPLocation create(final @NonNull IPWorld world, final @NonNull Vector3DiConst position);

    /**
     * Creates a new IPLocation.
     *
     * @param world    The world.
     * @param position The position in the world
     * @return A new IPLocation object.
     */
    @NonNull IPLocation create(final @NonNull IPWorld world, final @NonNull Vector3DdConst position);

    /**
     * Creates a new IPLocation.
     *
     * @param worldName The name of the world.
     * @param x         The x coordinate.
     * @param y         The y coordinate.
     * @param z         The z coordinate.
     * @return A new IPLocation object.
     */
    @NonNull IPLocation create(final @NonNull String worldName, final double x, final double y, final double z);

    /**
     * Creates a new IPLocation.
     *
     * @param worldName The name of the world.
     * @param position  The position in the world
     * @return A new IPLocation object.
     */
    @NonNull IPLocation create(final @NonNull String worldName, final @NonNull Vector3DiConst position);

    /**
     * Creates a new IPLocation.
     *
     * @param worldName The name of the world.
     * @param position  The position in the world
     * @return A new IPLocation object.
     */
    @NonNull IPLocation create(final @NonNull String worldName, final @NonNull Vector3DdConst position);
}
