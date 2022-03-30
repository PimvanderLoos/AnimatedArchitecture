package nl.pim16aap2.bigdoors.audio;

import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.debugging.IDebuggable;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class AudioConfigurator implements IRestartable, IDebuggable
{
    private final Path file;
    private @Nullable AudioSet defaultAudio = null;
    private final Map<DoorType, AudioSet> audioMap = new HashMap<>();

    @Inject//
    AudioConfigurator(
        @Named("pluginBaseDirectory") Path baseDir, RestartableHolder restartableHolder,
        DebuggableRegistry debuggableRegistry)
    {
        file = baseDir.resolve("audio_config.json");

        restartableHolder.registerRestartable(this);
        debuggableRegistry.registerDebuggable(this);
    }

    public AudioSet getAudioSet(AbstractDoor door)
    {
        return new AudioSet(null, null);
    }

    private void processAudioConfig()
    {

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
        defaultAudio = null;
    }

    @Override
    public String getDebugInformation()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("AudioConfigReader File: '").append(file).append("'\n")
          .append("Default: ").append(defaultAudio).append('\n')
          .append("AudioSets:\n");
        audioMap.forEach((key, val) -> sb.append("  Type: ").append(key).append(", Audio: ").append(val).append('\n'));
        return sb.toString();
    }
}
