package nl.pim16aap2.animatedarchitecture.structures.bigdoor;

import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.creator.CreatorTestsUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(1)
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

        Assertions.assertNotNull(StructureTypeBigDoor.get());

        final CreatorBigDoor creator = new CreatorBigDoor(context, player, null);
        final BigDoor actualStructure = new BigDoor(constructStructureBase(getTemporaryUid(creator)));
        testCreation(creator, actualStructure,
            structureName,
            min.toLocation(locationFactory, world),
            max.toLocation(locationFactory, world),
            rotationPoint.toLocation(locationFactory, world),
            powerblock.toLocation(locationFactory, world),
            false,
            openDirection,
            true
        );
    }
}
