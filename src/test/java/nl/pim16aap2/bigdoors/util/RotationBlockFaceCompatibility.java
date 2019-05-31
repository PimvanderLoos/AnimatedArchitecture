package nl.pim16aap2.bigdoors.util;

import static org.junit.Assert.fail;

import org.junit.Test;

public class RotationBlockFaceCompatibility
{
    @Test
    public void test()
    {
        for (MyBlockFace mbf : MyBlockFace.values())
        {
            try
            {
                RotateDirection.valueOf(mbf.toString());
            }
            catch (IllegalArgumentException e)
            {
                fail("Failed to get RotateDirection value of: \"" + mbf.toString() + "\"");
            }
        }
    }
}
