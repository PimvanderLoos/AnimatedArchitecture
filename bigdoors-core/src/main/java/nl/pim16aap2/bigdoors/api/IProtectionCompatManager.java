package nl.pim16aap2.bigdoors.api;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;

import java.util.Optional;

/**
 * Class that manages all objects of IProtectionCompat.
 *
 * @author Pim
 */
public interface IProtectionCompatManager
{
    /**
     * Check if a player can break a block at a given location.
     *
     * @param player The {@link IPPlayer}.
     * @param loc    The {@link IPLocation} to check.
     * @return The name of the IProtectionCompat that objects, if any, or an empty Optional if allowed by all compats.
     */
    @NonNull Optional<String> canBreakBlock(final @NonNull IPPlayer player, final @NonNull IPLocationConst loc);

    /**
     * Check if a player can break all blocks between two locations.
     *
     * @param player The {@link IPPlayer}.
     * @param pos1   The start position to check.
     * @param pos2   The end position to check.
     * @param world  The world.
     * @return The name of the IProtectionCompat that objects, if any, or an empty Optional if allowed by all compats.
     */
    @NonNull Optional<String> canBreakBlocksBetweenLocs(final @NonNull IPPlayer player,
                                                        final @NonNull Vector3DiConst pos1,
                                                        final @NonNull Vector3DiConst pos2,
                                                        final @NonNull IPWorld world);
}
