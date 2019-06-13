package nl.pim16aap2.bigdoors.util;

import static org.junit.Assert.fail;

import java.util.HashSet;

import org.junit.Test;

import nl.pim16aap2.bigdoors.doors.DoorType;

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
