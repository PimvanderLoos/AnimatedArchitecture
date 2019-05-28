package nl.pim16aap2.bigdoors.util;

import static org.junit.Assert.fail;

import java.util.HashSet;

import org.junit.Test;

// TODO: Test that Creators and Openers of all enabled types can be properly retrieved (e.g. in BigDoors::getDoorOpener(DoorType type);
//       And that they are properly initialized.

public class DoorTypeTest
{
    HashSet<Integer> doorTypeValues = new HashSet<>();

    @Test
    public void test()
    {
        for (DoorType type : DoorType.values())
        {
            int value = DoorType.getValue(type);
            if (doorTypeValues.contains(value))
                fail("Duplicate doorValue on value: " + value);
            else
                doorTypeValues.add(value);
        }
    }

}
