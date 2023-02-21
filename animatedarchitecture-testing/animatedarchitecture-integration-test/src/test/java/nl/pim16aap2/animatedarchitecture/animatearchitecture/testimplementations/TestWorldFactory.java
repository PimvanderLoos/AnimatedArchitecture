package nl.pim16aap2.animatedarchitecture.testimplementations;

import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IWorldFactory;

public class TestWorldFactory implements IWorldFactory
{
    @Override
    public IWorld create(String worldName)
    {
        return new nl.pim16aap2.animatedarchitecture.testimplementations.TestWorld(worldName);
    }
}
