package nl.pim16aap2.animatedarchitecture.core.audio;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * Represents a set of audio descriptions that together define the set of audio descriptions to be used in an
 * animation.
 *
 * @param activeAudio
 *     The audio to use while the animation is active. Taking a structure as an example, this could be sound of the
 *     scraping of the bottom part of the structure along the floor.
 * @param endAudio
 *     The audio to use at the end of the animation when it is shutting down. Looking at a structure again, this could
 *     describe the sound of the structure slamming into the structure frame.
 */
public record AudioSet(@Nullable AudioDescription activeAudio, @Nullable AudioDescription endAudio)
{
    /**
     * @return True if all audio descriptions in this set are empty.
     */
    boolean isEmpty()
    {
        return activeAudio == null && endAudio == null;
    }

    public static final class Deserializer implements JsonDeserializer<AudioSet>
    {
        private static boolean isNull(JsonElement src)
        {
            return src.isJsonPrimitive() &&
                src.getAsJsonPrimitive().isString() &&
                "null".equals(src.getAsJsonPrimitive().getAsString());
        }

        @Override
        public @Nullable AudioSet deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException
        {
            if (isNull(json))
                return null;

            final JsonObject jsonObject = json.getAsJsonObject();
            return new AudioSet(
                getAudioDescription(context, jsonObject.get("activeAudio")),
                getAudioDescription(context, jsonObject.get("endAudio")));
        }

        private @Nullable AudioDescription getAudioDescription(JsonDeserializationContext context, JsonElement src)
        {
            if (isNull(src))
                return null;
            return context.deserialize(src, AudioDescription.class);
        }
    }
}
