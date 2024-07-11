package nl.pim16aap2.animatedarchitecture.core.util;

import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector2Di;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocationUtilTest
{
    @Test
    void testChunkCoords()
    {
        assertEquals(new Vector2Di(7, 13), LocationUtil.getChunkCoords(new Vector3Di(126, 9999, 223)));
    }

    @ParameterizedTest
    @MethodSource("chunkCoords")
    void testChunkId0(Vector2Di chunkCoords)
    {
        final long chunkId = LocationUtil.getChunkId(chunkCoords);
        assertEquals(chunkCoords, LocationUtil.getChunkFromId(chunkId));
    }

    @Test
    void testGetChunkCoords()
    {
        final var position = new Vector3Di(32, 0, 64);
        final var expected = new Vector2Di(2, 4);
        final var result = LocationUtil.getChunkCoords(position);
        assertEquals(expected, result);
    }

    @Test
    void testSimpleChunkSpaceLocationHash()
    {
        final int x = 20; // In chunk-space:  4 (0100)
        final int z = 30; // In chunk-space: 14 (1110)

        final int y = 40; // In chunk-space: 40 (101000)

        // Expected: [y] [x] [z] = [101000] [0100] [1110]
        final long expected = 0b101000_0100_1110L;

        assertEquals(expected, LocationUtil.simpleChunkSpaceLocationHash(x, y, z));
    }

    static Stream<Vector2Di> chunkCoords()
    {
        return Stream.of(
            new Vector2Di(10, 3),
            new Vector2Di(-257, 12),
            new Vector2Di(15, -211),
            new Vector2Di(-6, -9)
        );
    }
}
