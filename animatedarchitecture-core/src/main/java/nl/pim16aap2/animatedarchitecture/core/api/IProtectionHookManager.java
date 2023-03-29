package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;

import java.util.Optional;

/**
 * Class that manages all objects of IProtectionCompat.
 *
 * @author Pim
 */
public interface IProtectionHookManager
{
    /**
     * Check if a player can break a block at a given location.
     *
     * @param player
     *     The {@link IPlayer}.
     * @param loc
     *     The {@link ILocation} to check.
     * @return The name of the IProtectionCompat that objects, if any, or an empty Optional if allowed by all compats.
     */
    Optional<String> canBreakBlock(IPlayer player, ILocation loc);

    /**
     * Check if a player can break all blocks in a cuboid.
     *
     * @param player
     *     The {@link IPlayer}.
     * @param cuboid
     *     The {@link Cuboid} to check.
     * @param world
     *     The world.
     * @return The name of the IProtectionCompat that objects, if any, or an empty Optional if allowed by all compats.
     */
    Optional<String> canBreakBlocksBetweenLocs(IPlayer player, Cuboid cuboid, IWorld world);

    /**
     * @return True if all checks for block-breaking access can be skipped. This may happen when no hooks are enabled.
     */
    boolean canSkipCheck();
}
