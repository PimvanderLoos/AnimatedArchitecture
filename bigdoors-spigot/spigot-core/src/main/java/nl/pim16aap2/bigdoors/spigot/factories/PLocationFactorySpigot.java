package nl.pim16aap2.bigdoors.spigot.factories;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PLocationSpigot;
import nl.pim16aap2.bigdoors.util.vector.IVector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents an implementation of {@link IPLocationFactory} for the Spigot platform.
 *
 * @author Pim
 */
public class PLocationFactorySpigot implements IPLocationFactory
{
    @Override
    @NotNull
    public IPLocation create(final @NotNull IPWorld world, final double x, final double y, final double z)
    {
        return new PLocationSpigot(world, x, y, z);
    }

    @Override
    @NotNull
    public IPLocation create(final @NotNull IPWorld world, final @NotNull IVector3DiConst position)
    {
        return create(world, position.getX(), position.getY(), position.getZ());
    }

    @Override
    @NotNull
    public IPLocation create(final @NotNull IPWorld world, final @NotNull IVector3DdConst position)
    {
        return create(world, position.getX(), position.getY(), position.getZ());
    }

    @Override
    @NotNull
    public IPLocation create(final @NotNull UUID worldUUID, final double x, final double y, final double z)
    {
        return create(BigDoors.get().getPlatform().getPWorldFactory().create(worldUUID), x, y, z);
    }

    @Override
    @NotNull
    public IPLocation create(final @NotNull UUID worldUUID, final @NotNull IVector3DiConst position)
    {
        return create(worldUUID, position.getX(), position.getY(), position.getZ());
    }

    @Override
    @NotNull
    public IPLocation create(final @NotNull UUID worldUUID, final @NotNull IVector3DdConst position)
    {
        return create(worldUUID, position.getX(), position.getY(), position.getZ());
    }
}
