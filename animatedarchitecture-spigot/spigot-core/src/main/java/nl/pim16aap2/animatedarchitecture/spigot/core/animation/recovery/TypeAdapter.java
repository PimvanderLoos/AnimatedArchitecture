package nl.pim16aap2.animatedarchitecture.spigot.core.animation.recovery;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import lombok.RequiredArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.BlockStateManipulator;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import javax.inject.Provider;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.Objects;

/**
 * Contains type adapters for various types.
 */
public final class TypeAdapter
{
    /**
     * Represents a type adapter for {@link BlockData} objects.
     */
    public static final class BlockDataAdapter implements JsonSerializer<BlockData>, JsonDeserializer<BlockData>
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
    public static final class WorldAdapter implements JsonSerializer<World>, JsonDeserializer<World>
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
    public static final class ColorAdapter implements JsonSerializer<Color>, JsonDeserializer<Color>
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
     * Represents a type adapter for {@link BlockState} objects.
     */
    @RequiredArgsConstructor
    public static final class BlockStateAdapter implements JsonSerializer<BlockState>
    {
        private final Provider<Gson> gsonProvider;
        private final BlockStateManipulator blockStateManipulator;

        @Override
        public JsonElement serialize(BlockState src, Type typeOfSrc, JsonSerializationContext context)
        {
            return blockStateManipulator.serialize(gsonProvider.get(), src);
        }
    }

    /**
     * Represents a type adapter for {@link NamespacedKey} objects.
     */
    public static final class NamespacedKeyAdapter
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
     * Represents a type adapter for byte arrays.
     */
    public static final class ByteArrayAdapter
        implements JsonSerializer<byte[]>, JsonDeserializer<byte[]>
    {
        @Override
        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException
        {
            return Base64.getDecoder().decode(json.getAsString());
        }

        @Override
        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context)
        {
            return new JsonPrimitive(
                Base64.getEncoder().encodeToString(src)
            );
        }
    }

    /**
     * Represents a type adapter for {@link ConfigurationSerializable} objects.
     *
     * @param <T>
     *     The type of the configuration serializable object.
     */
    public static class ConfigurationSerializableAdapter<T extends ConfigurationSerializable>
        implements JsonSerializer<T>, JsonDeserializer<T>
    {
        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException
        {
            final YamlConfiguration config = new YamlConfiguration();
            try
            {
                config.loadFromString(json.getAsString());
                return Util.requireNonNull((T) config.get("object"), "Object deserialized from '" + json + "'");
            }
            catch (InvalidConfigurationException e)
            {
                throw new RuntimeException("Failed to load data from json '" + json + "'", e);
            }
        }

        @Override
        public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context)
        {
            final YamlConfiguration config = new YamlConfiguration();
            config.set("object", src);
            return new JsonPrimitive(config.saveToString());
        }
    }
}
