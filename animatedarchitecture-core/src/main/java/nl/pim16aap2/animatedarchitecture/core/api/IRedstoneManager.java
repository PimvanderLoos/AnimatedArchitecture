package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

/**
 * Manages redstone status checks for power blocks.
 *
 * @author Pim
 */
public interface IRedstoneManager
{
    /**
     * Checks if a block is a power block (correct block type) and if it is powered.
     *
     * @param world
     *     The world the block is in.
     * @param position
     *     The position of the block.
     * @return True if the block at the given position is a valid power block that is receiving an active redstone
     * signal.
     */
    RedstoneStatus isBlockPowered(IWorld world, Vector3Di position);

    default RedstoneStatus isBlockPowered(ILocation location)
    {
        return isBlockPowered(location.getWorld(), location.getPosition());
    }

    enum RedstoneStatus
    {
        POWERED,
        UNPOWERED,
        DISABLED;
    }
}
