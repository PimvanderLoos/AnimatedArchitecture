package nl.pim16aap2.animatedarchitecture.core.audio;

import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AudioConfiguratorTest
{
    static final AudioDescription DESC_0 = new AudioDescription("audio_description_0", 0.1f, 0.1f, 1);
    static final AudioDescription DESC_1 = new AudioDescription("audio_description_1", 0.3f, 0.3f, 3);
    static final AudioDescription DESC_2 = new AudioDescription("audio_description_2", 0.5f, 0.5f, 5);
    static final AudioDescription DESC_3 = new AudioDescription("audio_description_3", 0.7f, 0.7f, 7);

    static final AudioSet SET_0 = new AudioSet(DESC_0, DESC_1);
    static final AudioSet SET_1 = new AudioSet(DESC_0, DESC_2);
    static final AudioSet SET_2 = new AudioSet(null, DESC_3);
    static final AudioSet SET_3 = new AudioSet(DESC_2, null);
    static final AudioSet SET_EMPTY = new AudioSet(null, null);

    static final String KEY_0 = "type_0";
    static final String KEY_1 = "type_1";
    static final String KEY_2 = "type_2";
    static final String KEY_3 = "type_3";
    static final String KEY_4 = "type_4";
    static final String KEY_5 = "type_5";

    static final StructureType TYPE_0 = newStructureType(KEY_0);
    static final StructureType TYPE_1 = newStructureType(KEY_1);
    static final StructureType TYPE_2 = newStructureType(KEY_2);
    static final StructureType TYPE_3 = newStructureType(KEY_3);
    static final StructureType TYPE_4 = newStructureType(KEY_4);
    static final StructureType TYPE_5 = newStructureType(KEY_5);

    @InjectMocks
    private AudioConfigurator configurator;

    @Mock
    private AudioConfigIO audioConfigIO;
    @Mock
    private RestartableHolder restartableHolder;
    @Mock
    private DebuggableRegistry debuggableRegistry;
    @Mock
    private StructureTypeManager structureTypeManager;

    @AfterEach
    void tearDown()
    {
        verify(debuggableRegistry).registerDebuggable(configurator);
        verify(restartableHolder).registerRestartable(configurator);

        verifyNoMoreInteractions(
            audioConfigIO,
            restartableHolder,
            debuggableRegistry,
            structureTypeManager
        );
    }

    @Test
    void generateConfigData_shouldGenerateCorrectOutput()
    {
        // setup
        when(structureTypeManager
            .getRegisteredStructureTypes())
            .thenReturn(List.of(TYPE_0, TYPE_1, TYPE_2, TYPE_3, TYPE_4, TYPE_5));

        when(TYPE_0.getAudioSet()).thenReturn(SET_1);
        when(TYPE_1.getAudioSet()).thenReturn(null);
        when(TYPE_2.getAudioSet()).thenReturn(SET_2);
        when(TYPE_3.getAudioSet()).thenReturn(SET_3); // Adds key that won't be in parsed set
        when(TYPE_4.getAudioSet()).thenReturn(null); // Null for both
        when(TYPE_5.getAudioSet()).thenReturn(SET_EMPTY); // Empty one

        final Map<String, @Nullable AudioSet> parsed = new LinkedHashMap<>();
        parsed.put(KEY_0, SET_EMPTY); // Overrides default
        parsed.put(KEY_1, SET_0); // Adds key that didn't exist in default
        parsed.put(KEY_2, null); // Replaces with null
        parsed.put(KEY_4, null); // Null for both

        when(audioConfigIO.readConfig()).thenReturn(parsed);
        when(structureTypeManager
            .getRegisteredStructureTypes())
            .thenReturn(List.of(TYPE_0, TYPE_1, TYPE_2, TYPE_3, TYPE_4, TYPE_5));

        // execute
        final AudioConfigurator.ConfigData configData = configurator.generateConfigData();
        final Map<StructureType, @Nullable AudioSet> output = configData.sets();

        // verify
        assertThat(SET_EMPTY).isEqualTo(output.get(TYPE_0));
        assertThat(SET_0).isEqualTo(output.get(TYPE_1));
        assertThat(output.get(TYPE_2)).isNull();
        assertThat(output).containsKey(TYPE_2);
        assertThat(SET_3).isEqualTo(output.get(TYPE_3));
        assertThat(output.get(TYPE_4)).isNull();
        assertThat(output).containsKey(TYPE_4);
        assertThat(SET_EMPTY).isEqualTo(output.get(TYPE_5));
        assertThat(configData.defaultSet()).isNull();
    }

    @Test
    void getFinalMap()
    {
        final Map<StructureType, @Nullable AudioSet> merged = new LinkedHashMap<>();
        merged.put(TYPE_0, SET_0);
        merged.put(TYPE_1, null);
        merged.put(TYPE_2, SET_EMPTY);

        Map<StructureType, AudioSet> result = configurator.getFinalMap(new AudioConfigurator.ConfigData(null, merged));
        assertThat(result.values()).containsExactly(SET_0, SET_EMPTY, SET_EMPTY);

        result = configurator.getFinalMap(new AudioConfigurator.ConfigData(SET_EMPTY, merged));
        assertThat(result.values()).containsExactly(SET_0, SET_EMPTY, SET_EMPTY);

        result = configurator.getFinalMap(new AudioConfigurator.ConfigData(SET_1, merged));
        assertThat(result.values()).containsExactlyInAnyOrder(SET_0, SET_1, SET_EMPTY);
    }

    private static StructureType newStructureType(String simpleName)
    {
        final StructureType structureType = Mockito.mock(StructureType.class);
        when(structureType.getKey()).thenReturn(simpleName);
        return structureType;
    }
}
