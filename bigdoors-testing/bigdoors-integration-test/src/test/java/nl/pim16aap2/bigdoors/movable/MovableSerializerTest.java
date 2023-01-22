package nl.pim16aap2.bigdoors.movable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.managers.MovableRegistry;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.moveblocks.MovementRequestData;
import nl.pim16aap2.bigdoors.testimplementations.TestPWorld;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.Rectangle;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

class MovableSerializerTest
{
    private MovableBase movableBase;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        final AssistedFactoryMocker<MovableBase, MovableBase.IFactory> assistedFactoryMocker =
            new AssistedFactoryMocker<>(MovableBase.class, MovableBase.IFactory.class);

        final MovableRegistry movableRegistry = assistedFactoryMocker.getMock(MovableRegistry.class);
        Mockito.when(movableRegistry.registerMovable(Mockito.any())).thenReturn(true);

        final MovableBaseBuilder factory = new MovableBaseBuilder(assistedFactoryMocker.getFactory());


        final String movableName = "randomDoorName";
        final Vector3Di zeroPos = new Vector3Di(0, 0, 0);
        final PPlayerData playerData = new PPlayerData(UUID.randomUUID(), "player", -1, -1, true, true);
        final MovableOwner movableOwner = new MovableOwner(1, PermissionLevel.CREATOR, playerData);

        movableBase = factory.builder().uid(1).name(movableName).cuboid(new Cuboid(zeroPos, zeroPos))
                             .rotationPoint(zeroPos).powerBlock(zeroPos).world(new TestPWorld("worldName"))
                             .isOpen(false).isLocked(false).openDir(RotateDirection.DOWN).primeOwner(movableOwner)
                             .build();
    }

    @Test
    void instantiate()
    {
        final var instantiator = Assertions.assertDoesNotThrow(() -> new MovableSerializer<>(TestMovableType.class));
        final TestMovableType base = new TestMovableType(movableBase, "test", true, 42);

        TestMovableType test = Assertions.assertDoesNotThrow(
            () -> instantiator.instantiate(movableBase, new ArrayList<>(Arrays.asList("test", true, 42))));
        Assertions.assertEquals(base, test);

        test = Assertions.assertDoesNotThrow(
            () -> instantiator.instantiate(movableBase, new ArrayList<>(Arrays.asList("alternativeName", true, 42))));
        Assertions.assertEquals("alternativeName", test.getTestName());
    }

    @Test
    void instantiateUnsafe()
    {
        final var instantiator = Assertions.assertDoesNotThrow(
            () -> new MovableSerializer<>(TestMovableSubType.class));
        final TestMovableSubType base = new TestMovableSubType(movableBase, "test", true, 42, 1);

        TestMovableType test = Assertions.assertDoesNotThrow(
            () -> instantiator.instantiate(movableBase, new ArrayList<>(Arrays.asList("test", true, 42, 1))));
        Assertions.assertEquals(base, test);

        test = Assertions.assertDoesNotThrow(
            () -> instantiator.instantiate(movableBase,
                                           new ArrayList<>(Arrays.asList("alternativeName", true, 42, 1))));
        Assertions.assertEquals("alternativeName", test.getTestName());
    }

    @Test
    void serialize()
    {
        final MovableSerializer<TestMovableType> instantiator =
            Assertions.assertDoesNotThrow(() -> new MovableSerializer<>(TestMovableType.class));
        final TestMovableType testMovableType = new TestMovableType(movableBase, "test", true, 42);

        final byte[] serialized = Assertions.assertDoesNotThrow(() -> instantiator.serialize(testMovableType));
        Assertions.assertEquals(testMovableType,
                                Assertions.assertDoesNotThrow(() -> instantiator.deserialize(movableBase, serialized)));
    }

    @Test
    void subclass()
    {
        final var instantiator = Assertions.assertDoesNotThrow(
            () -> new MovableSerializer<>(TestMovableSubType.class));
        final TestMovableSubType testMovableSubType1 = new TestMovableSubType(movableBase, "test", true, 42, 6);

        final byte[] serialized = Assertions.assertDoesNotThrow(() -> instantiator.serialize(testMovableSubType1));
        final var testMovableSubType2 = Assertions.assertDoesNotThrow(
            () -> instantiator.deserialize(movableBase, serialized));

        Assertions.assertEquals(testMovableSubType1, testMovableSubType2);
    }

    // This class is a nullability nightmare, but that doesn't matter, because none of the methods are used;
    // It's only used for testing serialization and the methods are therefore just stubs.
    @SuppressWarnings("ConstantConditions")
    // Don't call super for equals etc., as we don't care about the equality
    // of the parameters that aren't serialized anyway.
    @EqualsAndHashCode(callSuper = false)
    private static class TestMovableType extends AbstractMovable
    {
        @PersistentVariable
        @Getter
        protected String testName;

        @PersistentVariable
        @Getter
        protected boolean isCoolType;

        @PersistentVariable
        @Getter
        private int blockTestCount;

        private static final MovableType MOVABLE_TYPE = Mockito.mock(MovableType.class);

        @SuppressWarnings("unused")
        public TestMovableType(MovableBase movableBase)
        {
            super(movableBase);
        }

        public TestMovableType(MovableBase movableBase, String testName, boolean isCoolType, int blockTestCount)
        {
            super(movableBase);
            this.testName = testName;
            this.isCoolType = isCoolType;
            this.blockTestCount = blockTestCount;
        }

        @Override
        protected double getLongestAnimationCycleDistance()
        {
            return 0;
        }

        @Override
        public MovableType getType()
        {
            return MOVABLE_TYPE;
        }

        @Override
        public boolean canSkipAnimation()
        {
            return false;
        }

        @Override
        public Rectangle getAnimationRange()
        {
            return null;
        }

        @Override
        public RotateDirection getCurrentToggleDir()
        {
            return null;
        }

        @Override
        public Optional<Cuboid> getPotentialNewCoordinates()
        {
            return Optional.empty();
        }

        @Override
        public RotateDirection cycleOpenDirection()
        {
            return null;
        }

        @Override
        protected BlockMover constructBlockMover(MovementRequestData data)
        {
            return null;
        }
    }

    @EqualsAndHashCode(callSuper = true)
    private static class TestMovableSubType extends TestMovableType
    {
        @PersistentVariable
        @Getter
        private final int subclassTestValue;

        public TestMovableSubType(
            MovableBase movableBase, String testName, boolean isCoolType, int blockTestCount, int subclassTestValue)
        {
            super(movableBase, testName, isCoolType, blockTestCount);

            this.testName = testName;
            this.isCoolType = isCoolType;

            this.subclassTestValue = subclassTestValue;
        }
    }
}
