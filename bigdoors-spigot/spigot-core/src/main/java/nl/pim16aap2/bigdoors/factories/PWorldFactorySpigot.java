package nl.pim16aap2.bigdoors.factories;

import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.implementations.PWorldSpigot;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents an implementation of {@link IPLocationFactory} for the Spigot platform.
 *
 * @author Pim
 */
public class PWorldFactorySpigot implements IPWorldFactory
{
    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IPWorld create(final @NotNull UUID worldUUID)
    {
        return new PWorldSpigot(worldUUID);
    }
}
