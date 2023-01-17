package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.movabletypes.MovableType;

import java.util.OptionalDouble;

public interface IEconomyManager
{
    /**
     * Buys a movable for a player.
     *
     * @param player
     *     The player whose bank account to use.
     * @param world
     *     The world the movable is in.
     * @param type
     *     The {@link MovableType} of the movable.
     * @param blockCount
     *     The number of blocks in the movable.
     * @return True if the player bought the movable successfully.
     */
    boolean buyMovable(IPPlayer player, IPWorld world, MovableType type, int blockCount);

    /**
     * Gets the price of {@link MovableType} for a specific number of blocks.
     *
     * @param type
     *     The {@link MovableType}.
     * @param blockCount
     *     The number of blocks.
     * @return The price of this {@link MovableType} with this number of blocks.
     */
    OptionalDouble getPrice(MovableType type, int blockCount);

    /**
     * Checks if the economy manager is enabled.
     *
     * @return True if the economy manager is enabled.
     */
    boolean isEconomyEnabled();
}
