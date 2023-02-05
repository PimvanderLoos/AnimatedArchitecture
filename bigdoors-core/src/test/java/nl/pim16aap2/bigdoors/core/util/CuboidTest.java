package nl.pim16aap2.bigdoors.core.util;

import nl.pim16aap2.bigdoors.core.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class CuboidTest
{
    public final double EPSILON = 1E-6;

    @Test
    void getVolume()
    {
        final Vector3Di min = new Vector3Di(0, 0, 0);
        final Vector3Di max = new Vector3Di(10, -10, 10);
        final Cuboid cuboid = new Cuboid(min, max);

        Assertions.assertEquals(11 * 11 * 11, cuboid.getVolume());
    }

    @Test
    void moveTest()
    {
        final Vector3Di min0 = new Vector3Di(12, 45, 68);
        final Vector3Di max0 = new Vector3Di(22, 65, 88);

        final Vector3Di min1 = new Vector3Di(13, 49, 76);
        final Vector3Di max1 = new Vector3Di(23, 69, 96);

        Cuboid cuboid = new Cuboid(min0, max0);
        cuboid = cuboid.move(1, 4, 8);
        Assertions.assertEquals(min1, cuboid.getMin());
        Assertions.assertEquals(max1, cuboid.getMax());
    }

    @Test
    void changeDimensionsTest()
    {
        final Vector3Di min0 = new Vector3Di(12, 45, 68);
        final Vector3Di max0 = new Vector3Di(22, 65, 88);

        final Vector3Di min1 = new Vector3Di(11, 41, 60);
        final Vector3Di max1 = new Vector3Di(23, 69, 96);

        final Cuboid cuboid = new Cuboid(min0, max0).grow(1, 4, 8);
        Assertions.assertEquals(min1, cuboid.getMin());
        Assertions.assertEquals(max1, cuboid.getMax());
    }

    @Test
    void changeDimensionsInvTest()
    {
        final Vector3Di min0 = new Vector3Di(12, 45, 68);
        final Vector3Di max0 = new Vector3Di(22, 65, 70);

        final Vector3Di min1 = new Vector3Di(13, 49, 62);
        final Vector3Di max1 = new Vector3Di(21, 61, 76);

        final Cuboid cuboid = new Cuboid(min0, max0).grow(-1, -4, -8);
        Assertions.assertEquals(min1, cuboid.getMin());
        Assertions.assertEquals(max1, cuboid.getMax());
    }

    @Test
    void isPosInsideCuboid()
    {
        final Vector3Di min = new Vector3Di(0, 0, 0);
        final Vector3Di max = new Vector3Di(10, 10, 10);
        final Cuboid cuboid = new Cuboid(min, max);

        final Vector3Di pos0 = new Vector3Di(5, 5, 5);
        final Vector3Di pos1 = new Vector3Di(10, 0, 5);
        final Vector3Di pos2 = new Vector3Di(11, 0, 5);
        final Vector3Di pos3 = new Vector3Di(10, 11, 0);
        final Vector3Di pos4 = new Vector3Di(11, 11, 11);

        Assertions.assertTrue(cuboid.isPosInsideCuboid(pos0));
        Assertions.assertTrue(cuboid.isPosInsideCuboid(pos1));
        Assertions.assertFalse(cuboid.isPosInsideCuboid(pos2));
        Assertions.assertFalse(cuboid.isPosInsideCuboid(pos3));
        Assertions.assertFalse(cuboid.isPosInsideCuboid(pos4));
    }

    @Test
    void getMin()
    {
        final Vector3Di val1 = new Vector3Di(10, -10, 10);
        final Vector3Di val2 = new Vector3Di(11, 20, 9);

        final Vector3Di actualMin = new Vector3Di(10, -10, 9);

        Cuboid cuboid = new Cuboid(val1, val2);
        Assertions.assertEquals(actualMin, cuboid.getMin());
    }

    @Test
    void getMax()
    {
        final Vector3Di val1 = new Vector3Di(10, -10, 10);
        final Vector3Di val2 = new Vector3Di(11, 20, 9);

        final Vector3Di actualMax = new Vector3Di(11, 20, 10);

        Cuboid cuboid = new Cuboid(val1, val2);
        Assertions.assertEquals(actualMax, cuboid.getMax());
    }

    @Test
    void getCenterBlock()
    {
        final Vector3Di val1 = new Vector3Di(0, 11, 30);
        final Vector3Di val2 = new Vector3Di(10, -2, 30);

        final Vector3Di center = new Vector3Di(5, 4, 30);

        final Cuboid cuboid = new Cuboid(val1, val2);
        Assertions.assertEquals(center, cuboid.getCenterBlock());
    }

    @Test
    void getDimensions()
    {
        final Vector3Di val1 = new Vector3Di(0, 11, 30);
        final Vector3Di val2 = new Vector3Di(10, 0, 30);

        final Vector3Di dimensions = new Vector3Di(11, 12, 1);
        Assertions.assertEquals(dimensions, new Cuboid(val1, val2).getDimensions());
    }

    @Test
    void getCenter()
    {
        final Vector3Di val1 = new Vector3Di(1, 11, 30);
        final Vector3Di val2 = new Vector3Di(10, -2, 30);

        final Vector3Dd center = new Vector3Dd(5.5d, 4.5d, 30);

        final Vector3Dd foundCenter = new Cuboid(val1, val2).getCenter();
        Assertions.assertTrue(Math.abs(center.x() - foundCenter.x()) < EPSILON);
        Assertions.assertTrue(Math.abs(center.y() - foundCenter.y()) < EPSILON);
        Assertions.assertTrue(Math.abs(center.z() - foundCenter.z()) < EPSILON);
    }

    @Test
    void testIsInRange()
    {
        final Vector3Di val1 = new Vector3Di(-10, 0, 10);
        final Vector3Di val2 = new Vector3Di(20, 30, 40);
        final Cuboid cuboid = new Cuboid(val1, val2);

        //noinspection ResultOfMethodCallIgnored
        Assertions.assertThrows(IllegalArgumentException.class, () -> cuboid.isInRange(val1, -1));
        Assertions.assertTrue(cuboid.isInRange(cuboid.getCenterBlock(), 0));
        Assertions.assertTrue(cuboid.isInRange(cuboid.getCenterBlock(), 1));

        Assertions.assertTrue(cuboid.isInRange(new Vector3Di(-11, 31, 10), 1));
        Assertions.assertFalse(cuboid.isInRange(new Vector3Di(-11, 31, 10), 0));
        Assertions.assertTrue(cuboid.isInRange(new Vector3Di(-10, 0, 40), 1));
    }

    @Test
    void testFromDoubles()
    {
        final Vector3Dd val1 = new Vector3Dd(-4.1d, 16.5d, 21.9d);
        final Vector3Dd val2 = new Vector3Dd(4.1d, 16.5d, -21.1d);

        final Vector3Di nearMin = new Vector3Di(-4, 17, -21);
        final Vector3Di nearMax = new Vector3Di(4, 17, 22);
        final Cuboid near = Cuboid.of(val1, val2, Cuboid.RoundingMode.NEAREST);
        Assertions.assertEquals(nearMin, near.getMin());
        Assertions.assertEquals(nearMax, near.getMax());

        final Vector3Di inMin = new Vector3Di(-4, 16, -21);
        final Vector3Di inMax = new Vector3Di(4, 17, 21);
        final Cuboid in = Cuboid.of(val1, val2, Cuboid.RoundingMode.INWARD);
        Assertions.assertEquals(inMin, in.getMin());
        Assertions.assertEquals(inMax, in.getMax());

        final Vector3Di outMin = new Vector3Di(-5, 16, -22);
        final Vector3Di outMax = new Vector3Di(5, 17, 22);
        final Cuboid out = Cuboid.of(val1, val2, Cuboid.RoundingMode.OUTWARD);
        Assertions.assertEquals(outMin, out.getMin());
        Assertions.assertEquals(outMax, out.getMax());
    }

    @Test
    void testCorners()
    {
        final Vector3Di val1 = new Vector3Di(10, 99, 25);
        final Vector3Di val2 = new Vector3Di(44, 11, 30);
        final Cuboid cuboid = new Cuboid(val1, val2);

        Assertions.assertEquals(
            Set.of(cuboid.getCorners()),
            Set.of(
                new Vector3Di(10, 11, 25), new Vector3Di(10, 11, 30),
                new Vector3Di(44, 11, 25), new Vector3Di(44, 11, 30),
                new Vector3Di(10, 99, 25), new Vector3Di(10, 99, 30),
                new Vector3Di(44, 99, 25), new Vector3Di(44, 99, 30)));
    }

    @Test
    void testAsFlatRectangle()
    {
        final Vector3Di val1 = new Vector3Di(10, 99, 25);
        final Vector3Di val2 = new Vector3Di(44, 11, 30);
        final Cuboid cuboid = new Cuboid(val1, val2);

        Assertions.assertEquals(new Rectangle(new Vector2Di(10, 25), new Vector2Di(44, 30)),
                                cuboid.asFlatRectangle());
    }
}
