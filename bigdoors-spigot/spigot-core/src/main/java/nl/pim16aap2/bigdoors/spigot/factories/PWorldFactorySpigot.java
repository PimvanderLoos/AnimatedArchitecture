package nl.pim16aap2.bigdoors.spigot.factories;

import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PWorldSpigot;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents an implementation of {@link IPLocationFactory} for the Spigot platform.
 *
 * @author Pim
 */
@Singleton
public class PWorldFactorySpigot implements IPWorldFactory
{
    @Inject
    public PWorldFactorySpigot()
    {
    }

    @Override
    public IPWorld create(String worldName)
    {
        return new PWorldSpigot(worldName);
    }
}
