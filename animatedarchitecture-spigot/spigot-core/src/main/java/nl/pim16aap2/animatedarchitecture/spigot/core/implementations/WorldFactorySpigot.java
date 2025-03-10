package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ILocationFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IWorldFactory;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WorldSpigot;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents an implementation of {@link ILocationFactory} for the Spigot platform.
 */
@Singleton
public class WorldFactorySpigot implements IWorldFactory
{
    @Inject
    public WorldFactorySpigot()
    {
    }

    @Override
    public IWorld create(String worldName)
    {
        return new WorldSpigot(worldName);
    }
}
