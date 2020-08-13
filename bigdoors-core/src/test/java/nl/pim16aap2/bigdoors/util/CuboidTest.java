package nl.pim16aap2.bigdoors.util;

import junit.framework.Assert;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class CuboidTest
{
    @Test
    void getVolume()
    {
        final @NotNull Vector3Di min = new Vector3Di(0, 0, 0);
        final @NotNull Vector3Di max = new Vector3Di(10, -10, 10);
        final @NotNull Cuboid cuboid = new Cuboid(min, max);

        Assert.assertEquals(11 * 11 * 11, cuboid.getVolume().intValue());
    }

    @Test
    void updatePositions()
    {
        final @NotNull Vector3Di min = new Vector3Di(0, 0, 0);
        final @NotNull Vector3Di max = new Vector3Di(10, 10, 10);
        final @NotNull Vector3Di newMax = new Vector3Di(20, 20, 20);
        final @NotNull Cuboid cuboid = new Cuboid(min, max);

        cuboid.updatePositions(newMax, max);
        Assert.assertEquals(max, cuboid.getMin());
        Assert.assertEquals(newMax, cuboid.getMax());
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

        Assert.assertTrue(cuboid.isPosInsideCuboid(pos0));
        Assert.assertTrue(cuboid.isPosInsideCuboid(pos1));
        Assert.assertFalse(cuboid.isPosInsideCuboid(pos2));
        Assert.assertFalse(cuboid.isPosInsideCuboid(pos3));
        Assert.assertFalse(cuboid.isPosInsideCuboid(pos4));
    }

    @Test
    void getMin()
    {
        final @NotNull Vector3Di val1 = new Vector3Di(10, -10, 10);
        final @NotNull Vector3Di val2 = new Vector3Di(11, 20, 9);

        final @NotNull Vector3Di actualMin = new Vector3Di(10, -10, 9);

        final @NotNull Cuboid cuboid = new Cuboid(val1, val2);
        Assert.assertEquals(actualMin, cuboid.getMin());

        cuboid.updatePositions(val2, val1);
        Assert.assertEquals(actualMin, cuboid.getMin());
    }

    @Test
    void getMax()
    {
        final @NotNull Vector3Di val1 = new Vector3Di(10, -10, 10);
        final @NotNull Vector3Di val2 = new Vector3Di(11, 20, 9);

        final @NotNull Vector3Di actualMax = new Vector3Di(11, 20, 10);

        final @NotNull Cuboid cuboid = new Cuboid(val1, val2);
        Assert.assertEquals(actualMax, cuboid.getMax());

        cuboid.updatePositions(val2, val1);
        Assert.assertEquals(actualMax, cuboid.getMax());
    }

    @Test
    void getCenterBlock()
    {
        final @NotNull Vector3Di val1 = new Vector3Di(0, 11, 30);
        final @NotNull Vector3Di val2 = new Vector3Di(10, 0, 30);

        final @NotNull Vector3Di center = new Vector3Di(5, 5, 30);

        final @NotNull Cuboid cuboid = new Cuboid(val1, val2);
        Assert.assertEquals(center, cuboid.getCenterBlock());
    }

    @Test
    void getDimensions()
    {
        final @NotNull Vector3Di val1 = new Vector3Di(0, 11, 30);
        final @NotNull Vector3Di val2 = new Vector3Di(10, 0, 30);

        final @NotNull Vector3Di dimensions = new Vector3Di(11, 12, 1);
        Assert.assertEquals(dimensions, new Cuboid(val1, val2).getDimensions());
    }

    @Test
    void getCenter()
    {
        final @NotNull Vector3Di val1 = new Vector3Di(0, 11, 30);
        final @NotNull Vector3Di val2 = new Vector3Di(10, 0, 30);

        final @NotNull Vector3Dd center = new Vector3Dd(5, 5.5d, 30);

        final @NotNull Vector3Dd foundCenter = new Cuboid(val1, val2).getCenter();
        Assert.assertTrue(Math.abs(center.getX() - foundCenter.getX()) < UnitTestUtil.EPSILON);
        Assert.assertTrue(Math.abs(center.getY() - foundCenter.getY()) < UnitTestUtil.EPSILON);
        Assert.assertTrue(Math.abs(center.getZ() - foundCenter.getZ()) < UnitTestUtil.EPSILON);
    }
}
