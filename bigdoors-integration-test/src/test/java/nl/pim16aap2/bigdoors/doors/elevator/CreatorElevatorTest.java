package nl.pim16aap2.bigdoors.doors.elevator;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorTestsUtil;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreatorElevatorTest extends CreatorTestsUtil
{
    private static final int blocksToMove = 17;

    @BeforeEach
    public void setup()
    {
        super.beforeEach();
    }

    @Test
    public void createElevator()
    {
        engine = new Cuboid(min, max).getCenterBlock();
        openDirection = RotateDirection.UP;
        String openDirectionName = "0";

        final @NonNull Elevator actualDoor = new Elevator(constructDoorData(), blocksToMove);
        final @NonNull CreatorElevator creator = new CreatorElevator(PLAYER);
        testCreation(creator, actualDoor,
                     doorName,
                     min.toLocation(world),
                     max.toLocation(world),
                     powerblock.toLocation(world),
                     openDirectionName,
                     blocksToMove);
    }
}
