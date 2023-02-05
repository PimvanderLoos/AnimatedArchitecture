package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.core.api.IPWorld;
import nl.pim16aap2.bigdoors.core.api.factories.IPWorldFactory;

public class TestPWorldFactory implements IPWorldFactory
{
    @Override
    public IPWorld create(String worldName)
    {
        return new TestPWorld(worldName);
    }
}
