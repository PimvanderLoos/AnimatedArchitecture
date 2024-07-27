package nl.pim16aap2.animatedarchitecture.spigot.core.animation.recovery;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.BlockStateManipulator;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the data type for the recovery data of an animated block.
 * <p>
 * This class is used to store the recovery data of an animated block. These data are used to recover the state of an
 * animated block that has been 'orphaned' (e.g. during a server crash).
 */
public final class AnimatedBlockRecoveryDataType implements PersistentDataType<String, IAnimatedBlockRecoveryData>
{
    private final Gson gson;
    private final BlockStateManipulator blockStateManipulator;

    @Inject
    AnimatedBlockRecoveryDataType(BlockStateManipulator blockStateManipulator)
    {
        final GsonBuilder gsonBuilder = new GsonBuilder()
            .registerTypeHierarchyAdapter(BlockState.class, new BlockStateAdapter(this))
            .registerTypeHierarchyAdapter(BlockData.class, new BlockDataAdapter())
            .registerTypeHierarchyAdapter(World.class, new WorldAdapter())
            .registerTypeHierarchyAdapter(ItemStack.class, new ItemStackAdapter())
            .registerTypeAdapter(NamespacedKey.class, new NamespacedKeyAdapter())
            .registerTypeAdapter(DyeColor.class, new DyeColorAdapter())
            .registerTypeAdapter(Color.class, new ColorAdapter());

        this.blockStateManipulator = blockStateManipulator;

        gson = gsonBuilder.create();
    }

    @Override
    public Class<String> getPrimitiveType()
    {
        return String.class;
    }

    @Override
    public Class<IAnimatedBlockRecoveryData> getComplexType()
    {
        return IAnimatedBlockRecoveryData.class;
    }

    @Override
    public String toPrimitive(IAnimatedBlockRecoveryData complex, PersistentDataAdapterContext context)
    {
        try
        {
            //  return gson.toJson(jsonObject);

            final String result = gson.toJson(complex);

            final JsonObject jsonObject = gson.toJsonTree(complex).getAsJsonObject();
            if (!jsonObject.getAsJsonObject("blockState").isEmpty())
                System.out.println(result);

            return result;
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Failed to serialize RecoveryData: " + complex, e);
        }
    }

    @Override
    public IAnimatedBlockRecoveryData fromPrimitive(String primitive, PersistentDataAdapterContext context)
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

    /**
     * Represents a type adapter for {@link BlockData} objects.
     */
    private static final class BlockDataAdapter implements JsonSerializer<BlockData>, JsonDeserializer<BlockData>
    {
        @Override
        public JsonElement serialize(BlockData src, Type typeOfSrc, JsonSerializationContext context)
        {
            return new JsonPrimitive(src.getAsString());
        }

        @Override
        public BlockData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException
        {
            return Bukkit.createBlockData(json.getAsString());
        }
    }

    /**
     * Represents a type adapter for {@link World} objects.
     */
    private static final class WorldAdapter implements JsonSerializer<World>, JsonDeserializer<World>
    {
        @Override
        public JsonElement serialize(World src, Type typeOfSrc, JsonSerializationContext context)
        {
            return new JsonPrimitive(src.getName());
        }

        @Override
        public World deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException
        {
            return Objects.requireNonNull(Bukkit.getWorld(json.getAsString()));
        }
    }

    /**
     * Represents a type adapter for {@link Color} objects.
     */
    private static final class ColorAdapter implements JsonSerializer<Color>, JsonDeserializer<Color>
    {
        @Override
        public JsonElement serialize(Color src, Type typeOfSrc, JsonSerializationContext context)
        {
            return new JsonPrimitive(src.asRGB());
        }

        @Override
        public Color deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException
        {
            return Color.fromARGB(json.getAsInt());
        }
    }

    /**
     * Represents a type adapter for {@link DyeColor} objects.
     */
    private static final class DyeColorAdapter implements JsonSerializer<DyeColor>, JsonDeserializer<DyeColor>
    {
        @Override
        public JsonElement serialize(DyeColor src, Type typeOfSrc, JsonSerializationContext context)
        {
            return new JsonPrimitive(src.name());
        }

        @Override
        public DyeColor deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException
        {
            return DyeColor.valueOf(json.getAsString());
        }
    }

    /**
     * Represents a type adapter for {@link BlockState} objects.
     */
    @RequiredArgsConstructor
    private static final class BlockStateAdapter implements JsonSerializer<BlockState>
    {
        private final AnimatedBlockRecoveryDataType animatedBlockRecoveryDataType;

        @Override
        public JsonElement serialize(BlockState src, Type typeOfSrc, JsonSerializationContext context)
        {
            return animatedBlockRecoveryDataType.blockStateManipulator.serialize(
                animatedBlockRecoveryDataType.gson,
                src
            );
        }
    }

    /**
     * Represents a type adapter for {@link NamespacedKey} objects.
     */
    private static final class NamespacedKeyAdapter
        implements JsonSerializer<NamespacedKey>, JsonDeserializer<NamespacedKey>
    {
        @Override
        public JsonElement serialize(NamespacedKey src, Type typeOfSrc, JsonSerializationContext context)
        {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public NamespacedKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException
        {
            return Util.requireNonNull(
                NamespacedKey.fromString(json.getAsString()),
                "namespaced key from string '" + json + "'"
            );
        }
    }

    /**
     * Represents a type adapter for {@link ConfigurationSerializable} objects.
     */
    private static abstract class ConfigurationSerializableAdapter<T extends ConfigurationSerializable>
        implements JsonSerializer<T>, JsonDeserializer<T>
    {
        private static final Type CONFIGURATION_SERIALIZABLE_SERIALIZATION_TYPE =
            new TypeToken<Map<String, Object>>() {}.getType();

        @Override
        public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context)
        {
            return context.serialize(src.serialize(), CONFIGURATION_SERIALIZABLE_SERIALIZATION_TYPE);
        }

        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException
        {
            return fromMap(context.deserialize(json, CONFIGURATION_SERIALIZABLE_SERIALIZATION_TYPE));
        }

        /**
         * Deserializes the given map to the object of type {@link T}.
         *
         * @param map
         *     The map to deserialize.
         * @return The deserialized object.
         */
        protected abstract T fromMap(Map<String, Object> map);
    }

    /**
     * Represents a type adapter for {@link ItemStack} objects.
     */
    private static final class ItemStackAdapter extends ConfigurationSerializableAdapter<ItemStack>
    {
        @Override
        protected ItemStack fromMap(Map<String, Object> map)
        {
            return ItemStack.deserialize(map);
        }
    }
}
