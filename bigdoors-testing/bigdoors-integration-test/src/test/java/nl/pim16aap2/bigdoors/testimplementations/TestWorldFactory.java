package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.core.api.IWorld;
import nl.pim16aap2.bigdoors.core.api.factories.IWorldFactory;

public class TestWorldFactory implements IWorldFactory
{
    @Override
    public IWorld create(String worldName)
    {
        return new TestWorld(worldName);
    }
}
