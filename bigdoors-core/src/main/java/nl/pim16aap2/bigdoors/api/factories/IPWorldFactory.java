package nl.pim16aap2.bigdoors.api.factories;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPWorld;

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
     * @param worldName The name of the world.
     * @return A new IPWorld object.
     */
    @NonNull IPWorld create(@NonNull String worldName);
}
