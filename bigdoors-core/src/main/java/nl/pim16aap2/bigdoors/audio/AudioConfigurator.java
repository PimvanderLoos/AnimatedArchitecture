package nl.pim16aap2.bigdoors.audio;

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
    private final Map<DoorType, AudioSet> audioMap = new HashMap<>();

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

    void processAudioConfig()
    {
        final Map<DoorType, @Nullable AudioSet> defaults = getDefaults();
        final Map<String, @Nullable AudioSet> parsed = audioConfigIO.readConfig();

        final @Nullable AudioSet defaultAudioSet = parsed.get(KEY_DEFAULT);
        final Map<DoorType, @Nullable AudioSet> merged = mergeMaps(parsed, defaults, defaultAudioSet);
        audioConfigIO.writeConfig(merged, defaultAudioSet);

        merged.forEach((key, val) -> audioMap.put(key, val == null ? EMPTY_AUDIO_SET : val));
        Runtime.getRuntime().halt(0);
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

    Map<DoorType, @Nullable AudioSet> mergeMaps(
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
}
