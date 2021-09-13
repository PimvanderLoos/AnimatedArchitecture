package nl.pim16aap2.bigdoors.doors.bigdoor;

import nl.pim16aap2.bigdoors.tooluser.creator.CreatorTestsUtil;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreatorBigDoorTest extends CreatorTestsUtil
{
    @BeforeEach
    void setup()
    {
        super.beforeEach();
    }

    @Test
    void createBigDoor()
    {
        openDirection = RotateDirection.CLOCKWISE;
        String openDirectionName = "0";

        final BigDoor actualDoor = new BigDoor(constructDoorBase());
        Assertions.assertNotNull(DoorTypeBigDoor.get());
        final CreatorBigDoor creator = new CreatorBigDoor(context, player);
        testCreation(creator, actualDoor,
                     doorName,
                     min.toLocation(locationFactory, world),
                     max.toLocation(locationFactory, world),
                     engine.toLocation(locationFactory, world),
                     powerblock.toLocation(locationFactory, world),
                     openDirectionName);
    }
}
