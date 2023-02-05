package nl.pim16aap2.bigdoors.core.util.vector;

import nl.pim16aap2.bigdoors.core.util.MathUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class Vector3DiTest
{

    @Test
    void rotateAroundXAxis()
    {
        final Vector3Di pivotPoint = new Vector3Di(-5, 76, 203);
        final Vector3Di startPoint = new Vector3Di(-9, 83, 203);
        final Vector3Di endPointPos = new Vector3Di(-9, 76, 210);
        final Vector3Di endPointNeg = new Vector3Di(-9, 76, 196);

        final double radiansPos = MathUtil.HALF_PI;
        final double radiansNeg = -MathUtil.HALF_PI;

        Assertions.assertEquals(endPointPos, startPoint.rotateAroundXAxis(pivotPoint, radiansPos));
        Assertions.assertEquals(endPointNeg, startPoint.rotateAroundXAxis(pivotPoint, radiansNeg));
    }

    @Test
    void rotateAroundYAxis()
    {
        final Vector3Di pivotPoint = new Vector3Di(126, 78, 223);
        final Vector3Di startPoint = new Vector3Di(126, 75, 216);
        final Vector3Di endPointPos = new Vector3Di(133, 75, 223);
        final Vector3Di endPointNeg = new Vector3Di(119, 75, 223);

        final double radiansPos = MathUtil.HALF_PI;
        final double radiansNeg = -MathUtil.HALF_PI;

        Assertions.assertEquals(endPointPos, startPoint.rotateAroundYAxis(pivotPoint, radiansPos));
        Assertions.assertEquals(endPointNeg, startPoint.rotateAroundYAxis(pivotPoint, radiansNeg));
    }

    @Test
    void rotateAroundZAxis()
    {
        final Vector3Di pivotPoint = new Vector3Di(85, 79, 219);
        final Vector3Di startPoint = new Vector3Di(79, 79, 221);
        final Vector3Di endPointPos = new Vector3Di(85, 85, 221);
        final Vector3Di endPointNeg = new Vector3Di(85, 73, 221);

        final double radiansPos = MathUtil.HALF_PI;
        final double radiansNeg = -MathUtil.HALF_PI;

        Assertions.assertEquals(endPointPos, startPoint.rotateAroundZAxis(pivotPoint, radiansPos));
        Assertions.assertEquals(endPointNeg, startPoint.rotateAroundZAxis(pivotPoint, radiansNeg));
    }
}
