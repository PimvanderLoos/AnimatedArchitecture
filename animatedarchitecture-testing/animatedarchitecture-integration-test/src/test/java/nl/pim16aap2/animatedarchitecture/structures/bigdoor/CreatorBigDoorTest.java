package nl.pim16aap2.animatedarchitecture.structures.bigdoor;

import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.creator.CreatorTestsUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Objects;

@Timeout(1)
class CreatorBigDoorTest extends CreatorTestsUtil
{
    private final StructureType type = Objects.requireNonNull(StructureTypeBigDoor.get());

    @BeforeEach
    void setup()
    {
        super.beforeEach();
    }

    @Test
    void createBigDoor()
    {
        final Vector3Di rotationPoint = cuboid.getCenterBlock();
        final boolean openStatus = false;

        openDirection = MovementDirection.CLOCKWISE;

        final CreatorBigDoor creator = new CreatorBigDoor(context, player, null);
        final Structure actualStructure = constructStructure(
            type,
            getTemporaryUid(creator),
            Property.OPEN_STATUS, openStatus,
            Property.ROTATION_POINT, rotationPoint
        );

        testCreation(creator, actualStructure,
            structureName,
            min.toLocation(locationFactory, world),
            max.toLocation(locationFactory, world),
            rotationPoint.toLocation(locationFactory, world),
            powerblock.toLocation(locationFactory, world),
            openStatus,
            openDirection,
            true
        );
    }
}
