package nl.pim16aap2.bigdoors.spigot.factories;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PLocationSpigot;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an implementation of {@link IPLocationFactory} for the Spigot platform.
 *
 * @author Pim
 */
public class PLocationFactorySpigot implements IPLocationFactory
{
    @Override
    public @NotNull IPLocation create(final @NotNull IPWorld world, final double x, final double y, final double z)
    {
        return new PLocationSpigot(world, x, y, z);
    }

    @Override
    public @NotNull IPLocation create(final @NotNull IPWorld world, final @NotNull Vector3Di position)
    {
        return create(world, position.x(), position.y(), position.z());
    }

    @Override
    public @NotNull IPLocation create(final @NotNull IPWorld world, final @NotNull Vector3Dd position)
    {
        return create(world, position.x(), position.y(), position.z());
    }

    @Override
    public @NotNull IPLocation create(final @NotNull String worldName, final double x, final double y, final double z)
    {
        return create(BigDoors.get().getPlatform().getPWorldFactory().create(worldName), x, y, z);
    }

    @Override
    public @NotNull IPLocation create(final @NotNull String worldName, final @NotNull Vector3Di position)
    {
        return create(worldName, position.x(), position.y(), position.z());
    }

    @Override
    public @NotNull IPLocation create(final @NotNull String worldName, final @NotNull Vector3Dd position)
    {
        return create(worldName, position.x(), position.y(), position.z());
    }
}
