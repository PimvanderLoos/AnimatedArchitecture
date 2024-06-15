package nl.pim16aap2.animatedarchitecture.structures.bigdoor;

import nl.altindag.log.LogCaptor;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.creator.CreatorTestsUtil;
import nl.pim16aap2.testing.logging.WithLogCapture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@WithLogCapture
@Timeout(1)
class CreatorBigDoorTest extends CreatorTestsUtil
{
    @BeforeEach
    void setup()
    {
        super.beforeEach();
    }

    @Test
    void createBigDoor(LogCaptor logCaptor)
    {
        logCaptor.setLogLevelToInfo();

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
