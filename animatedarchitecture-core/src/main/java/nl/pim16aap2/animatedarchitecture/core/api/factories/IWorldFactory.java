package nl.pim16aap2.animatedarchitecture.core.api.factories;

import nl.pim16aap2.animatedarchitecture.core.api.IWorld;

/**
 * Represents a factory for {@link IWorld} objects.
 *
 * @author Pim
 */
public interface IWorldFactory
{
    /**
     * Creates a new IWorld.
     *
     * @param worldName
     *     The name of the world.
     * @return A new IWorld object.
     */
    IWorld create(String worldName);
}
