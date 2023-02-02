package nl.pim16aap2.bigdoors.movable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.movable.serialization.Deserialization;
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
import org.jetbrains.annotations.Nullable;
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
        final var instantiator =
            Assertions.assertDoesNotThrow(() -> new MovableSerializer<>(TestMovableType.class));
        final TestMovableType base = new TestMovableType(movableBase, "test", true, 42);

        TestMovableType test = Assertions.assertDoesNotThrow(
            () -> instantiator.instantiate(movableBase, 1,
                                           Map.of("testName", "test",
                                                  "isCoolType", true,
                                                  "blockTestCount", 42)));
        Assertions.assertEquals(base, test);

        test = Assertions.assertDoesNotThrow(
            () -> instantiator.instantiate(movableBase, 1,
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

        final String serialized = Assertions.assertDoesNotThrow(() -> instantiator.serialize(testMovableType));
        Assertions.assertEquals(
            testMovableType,
            Assertions.assertDoesNotThrow(() -> instantiator.deserialize(movableBase, 1, serialized)));
    }

    @Test
    void subclass()
    {
        final var instantiator = Assertions.assertDoesNotThrow(
            () -> new MovableSerializer<>(TestMovableSubType.class));
        final TestMovableSubType testMovableSubType1 = new TestMovableSubType(movableBase, "testSubClass", 6, true, 42);

        final String serialized = Assertions.assertDoesNotThrow(() -> instantiator.serialize(testMovableSubType1));
        final var testMovableSubType2 = Assertions.assertDoesNotThrow(
            () -> instantiator.deserialize(movableBase, 1, serialized));

        Assertions.assertEquals(testMovableSubType1, testMovableSubType2);
    }

    @Test
    void testAmbiguityParams()
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new MovableSerializer<>(TestMovableSubTypeAmbiguousParameterTypes.class));
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new MovableSerializer<>(TestMovableSubTypeAmbiguousParameterNames.class));
    }

    @Test
    void testMissingData()
        throws Exception
    {
        final var instantiator = Assertions.assertDoesNotThrow(
            () -> new MovableSerializer<>(TestMovableType.class));
        final TestMovableType testMovableType0 = new TestMovableType(movableBase, null, true, 42);

        final TestMovableType testMovableType1 =
            instantiator.instantiate(movableBase, 1,
                                     Map.of("isCoolType", true,
                                            "blockTestCount", 42));

        Assertions.assertEquals(testMovableType0, testMovableType1);
    }

    @Test
    void testInvalidMissingData()
    {
        final var instantiator = Assertions.assertDoesNotThrow(
            () -> new MovableSerializer<>(TestMovableType.class));

        // The mapping for 'int blockTestCount' is missing, in which case we throw an exception.
        // Only objects can be missing.
        Assertions.assertThrows(
            Exception.class,
            () -> instantiator.instantiate(movableBase, 1,
                                           Map.of("testName", "testName",
                                                  "isCoolType", false)));
    }

    @Test
    void testAmbiguousClass()
    {
        Assertions.assertThrows(Exception.class,
                                () -> new MovableSerializer<>(TestMovableSubTypeAmbiguousFieldTypes.class));
        Assertions.assertThrows(Exception.class,
                                () -> new MovableSerializer<>(TestMovableSubTypeAmbiguousFieldNames.class));
    }

    @Test
    void testVersioning()
    {
        final var instantiator = Assertions.assertDoesNotThrow(
            () -> new MovableSerializer<>(TestMovableSubTypeConstructorVersions.class));

        Assertions.assertEquals(-1, instantiator.deserialize(movableBase, 999, "{}").version);
        Assertions.assertEquals(1, instantiator.deserialize(movableBase, 1, "{}").version);
        Assertions.assertEquals(2, instantiator.deserialize(movableBase, 2, "{}").version);
    }

    @Test
    void testVersioningWithoutFallback()
    {
        final var instantiator = Assertions.assertDoesNotThrow(
            () -> new MovableSerializer<>(TestMovableSubTypeConstructorVersionsNoFallback.class));

        Assertions.assertThrows(RuntimeException.class, () -> instantiator.deserialize(movableBase, 999, "{}"));
        Assertions.assertEquals(1, instantiator.deserialize(movableBase, 1, "{}").version);
        Assertions.assertEquals(2, instantiator.deserialize(movableBase, 2, "{}").version);
    }

    @Test
    void testAmbiguousVersions()
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new MovableSerializer<>(TestMovableSubTypeAmbiguousConstructorVersions.class));
    }

    // This class is a nullability nightmare, but that doesn't matter, because none of the methods are used;
    // It's only used for testing serialization and the methods are therefore just stubs.
    @SuppressWarnings("ConstantConditions")
    // Don't call super for equals etc., as we don't care about the equality
    // of the parameters that aren't serialized anyway.
    @EqualsAndHashCode(callSuper = false)
    private static class TestMovableType extends AbstractMovable
    {
        private static final MovableType MOVABLE_TYPE = Mockito.mock(MovableType.class);

        @EqualsAndHashCode.Exclude
        @SuppressWarnings({"unused", "FieldCanBeLocal"})
        private final ReentrantReadWriteLock lock;

        @Getter
        @PersistentVariable
        protected @Nullable String testName;

        @Getter
        @PersistentVariable
        protected boolean isCoolType;

        @Getter
        @PersistentVariable("blockTestCount")
        private int blockTestCount;

        public TestMovableType(AbstractMovable.MovableBaseHolder movableBase)
        {
            super(movableBase, MOVABLE_TYPE);
            this.lock = super.getLock();
        }

        @Deserialization
        public TestMovableType(
            AbstractMovable.MovableBaseHolder base,
            @Nullable String testName,
            boolean isCoolType,
            int blockTestCount)
        {
            this(base);
            this.testName = testName;
            this.isCoolType = isCoolType;
            this.blockTestCount = blockTestCount;
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

        @Deserialization
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
    private static class TestMovableSubTypeAmbiguousParameterTypes extends TestMovableType
    {
        @PersistentVariable("ambiguousInteger1")
        private final int ambiguousInteger1;

        @Deserialization
        public TestMovableSubTypeAmbiguousParameterTypes(
            AbstractMovable.MovableBaseHolder base,
            String testName,
            int ambiguousInteger0,
            boolean isCoolType,
            int ambiguousInteger1)
        {
            super(base, testName, isCoolType, ambiguousInteger0);
            this.ambiguousInteger1 = ambiguousInteger1;
            this.testName = testName;
            this.isCoolType = isCoolType;
        }
    }

    @SuppressWarnings("unused")
    @EqualsAndHashCode(callSuper = true)
    private static class TestMovableSubTypeAmbiguousParameterNames extends TestMovableType
    {
        @Deserialization
        public TestMovableSubTypeAmbiguousParameterNames(
            AbstractMovable.MovableBaseHolder base,
            @PersistentVariable("ambiguous") UUID o0,
            @PersistentVariable("ambiguous") String o1)
        {
            super(base);
        }
    }

    @SuppressWarnings("unused")
    @EqualsAndHashCode(callSuper = true)
    private static class TestMovableSubTypeAmbiguousFieldTypes extends TestMovableType
    {
        @PersistentVariable
        private int ambiguousInteger0;

        @PersistentVariable
        private int ambiguousInteger1;

        @Deserialization
        public TestMovableSubTypeAmbiguousFieldTypes(AbstractMovable.MovableBaseHolder base)
        {
            super(base);
        }
    }

    @SuppressWarnings("unused")
    @EqualsAndHashCode(callSuper = true)
    private static class TestMovableSubTypeAmbiguousFieldNames extends TestMovableType
    {
        @PersistentVariable("ambiguous")
        private int field0;

        @PersistentVariable("ambiguous")
        private String field1;

        @Deserialization
        public TestMovableSubTypeAmbiguousFieldNames(AbstractMovable.MovableBaseHolder base)
        {
            super(base);
        }
    }

    @SuppressWarnings("unused")
    @EqualsAndHashCode(callSuper = true)
    private static class TestMovableSubTypeConstructorVersions extends TestMovableType
    {
        @Getter
        private final int version;

        private TestMovableSubTypeConstructorVersions(AbstractMovable.MovableBaseHolder base, int version)
        {
            super(base);
            this.version = version;
        }

        @Deserialization
        public TestMovableSubTypeConstructorVersions(AbstractMovable.MovableBaseHolder base)
        {
            this(base, -1);
        }

        @Deserialization(version = 1)
        public TestMovableSubTypeConstructorVersions(AbstractMovable.MovableBaseHolder base, Object o0)
        {
            this(base, 1);
        }

        @Deserialization(version = 2)
        public TestMovableSubTypeConstructorVersions(AbstractMovable.MovableBaseHolder base, Object o0, String o1)
        {
            this(base, 2);
        }
    }

    @SuppressWarnings("unused")
    @EqualsAndHashCode(callSuper = true)
    private static class TestMovableSubTypeConstructorVersionsNoFallback extends TestMovableType
    {
        @Getter
        private final int version;

        private TestMovableSubTypeConstructorVersionsNoFallback(AbstractMovable.MovableBaseHolder base, int version)
        {
            super(base);
            this.version = version;
        }

        @Deserialization(version = 1)
        public TestMovableSubTypeConstructorVersionsNoFallback(AbstractMovable.MovableBaseHolder base, Object o0)
        {
            this(base, 1);
        }

        @Deserialization(version = 2)
        public TestMovableSubTypeConstructorVersionsNoFallback(
            AbstractMovable.MovableBaseHolder base, Object o0, String o1)
        {
            this(base, 2);
        }
    }

    @SuppressWarnings("unused")
    @EqualsAndHashCode(callSuper = true)
    private static class TestMovableSubTypeAmbiguousConstructorVersions extends TestMovableType
    {
        @Deserialization(version = 1)
        public TestMovableSubTypeAmbiguousConstructorVersions(AbstractMovable.MovableBaseHolder base)
        {
            super(base);
        }

        @Deserialization(version = 1)
        public TestMovableSubTypeAmbiguousConstructorVersions(AbstractMovable.MovableBaseHolder base, Object o)
        {
            super(base);
        }
    }
}
