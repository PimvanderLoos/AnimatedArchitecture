package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UtilTest
{
    @Test
    void testChunkId()
    {
        testChunkId(new Vector2Di(10, 3));
        testChunkId(new Vector2Di(-257, 12));
        testChunkId(new Vector2Di(15, -211));
        testChunkId(new Vector2Di(-6, -9));
    }

    @Test
    void isNumerical()
    {
        Assertions.assertTrue(Util.isNumerical("-1"));
        Assertions.assertTrue(Util.isNumerical("1"));
        Assertions.assertTrue(Util.isNumerical("9999999"));

        Assertions.assertFalse(Util.isNumerical("-"));
        Assertions.assertFalse(Util.isNumerical("1-"));
        Assertions.assertFalse(Util.isNumerical("a"));
        Assertions.assertFalse(Util.isNumerical(":"));
        Assertions.assertFalse(Util.isNumerical("/"));
        Assertions.assertFalse(Util.isNumerical("99999999 "));
        Assertions.assertFalse(Util.isNumerical("99999999a"));
    }

    private static void testChunkId(Vector2Di chunkCoords)
    {
        final long chunkId = Util.getChunkId(chunkCoords);
        Assertions.assertEquals(chunkCoords, Util.getChunkFromId(chunkId));
    }
}
