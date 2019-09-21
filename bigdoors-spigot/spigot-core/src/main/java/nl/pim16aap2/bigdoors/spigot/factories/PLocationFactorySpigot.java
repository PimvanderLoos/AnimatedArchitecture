package nl.pim16aap2.bigdoors.spigot.factories;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PLocationSpigot;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents an implementation of {@link IPLocationFactory} for the Spigot platform.
 *
 * @author Pim
 */
public class PLocationFactorySpigot implements IPLocationFactory
{
    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IPLocation create(final @NotNull IPWorld world, final double x, final double y, final double z)
    {
        return new PLocationSpigot(world, x, y, z);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IPLocation create(@NotNull IPWorld world, @NotNull Vector3Di position)
    {
        return create(world, position.getX(), position.getY(), position.getZ());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IPLocation create(@NotNull IPWorld world, @NotNull Vector3Dd position)
    {
        return create(world, position.getX(), position.getY(), position.getZ());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IPLocation create(@NotNull UUID worldUUID, double x, double y, double z)
    {
        return create(BigDoors.get().getPlatform().getPWorldFactory().create(worldUUID), x, y, z);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IPLocation create(@NotNull UUID worldUUID, @NotNull Vector3Di position)
    {
        return create(worldUUID, position.getX(), position.getY(), position.getZ());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IPLocation create(@NotNull UUID worldUUID, @NotNull Vector3Dd position)
    {
        return create(worldUUID, position.getX(), position.getY(), position.getZ());
    }
}
