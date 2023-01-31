package nl.pim16aap2.bigdoors.movable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.movable.serialization.DeserializationConstructor;
import nl.pim16aap2.bigdoors.movable.serialization.PersistentVariable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.moveblocks.MovementRequestData;
import nl.pim16aap2.bigdoors.testimplementations.TestPWorld;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.Rectangle;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

class MovableSerializerTest
{
    private AbstractMovable.MovableBaseHolder movableBase;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        final AssistedFactoryMocker<MovableBase, MovableBase.IFactory> assistedFactoryMocker =
            new AssistedFactoryMocker<>(MovableBase.class, MovableBase.IFactory.class);

        final MovableRegistry movableRegistry = assistedFactoryMocker.getMock(MovableRegistry.class);
        Mockito.when(movableRegistry.computeIfAbsent(Mockito.anyLong(), Mockito.any()))
               .thenAnswer(invocation -> invocation.getArgument(1, Supplier.class).get());

        final MovableBaseBuilder factory = new MovableBaseBuilder(assistedFactoryMocker.getFactory());


        final String movableName = "randomDoorName";
        final Vector3Di zeroPos = new Vector3Di(0, 0, 0);
        final PPlayerData playerData = new PPlayerData(UUID.randomUUID(), "player", -1, -1, true, true);
        final MovableOwner movableOwner = new MovableOwner(1, PermissionLevel.CREATOR, playerData);

        movableBase = factory.builder().uid(1).name(movableName).cuboid(new Cuboid(zeroPos, zeroPos))
                             .rotationPoint(zeroPos).powerBlock(zeroPos).world(new TestPWorld("worldName"))
                             .isOpen(false).isLocked(false).openDir(MovementDirection.DOWN).primeOwner(movableOwner)
                             .build();
    }

    @Test
    void instantiate()
    {
        final var instantiator = Assertions.assertDoesNotThrow(() -> new MovableSerializer<>(TestMovableType.class));
        final TestMovableType base = new TestMovableType(movableBase, "test", true, 42);

        TestMovableType test = Assertions.assertDoesNotThrow(
            () -> instantiator.instantiate(movableBase,
                                           Map.of("testName", "test",
                                                  "isCoolType", true,
                                                  "blockTestCount", 42)));
        Assertions.assertEquals(base, test);

        test = Assertions.assertDoesNotThrow(
            () -> instantiator.instantiate(movableBase,
                                           Map.of("testName", "alternativeName",
                                                  "isCoolType", true,
                                                  "blockTestCount", 42)));
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
        final TestMovableSubType testMovableSubType1 = new TestMovableSubType(movableBase, "testSubClass", 6, true, 42);

        final byte[] serialized = Assertions.assertDoesNotThrow(() -> instantiator.serialize(testMovableSubType1));
        final var testMovableSubType2 = Assertions.assertDoesNotThrow(
            () -> instantiator.deserialize(movableBase, serialized));

        Assertions.assertEquals(testMovableSubType1, testMovableSubType2);
    }

    @Test
    void testAmbiguity()
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new MovableSerializer<>(TestMovableSubTypeAmbiguous.class));
    }

    // This class is a nullability nightmare, but that doesn't matter, because none of the methods are used;
    // It's only used for testing serialization and the methods are therefore just stubs.
    @SuppressWarnings("ConstantConditions")
    // Don't call super for equals etc., as we don't care about the equality
    // of the parameters that aren't serialized anyway.
    @EqualsAndHashCode(callSuper = false)
    private static class TestMovableType extends AbstractMovable
    {
        @EqualsAndHashCode.Exclude
        @SuppressWarnings({"unused", "FieldCanBeLocal"})
        private final ReentrantReadWriteLock lock;

        @Getter
        @PersistentVariable
        protected String testName;

        @Getter
        @PersistentVariable
        protected boolean isCoolType;

        @Getter
        @PersistentVariable("blockTestCount")
        private int blockTestCount;

        private static final MovableType MOVABLE_TYPE = Mockito.mock(MovableType.class);

        public TestMovableType(AbstractMovable.MovableBaseHolder movableBase)
        {
            super(movableBase);
            this.lock = super.getLock();
        }

        @DeserializationConstructor
        public TestMovableType(
            AbstractMovable.MovableBaseHolder base,
            String testName,
            boolean isCoolType,
            int blockTestCount)
        {
            this(base);
            this.testName = testName;
            this.isCoolType = isCoolType;
            this.blockTestCount = blockTestCount;
        }

        @Override
        public MovableType getType()
        {
            return MOVABLE_TYPE;
        }

        @Override
        protected double calculateAnimationCycleDistance()
        {
            return 0;
        }

        @Override
        protected Rectangle calculateAnimationRange()
        {
            return new Rectangle(new Vector2Di(0, 0), new Vector2Di(0, 0));
        }

        @Override
        public boolean canSkipAnimation()
        {
            return false;
        }

        @Override
        public MovementDirection getCurrentToggleDir()
        {
            return null;
        }

        @Override
        public Optional<Cuboid> getPotentialNewCoordinates()
        {
            return Optional.empty();
        }

        @Override
        public MovementDirection cycleOpenDirection()
        {
            return null;
        }

        @Override
        protected IAnimationComponent constructAnimationComponent(MovementRequestData data)
        {
            return null;
        }
    }

    @EqualsAndHashCode(callSuper = true)
    private static class TestMovableSubType extends TestMovableType
    {
        @EqualsAndHashCode.Exclude
        @SuppressWarnings({"unused", "FieldCanBeLocal"})
        private final ReentrantReadWriteLock lock;

        @Getter
        @PersistentVariable("subclassTestValue")
        private final int subclassTestValue;

        @DeserializationConstructor
        public TestMovableSubType(
            AbstractMovable.MovableBaseHolder base,
            String testName,
            @PersistentVariable("subclassTestValue") int subclassTestValue,
            boolean isCoolType,
            @PersistentVariable("blockTestCount") int blockTestCount)
        {
            super(base, testName, isCoolType, blockTestCount);
            this.lock = super.getLock();

            this.testName = testName;
            this.isCoolType = isCoolType;
            this.subclassTestValue = subclassTestValue;
        }

        @SuppressWarnings("unused")
        public TestMovableSubType(AbstractMovable.MovableBaseHolder base)
        {
            this(base, "", -1, false, -1);
        }
    }

    @EqualsAndHashCode(callSuper = true)
    private static class TestMovableSubTypeAmbiguous extends TestMovableType
    {
        @DeserializationConstructor
        public TestMovableSubTypeAmbiguous(
            AbstractMovable.MovableBaseHolder base,
            String testName,
            int ambiguousInteger0,
            boolean isCoolType,
            int ambiguousInteger1)
        {
            super(base, testName, isCoolType, ambiguousInteger0);
            this.testName = testName;
            this.isCoolType = isCoolType;
        }
    }
}
