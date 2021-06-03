package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;

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
    @NotNull IPLocation create(@NotNull IPWorld world, double x, double y, double z);

    /**
     * Creates a new IPLocation.
     *
     * @param world    The world.
     * @param position The position in the world
     * @return A new IPLocation object.
     */
    @NotNull IPLocation create(@NotNull IPWorld world, @NotNull Vector3DiConst position);

    /**
     * Creates a new IPLocation.
     *
     * @param world    The world.
     * @param position The position in the world
     * @return A new IPLocation object.
     */
    @NotNull IPLocation create(@NotNull IPWorld world, @NotNull Vector3DdConst position);

    /**
     * Creates a new IPLocation.
     *
     * @param worldName The name of the world.
     * @param x         The x coordinate.
     * @param y         The y coordinate.
     * @param z         The z coordinate.
     * @return A new IPLocation object.
     */
    @NotNull IPLocation create(@NotNull String worldName, double x, double y, double z);

    /**
     * Creates a new IPLocation.
     *
     * @param worldName The name of the world.
     * @param position  The position in the world
     * @return A new IPLocation object.
     */
    @NotNull IPLocation create(@NotNull String worldName, @NotNull Vector3DiConst position);

    /**
     * Creates a new IPLocation.
     *
     * @param worldName The name of the world.
     * @param position  The position in the world
     * @return A new IPLocation object.
     */
    @NotNull IPLocation create(@NotNull String worldName, @NotNull Vector3DdConst position);
}
