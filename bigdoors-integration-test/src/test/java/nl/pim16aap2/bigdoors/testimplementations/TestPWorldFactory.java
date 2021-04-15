package nl.pim16aap2.bigdoors.testimplementations;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;

public class TestPWorldFactory implements IPWorldFactory
{
    @Override
    public @NonNull IPWorld create(final @NonNull String worldName)
    {
        return new TestPWorld(worldName);
    }
}
