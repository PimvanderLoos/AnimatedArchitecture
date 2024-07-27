package nl.pim16aap2.animatedarchitecture.spigot.core.animation.recovery;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
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
     * @param animatedBlockRecoveryDataSerializer@return
     *     True if a recovery action was performed, false otherwise.
     * @throws RecoveryFailureException
     *     If something prevented the recovery action from being successful.
     */
    @CheckReturnValue
    boolean recover(AnimatedBlockRecoveryDataSerializer animatedBlockRecoveryDataSerializer)
        throws RecoveryFailureException;

    /**
     * Creates a new recovery data object.
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
     * @param blockState
     *     The block state of the block.
     * @return The recovery data object.
     */
    static IAnimatedBlockRecoveryData of(World world, Vector3Di position, BlockData data, BlockState blockState)
    {
        return new AnimatedBlockRecoveryDataWithRawBlockState(
            new AnimatedBlockRecoveryDataBaseData(world, position, data),
            blockState
        );
    }

    /**
     * Represents the base data for an animated block recovery data object.
     * <p>
     * The recovery data consists of the world, position, and block data of the block that needs to be recovered.
     * <p>
     * The recovery action is performed by placing the block data at the specified position in the specified world.
     *
     * @param world
     *     The world the block exists in.
     * @param position
     *     The position of the block.
     * @param blockData
     *     The recovery data.
     */
    record AnimatedBlockRecoveryDataBaseData(
        World world,
        Vector3Di position,
        BlockData blockData
    ) implements IAnimatedBlockRecoveryData
    {
        public AnimatedBlockRecoveryDataBaseData
        {
            // We need to make a deep copy of BlockData because it is mutable and
            // may be altered after this object is created.
            Bukkit.createBlockData(blockData.getAsString());
        }

        @Override
        public boolean recover(AnimatedBlockRecoveryDataSerializer animatedBlockRecoveryDataSerializer)
        {
            world.setBlockData(position.x(), position.y(), position.z(), blockData);
            return true;
        }
    }

    /**
     * Represents an object that stores recovery data for an animated block.
     * <p>
     * This object is partially serialized, meaning that the block state is serialized as a string. The block state is
     * handled this way because deserializing block states is not a 'pure' operation and can cause updates to the block
     * in the world.
     *
     * @param baseData
     *     The base data for the recovery data.
     * @param serializedBlockState
     *     The serialized block state.
     */
    record AnimatedBlockRecoveryDataWithSerializedBlockState(
        @SerializedName("baseData")
        AnimatedBlockRecoveryDataBaseData baseData,
        JsonObject serializedBlockState
    ) implements IAnimatedBlockRecoveryData
    {

        @Override
        public boolean recover(AnimatedBlockRecoveryDataSerializer animatedBlockRecoveryDataSerializer)
        {
            if (!baseData.recover(animatedBlockRecoveryDataSerializer))
                return false;

            System.out.println(
                "Applied blockdata! Location: " + baseData.position +
                    ", type: " + baseData.blockData.getMaterial() +
                    ", block at: " +
                    baseData.world.getBlockAt(baseData.position.x(), baseData.position.y(), baseData.position.z()));

            animatedBlockRecoveryDataSerializer.applySerializedBlockState(
                baseData.world,
                baseData.position,
                serializedBlockState
            );
            System.out.println("Applied blockstate!\n\n");
            return true;
        }
    }

    /**
     * Represents an object that stores recovery data for an animated block.
     * <p>
     * This object contains the raw block state (as opposed to a serialized block state). This means that this object
     * cannot be used to recover the block state. {@link AnimatedBlockRecoveryDataWithSerializedBlockState} should be
     * used instead.
     *
     * @param baseData
     *     The base data for the recovery data.
     * @param blockState
     *     The raw block state.
     */
    record AnimatedBlockRecoveryDataWithRawBlockState(
        @SerializedName("baseData")
        AnimatedBlockRecoveryDataBaseData baseData,
        BlockState blockState
    ) implements IAnimatedBlockRecoveryData
    {
        @Override
        public boolean recover(AnimatedBlockRecoveryDataSerializer animatedBlockRecoveryDataSerializer)
            throws RecoveryFailureException
        {
            throw new RecoveryFailureException("Recovery action cannot be performed with raw block state!");
        }
    }

    /**
     * Represents an empty recovery data object.
     * <p>
     * This object does nothing when {@link #recover(AnimatedBlockRecoveryDataSerializer)} is called.
     * <p>
     * This is useful in situations where no recovery action is needed (e.g. when the block is a preview block and does
     * not alter the world).
     *
     * @see #EMPTY
     */
    record EmptyRecoveryData() implements IAnimatedBlockRecoveryData
    {
        @Override
        public boolean recover(AnimatedBlockRecoveryDataSerializer animatedBlockRecoveryDataSerializer)
        {
            return false;
        }
    }
}
