package nl.pim16aap2.bigdoors.tooluser.creator;

import nl.pim16aap2.bigdoors.core.UnitTestUtil;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.core.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.core.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.core.tooluser.creator.CreatorTest;
import nl.pim16aap2.bigdoors.core.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * This class tests the general creation flow of the creator process.
 * <p>
 * The specific methods are test in {@link CreatorTest}.
 */
@Timeout(1)
class CreatorFullTest extends CreatorTestsUtil
{
    private static StructureType structureType;

    @Test
    void runThroughProcess()
    {
        rotationPoint = new Cuboid(min, max).getCenterBlock();
        openDirection = MovementDirection.NORTH;

        structureType = Mockito.mock(StructureType.class);
        Mockito.when(structureType.getValidOpenDirections())
               .thenReturn(EnumSet.of(MovementDirection.NORTH, MovementDirection.SOUTH));

        final var structure = Mockito.mock(AbstractStructure.class);
        Mockito.when(structure.getType()).thenReturn(structureType);

        final var creator = new CreatorTestImpl(context, player, structure);

        setEconomyEnabled(true);
        setEconomyPrice(12.34);
        setBuyStructure(true);

        testCreation(creator, structure,
                     structureName,
                     UnitTestUtil.getLocation(min, world),
                     UnitTestUtil.getLocation(max, world),
                     UnitTestUtil.getLocation(rotationPoint, world),
                     UnitTestUtil.getLocation(powerblock, world),
                     false,
                     openDirection,
                     true);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void delayedOpenDirectionInput()
    {
        rotationPoint = new Cuboid(min, max).getCenterBlock();
        openDirection = MovementDirection.NORTH;

        structureType = Mockito.mock(StructureType.class);
        Mockito.when(structureType.getValidOpenDirections())
               .thenReturn(EnumSet.of(MovementDirection.NORTH, MovementDirection.SOUTH));

        final var structure = Mockito.mock(AbstractStructure.class);
        Mockito.when(structure.getType()).thenReturn(structureType);

        final var creator = new CreatorTestImpl(context, player, structure);

        setEconomyEnabled(true);
        setEconomyPrice(12.34);
        setBuyStructure(true);

        applySteps(creator,
                   structureName,
                   UnitTestUtil.getLocation(min, world),
                   UnitTestUtil.getLocation(max, world),
                   UnitTestUtil.getLocation(rotationPoint, world),
                   UnitTestUtil.getLocation(powerblock, world));

        Assertions.assertDoesNotThrow(() -> delayedCommandInputManager.getInputRequest(player).get()
                                                                      .provide(false).join());
        Assertions.assertDoesNotThrow(() -> delayedCommandInputManager.getInputRequest(player).get()
                                                                      .provide(MovementDirection.EAST).join());
        Assertions.assertDoesNotThrow(() -> delayedCommandInputManager.getInputRequest(player).get()
                                                                      .provide(openDirection).join());

        testCreation(creator, structure, true);
    }

    private static class CreatorTestImpl extends Creator
    {
        private final AbstractStructure structure;

        protected CreatorTestImpl(ToolUser.Context context, IPlayer player, AbstractStructure structure)
        {
            super(context, player, null);
            this.structure = structure;
        }

        @Override
        protected List<IStep> generateSteps()
            throws InstantiationException
        {
            return Arrays.asList(factorySetName.messageKey("CREATOR_BASE_GIVE_NAME").construct(),
                                 factorySetFirstPos.messageKey("CREATOR_BIG_DOOR_STEP1").construct(),
                                 factorySetSecondPos.messageKey("CREATOR_BIG_DOOR_STEP2").construct(),
                                 factorySetRotationPointPos.messageKey("CREATOR_BIG_DOOR_STEP3").construct(),
                                 factorySetPowerBlockPos.messageKey("CREATOR_BASE_SET_POWER_BLOCK").construct(),
                                 factorySetOpenStatus.messageKey("CREATOR_BASE_SET_OPEN_DIR").construct(),
                                 factorySetOpenDir.messageKey("CREATOR_BASE_SET_OPEN_DIR").construct(),
                                 factoryConfirmPrice.messageKey("CREATOR_BASE_CONFIRM_PRICE").construct(),
                                 factoryCompleteProcess.messageKey("CREATOR_BIG_DOOR_SUCCESS").construct());
        }

        @Override
        protected void giveTool()
        {
        }

        @Override
        protected AbstractStructure constructStructure()
        {
            return structure;
        }

        @Override
        protected StructureType getStructureType()
        {
            return structureType;
        }
    }
}
