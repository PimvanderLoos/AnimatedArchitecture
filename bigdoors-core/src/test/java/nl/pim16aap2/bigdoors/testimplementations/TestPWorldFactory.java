package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TestPWorldFactory implements IPWorldFactory
{
    @Override
    @NotNull
    public IPWorld create(@NotNull UUID worldUUID)
    {
        return new TestPWorld(worldUUID);
    }
}
