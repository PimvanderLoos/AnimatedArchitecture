package nl.pim16aap2.bigdoors.audio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.debugging.IDebuggable;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Represents a class used to read and access the audio configuration.
 *
 * @author Pim
 */
@Flogger
public final class AudioConfigurator implements IRestartable, IDebuggable
{
    private static final AudioSet EMPTY_AUDIO_SET = new AudioSet(null, null);
    private static final String KEY_DEFAULT = "DEFAULT";

    private final Path file;
    private final DoorTypeManager doorTypeManager;
    private final Map<DoorType, AudioSet> audioMap = new HashMap<>();

    private final Gson gson = new GsonBuilder()
        .serializeNulls()
        .setPrettyPrinting()
        .registerTypeAdapter(AudioSet.class, new AudioSet.Deserializer())
        .registerTypeAdapter(AudioDescription.class, new AudioDescription.Deserializer())
        .create();

    @Inject//
    AudioConfigurator(
        @Named("pluginBaseDirectory") Path baseDir, RestartableHolder restartableHolder,
        DebuggableRegistry debuggableRegistry, DoorTypeManager doorTypeManager)
    {
        file = baseDir.resolve("audio_config.json");
        this.doorTypeManager = doorTypeManager;

        restartableHolder.registerRestartable(this);
        debuggableRegistry.registerDebuggable(this);
    }

    /**
     * Retrieves the audio set mapped for a door.
     * <p>
     * This method will use the user-specified configuration for retrieving the mapping. If the user did not modify
     * this, the mapping is simply the same as the door type's default audio set.
     *
     * @param door
     *     The door for which to retrieve the audio set to use.
     * @return The audio set mapped for the given door.
     */
    public AudioSet getAudioSet(AbstractDoor door)
    {
        final @Nullable AudioSet ret = audioMap.get(door.getDoorType());
        return ret == null ? EMPTY_AUDIO_SET : ret;
    }

    private void processAudioConfig()
    {
        final Collection<DoorType> enabledDoorTypes = doorTypeManager.getEnabledDoorTypes();
        final Map<DoorType, @Nullable AudioSet> defaultMap = new HashMap<>(enabledDoorTypes.size());
        doorTypeManager.getEnabledDoorTypes().forEach(type -> defaultMap.put(type, type.getAudioSet()));

        final Map<String, @Nullable AudioSet> parsed = readConfig();
        final @Nullable AudioSet defaultAudioSet = parsed.get(KEY_DEFAULT);

        final Map<DoorType, @Nullable AudioSet> merged = mergeMaps(parsed, defaultMap, defaultAudioSet);

        writeConfig(merged, defaultAudioSet);

        merged.forEach((key, val) -> audioMap.put(key, val == null ? EMPTY_AUDIO_SET : val));
    }

    private Map<DoorType, @Nullable AudioSet> mergeMaps(
        Map<String, @Nullable AudioSet> parsed, Map<DoorType, @Nullable AudioSet> defaults,
        @Nullable AudioSet defaultSet)
    {
        final LinkedHashMap<DoorType, @Nullable AudioSet> merged = new LinkedHashMap<>(defaults);
        if (defaultSet != null)
            for (final var entry : merged.entrySet())
                if (entry.getValue() == null)
                    entry.setValue(defaultSet);

        for (final Map.Entry<String, @Nullable AudioSet> entry : parsed.entrySet())
        {
            if (KEY_DEFAULT.equals(entry.getKey()))
                continue;
            doorTypeManager.getDoorType(entry.getKey()).ifPresent(type -> merged.put(type, entry.getValue()));
        }
        return merged;
    }

    private Map<String, @Nullable AudioSet> readConfig()
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
            if ("\"null\"".equals(entry.getValue().toString()))
                audioSet = null;
            else
                audioSet = gson.fromJson(entry.getValue(), AudioSet.class);
            parsed.put(name, audioSet);
        }
        return parsed;
    }

    private void writeConfig(Map<DoorType, @Nullable AudioSet> defaultMap, @Nullable AudioSet defaultAudioSet)
    {
        final JsonObject base = new JsonObject();
        appendToJsonObject(gson, base, KEY_DEFAULT, defaultAudioSet);
        defaultMap.forEach((key, val) -> appendToJsonObject(gson, base, key.getSimpleName(), val));

        try
        {
            Files.writeString(file, gson.toJson(base), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        }
        catch (IOException e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to write audio config!");
        }
    }

    private void appendToJsonObject(Gson gson, JsonObject jsonObject, String typeName, @Nullable AudioSet audioSet)
    {
        if (audioSet == null || audioSet.isEmpty())
            jsonObject.addProperty(typeName, gson.toJson(null));
        else
            jsonObject.add(typeName, gson.toJsonTree(audioSet));
    }

    @Override
    public synchronized void initialize()
    {
        processAudioConfig();
    }

    @Override
    public synchronized void shutDown()
    {
        audioMap.clear();
    }

    @Override
    public String getDebugInformation()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("AudioConfigReader File: '").append(file).append("'\n")
          .append("AudioSets:\n");
        audioMap.forEach((key, val) -> sb.append("  Type: ").append(key).append(", Audio: ").append(val).append('\n'));
        return sb.toString();
    }
}
