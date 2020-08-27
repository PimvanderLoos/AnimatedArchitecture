package nl.pim16aap2.bigdoors.tooluser.creator;

import nl.pim16aap2.bigdoors.doors.Elevator;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class CreatorElevatorTest extends CreatorTestsUtil
{
    private static final int blocksToMove = 17;

    @Test
    public void createElevator()
        throws InterruptedException
    {
        engine = new Cuboid(min, max).getCenterBlock();
        openDirection = RotateDirection.UP;
        String openDirectionName = "0";

        final @NotNull Elevator actualDoor = new Elevator(constructDoorData(), blocksToMove);
        final @NotNull CreatorElevator creator = new CreatorElevator(PLAYER);
        testCreation(creator, actualDoor,
                     doorName,
                     min.toLocation(world),
                     max.toLocation(world),
                     powerblock.toLocation(world),
                     openDirectionName,
                     blocksToMove);
    }
}
