package nl.pim16aap2.bigdoors.core.api.factories;

import nl.pim16aap2.bigdoors.core.api.IPWorld;

/**
 * Represents a factory for {@link IPWorld} objects.
 *
 * @author Pim
 */
public interface IPWorldFactory
{
    /**
     * Creates a new IPWorld.
     *
     * @param worldName
     *     The name of the world.
     * @return A new IPWorld object.
     */
    IPWorld create(String worldName);
}
