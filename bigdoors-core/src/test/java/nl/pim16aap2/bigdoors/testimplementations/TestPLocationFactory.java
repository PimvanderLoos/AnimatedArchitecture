package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TestPLocationFactory implements IPLocationFactory
{
    @Override
    @NotNull
    public IPLocation create(final @NotNull IPWorld world, final double x, final double y, final double z)
    {
        return new TestPLocation(world, x, y, z);
    }

    @Override
    @NotNull
    public IPLocation create(final @NotNull IPWorld world, final @NotNull Vector3DiConst position)
    {
        return create(world, position.getX(), position.getY(), position.getZ());
    }

    @Override
    @NotNull
    public IPLocation create(final @NotNull IPWorld world, final @NotNull Vector3DdConst position)
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
    public IPLocation create(final @NotNull UUID worldUUID, final @NotNull Vector3DiConst position)
    {
        return create(BigDoors.get().getPlatform().getPWorldFactory().create(worldUUID),
                      position.getX(), position.getY(), position.getZ());
    }

    @Override
    @NotNull
    public IPLocation create(final @NotNull UUID worldUUID, final @NotNull Vector3DdConst position)
    {
        return create(BigDoors.get().getPlatform().getPWorldFactory().create(worldUUID),
                      position.getX(), position.getY(), position.getZ());
    }
}
