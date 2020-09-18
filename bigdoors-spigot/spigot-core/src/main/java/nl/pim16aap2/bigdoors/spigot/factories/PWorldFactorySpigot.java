package nl.pim16aap2.bigdoors.spigot.factories;

import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PWorldSpigot;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an implementation of {@link IPLocationFactory} for the Spigot platform.
 *
 * @author Pim
 */
public class PWorldFactorySpigot implements IPWorldFactory
{
    @Override
    public @NotNull IPWorld create(final @NotNull String worldName)
    {
        return new PWorldSpigot(worldName);
    }
}
