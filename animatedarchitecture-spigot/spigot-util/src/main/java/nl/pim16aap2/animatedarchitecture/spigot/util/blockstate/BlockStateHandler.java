package nl.pim16aap2.animatedarchitecture.spigot.util.blockstate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.flogger.Flogger;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a class that can handle block states.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Flogger
public abstract class BlockStateHandler<T>
{
    /**
     * The class of the block state that this handler can handle.
     * <p>
     * Note that this is not always a direct subclass of {@link BlockState}, but can also be an interface that the block
     * state implements.
     */
    @Getter
    private final Class<T> blockStateClass;

    /**
     * Checks if the provided block states are of the correct type.
     * <p>
     * Both the source and the target block state should be instances of {@link #blockStateClass}.
     *
     * @param source
     *     The source block state. Can be {@code null} to ignore the source block state.
     * @param target
     *     The target block state. Can be {@code null} to ignore the target block state.
     * @throws IllegalArgumentException
     *     If the provided block state is not of the correct type.
     */
    private void checkBlockStateClass(@Nullable BlockState source, @Nullable BlockState target)
    {
        if (source != null && !blockStateClass.isInstance(source))
            throw new IllegalArgumentException(
                "Expected the source block state to be of type " + blockStateClass.getSimpleName() +
                    ", but got " + source.getClass().getSimpleName() + " instead."
            );

        if (target != null && !blockStateClass.isInstance(target))
            throw new IllegalArgumentException(
                "Expected the target block state to be of type " + blockStateClass.getSimpleName() +
                    ", but got " + target.getClass().getSimpleName() + " instead."
            );
    }

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
        checkBlockStateClass(source, target);
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

    /**
     * Serializes the block state to a {@link JsonObject}.
     *
     * @param gson
     * @param source
     *     The block state to serialize.
     * @param jsonObject
     *     The {@link JsonObject} to append the serialized data to.
     */
    public final void appendSerializedData(Gson gson, BlockState source, JsonObject jsonObject)
    {
        checkBlockStateClass(source, null);
        appendSerializedData(gson, blockStateClass.cast(source), jsonObject);
    }

    /**
     * Appends the serialized data of the block state to the {@link JsonObject}.
     *
     * @param gson
     *     The {@link Gson} instance to use for serialization.
     * @param source
     *     The block state to serialize.
     * @param jsonObject
     *     The {@link JsonObject} to append the serialized data to.
     */
    protected abstract void appendSerializedData(Gson gson, T source, JsonObject jsonObject);

    /**
     * Applies the serialized data to the target block state.
     * <p>
     * Note that this method requires the block to already be placed in the world and of the correct type.
     *
     * @param gson
     *     The {@link Gson} instance to use for deserialization.
     * @param target
     *     The target block state to apply the serialized block state to.
     * @param serializedBlockState
     *     The serialized block state to apply to the target block state.
     * @throws IllegalArgumentException
     *     If the provided block state is not of the correct type.
     */
    public final void applySerializedData(Gson gson, BlockState target, JsonObject serializedBlockState)
    {
        checkBlockStateClass(null, target);

        applySerializedBlockState(gson, blockStateClass.cast(target), serializedBlockState);
    }

    /**
     * Applies the serialized block state to the target block state.
     *
     * @param gson
     *     The {@link Gson} instance to use for deserialization.
     * @param target
     *     The target block state to apply the serialized block state to.
     * @param serializedBlockState
     *     The serialized block state to apply to the target block state.
     */
    protected abstract void applySerializedBlockState(Gson gson, T target, JsonObject serializedBlockState);
}
