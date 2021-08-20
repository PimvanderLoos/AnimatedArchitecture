package nl.pim16aap2.bigdoors.doors;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.val;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.testimplementations.TestPWorld;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static nl.pim16aap2.bigdoors.UnitTestUtil.initPlatform;

class DoorSerializerTest
{
    private static final DoorBase doorData;

    static
    {
        final String name = "randomDoorName";
        final Vector3Di pos = new Vector3Di(0, 0, 0);
        final PPlayerData playerData = new PPlayerData(UUID.randomUUID(), "player", -1, -1, true, true);
        final DoorOwner doorOwner = new DoorOwner(1, 0, playerData);
        doorData = new DoorBase(1, name, new Cuboid(pos, pos), pos, pos, new TestPWorld("worldName"),
                                false, false, RotateDirection.DOWN, doorOwner);
    }

    @BeforeEach
    void init()
    {
        val platform = initPlatform();
        val doorRegistry = Mockito.mock(DoorRegistry.class);
        Mockito.when(platform.getDoorRegistry()).thenReturn(doorRegistry);
        Mockito.when(doorRegistry.registerDoor(Mockito.any())).thenReturn(true);
    }

    @Test
    void instantiate()
    {
        val instantiator = Assertions.assertDoesNotThrow(() -> new DoorSerializer<>(TestDoorType.class));
        final TestDoorType base = new TestDoorType(doorData, "test", true, 42);

        TestDoorType test = Assertions.assertDoesNotThrow(
            () -> instantiator.instantiate(doorData, new ArrayList<>(Arrays.asList("test", true, 42))));
        Assertions.assertEquals(base, test);

        test = Assertions.assertDoesNotThrow(
            () -> instantiator.instantiate(doorData, new ArrayList<>(Arrays.asList("alternativeName", true, 42))));
        Assertions.assertEquals("alternativeName", test.getTestName());
    }

    @Test
    void instantiateUnsafe()
    {
        val instantiator = Assertions.assertDoesNotThrow(() -> new DoorSerializer<>(TestDoorSubType.class));
        final TestDoorSubType base = new TestDoorSubType(doorData, "test", true, 42, 1);

        TestDoorType test = Assertions.assertDoesNotThrow(
            () -> instantiator.instantiate(doorData, new ArrayList<>(Arrays.asList("test", true, 42, 1))));
        Assertions.assertEquals(base, test);

        test = Assertions.assertDoesNotThrow(
            () -> instantiator.instantiate(doorData, new ArrayList<>(Arrays.asList("alternativeName", true, 42, 1))));
        Assertions.assertEquals("alternativeName", test.getTestName());
    }

    @Test
    void serialize()
    {
        final DoorSerializer<TestDoorType> instantiator =
            Assertions.assertDoesNotThrow(() -> new DoorSerializer<>(TestDoorType.class));
        final TestDoorType testDoorType1 = new TestDoorType(doorData, "test", true, 42);

        final byte[] serialized = Assertions.assertDoesNotThrow(() -> instantiator.serialize(testDoorType1));
        Assertions.assertEquals(testDoorType1,
                                Assertions.assertDoesNotThrow(() -> instantiator.deserialize(doorData, serialized)));
    }

    @Test
    void subclass()
    {
        val instantiator = Assertions.assertDoesNotThrow(() -> new DoorSerializer<>(TestDoorSubType.class));
        final TestDoorSubType testDoorSubType1 = new TestDoorSubType(doorData, "test", true, 42, 6);

        final byte[] serialized = Assertions.assertDoesNotThrow(() -> instantiator.serialize(testDoorSubType1));
        val testDoorSubType2 = Assertions.assertDoesNotThrow(() -> instantiator.deserialize(doorData, serialized));

        Assertions.assertEquals(testDoorSubType1, testDoorSubType2);
    }

    // This class is a nullability nightmare, but that doesn't matter, because none of the methods are used;
    // It's only used for testing serialization and the methods are therefore just stubs.
    @SuppressWarnings("ConstantConditions")
    // Don't call super for equals etc., as we don't care about the equality
    // of the parameters that aren't serialized anyway.
    @EqualsAndHashCode(callSuper = false)
    private static class TestDoorType extends AbstractDoor
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

        private static final DoorType DOOR_TYPE;

        static
        {
            DOOR_TYPE = Mockito.mock(DoorType.class);
            Mockito.when(DOOR_TYPE.getDoorSerializer()).thenReturn(Optional.empty());
        }

        @SuppressWarnings("unused")
        public TestDoorType(DoorBase doorBase)
        {
            super(doorBase);
        }

        public TestDoorType(DoorBase doorBase, String testName, boolean isCoolType, int blockTestCount)
        {
            super(doorBase);
            this.testName = testName;
            this.isCoolType = isCoolType;
            this.blockTestCount = blockTestCount;
        }

        @Override
        public DoorType getDoorType()
        {
            return DOOR_TYPE;
        }

        @Override
        protected BlockMover constructBlockMover(DoorActionCause cause,
                                                 double time, boolean skipAnimation,
                                                 Cuboid newCuboid,
                                                 IPPlayer responsible,
                                                 DoorActionType actionType)
        {
            return null;
        }

        @Override
        public boolean canSkipAnimation()
        {
            return false;
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
    }

    @EqualsAndHashCode(callSuper = true)
    private static class TestDoorSubType extends TestDoorType
    {
        @PersistentVariable
        @Getter
        private final int subclassTestValue;

        public TestDoorSubType(DoorBase doorBase, String testName, boolean isCoolType, int blockTestCount,
                               int subclassTestValue)
        {
            super(doorBase, testName, isCoolType, blockTestCount);

            this.testName = testName;
            this.isCoolType = isCoolType;

            this.subclassTestValue = subclassTestValue;
        }
    }
}
