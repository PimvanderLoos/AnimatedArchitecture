package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.vector.IVector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

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
    @NotNull
    IPLocation create(final @NotNull IPWorld world, final double x, final double y, final double z);

    /**
     * Creates a new IPLocation.
     *
     * @param world    The world.
     * @param position The position in the world
     * @return A new IPLocation object.
     */
    @NotNull
    IPLocation create(final @NotNull IPWorld world, final @NotNull IVector3DiConst position);

    /**
     * Creates a new IPLocation.
     *
     * @param world    The world.
     * @param position The position in the world
     * @return A new IPLocation object.
     */
    @NotNull
    IPLocation create(final @NotNull IPWorld world, final @NotNull IVector3DdConst position);

    /**
     * Creates a new IPLocation.
     *
     * @param worldUUID The uuid of the world.
     * @param x         The x coordinate.
     * @param y         The y coordinate.
     * @param z         The z coordinate.
     * @return A new IPLocation object.
     */
    @NotNull
    IPLocation create(final @NotNull UUID worldUUID, final double x, final double y, final double z);

    /**
     * Creates a new IPLocation.
     *
     * @param worldUUID The uuid of the world.
     * @param position  The position in the world
     * @return A new IPLocation object.
     */
    @NotNull
    IPLocation create(final @NotNull UUID worldUUID, final @NotNull IVector3DiConst position);

    /**
     * Creates a new IPLocation.
     *
     * @param worldUUID The uuid of the world.
     * @param position  The position in the world
     * @return A new IPLocation object.
     */
    @NotNull
    IPLocation create(final @NotNull UUID worldUUID, final @NotNull IVector3DdConst position);
}
