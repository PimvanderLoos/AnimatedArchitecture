package nl.pim16aap2.bigdoors.movable.bigdoor;

import nl.pim16aap2.bigdoors.tooluser.creator.CreatorTestsUtil;
import nl.pim16aap2.bigdoors.util.MovementDirection;
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
        openDirection = MovementDirection.CLOCKWISE;

        final BigDoor actualMovable = new BigDoor(constructMovableBase());
        Assertions.assertNotNull(MovableBigDoor.get());
        final CreatorBigDoor creator = new CreatorBigDoor(context, player);
        testCreation(creator, actualMovable,
                     movableName,
                     min.toLocation(locationFactory, world),
                     max.toLocation(locationFactory, world),
                     rotationPoint.toLocation(locationFactory, world),
                     powerblock.toLocation(locationFactory, world),
                     false,
                     openDirection);
    }
}
