package nl.pim16aap2.animatedarchitecture.core.structures;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.annotations.Deserialization;
import nl.pim16aap2.animatedarchitecture.core.annotations.PersistentVariable;
import nl.pim16aap2.animatedarchitecture.core.api.PlayerData;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector2Di;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.testimplementations.TestWorld;
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

class StructureSerializerTest
{
    private AbstractStructure.BaseHolder structureBase;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        final AssistedFactoryMocker<StructureBase, StructureBase.IFactory> assistedFactoryMocker =
            new AssistedFactoryMocker<>(StructureBase.class, StructureBase.IFactory.class);

        final StructureRegistry structureRegistry = assistedFactoryMocker.getMock(StructureRegistry.class);
        Mockito.when(structureRegistry.computeIfAbsent(Mockito.anyLong(), Mockito.any()))
               .thenAnswer(invocation -> invocation.getArgument(1, Supplier.class).get());

        final StructureBaseBuilder factory = new StructureBaseBuilder(assistedFactoryMocker.getFactory());


        final String structureName = "randomDoorName";
        final Vector3Di zeroPos = new Vector3Di(0, 0, 0);
        final PlayerData playerData = new PlayerData(UUID.randomUUID(), "player", -1, -1, true, true);
        final StructureOwner structureOwner = new StructureOwner(1, PermissionLevel.CREATOR, playerData);

        structureBase = factory.builder().uid(1).name(structureName).cuboid(new Cuboid(zeroPos, zeroPos))
                               .rotationPoint(zeroPos).powerBlock(zeroPos).world(new TestWorld("worldName"))
                               .isOpen(false).isLocked(false).openDir(MovementDirection.DOWN).primeOwner(structureOwner)
                               .build();
    }

    @Test
    void instantiate()
    {
        final var instantiator =
            Assertions.assertDoesNotThrow(() -> new StructureSerializer<>(TestStructureType.class, 1));
        final TestStructureType base = new TestStructureType(structureBase, "test", true, 42);

        TestStructureType test = Assertions.assertDoesNotThrow(
            () -> instantiator.instantiate(structureBase, 1,
                                           Map.of("testName", "test",
                                                  "isCoolType", true,
                                                  "blockTestCount", 42)));
        Assertions.assertEquals(base, test);

        test = Assertions.assertDoesNotThrow(
            () -> instantiator.instantiate(structureBase, 1,
                                           Map.of("testName", "alternativeName",
                                                  "isCoolType", true,
                                                  "blockTestCount", 42)));
        Assertions.assertEquals("alternativeName", test.getTestName());
    }

    @Test
    void serialize()
    {
        final StructureSerializer<TestStructureType> instantiator =
            Assertions.assertDoesNotThrow(() -> new StructureSerializer<>(TestStructureType.class, 1));
        final TestStructureType testStructureType = new TestStructureType(structureBase, "test", true, 42);

        final String serialized = Assertions.assertDoesNotThrow(() -> instantiator.serialize(testStructureType));
        Assertions.assertEquals(
            testStructureType,
            Assertions.assertDoesNotThrow(() -> instantiator.deserialize(structureBase, 1, serialized)));
    }

    @Test
    void subclass()
    {
        final var instantiator = Assertions.assertDoesNotThrow(
            () -> new StructureSerializer<>(TestStructureSubType.class, 1));
        final TestStructureSubType realObj =
            new TestStructureSubType(structureBase, "testSubClass", 6, true, 42);

        final String serialized = Assertions.assertDoesNotThrow(() -> instantiator.serialize(realObj));
        final var deserialized = Assertions.assertDoesNotThrow(
            () -> instantiator.deserialize(structureBase, 1, serialized));

        Assertions.assertEquals(realObj, deserialized);
    }

    @Test
    void testEnumSerialization()
    {
        final var instantiator = Assertions.assertDoesNotThrow(
            () -> new StructureSerializer<>(TestStructureSubTypeEnum.class, 2));

        final TestStructureSubTypeEnum realObj =
            new TestStructureSubTypeEnum(structureBase, TestStructureSubTypeEnum.ArbitraryEnum.ENTRY_0,
                                         TestStructureSubTypeEnum.ArbitraryEnum.ENTRY_2);

        final String serialized = Assertions.assertDoesNotThrow(() -> instantiator.serialize(realObj));
        final var deserialized = Assertions.assertDoesNotThrow(
            () -> instantiator.deserialize(structureBase, 1, serialized));

        Assertions.assertEquals(realObj, deserialized);
    }

    @Test
    void testAmbiguityParams()
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new StructureSerializer<>(TestStructureSubTypeAmbiguousParameterTypes.class, 1));
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new StructureSerializer<>(TestStructureSubTypeAmbiguousParameterNames.class, 1));
    }

    @Test
    void testMissingData()
        throws Exception
    {
        final var instantiator = Assertions.assertDoesNotThrow(
            () -> new StructureSerializer<>(TestStructureType.class, 1));
        final TestStructureType realObj = new TestStructureType(structureBase, null, true, 42);

        final TestStructureType deserialized =
            instantiator.instantiate(structureBase, 1,
                                     Map.of("isCoolType", true,
                                            "blockTestCount", 42));

        Assertions.assertEquals(realObj, deserialized);
    }

    @Test
    void testInvalidMissingData()
    {
        final var instantiator = Assertions.assertDoesNotThrow(
            () -> new StructureSerializer<>(TestStructureType.class, 1));

        // The mapping for 'int blockTestCount' is missing, in which case we throw an exception.
        // Only objects can be missing.
        Assertions.assertThrows(
            Exception.class,
            () -> instantiator.instantiate(structureBase, 1,
                                           Map.of("testName", "testName",
                                                  "isCoolType", false)));
    }

    @Test
    void testAmbiguousNames()
    {
        Assertions.assertThrows(Exception.class,
                                () -> new StructureSerializer<>(TestStructureSubTypeAmbiguousFieldNames.class, 1));
    }

    @Test
    void testVersioning()
    {
        final var instantiator = Assertions.assertDoesNotThrow(
            () -> new StructureSerializer<>(TestStructureSubTypeConstructorVersions.class, 2));

        Assertions.assertEquals(1, instantiator.deserialize(structureBase, 1, "{}").version);
        Assertions.assertEquals(2, instantiator.deserialize(structureBase, 2, "{}").version);
        Assertions.assertThrows(RuntimeException.class, () -> instantiator.deserialize(structureBase, 4, "{}"));
    }

    @Test
    void testVersioningWithoutFallback()
    {
        final var instantiator = Assertions.assertDoesNotThrow(
            () -> new StructureSerializer<>(TestStructureSubTypeConstructorVersionsNoFallback.class, 2));

        Assertions.assertThrows(RuntimeException.class, () -> instantiator.deserialize(structureBase, 999, "{}"));
        Assertions.assertEquals(1, instantiator.deserialize(structureBase, 1, "{}").version);
        Assertions.assertEquals(2, instantiator.deserialize(structureBase, 2, "{}").version);
    }

    @Test
    void testAmbiguousVersions()
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new StructureSerializer<>(
                                    TestStructureSubTypeAmbiguousConstructorVersions.class, 1));
    }

    // This class is a nullability nightmare, but that doesn't matter, because none of the methods are used;
    // It's only used for testing serialization and the methods are therefore just stubs.
    @SuppressWarnings("ConstantConditions")
    // Don't call super for equals etc., as we don't care about the equality
    // of the parameters that aren't serialized anyway.
    @EqualsAndHashCode(callSuper = false)
    private static class TestStructureType extends AbstractStructure
    {
        private static final StructureType STRUCTURE_TYPE = Mockito.mock(StructureType.class);

        @EqualsAndHashCode.Exclude
        @SuppressWarnings({"unused", "FieldCanBeLocal"})
        private final ReentrantReadWriteLock lock;

        @Getter
        @PersistentVariable("testName")
        protected @Nullable String testName;

        @Getter
        @PersistentVariable("isCoolType")
        protected boolean isCoolType;

        @Getter
        @PersistentVariable("blockTestCount")
        private int blockTestCount;

        public TestStructureType(BaseHolder structureBase)
        {
            super(structureBase, STRUCTURE_TYPE);
            this.lock = super.getLock();
        }

        @Deserialization
        public TestStructureType(
            BaseHolder base,
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
        public MovementDirection getCycledOpenDirection()
        {
            return null;
        }

        @Override
        protected IAnimationComponent constructAnimationComponent(AnimationRequestData data)
        {
            return null;
        }
    }

    @EqualsAndHashCode(callSuper = true)
    private static class TestStructureSubType extends TestStructureType
    {
        @EqualsAndHashCode.Exclude
        @SuppressWarnings({"unused", "FieldCanBeLocal"})
        private final ReentrantReadWriteLock lock;

        @Getter
        @PersistentVariable(value = "subclassTestValue")
        private final int subclassTestValue;

        @Deserialization
        public TestStructureSubType(
            BaseHolder base,
            String testName,
            @PersistentVariable(value = "subclassTestValue") int subclassTestValue,
            boolean isCoolType,
            @PersistentVariable(value = "blockTestCount") int blockTestCount)
        {
            super(base, testName, isCoolType, blockTestCount);
            this.lock = super.getLock();

            this.testName = testName;
            this.isCoolType = isCoolType;
            this.subclassTestValue = subclassTestValue;
        }

        @SuppressWarnings("unused")
        public TestStructureSubType(BaseHolder base)
        {
            this(base, "", -1, false, -1);
        }
    }

    @EqualsAndHashCode(callSuper = true)
    private static class TestStructureSubTypeAmbiguousParameterTypes extends TestStructureType
    {
        @PersistentVariable(value = "ambiguousInteger1")
        private final int ambiguousInteger1;

        @Deserialization
        public TestStructureSubTypeAmbiguousParameterTypes(
            BaseHolder base,
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
    private static class TestStructureSubTypeAmbiguousParameterNames extends TestStructureType
    {
        @Deserialization
        public TestStructureSubTypeAmbiguousParameterNames(
            BaseHolder base,
            @PersistentVariable(value = "ambiguous") UUID o0,
            @PersistentVariable(value = "ambiguous") String o1)
        {
            super(base);
        }
    }

    @SuppressWarnings("unused")
    @EqualsAndHashCode(callSuper = true)
    private static class TestStructureSubTypeAmbiguousFieldNames extends TestStructureType
    {
        @PersistentVariable("ambiguous")
        private int field0;

        @PersistentVariable("ambiguous")
        private String field1;

        @Deserialization
        public TestStructureSubTypeAmbiguousFieldNames(BaseHolder base)
        {
            super(base);
        }
    }

    @SuppressWarnings("unused")
    @EqualsAndHashCode(callSuper = true)
    private static class TestStructureSubTypeConstructorVersions extends TestStructureType
    {
        @Getter
        private final int version;

        private TestStructureSubTypeConstructorVersions(BaseHolder base, int version)
        {
            super(base);
            this.version = version;
        }

        @Deserialization
        public TestStructureSubTypeConstructorVersions(BaseHolder base)
        {
            this(base, -1);
        }

        @Deserialization(version = 1)
        public TestStructureSubTypeConstructorVersions(BaseHolder base, Object o0)
        {
            this(base, 1);
        }

        @Deserialization(version = 2)
        public TestStructureSubTypeConstructorVersions(BaseHolder base, Object o0, String o1)
        {
            this(base, 2);
        }
    }

    @SuppressWarnings("unused")
    @EqualsAndHashCode(callSuper = true)
    private static class TestStructureSubTypeConstructorVersionsNoFallback extends TestStructureType
    {
        @Getter
        private final int version;

        private TestStructureSubTypeConstructorVersionsNoFallback(BaseHolder base, int version)
        {
            super(base);
            this.version = version;
        }

        @Deserialization(version = 1)
        public TestStructureSubTypeConstructorVersionsNoFallback(BaseHolder base, Object o0)
        {
            this(base, 1);
        }

        @Deserialization(version = 2)
        public TestStructureSubTypeConstructorVersionsNoFallback(
            BaseHolder base, Object o0, String o1)
        {
            this(base, 2);
        }
    }

    @SuppressWarnings("unused")
    @EqualsAndHashCode(callSuper = true)
    private static class TestStructureSubTypeAmbiguousConstructorVersions extends TestStructureType
    {
        @Deserialization(version = 1)
        public TestStructureSubTypeAmbiguousConstructorVersions(BaseHolder base)
        {
            super(base);
        }

        @Deserialization(version = 1)
        public TestStructureSubTypeAmbiguousConstructorVersions(BaseHolder base, Object o)
        {
            super(base);
        }
    }


    @EqualsAndHashCode(callSuper = true)
    private static class TestStructureSubTypeEnum extends TestStructureType
    {
        @PersistentVariable("val0")
        private final ArbitraryEnum val0;
        @PersistentVariable("val1")
        private final ArbitraryEnum val1;

        @Deserialization(version = 1)
        public TestStructureSubTypeEnum(
            BaseHolder base,
            @PersistentVariable("val0") ArbitraryEnum val0,
            @PersistentVariable("val1") ArbitraryEnum val1)
        {
            super(base);
            this.val0 = val0;
            this.val1 = val1;
        }

        public enum ArbitraryEnum
        {
            ENTRY_0,
            ENTRY_1,
            ENTRY_2
        }
    }
}
