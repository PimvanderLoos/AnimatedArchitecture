package nl.pim16aap2.bigdoors.structures.bigdoor;

import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorTestsUtil;
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

        final BigDoor actualStructure = new BigDoor(constructStructureBase());
        Assertions.assertNotNull(StructureTypeBigDoor.get());
        final CreatorBigDoor creator = new CreatorBigDoor(context, player, null);
        testCreation(creator, actualStructure,
                     structureName,
                     min.toLocation(locationFactory, world),
                     max.toLocation(locationFactory, world),
                     rotationPoint.toLocation(locationFactory, world),
                     powerblock.toLocation(locationFactory, world),
                     false,
                     openDirection);
    }
}