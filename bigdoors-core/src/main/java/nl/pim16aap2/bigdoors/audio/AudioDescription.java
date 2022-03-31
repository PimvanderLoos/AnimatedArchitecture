package nl.pim16aap2.bigdoors.audio;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Describes a sound, with its pitch and volume etc.
 *
 * @param sound
 *     The name of the sound to play.
 * @param volume
 *     The volume at which to play the sound.
 * @param pitch
 *     The pitch of the sound.
 * @param duration
 *     The duration of the sound.
 * @author Pim
 */
public record AudioDescription(String sound, float volume, float pitch, int duration)
{
    public static final class Deserializer implements JsonDeserializer<AudioDescription>
    {
        @Override
        public AudioDescription deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException
        {
            final JsonObject jsonObject = json.getAsJsonObject();
            return new AudioDescription(
                jsonObject.get("sound").getAsString(),
                jsonObject.get("volume").getAsFloat(),
                jsonObject.get("pitch").getAsFloat(),
                jsonObject.get("duration").getAsInt());
        }
    }
}
