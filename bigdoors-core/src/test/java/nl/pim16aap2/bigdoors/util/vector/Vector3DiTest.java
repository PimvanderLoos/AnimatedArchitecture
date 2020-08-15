package nl.pim16aap2.bigdoors.util.vector;

import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class Vector3DiTest
{

    @Test
    void rotateAroundXAxis()
    {
        final @NotNull Vector3Di pivotPoint = new Vector3Di(-5, 76, 203);
        final @NotNull Vector3Di startPoint = new Vector3Di(-9, 83, 203);
        final @NotNull Vector3Di endPointPos = new Vector3Di(-9, 76, 210);
        final @NotNull Vector3Di endPointNeg = new Vector3Di(-9, 76, 196);

        final double radiansPos = Math.PI / 2;
        final double radiansNeg = -Math.PI / 2;

        Assert.assertEquals(endPointPos, startPoint.clone().rotateAroundXAxis(pivotPoint, radiansPos));
        Assert.assertEquals(endPointNeg, startPoint.clone().rotateAroundXAxis(pivotPoint, radiansNeg));
    }

    @Test
    void rotateAroundYAxis()
    {
        final @NotNull Vector3Di pivotPoint = new Vector3Di(126, 78, 223);
        final @NotNull Vector3Di startPoint = new Vector3Di(126, 75, 216);
        final @NotNull Vector3Di endPointPos = new Vector3Di(133, 75, 223);
        final @NotNull Vector3Di endPointNeg = new Vector3Di(119, 75, 223);

        final double radiansPos = Math.PI / 2;
        final double radiansNeg = -Math.PI / 2;

        Assert.assertEquals(endPointPos, startPoint.clone().rotateAroundYAxis(pivotPoint, radiansPos));
        Assert.assertEquals(endPointNeg, startPoint.clone().rotateAroundYAxis(pivotPoint, radiansNeg));
    }

    @Test
    void rotateAroundZAxis()
    {
        final @NotNull Vector3Di pivotPoint = new Vector3Di(85, 79, 219);
        final @NotNull Vector3Di startPoint = new Vector3Di(79, 79, 221);
        final @NotNull Vector3Di endPointPos = new Vector3Di(85, 85, 221);
        final @NotNull Vector3Di endPointNeg = new Vector3Di(85, 73, 221);

        final double radiansPos = Math.PI / 2;
        final double radiansNeg = -Math.PI / 2;

        Assert.assertEquals(endPointPos, startPoint.clone().rotateAroundZAxis(pivotPoint, radiansPos));
        Assert.assertEquals(endPointNeg, startPoint.clone().rotateAroundZAxis(pivotPoint, radiansNeg));
    }
}
