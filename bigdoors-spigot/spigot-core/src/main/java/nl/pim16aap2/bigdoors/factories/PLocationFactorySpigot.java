package nl.pim16aap2.bigdoors.factories;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.implementations.PLocationSpigot;
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
    public IPLocation create(final @NotNull IPWorld world, final double x, final double y, final double z)
    {
        return new PLocationSpigot(world, x, y, z);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IPLocation create(@NotNull UUID worldUUID, double x, double y, double z)
    {
        return create(BigDoors.get().getPlatform().getPWorldFactory().create(worldUUID), x, y, z);
    }
}
