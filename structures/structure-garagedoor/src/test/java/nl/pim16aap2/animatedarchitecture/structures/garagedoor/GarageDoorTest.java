package nl.pim16aap2.animatedarchitecture.structures.garagedoor;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureBaseBuilder;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class GarageDoorTest
{
    private StructureBaseBuilder structureBaseBuilder;

    @BeforeEach
    public void beforeEach()
        throws Exception
    {
        structureBaseBuilder = UnitTestUtil.newStructureBaseBuilder().structureBaseBuilder();
    }

    @Test
    void getPotentialNewCoordinates()
    {
        for (final var openingData : GarageDoorTestUtil.OPENING_DATA_LIST)
        {
            verifyPotentialNewCoordinates(openingData);
            verifyPotentialNewCoordinates(openingData.getOpposite());
        }
    }

    /**
     * Verifies that the potential new coordinates are match the expected ones for the given data.
     * <p>
     * The reverse is also verified, to ensure that the coordinates are correct for the other direction.
     */
    private void verifyPotentialNewCoordinates(GarageDoorTestUtil.OpeningData openingData)
    {
        final GarageDoor garageDoor = openingData.createGarageDoor(structureBaseBuilder);

        final Optional<Cuboid> potentialNewCoordinates = garageDoor.getPotentialNewCoordinates();
        Assertions.assertTrue(
            potentialNewCoordinates.isPresent(),
            "Potential new coordinates should be present for " + openingData
        );

        final Cuboid expectedCuboid = openingData.endCuboid();
        final Cuboid actualCuboid = potentialNewCoordinates.orElseThrow();
        Assertions.assertEquals(
            expectedCuboid,
            actualCuboid,
            "Expected cuboid:\n  <" + expectedCuboid + ">\nbut got:\n  <" + actualCuboid + ">\nfor " + openingData
        );
    }
}
