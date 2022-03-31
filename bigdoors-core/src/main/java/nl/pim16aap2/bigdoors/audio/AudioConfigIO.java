package nl.pim16aap2.bigdoors.audio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Handles all I/O for the audio config.
 *
 * @author Pim
 */
@Flogger
class AudioConfigIO
{
    private static final Gson GSON = new GsonBuilder()
        .serializeNulls()
        .setPrettyPrinting()
        .registerTypeAdapter(AudioSet.class, new AudioSet.Deserializer())
        .registerTypeAdapter(AudioDescription.class, new AudioDescription.Deserializer())
        .create();

    private final Path file;

    @Inject AudioConfigIO(@Named("pluginBaseDirectory") Path baseDir)
    {
        file = baseDir.resolve("audio_config.json");
    }

    Map<String, @Nullable AudioSet> readConfig()
    {
        if (!Files.isRegularFile(file))
            return Collections.emptyMap();

        // Older versions of gson don't have the new method yet, so this avoids
        // breaking on older versions.
        @SuppressWarnings("deprecation")//
        final JsonParser jsonParser = new JsonParser();
        try
        {
            final Reader reader = Files.newBufferedReader(file);
            @SuppressWarnings("deprecation")//
            final JsonObject base = jsonParser.parse(reader).getAsJsonObject();
            return readConfig(base);
        }
        catch (Exception | AssertionError e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to read audio config!");
        }
        return Collections.emptyMap();
    }

    private Map<String, @Nullable AudioSet> readConfig(JsonObject base)
    {
        final Map<String, @Nullable AudioSet> parsed = new LinkedHashMap<>();
        for (final Map.Entry<String, JsonElement> entry : base.getAsJsonObject().entrySet())
        {
            final String name = entry.getKey();
            final @Nullable AudioSet audioSet;
            audioSet = GSON.fromJson(entry.getValue(), AudioSet.class);
            parsed.put(name, audioSet);
        }
        return parsed;
    }

    void writeConfig(Map<DoorType, @Nullable AudioSet> defaultMap, @Nullable AudioSet defaultAudioSet)
    {
        final JsonObject base = new JsonObject();
        appendToJsonObject(GSON, base, AudioConfigurator.KEY_DEFAULT, defaultAudioSet);
        defaultMap.forEach((key, val) -> appendToJsonObject(GSON, base, key.getSimpleName(), val));

        try
        {
            Files.writeString(file, GSON.toJson(base), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        }
        catch (IOException e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to write audio config!");
        }
    }

    private void appendToJsonObject(Gson gson, JsonObject jsonObject, String typeName, @Nullable AudioSet audioSet)
    {
        if (audioSet == null || audioSet.isEmpty())
            jsonObject.add(typeName, null);
        else
            jsonObject.add(typeName, gson.toJsonTree(audioSet));
    }
}
