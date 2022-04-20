package nl.pim16aap2.bigdoors.doors.elevator;

import nl.pim16aap2.bigdoors.tooluser.creator.CreatorTestsUtil;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreatorElevatorTest extends CreatorTestsUtil
{
    private static final int blocksToMove = 17;

    @BeforeEach
    void setup()
    {
        super.beforeEach();
    }

    @Test
    void createElevator()
    {
        rotationPoint = new Cuboid(min, max).getCenterBlock();
        openDirection = RotateDirection.UP;
        String openDirectionName = "0";

        final Elevator actualDoor = new Elevator(constructDoorBase(), blocksToMove);
        final CreatorElevator creator = new CreatorElevator(context, player);
        testCreation(creator, actualDoor,
                     doorName,
                     min.toLocation(locationFactory, world),
                     max.toLocation(locationFactory, world),
                     powerblock.toLocation(locationFactory, world),
                     openDirectionName,
                     blocksToMove);
    }
}
