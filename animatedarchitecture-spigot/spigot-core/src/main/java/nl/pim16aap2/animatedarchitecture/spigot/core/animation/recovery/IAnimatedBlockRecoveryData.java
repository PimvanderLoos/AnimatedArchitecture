package nl.pim16aap2.animatedarchitecture.spigot.core.animation.recovery;

import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.BlockStateManipulator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

import javax.annotation.CheckReturnValue;

/**
 * Represents an object that stores recovery data for an animated block.
 */
public sealed interface IAnimatedBlockRecoveryData
{
    /**
     * An empty recovery data object.
     *
     * @see EmptyRecoveryData
     */
    IAnimatedBlockRecoveryData EMPTY = new EmptyRecoveryData();

    /**
     * Attempts recovery of the animated block display.
     * <p>
     * This method gives the animated block the opportunity to recover from a crash or other unexpected event.
     * <p>
     * No guarantees are made about the exact behavior of this method, but as long as any kind of recovery action is
     * performed, this method should return true. Only when this method is a no-op should it return false.
     *
     * @param blockStateManipulator
     *     The block state manipulator to use for the recovery action.
     * @return True if a recovery action was performed, false otherwise.
     *
     * @throws RecoveryFailureException
     *     If something prevented the recovery action from being successful.
     */
    @CheckReturnValue
    boolean recover(BlockStateManipulator blockStateManipulator)
        throws RecoveryFailureException;

    /**
     * Represents an object that stores recovery data for an animated block.
     * <p>
     * The recovery data consists of the world, position, and block data of the block that needs to be recovered.
     * <p>
     * The recovery action is performed by placing the block data at the specified position in the specified world.
     *
     * @param world
     *     The world the block exists in.
     * @param position
     *     The position of the block.
     * @param data
     *     The recovery data.
     */
    record AnimatedBlockRecoveryData(
        World world,
        Vector3Di position,
        BlockData data,
        BlockState blockState
    ) implements IAnimatedBlockRecoveryData
    {
        public AnimatedBlockRecoveryData
        {
            // We need to make a deep copy of BlockData because it is mutable and
            // may be altered after this object is created.
            Bukkit.createBlockData(data.getAsString());
        }

        @Override
        public boolean recover(BlockStateManipulator blockStateManipulator)
        {
            world.setBlockData(position.x(), position.y(), position.z(), data);
            blockState.update(true, false);
            return true;
        }
    }

    /**
     * Represents an empty recovery data object.
     * <p>
     * This object does nothing when {@link #recover(BlockStateManipulator)} is called.
     * <p>
     * This is useful in situations where no recovery action is needed (e.g. when the block is a preview block and does
     * not alter the world).
     *
     * @see #EMPTY
     */
    record EmptyRecoveryData() implements IAnimatedBlockRecoveryData
    {
        @Override
        public boolean recover(BlockStateManipulator blockStateManipulator)
        {
            return false;
        }
    }
}
