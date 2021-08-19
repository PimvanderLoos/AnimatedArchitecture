package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;

public class TestPWorldFactory implements IPWorldFactory
{
    @Override
    public IPWorld create(final String worldName)
    {
        return new TestPWorld(worldName);
    }
}
