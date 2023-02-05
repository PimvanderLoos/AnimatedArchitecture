package nl.pim16aap2.bigdoors.core.util.vector;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

class IVector3DTest
{
    @Test
    void magnitudeInt()
    {
        final Random random = new Random();
        final int[] arr = new int[3];
        int sqSum = 0;
        for (int idx = 0; idx < 3; ++idx)
        {
            final int val = random.nextInt(1000);
            arr[idx] = val;
            sqSum += (val * val);
        }

        final Vector3Di vec = new Vector3Di(arr[0], arr[1], arr[2]);
        Assertions.assertTrue(Math.abs(Math.sqrt(sqSum) - vec.magnitude()) < 0.0000001D);
    }

    @Test
    void magnitudeDouble()
    {
        final Random random = new Random();
        final double[] arr = new double[3];
        double sqSum = 0;
        for (int idx = 0; idx < 3; ++idx)
        {
            final double val = random.nextDouble(1000D);
            arr[idx] = val;
            sqSum += (val * val);
        }

        final Vector3Dd vec = new Vector3Dd(arr[0], arr[1], arr[2]);
        Assertions.assertTrue(Math.abs(Math.sqrt(sqSum) - vec.magnitude()) < 0.0000001D);
    }
}
