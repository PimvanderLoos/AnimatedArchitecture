package nl.pim16aap2.bigdoors.audio;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a class used to read and access the audio configuration.
 *
 * @author Pim
 */
@Flogger
public final class AudioConfigurator implements IRestartable, IDebuggable
{
    public static final String KEY_DEFAULT = "DEFAULT";
    private static final AudioSet EMPTY_AUDIO_SET = new AudioSet(null, null);

    private final AudioConfigIO audioConfigIO;
    private final DoorTypeManager doorTypeManager;
    @Getter(AccessLevel.PACKAGE)
    private Map<DoorType, AudioSet> audioMap = new HashMap<>();

    @Inject//
    AudioConfigurator(
        AudioConfigIO audioConfigIO, RestartableHolder restartableHolder,
        DebuggableRegistry debuggableRegistry, DoorTypeManager doorTypeManager)
    {
        this.audioConfigIO = audioConfigIO;
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
        final ConfigData configData = generateConfigData();
        audioConfigIO.writeConfig(configData.sets, configData.defaultSet());
        this.audioMap = getFinalMap(configData);
    }

    /**
     * Generates the final map from the provided input configuration data. This is done by replacing null values with
     * the designated fallback value. The fallback value will be {@link ConfigData#defaultSet} if that is non-null and
     * otherwise {@link #EMPTY_AUDIO_SET}.
     *
     * @param configData
     *     The config data to use to generate the final map.
     * @return The generated final map.
     */
    Map<DoorType, AudioSet> getFinalMap(ConfigData configData)
    {
        @SuppressWarnings("NullAway") // NullAway currently does not work well with nullable annotations in generics.
        final Map<DoorType, AudioSet> ret = new LinkedHashMap<>(configData.sets.size());

        final AudioSet fallback = configData.defaultSet == null ? EMPTY_AUDIO_SET : configData.defaultSet;
        configData.sets.forEach((key, val) -> ret.put(key, val == null ? fallback : val));
        return ret;
    }

    /**
     * @return The configured data based on the defaults provided by the door types and the user-specified
     * configurations.
     */
    ConfigData generateConfigData()
    {
        final Map<DoorType, @Nullable AudioSet> defaults = getDefaults();
        final Map<String, @Nullable AudioSet> parsed = audioConfigIO.readConfig();

        @SuppressWarnings("NullAway") // NullAway currently does not work well with nullable annotations in generics.
        final @Nullable AudioSet defaultAudioSet = parsed.get(KEY_DEFAULT);
        return new ConfigData(defaultAudioSet, mergeMaps(parsed, defaults));
    }

    /**
     * @return The default audio set mappings for each door type as specified by the door type itself.
     */
    private Map<DoorType, @Nullable AudioSet> getDefaults()
    {
        final Collection<DoorType> enabledDoorTypes = doorTypeManager.getEnabledDoorTypes();
        final Map<DoorType, @Nullable AudioSet> defaultMap = new LinkedHashMap<>(enabledDoorTypes.size());
        doorTypeManager.getEnabledDoorTypes().forEach(type -> defaultMap.put(type, type.getAudioSet()));
        return defaultMap;
    }

    // NullAway currently does not work well with nullable annotations in generics.
    @SuppressWarnings("NullAway")
    private Map<DoorType, @Nullable AudioSet> mergeMaps(
        Map<String, @Nullable AudioSet> parsed, Map<DoorType, @Nullable AudioSet> defaults)
    {
        final LinkedHashMap<DoorType, @Nullable AudioSet> merged = new LinkedHashMap<>(defaults);
        for (final Map.Entry<String, @Nullable AudioSet> entry : parsed.entrySet())
        {
            if (KEY_DEFAULT.equals(entry.getKey()))
                continue;
            doorTypeManager.getDoorType(entry.getKey()).ifPresent(type -> merged.put(type, entry.getValue()));
        }
        return merged;
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
        sb.append("AudioSets:\n");
        audioMap.forEach((key, val) -> sb.append("  Type: ").append(key).append(", Audio: ").append(val).append('\n'));
        return sb.toString();
    }

    @AllArgsConstructor
    static class ConfigData
    {
        private final @Nullable AudioSet defaultSet;
        private final Map<DoorType, @Nullable AudioSet> sets;

        @Nullable AudioSet defaultSet()
        {
            return defaultSet;
        }

        // NullAway currently does not work well with nullable annotations in generics.
        @SuppressWarnings("NullAway")
        Map<DoorType, @Nullable AudioSet> sets()
        {
            return sets;
        }
    }
}
