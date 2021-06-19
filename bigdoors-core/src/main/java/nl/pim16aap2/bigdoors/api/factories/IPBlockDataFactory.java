package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.api.ICustomCraftFallingBlock;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.PBlockData;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Represents a factory interface used to construct {@link PBlockData} objects.
 *
 * @author Pim
 */
public interface IPBlockDataFactory
{
    /**
     * Creates a new {@link PBlockData} of the block at the location if allowed and possible.
     *
     * @param loc    The location at which the {@link ICustomCraftFallingBlock} will be spawned.
     * @param bottom True if this is the lowest block of the object to move.
     * @param radius The radius of the block to an arbitrary point.
     * @return The {@link ICustomCraftFallingBlock} that was constructed.
     */
    @NotNull Optional<PBlockData> create(@NotNull IPLocationConst loc, boolean bottom, float radius, float startAngle)
        throws Exception;
}
