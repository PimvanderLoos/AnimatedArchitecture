package nl.pim16aap2.animatedarchitecture.testimplementations;

import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ILocationFactory;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import org.jetbrains.annotations.NotNull;

public class TestLocationFactory implements ILocationFactory
{
    @Override
    public @NotNull ILocation create(@NotNull IWorld world, double x, double y, double z)
    {
        return new nl.pim16aap2.animatedarchitecture.testimplementations.TestLocation(world, x, y, z);
    }

    @Override
    public @NotNull ILocation create(@NotNull IWorld world, @NotNull IVector3D position)
    {
        return create(world, position.xD(), position.yD(), position.zD());
    }

    @Override
    public @NotNull ILocation create(@NotNull String worldName, double x, double y, double z)
    {
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public @NotNull ILocation create(@NotNull String worldName, @NotNull IVector3D position)
    {
        throw new UnsupportedOperationException("Not implemented!");
    }
}
