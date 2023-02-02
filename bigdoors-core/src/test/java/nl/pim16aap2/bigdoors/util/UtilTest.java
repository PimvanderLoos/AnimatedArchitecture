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
    void isValidStructureName()
    {
        Assertions.assertFalse(Util.isValidStructureName(""));
        Assertions.assertFalse(Util.isValidStructureName(null));
        Assertions.assertFalse(Util.isValidStructureName("0"));
        Assertions.assertFalse(Util.isValidStructureName("0999"));
        Assertions.assertFalse(Util.isValidStructureName("321"));
        Assertions.assertFalse(Util.isValidStructureName("my door"));
        Assertions.assertFalse(Util.isValidStructureName("myDoor!"));
        Assertions.assertFalse(Util.isValidStructureName("myDoor?"));

        Assertions.assertTrue(Util.isValidStructureName("3-21"));
        Assertions.assertTrue(Util.isValidStructureName("mydoor"));
        Assertions.assertTrue(Util.isValidStructureName("MyDoor"));
        Assertions.assertTrue(Util.isValidStructureName("A"));
        Assertions.assertTrue(Util.isValidStructureName("a"));
        Assertions.assertTrue(Util.isValidStructureName("a0"));
        Assertions.assertTrue(Util.isValidStructureName("0a0"));
    }
}
