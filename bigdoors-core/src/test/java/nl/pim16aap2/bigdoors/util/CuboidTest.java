package nl.pim16aap2.bigdoors.util;

import lombok.val;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CuboidTest
{
    public final double EPSILON = 1E-6;

    @Test
    void getVolume()
    {
        final @NotNull Vector3Di min = new Vector3Di(0, 0, 0);
        final @NotNull Vector3Di max = new Vector3Di(10, -10, 10);
        final @NotNull Cuboid cuboid = new Cuboid(min, max);

        Assertions.assertEquals(11 * 11 * 11, cuboid.getVolume());
    }

    @Test
    void updatePositions()
    {
        final @NotNull Vector3Di min = new Vector3Di(0, 0, 0);
        final @NotNull Vector3Di max = new Vector3Di(10, 10, 10);
        final @NotNull Vector3Di newMax = new Vector3Di(20, 20, 20);
        final @NotNull Cuboid cuboid = new Cuboid(min, max);

        cuboid.updatePositions(newMax, max);
        Assertions.assertEquals(max, cuboid.getMin());
        Assertions.assertEquals(newMax, cuboid.getMax());
    }

    @Test
    void moveTest()
    {
        final @NotNull Vector3Di min0 = new Vector3Di(12, 45, 68);
        final @NotNull Vector3Di max0 = new Vector3Di(22, 65, 88);

        final @NotNull Vector3Di min1 = new Vector3Di(13, 49, 76);
        final @NotNull Vector3Di max1 = new Vector3Di(23, 69, 96);

        final @NotNull Cuboid cuboid = new Cuboid(min0, max0);
        cuboid.move(1, 4, 8);
        Assertions.assertEquals(min1, cuboid.getMin());
        Assertions.assertEquals(max1, cuboid.getMax());
    }

    @Test
    void changeDimensionsTest()
    {
        final @NotNull Vector3Di min0 = new Vector3Di(12, 45, 68);
        final @NotNull Vector3Di max0 = new Vector3Di(22, 65, 88);

        final @NotNull Vector3Di min1 = new Vector3Di(11, 41, 60);
        final @NotNull Vector3Di max1 = new Vector3Di(23, 69, 96);

        final @NotNull Cuboid cuboid = new Cuboid(min0, max0);
        cuboid.changeDimensions(1, 4, 8);
        Assertions.assertEquals(min1, cuboid.getMin());
        Assertions.assertEquals(max1, cuboid.getMax());
    }

    @Test
    void changeDimensionsInvTest()
    {
        final @NotNull Vector3Di min0 = new Vector3Di(12, 45, 68);
        final @NotNull Vector3Di max0 = new Vector3Di(22, 65, 70);

        final @NotNull Vector3Di min1 = new Vector3Di(13, 49, 62);
        final @NotNull Vector3Di max1 = new Vector3Di(21, 61, 76);

        final @NotNull Cuboid cuboid = new Cuboid(min0, max0);
        cuboid.changeDimensions(-1, -4, -8);
        Assertions.assertEquals(min1, cuboid.getMin());
        Assertions.assertEquals(max1, cuboid.getMax());
    }

    @Test
    void isPosInsideCuboid()
    {
        final @NotNull Vector3Di min = new Vector3Di(0, 0, 0);
        final @NotNull Vector3Di max = new Vector3Di(10, 10, 10);
        final @NotNull Cuboid cuboid = new Cuboid(min, max);

        final @NotNull Vector3Di pos0 = new Vector3Di(5, 5, 5);
        final @NotNull Vector3Di pos1 = new Vector3Di(10, 0, 5);
        final @NotNull Vector3Di pos2 = new Vector3Di(11, 0, 5);
        final @NotNull Vector3Di pos3 = new Vector3Di(10, 11, 0);
        final @NotNull Vector3Di pos4 = new Vector3Di(11, 11, 11);

        Assertions.assertTrue(cuboid.isPosInsideCuboid(pos0));
        Assertions.assertTrue(cuboid.isPosInsideCuboid(pos1));
        Assertions.assertFalse(cuboid.isPosInsideCuboid(pos2));
        Assertions.assertFalse(cuboid.isPosInsideCuboid(pos3));
        Assertions.assertFalse(cuboid.isPosInsideCuboid(pos4));
    }

    @Test
    void getMin()
    {
        final @NotNull Vector3Di val1 = new Vector3Di(10, -10, 10);
        final @NotNull Vector3Di val2 = new Vector3Di(11, 20, 9);

        final @NotNull Vector3Di actualMin = new Vector3Di(10, -10, 9);

        final @NotNull Cuboid cuboid = new Cuboid(val1, val2);
        Assertions.assertEquals(actualMin, cuboid.getMin());

        cuboid.updatePositions(val2, val1);
        Assertions.assertEquals(actualMin, cuboid.getMin());
    }

    @Test
    void getMax()
    {
        final @NotNull Vector3Di val1 = new Vector3Di(10, -10, 10);
        final @NotNull Vector3Di val2 = new Vector3Di(11, 20, 9);

        final @NotNull Vector3Di actualMax = new Vector3Di(11, 20, 10);

        final @NotNull Cuboid cuboid = new Cuboid(val1, val2);
        Assertions.assertEquals(actualMax, cuboid.getMax());

        cuboid.updatePositions(val2, val1);
        Assertions.assertEquals(actualMax, cuboid.getMax());
    }

    @Test
    void getCenterBlock()
    {
        final @NotNull Vector3Di val1 = new Vector3Di(0, 11, 30);
        final @NotNull Vector3Di val2 = new Vector3Di(10, 0, 30);

        final @NotNull Vector3Di center = new Vector3Di(5, 5, 30);

        final @NotNull Cuboid cuboid = new Cuboid(val1, val2);
        Assertions.assertEquals(center, cuboid.getCenterBlock());
    }

    @Test
    void getDimensions()
    {
        final @NotNull Vector3Di val1 = new Vector3Di(0, 11, 30);
        final @NotNull Vector3Di val2 = new Vector3Di(10, 0, 30);

        final @NotNull Vector3Di dimensions = new Vector3Di(11, 12, 1);
        Assertions.assertEquals(dimensions, new Cuboid(val1, val2).getDimensions());
    }

    @Test
    void getCenter()
    {
        final @NotNull Vector3Di val1 = new Vector3Di(0, 11, 30);
        final @NotNull Vector3Di val2 = new Vector3Di(10, 0, 30);

        final @NotNull Vector3Dd center = new Vector3Dd(5, 5.5d, 30);

        final @NotNull Vector3Dd foundCenter = new Cuboid(val1, val2).getCenter();
        Assertions.assertTrue(Math.abs(center.getX() - foundCenter.getX()) < EPSILON);
        Assertions.assertTrue(Math.abs(center.getY() - foundCenter.getY()) < EPSILON);
        Assertions.assertTrue(Math.abs(center.getZ() - foundCenter.getZ()) < EPSILON);
    }

    @Test
    void testIsInRange()
    {
        @NotNull val val1 = new Vector3Di(-10, 0, 10);
        @NotNull val val2 = new Vector3Di(20, 30, 40);
        @NotNull val cuboid = new Cuboid(val1, val2);

        Assertions.assertThrows(IllegalArgumentException.class, () -> cuboid.isInRange(val1, -1));
        Assertions.assertTrue(cuboid.isInRange(cuboid.getCenterBlock(), 0));
        Assertions.assertTrue(cuboid.isInRange(cuboid.getCenterBlock(), 1));

        Assertions.assertTrue(cuboid.isInRange(new Vector3Di(-11, 31, 10), 1));
        Assertions.assertFalse(cuboid.isInRange(new Vector3Di(-11, 31, 10), 0));
        Assertions.assertTrue(cuboid.isInRange(new Vector3Di(-10, 0, 40), 1));
    }
}
