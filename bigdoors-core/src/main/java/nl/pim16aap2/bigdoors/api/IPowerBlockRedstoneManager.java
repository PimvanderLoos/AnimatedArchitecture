package nl.pim16aap2.bigdoors.api;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;

/**
 * Manages redstone status checks for power blocks.
 *
 * @author Pim
 */
public interface IPowerBlockRedstoneManager
{
    /**
     * Checks if a block is a power block (correct block type) and if it is powered.
     *
     * @param world    The world the block is in.
     * @param position The position of the block.
     * @return True if the block at the given position is a valid power block that is receiving an active redstone
     * signal.
     */
    boolean isBlockPowered(@NonNull IPWorld world, @NonNull Vector3DiConst position);
}
