package nl.pim16aap2.bigdoors.util.vector;

import junit.framework.Assert;
import org.junit.jupiter.api.Test;

class Vector3DiTest
{

    @Test
    void rotateAroundYAxis()
    {
        Vector3Di pivotPoint = new Vector3Di(126, 78, 223);
        Vector3Di startPoint = new Vector3Di(126, 75, 216);
        Vector3Di endPointClockwise = new Vector3Di(133, 75, 223);
        Vector3Di endPointCounterClockwise = new Vector3Di(119, 75, 223);

        final double radiansClockwise = Math.PI / 2;
        final double radiansCounterClockwise = -Math.PI / 2;

        Assert.assertEquals(endPointClockwise, startPoint.clone().rotateAroundYAxis(pivotPoint, radiansClockwise));
        Assert.assertEquals(endPointCounterClockwise,
                            startPoint.clone().rotateAroundYAxis(pivotPoint, radiansCounterClockwise));
    }
}
