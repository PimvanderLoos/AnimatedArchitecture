package nl.pim16aap2.bigdoors.audio;

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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a class used to read and access the audio configuration.
 *
 * @author Pim
 */
public final class AudioConfigurator implements IRestartable, IDebuggable
{
    private static final AudioSet EMPTY_AUDIO_SET = new AudioSet(null, null);

    private final Path file;
    private final DoorTypeManager doorTypeManager;
    private AudioSet defaultAudioSet = EMPTY_AUDIO_SET;
    private final Map<DoorType, AudioSet> audioMap = new HashMap<>();

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
        final @Nullable AudioSet typeAudioSet = door.getDoorType().getAudioSet();
        final AudioSet fallback = typeAudioSet == null ? defaultAudioSet : typeAudioSet;

        final @Nullable AudioSet result = audioMap.putIfAbsent(door.getDoorType(), fallback);
        return result == null ? fallback : result;
    }

    private void processAudioConfig()
    {
        doorTypeManager.getEnabledDoorTypes().forEach(
            type -> audioMap.put(type, type.getAudioSet() == null ? defaultAudioSet : type.getAudioSet()));

        // TODO: Implement config
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
        defaultAudioSet = EMPTY_AUDIO_SET;
    }

    @Override
    public String getDebugInformation()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("AudioConfigReader File: '").append(file).append("'\n")
          .append("Default: ").append(defaultAudioSet).append('\n')
          .append("AudioSets:\n");
        audioMap.forEach((key, val) -> sb.append("  Type: ").append(key).append(", Audio: ").append(val).append('\n'));
        return sb.toString();
    }
}
