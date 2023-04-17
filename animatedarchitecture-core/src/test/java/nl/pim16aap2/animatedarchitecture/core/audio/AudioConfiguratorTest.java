package nl.pim16aap2.animatedarchitecture.core.audio;

import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Mock
    AudioConfigIO audioConfigIO;
    @Mock
    RestartableHolder restartableHolder;
    @Mock
    DebuggableRegistry debuggableRegistry;
    @Mock
    StructureTypeManager structureTypeManager;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);
        Mockito.when(structureTypeManager.getRegisteredStructureTypes())
               .thenReturn(Set.of(TYPE_0, TYPE_1, TYPE_2, TYPE_3, TYPE_4, TYPE_5));
    }

    @SuppressWarnings("ConstantConditions") @Test
    void generateConfigData()
    {
        Mockito.when(TYPE_0.getAudioSet()).thenReturn(SET_1);
        Mockito.when(TYPE_1.getAudioSet()).thenReturn(null);
        Mockito.when(TYPE_2.getAudioSet()).thenReturn(SET_2);
        Mockito.when(TYPE_3.getAudioSet()).thenReturn(SET_3); // Adds key that won't be in parsed set
        Mockito.when(TYPE_4.getAudioSet()).thenReturn(null); // Null for both
        Mockito.when(TYPE_5.getAudioSet()).thenReturn(SET_EMPTY); // Empty one

        final Map<String, @Nullable AudioSet> parsed = new LinkedHashMap<>();
        parsed.put(KEY_0, SET_EMPTY); // Overrides default
        parsed.put(KEY_1, SET_0); // Adds key that didn't exist in default
        parsed.put(KEY_2, null); // Replaces with null
        parsed.put(KEY_4, null); // Null for both

        final AudioConfigurator configurator = new AudioConfigurator(audioConfigIO, restartableHolder,
                                                                     debuggableRegistry, structureTypeManager);
        Mockito.when(audioConfigIO.readConfig()).thenReturn(parsed);
        Mockito.when(structureTypeManager.getEnabledStructureTypes())
               .thenReturn(List.of(TYPE_0, TYPE_1, TYPE_2, TYPE_3, TYPE_4, TYPE_5));

        final AudioConfigurator.ConfigData configData = configurator.generateConfigData();
        final Map<StructureType, @Nullable AudioSet> output = configData.sets();

        Assertions.assertEquals(SET_EMPTY, output.get(TYPE_0));
        Assertions.assertEquals(SET_0, output.get(TYPE_1));
        Assertions.assertNull(output.get(TYPE_2));
        Assertions.assertTrue(output.containsKey(TYPE_2));
        Assertions.assertEquals(SET_3, output.get(TYPE_3));
        Assertions.assertNull(output.get(TYPE_4));
        Assertions.assertTrue(output.containsKey(TYPE_4));
        Assertions.assertEquals(SET_EMPTY, output.get(TYPE_5));
        Assertions.assertNull(configData.defaultSet());
    }

    @Test
    void getFinalMap()
    {
        final AudioConfigurator configurator = new AudioConfigurator(audioConfigIO, restartableHolder,
                                                                     debuggableRegistry, structureTypeManager);

        final Map<StructureType, @Nullable AudioSet> merged = new LinkedHashMap<>();
        merged.put(TYPE_0, SET_0);
        merged.put(TYPE_1, null);
        merged.put(TYPE_2, SET_EMPTY);

        Map<StructureType, AudioSet> result = configurator.getFinalMap(new AudioConfigurator.ConfigData(null, merged));
        Assertions.assertEquals(List.of(SET_0, SET_EMPTY, SET_EMPTY), new ArrayList<>(result.values()));

        result = configurator.getFinalMap(new AudioConfigurator.ConfigData(SET_EMPTY, merged));
        Assertions.assertEquals(List.of(SET_0, SET_EMPTY, SET_EMPTY), new ArrayList<>(result.values()));

        result = configurator.getFinalMap(new AudioConfigurator.ConfigData(SET_1, merged));
        Assertions.assertEquals(List.of(SET_0, SET_1, SET_EMPTY), new ArrayList<>(result.values()));
    }

    private static StructureType newStructureType(String simpleName)
    {
        final StructureType structureType = Mockito.mock(StructureType.class);
        Mockito.when(structureType.getSimpleName()).thenReturn(simpleName);
        return structureType;
    }
}
