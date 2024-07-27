package nl.pim16aap2.animatedarchitecture.spigot.core.animation.recovery;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.BlockStateManipulator;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Represents a serializer for {@link IAnimatedBlockRecoveryData} objects.
 * <p>
 * This class is used to store the recovery data of an animated block. These data are used to recover the state of an
 * animated block that has been 'orphaned' (e.g. during a server crash).
 */
@Flogger
public final class AnimatedBlockRecoveryDataSerializer
{
    private final Gson gson;
    private final BlockStateManipulator blockStateManipulator;

    @Inject
    AnimatedBlockRecoveryDataSerializer(BlockStateManipulator blockStateManipulator)
    {
        final Provider<Gson> gsonProvider = new Provider<>()
        {
            @Override
            public Gson get()
            {
                return AnimatedBlockRecoveryDataSerializer.this.gson;
            }
        };

        final GsonBuilder gsonBuilder = new GsonBuilder()
            .registerTypeHierarchyAdapter(
                BlockState.class,
                new TypeAdapter.BlockStateAdapter(gsonProvider, blockStateManipulator))
            .registerTypeHierarchyAdapter(
                ItemStack.class,
                new TypeAdapter.ConfigurationSerializableAdapter<ItemStack>())
            .registerTypeHierarchyAdapter(
                Pattern.class,
                new TypeAdapter.ConfigurationSerializableAdapter<Pattern>())
            .registerTypeHierarchyAdapter(
                PlayerProfile.class,
                new TypeAdapter.ConfigurationSerializableAdapter<PlayerProfile>())
            .registerTypeHierarchyAdapter(byte[].class, new TypeAdapter.ByteArrayAdapter())
            .registerTypeHierarchyAdapter(BlockData.class, new TypeAdapter.BlockDataAdapter())
            .registerTypeHierarchyAdapter(World.class, new TypeAdapter.WorldAdapter())
            .registerTypeAdapter(NamespacedKey.class, new TypeAdapter.NamespacedKeyAdapter())
            .registerTypeAdapter(Color.class, new TypeAdapter.ColorAdapter());

        this.blockStateManipulator = blockStateManipulator;

        gson = gsonBuilder.create();
    }


    /**
     * Serializes the given recovery data to a JSON string.
     * <p>
     * Note that only supported types can be serialized. Any unsupported types will be ignored.
     *
     * @param recoveryData
     *     The recovery data to serialize.
     * @return The serialized recovery data.
     */
    public String toJson(IAnimatedBlockRecoveryData recoveryData)
    {
        try
        {
            return gson.toJson(recoveryData);
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Failed to serialize RecoveryData: " + recoveryData, e);
        }
    }

    /**
     * Deserializes the given primitive to an {@link IAnimatedBlockRecoveryData} object.
     *
     * @param primitive
     *     The primitive to deserialize.
     * @return The deserialized recovery data.
     */
    public IAnimatedBlockRecoveryData fromJson(String primitive)
    {
        final JsonObject recoveryData = gson.fromJson(primitive, JsonObject.class);

        if (recoveryData.isEmpty())
            return IAnimatedBlockRecoveryData.EMPTY;

        final JsonElement baseDataElement = recoveryData.get("baseData");
        final IAnimatedBlockRecoveryData.AnimatedBlockRecoveryDataBaseData baseData;
        try
        {
            baseData = gson
                .fromJson(baseDataElement, IAnimatedBlockRecoveryData.AnimatedBlockRecoveryDataBaseData.class);
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException(
                "Failed to deserialize base data from primitive: '" + primitive + "'", e);
        }

        final @Nullable JsonObject blockStateElement = recoveryData.getAsJsonObject("blockState");
        if (blockStateElement == null || blockStateElement.isEmpty())
            return baseData;

        return new IAnimatedBlockRecoveryData.AnimatedBlockRecoveryDataWithSerializedBlockState(
            baseData,
            blockStateElement
        );
    }

    /**
     * Applies the serialized block state to the given complex.
     *
     * @param world
     *     The world to apply the serialized block state in.
     * @param position
     *     The position to apply the serialized block state at.
     * @param serializedBlockState
     *     The serialized block state to apply.
     */
    void applySerializedBlockState(World world, Vector3Di position, JsonObject serializedBlockState)
    {
        blockStateManipulator.applySerializedBlockState(
            gson,
            world,
            position,
            serializedBlockState
        );
    }
}
