package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
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

        Assertions.assertFalse(Util.isNumerical(null));
        Assertions.assertFalse(Util.isNumerical(""));
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

    @Test
    void testChunkCoords()
    {
        Assertions.assertEquals(new Vector2Di(7, 13), Util.getChunkCoords(new Vector3Di(126, 9999, 223)));
    }

    @Test
    void isValidMovableName()
    {
        Assertions.assertFalse(Util.isValidMovableName(""));
        Assertions.assertFalse(Util.isValidMovableName(null));
        Assertions.assertFalse(Util.isValidMovableName("0"));
        Assertions.assertFalse(Util.isValidMovableName("0999"));
        Assertions.assertFalse(Util.isValidMovableName("321"));
        Assertions.assertFalse(Util.isValidMovableName("my door"));
        Assertions.assertFalse(Util.isValidMovableName("myDoor!"));
        Assertions.assertFalse(Util.isValidMovableName("myDoor?"));

        Assertions.assertTrue(Util.isValidMovableName("3-21"));
        Assertions.assertTrue(Util.isValidMovableName("mydoor"));
        Assertions.assertTrue(Util.isValidMovableName("MyDoor"));
        Assertions.assertTrue(Util.isValidMovableName("A"));
        Assertions.assertTrue(Util.isValidMovableName("a"));
        Assertions.assertTrue(Util.isValidMovableName("a0"));
        Assertions.assertTrue(Util.isValidMovableName("0a0"));
    }
}
