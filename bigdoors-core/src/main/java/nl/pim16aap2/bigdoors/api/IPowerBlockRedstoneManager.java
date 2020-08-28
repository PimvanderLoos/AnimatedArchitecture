package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;

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
    boolean isBlockPowered(final @NotNull IPWorld world, final @NotNull Vector3DiConst position);
}
