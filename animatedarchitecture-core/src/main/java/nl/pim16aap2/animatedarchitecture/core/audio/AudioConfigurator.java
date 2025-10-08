package nl.pim16aap2.animatedarchitecture.core.audio;

import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.IRestartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a class used to read and access the audio configuration.
 */
@CustomLog
public final class AudioConfigurator implements IRestartable, IDebuggable
{
    public static final String KEY_DEFAULT = "DEFAULT";
    private static final AudioSet EMPTY_AUDIO_SET = new AudioSet(null, null);

    private final AudioConfigIO audioConfigIO;
    private final StructureTypeManager structureTypeManager;
    @Getter(AccessLevel.PACKAGE)
    private Map<StructureType, AudioSet> audioMap = new HashMap<>();

    @Inject
    AudioConfigurator(
        AudioConfigIO audioConfigIO,
        RestartableHolder restartableHolder,
        DebuggableRegistry debuggableRegistry,
        StructureTypeManager structureTypeManager)
    {
        this.audioConfigIO = audioConfigIO;
        this.structureTypeManager = structureTypeManager;

        restartableHolder.registerRestartable(this);
        debuggableRegistry.registerDebuggable(this);
    }

    /**
     * Retrieves the audio set mapped for a structure.
     * <p>
     * This method will use the user-specified configuration for retrieving the mapping. If the user did not modify
     * this, the mapping is simply the same as the structure type's default audio set.
     *
     * @param structureType
     *     The type of the structure for which to retrieve the audio set to use.
     * @return The audio set mapped for the given structure.
     */
    public AudioSet getAudioSet(StructureType structureType)
    {
        final @Nullable AudioSet ret = audioMap.get(structureType);
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
    Map<StructureType, AudioSet> getFinalMap(ConfigData configData)
    {
        final Map<StructureType, AudioSet> ret = new LinkedHashMap<>(configData.sets.size());

        final AudioSet fallback = configData.defaultSet == null ? EMPTY_AUDIO_SET : configData.defaultSet;
        configData.sets.forEach((key, val) -> ret.put(key, val == null ? fallback : val));
        return ret;
    }

    /**
     * Reads the configuration from file and merges it with the default values provided by the structure types.
     *
     * @return The configured data based on the defaults provided by the structure types and the user-specified
     * configurations.
     */
    ConfigData generateConfigData()
    {
        final Map<StructureType, @Nullable AudioSet> defaults = getDefaults();
        final Map<String, @Nullable AudioSet> parsed = audioConfigIO.readConfig();

        final @Nullable AudioSet defaultAudioSet = parsed.get(KEY_DEFAULT);
        return new ConfigData(defaultAudioSet, mergeMaps(parsed, defaults));
    }

    /**
     * @return The default audio set mappings for each structure type as specified by the structure type itself.
     */
    private Map<StructureType, @Nullable AudioSet> getDefaults()
    {
        final Collection<StructureType> enabledStructureTypesTypes = structureTypeManager.getEnabledStructureTypes();
        final Map<StructureType, @Nullable AudioSet> defaultMap =
            new LinkedHashMap<>(enabledStructureTypesTypes.size());
        structureTypeManager.getEnabledStructureTypes().forEach(type -> defaultMap.put(type, type.getAudioSet()));
        return defaultMap;
    }

    private Map<StructureType, @Nullable AudioSet> mergeMaps(
        Map<String, @Nullable AudioSet> parsed,
        Map<StructureType, @Nullable AudioSet> defaults)
    {
        final Map<String, StructureType> types = structureTypeManager
            .getEnabledStructureTypes().stream()
            .collect(Collectors.toMap(StructureType::getSimpleName, type -> type));

        final LinkedHashMap<StructureType, @Nullable AudioSet> merged = new LinkedHashMap<>(defaults);
        for (final Map.Entry<String, @Nullable AudioSet> entry : parsed.entrySet())
        {
            if (KEY_DEFAULT.equals(entry.getKey()))
                continue;

            final @Nullable StructureType type = types.get(entry.getKey());
            if (type != null)
                merged.put(type, entry.getValue());
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
        return "AudioSets: " +
            StringUtil.formatCollection(
                audioMap.entrySet(),
                entry -> "Type:  " + entry.getKey() + "\n  Audio: " + entry.getValue()
            );
    }

    @AllArgsConstructor
    static class ConfigData
    {
        private final @Nullable AudioSet defaultSet;
        private final Map<StructureType, @Nullable AudioSet> sets;

        @Nullable
        AudioSet defaultSet()
        {
            return defaultSet;
        }

        Map<StructureType, @Nullable AudioSet> sets()
        {
            return sets;
        }
    }
}
