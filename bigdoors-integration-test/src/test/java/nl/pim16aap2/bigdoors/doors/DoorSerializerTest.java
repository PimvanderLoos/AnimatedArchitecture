package nl.pim16aap2.bigdoors.doors;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.testimplementations.TestPWorld;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.CuboidConst;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

class DoorSerializerTest
{
    private static final AbstractDoorBase.DoorData doorData;

    static
    {
        final String name = "randomDoorName";
        final Vector3Di pos = new Vector3Di(0, 0, 0);
        final PPlayerData playerData = new PPlayerData(UUID.randomUUID(), "player", -1, -1, true, true);
        final DoorOwner doorOwner = new DoorOwner(1, 0, playerData);
        doorData = new AbstractDoorBase.DoorData(1, name, pos, pos, pos, pos, new TestPWorld("worldName"),
                                                 false, false, RotateDirection.DOWN, doorOwner);
    }

    @Test
    void instantiate()
    {
        final DoorSerializer<TestDoorType> instantiator = new DoorSerializer<>(TestDoorType.class);
        final TestDoorType base = new TestDoorType(doorData, "test", true, 42);

        TestDoorType test = instantiator.instantiate(doorData, new ArrayList<>(Arrays.asList("test", true, 42)));
        Assertions.assertEquals(base, test);

        test = instantiator.instantiate(doorData, new ArrayList<>(Arrays.asList("alternativeName", true, 42)));
        Assertions.assertEquals("alternativeName", test.getTestName());
    }

    @Test
    void serialize()
    {
        final DoorSerializer<TestDoorType> instantiator = new DoorSerializer<>(TestDoorType.class);
        final TestDoorType testDoorType1 = new TestDoorType(doorData, "test", true, 42);

        final byte[] serialized = instantiator.serialize(testDoorType1);
        Assertions.assertEquals(testDoorType1, instantiator.deserialize(doorData, serialized));
    }

    // Don't call super for equals etc, as we don't care about the equality
    // of the parameters that aren't serialized anyway.
    @EqualsAndHashCode(callSuper = false)
    private static class TestDoorType extends AbstractDoorBase
    {
        @PersistentVariable
        @Getter
        private String testName;

        @PersistentVariable
        @Getter
        private boolean isCoolType;

        @PersistentVariable
        @Getter
        private int blockTestCount;

        public TestDoorType(final @NonNull DoorData doorData)
        {
            super(doorData);
        }

        public TestDoorType(final @NonNull DoorData doorData, final @NonNull String testName,
                            final boolean isCoolType, final int blockTestCount)
        {
            this(doorData);
            this.testName = testName;
            this.isCoolType = isCoolType;
            this.blockTestCount = blockTestCount;
        }

        @Override
        public @NotNull DoorType getDoorType()
        {
            return null;
        }

        @Override
        protected @NotNull BlockMover constructBlockMover(@NotNull DoorActionCause cause,
                                                          double time, boolean skipAnimation,
                                                          @NotNull CuboidConst newCuboid,
                                                          @NotNull IPPlayer responsible,
                                                          @NotNull DoorActionType actionType)
        {
            return null;
        }

        @Override
        public boolean canSkipAnimation()
        {
            return false;
        }

        @Override
        public @NotNull RotateDirection getCurrentToggleDir()
        {
            return null;
        }

        @Override
        public @NotNull Optional<Cuboid> getPotentialNewCoordinates()
        {
            return Optional.empty();
        }

        @Override
        public @NotNull RotateDirection cycleOpenDirection()
        {
            return null;
        }

        @Override
        public @NotNull Vector2Di[] calculateChunkRange()
        {
            return new Vector2Di[0];
        }
    }
}
