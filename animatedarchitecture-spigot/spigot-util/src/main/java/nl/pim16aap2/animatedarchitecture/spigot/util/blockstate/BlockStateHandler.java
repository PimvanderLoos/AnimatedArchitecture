package nl.pim16aap2.animatedarchitecture.spigot.util.blockstate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

/**
 * Represents a class that can handle block states.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BlockStateHandler<T>
{
    /**
     * The class of the block state that this handler can handle.
     */
    @Getter
    private final Class<T> blockStateClass;

    /**
     * Applies the block state to the block.
     *
     * @param source
     *     The source block state to apply to the target block state.
     * @param target
     *     The target block state to apply the source block state to.
     * @throws IllegalArgumentException
     *     If the provided block state is not of the correct type.
     */
    final BlockState applyBlockState(BlockState source, BlockState target, Block block)
    {
        if (!blockStateClass.isInstance(source))
            throw new IllegalArgumentException(
                "Expected the source block state to be of type " + blockStateClass.getSimpleName() +
                    ", but got " + source.getClass().getSimpleName() + " instead."
            );

        if (!blockStateClass.isInstance(target))
            throw new IllegalArgumentException(
                "Expected the target block state to be of type " + blockStateClass.getSimpleName() +
                    ", but got " + target.getClass().getSimpleName() + " instead."
            );

        applyBlockState(blockStateClass.cast(source), blockStateClass.cast(target), block);
        return target;
    }

    /**
     * Applies the source block state to the target block state.
     * <p>
     * For example, if the source block state is some kind of container with items in it, this method should copy the
     * items from the source container to the target container.
     *
     * @param source
     *     The source block state to apply to the target block state.
     * @param target
     *     The target block state to apply the source block state to.
     * @param block
     */
    protected abstract void applyBlockState(T source, T target, Block block);
}
