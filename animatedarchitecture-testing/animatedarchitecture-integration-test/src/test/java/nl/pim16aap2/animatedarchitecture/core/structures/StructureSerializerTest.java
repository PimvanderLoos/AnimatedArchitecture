package nl.pim16aap2.animatedarchitecture.core.structures;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.annotations.Deserialization;
import nl.pim16aap2.animatedarchitecture.core.annotations.PersistentVariable;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.LimitContainer;
import nl.pim16aap2.animatedarchitecture.core.api.PlayerData;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyManager;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyManagerSerializer;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

class StructureSerializerTest
{
    private static final List<Property<?>> PROPERTIES = List.of(
        Property.OPEN_STATUS
    );

    private StructureBaseBuilder.IBuilderProperties structureBaseBuilder;
    private PropertyManager propertyManager;
    private String serializedProperties;
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

        final var limits = new LimitContainer(
            OptionalInt.empty(),
            OptionalInt.empty(),
            OptionalInt.empty(),
            OptionalInt.empty()
        );

        final PlayerData playerData = new PlayerData(UUID.randomUUID(), "player", limits, true, true);
        final StructureOwner structureOwner = new StructureOwner(1, PermissionLevel.CREATOR, playerData);

        structureBaseBuilder = factory
            .builder()
            .uid(1)
            .name(structureName)
            .cuboid(new Cuboid(zeroPos, zeroPos))
            .rotationPoint(zeroPos)
            .powerBlock(zeroPos)
            .world(new TestWorld("worldName"))
            .isOpen(false)
            .isLocked(false)
            .openDir(MovementDirection.DOWN)
            .primeOwner(structureOwner)
            .ownersOfStructure(null);

        propertyManager = PropertyManager.forType(TestStructureType0.INSTANCE);
        serializedProperties = PropertyManagerSerializer.serialize(propertyManager);

        structureBase = structureBaseBuilder.propertiesOfStructure(propertyManager).build();
    }

    @Test
    void instantiate()
    {
        final StructureSerializer<TestStructureImpl> instantiator =
            Assertions.assertDoesNotThrow(() -> new StructureSerializer<>(TestStructureType0.INSTANCE));
        final TestStructureImpl base = new TestStructureImpl(structureBase, "test", true, 42);

        TestStructureImpl test = Assertions.assertDoesNotThrow(() ->
            instantiator.instantiate(
                structureBase,
                1,
                Map.of(
                    "testName", "test",
                    "isCoolType", true,
                    "blockTestCount", 42))
        );

        Assertions.assertEquals(base, test);

        test = Assertions.assertDoesNotThrow(() ->
            instantiator.instantiate(
                structureBase,
                1,
                Map.of(
                    "testName", "alternativeName",
                    "isCoolType", true,
                    "blockTestCount", 42))
        );
        Assertions.assertEquals("alternativeName", test.getTestName());
    }

    @Test
    void serialize()
    {
        final StructureSerializer<TestStructureImpl> instantiator =
            Assertions.assertDoesNotThrow(() -> new StructureSerializer<>(TestStructureType0.INSTANCE));
        final TestStructureImpl testStructureType = new TestStructureImpl(structureBase, "test", true, 42);

        final var serialized = Assertions.assertDoesNotThrow(() -> instantiator.serializeTypeData(testStructureType));
        Assertions.assertEquals(
            testStructureType,
            Assertions.assertDoesNotThrow(() -> instantiator.deserialize(
                structureBaseBuilder,
                1,
                serialized,
                serializedProperties))
        );
    }

    @Test
    void serialize0()
    {
        final var instantiator = newSerializer(TestStructureImpl.class, 1);

        final var testStructure = new TestStructureImpl(structureBase, "test", true, 42);

        final String serialized = Assertions.assertDoesNotThrow(() -> instantiator.serializeTypeData(testStructure));
        Assertions.assertEquals(
            testStructure,
            Assertions.assertDoesNotThrow(() -> instantiator.deserialize(
                structureBaseBuilder,
                1,
                serialized,
                serializedProperties))
        );
    }

    @Test
    void subclass()
    {
        final var instantiator = newSerializer(TestStructureSubType.class, 1);

        final TestStructureSubType realObj =
            new TestStructureSubType(structureBase, "testSubClass", 6, true, 42);

        final String serialized = Assertions.assertDoesNotThrow(() -> instantiator.serializeTypeData(realObj));
        final var deserialized = Assertions.assertDoesNotThrow(
            () -> instantiator.deserialize(structureBaseBuilder, 1, serialized, serializedProperties));

        Assertions.assertEquals(realObj, deserialized);
    }

    @Test
    void testEnumSerialization()
    {
        final var instantiator = newSerializer(TestStructureSubTypeEnum.class, 2);

        final TestStructureSubTypeEnum realObj = new TestStructureSubTypeEnum(
            structureBase,
            TestStructureSubTypeEnum.ArbitraryEnum.ENTRY_0,
            TestStructureSubTypeEnum.ArbitraryEnum.ENTRY_2
        );

        final String serialized = Assertions.assertDoesNotThrow(() -> instantiator.serializeTypeData(realObj));
        final var deserialized = Assertions.assertDoesNotThrow(
            () -> instantiator.deserialize(structureBaseBuilder, 1, serialized, serializedProperties));

        Assertions.assertEquals(realObj, deserialized);
    }

    @Test
    void testAmbiguityParams()
    {
        final var type = mockStructureType(TestStructureSubTypeAmbiguousParameterTypes.class);

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new StructureSerializer<>(type, TestStructureSubTypeAmbiguousParameterTypes.class, 1)
        );

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new StructureSerializer<>(type, TestStructureSubTypeAmbiguousParameterNames.class, 1)
        );
    }

    @Test
    void testMissingData()
        throws Exception
    {
        final var instantiator = Assertions.assertDoesNotThrow(
            () -> new StructureSerializer<>(TestStructureType0.INSTANCE));

        final var realObj = new TestStructureImpl(structureBase, null, true, 42);
        final var deserialized = instantiator.instantiate(
            structureBase,
            1,
            Map.of(
                "isCoolType", true,
                "blockTestCount", 42)
        );

        Assertions.assertEquals(realObj, deserialized);
    }

    @Test
    void testInvalidMissingData()
    {
        final var instantiator = Assertions.assertDoesNotThrow(
            () -> new StructureSerializer<>(TestStructureType0.INSTANCE));

        // The mapping for 'int blockTestCount' is missing, in which case we throw an exception.
        // Only objects can be missing.
        Assertions.assertThrows(
            Exception.class,
            () -> instantiator.instantiate(
                structureBase,
                1,
                Map.of(
                    "testName", "testName",
                    "isCoolType", false))
        );
    }

    @Test
    void testAmbiguousNames()
    {
        final var type = mockStructureType(TestStructureSubTypeAmbiguousFieldNames.class);
        Assertions.assertThrows(
            Exception.class,
            () -> new StructureSerializer<>(type, TestStructureSubTypeAmbiguousFieldNames.class, 1)
        );
    }

    @Test
    void testVersioning()
    {
        final var instantiator = newSerializer(TestStructureSubTypeConstructorVersions.class, 2);

        Assertions.assertEquals(1, instantiator.deserialize(structureBaseBuilder, 1, "{}", "{}").version);
        Assertions.assertEquals(2, instantiator.deserialize(structureBaseBuilder, 2, "{}", "{}").version);
        Assertions.assertThrows(
            RuntimeException.class,
            () -> instantiator.deserialize(structureBaseBuilder, 4, "{}", "{}")
        );
    }

    @Test
    void testVersioningWithoutFallback()
    {
        final var instantiator = newSerializer(TestStructureSubTypeConstructorVersionsNoFallback.class, 2);

        Assertions.assertThrows(
            RuntimeException.class,
            () -> instantiator.deserialize(structureBaseBuilder, 999, "{}", "{}")
        );

        Assertions.assertEquals(1, instantiator.deserialize(structureBaseBuilder, 1, "{}", "{}").version);
        Assertions.assertEquals(2, instantiator.deserialize(structureBaseBuilder, 2, "{}", "{}").version);
    }

    @Test
    void testAmbiguousVersions()
    {
        final var type = mockStructureType(TestStructureSubTypeAmbiguousConstructorVersions.class);

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new StructureSerializer<>(type, TestStructureSubTypeAmbiguousConstructorVersions.class, 1)
        );
    }

    private static class TestStructureType0 extends StructureType
    {
        private static final TestStructureType0 INSTANCE = new TestStructureType0(
            "pluginName",
            "typeName",
            1,
            List.of(MovementDirection.DOWN),
            "localizationKey"
        );

        private TestStructureType0(
            String pluginName,
            String simpleName,
            int version,
            List<MovementDirection> validMovementDirections,
            String localizationKey)
        {
            super(pluginName, simpleName, version, validMovementDirections, PROPERTIES, localizationKey);
        }

        @Override
        public Class<? extends AbstractStructure> getStructureClass()
        {
            return TestStructureImpl.class;
        }

        @Override
        public Creator getCreator(ToolUser.Context context, IPlayer player, @Nullable String name)
        {
            throw new UnsupportedOperationException("Creator not implemented for test structure type.");
        }
    }

    // This class is a nullability nightmare, but that doesn't matter, because none of the methods are used;
    // It's only used for testing serialization and the methods are therefore just stubs.
    @SuppressWarnings("ConstantConditions")
    // Don't call super for equals etc., as we don't care about the equality
    // of the parameters that aren't serialized anyway.
    @EqualsAndHashCode(callSuper = false)
    private static class TestStructureImpl extends AbstractStructure
    {
        private static final StructureType STRUCTURE_TYPE = TestStructureType0.INSTANCE;

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

        public TestStructureImpl(BaseHolder structureBase)
        {
            super(structureBase, STRUCTURE_TYPE);
            this.lock = super.getLock();
        }

        @Deserialization
        public TestStructureImpl(
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
    private static class TestStructureSubType extends TestStructureImpl
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
    private static class TestStructureSubTypeAmbiguousParameterTypes extends TestStructureImpl
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
    private static class TestStructureSubTypeAmbiguousParameterNames extends TestStructureImpl
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
    private static class TestStructureSubTypeAmbiguousFieldNames extends TestStructureImpl
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
    private static class TestStructureSubTypeConstructorVersions extends TestStructureImpl
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
    private static class TestStructureSubTypeConstructorVersionsNoFallback extends TestStructureImpl
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
        public TestStructureSubTypeConstructorVersionsNoFallback(BaseHolder base, Object o0, String o1)
        {
            this(base, 2);
        }
    }

    @SuppressWarnings("unused")
    @EqualsAndHashCode(callSuper = true)
    private static class TestStructureSubTypeAmbiguousConstructorVersions extends TestStructureImpl
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
    private static class TestStructureSubTypeEnum extends TestStructureImpl
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

    private static <T extends AbstractStructure> StructureType mockStructureType(Class<T> clz)
    {
        final StructureType structureType = Mockito.mock(StructureType.class);
        Mockito.when(structureType.getProperties()).thenReturn(PROPERTIES);
        Mockito.doReturn(clz).when(structureType).getStructureClass();
        return structureType;
    }

    private <T extends AbstractStructure> StructureSerializer<T> newSerializer(Class<T> clazz, int version)
    {
        final var structureType = mockStructureType(clazz);
        return new StructureSerializer<>(structureType, clazz, version);
    }
}
