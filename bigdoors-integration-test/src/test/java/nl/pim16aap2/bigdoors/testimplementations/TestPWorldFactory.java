package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import org.jetbrains.annotations.NotNull;

public class TestPWorldFactory implements IPWorldFactory
{
    @Override
    public @NotNull IPWorld create(final @NotNull String worldName)
    {
        return new TestPWorld(worldName);
    }
}
