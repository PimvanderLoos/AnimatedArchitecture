package nl.pim16aap2.bigdoors.movable.elevator;

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

        final Elevator actualMovable = new Elevator(constructMovableBase(), blocksToMove);
        final CreatorElevator creator = new CreatorElevator(context, player);
        testCreation(creator, actualMovable,
                     movableName,
                     min.toLocation(locationFactory, world),
                     max.toLocation(locationFactory, world),
                     powerblock.toLocation(locationFactory, world),
                     false,
                     openDirection,
                     blocksToMove);
    }
}
