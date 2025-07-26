package nl.pim16aap2.animatedarchitecture.core.audio;

import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.testing.annotations.FileSystemTest;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class AudioConfigIOTest
{
    private static final String JSON =
        """
            {
              "DEFAULT": {
                "activeAudio": {
                  "sound": "bd.dragging2",
                  "volume": 0.1,
                  "pitch": 0.17,
                  "duration": 1500
                },
                "endAudio": null
              },
              "slidingdoor": {
                "activeAudio": {
                  "sound": "bd.dragging2",
                  "volume": 0.8,
                  "pitch": 0.7,
                  "duration": 15
                },
                "endAudio": {
                  "sound": "bd.thud",
                  "volume": 0.2,
                  "pitch": 0.15,
                  "duration": 5
                }
              },
              "flag": null,
              "garagedoor": {
                "activeAudio": null,
                "endAudio": null
              }
            }""";

    private static final AudioSet SET_DEFAULT = new AudioSet(
        new AudioDescription("bd.dragging2", 0.1f, 0.17f, 1500),
        null);

    private static final AudioSet SET_SLIDING_DOOR = new AudioSet(
        new AudioDescription("bd.dragging2", 0.8f, 0.7f, 15),
        new AudioDescription("bd.thud", 0.2f, 0.15f, 5));

    private static final AudioSet SET_GARAGE_DOOR = new AudioSet(null, null);

    @FileSystemTest
    void readConfig(Path rootDir)
        throws Exception
    {
        final Path file = rootDir.resolve("audio_config.json");
        Files.writeString(file, JSON, StandardOpenOption.CREATE_NEW);

        final Map<String, @Nullable AudioSet> read = new AudioConfigIO(rootDir).readConfig();

        Assertions.assertEquals(4, read.size());

        Assertions.assertEquals(SET_DEFAULT, read.get("DEFAULT"));
        Assertions.assertEquals(SET_SLIDING_DOOR, read.get("slidingdoor"));
        Assertions.assertEquals(SET_GARAGE_DOOR, read.get("garagedoor"));

        Assertions.assertTrue(read.containsKey("flag"));
        Assertions.assertNull(read.get("flag"));
    }

    @FileSystemTest
    void writeConfig(Path rootDir)
        throws Exception
    {
        final Path file = rootDir.resolve("audio_config.json");

        final Map<StructureType, @Nullable AudioSet> map = new LinkedHashMap<>();
        map.put(newDoorType("slidingdoor"), SET_SLIDING_DOOR);
        map.put(newDoorType("flag"), null);
        map.put(newDoorType("garagedoor"), new AudioSet(null, null));

        new AudioConfigIO(rootDir).writeConfig(map, SET_DEFAULT);

        final String contents = Files.readString(file);
        Assertions.assertEquals(JSON, contents);
    }

    private StructureType newDoorType(String simpleName)
    {
        final StructureType doorType = Mockito.mock(StructureType.class);
        Mockito.when(doorType.getSimpleName()).thenReturn(simpleName);
        return doorType;
    }
}
