package nl.pim16aap2.animatedarchitecture.structures.garagedoor;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.LazyValue;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.testing.reflection.ReflectionUtil;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class GarageDoorTestUtil
{
    public static final List<OpeningData> OPENING_DATA_LIST = List.of(
        new OpeningData(
            "North-South-OddY",
            MovementDirection.NORTH, new Vector3Di(150, 68, -51),
            new Vector3Di(147, 63, -51), new Vector3Di(153, 67, -51),
            new Vector3Di(147, 68, -56), new Vector3Di(153, 68, -52)
        ),
        new OpeningData(
            "North-South-EvenY",
            MovementDirection.NORTH, new Vector3Di(150, 68, -51),
            new Vector3Di(147, 62, -51), new Vector3Di(153, 67, -51),
            new Vector3Di(147, 68, -57), new Vector3Di(153, 68, -52)
        ),

        new OpeningData(
            "West-East-OddY",
            MovementDirection.WEST, new Vector3Di(-144, -68, -45),
            new Vector3Di(-144, -75, -48), new Vector3Di(-144, -69, -42),
            new Vector3Di(-151, -68, -48), new Vector3Di(-145, -68, -42)
        ),
        new OpeningData(
            "West-East-EvenY",
            MovementDirection.WEST, new Vector3Di(-144, -68, -45),
            new Vector3Di(-144, -74, -48), new Vector3Di(-144, -69, -42),
            new Vector3Di(-150, -68, -48), new Vector3Di(-145, -68, -42)
        ),

        new OpeningData(
            "South-North-OddY",
            MovementDirection.SOUTH, new Vector3Di(150, 68, 39),
            new Vector3Di(147, 62, 39), new Vector3Di(153, 67, 39),
            new Vector3Di(147, 68, 40), new Vector3Di(153, 68, 45)
        ),
        new OpeningData(
            "South-North-EvenY",
            MovementDirection.SOUTH, new Vector3Di(150, 68, 39),
            new Vector3Di(147, 63, 39), new Vector3Di(153, 67, 39),
            new Vector3Di(147, 68, 40), new Vector3Di(153, 68, 44)
        ),

        new OpeningData(
            "East-West-OddY",
            MovementDirection.EAST, new Vector3Di(156, -68, -45),
            new Vector3Di(156, -74, -48), new Vector3Di(156, -69, -42),
            new Vector3Di(157, -68, -48), new Vector3Di(162, -68, -42)
        ),
        new OpeningData(
            "East-West-EvenY",
            MovementDirection.EAST, new Vector3Di(156, -68, -45),
            new Vector3Di(156, -73, -48), new Vector3Di(156, -69, -42),
            new Vector3Di(157, -68, -48), new Vector3Di(161, -68, -42)
        )
    );

    /**
     * Represents the minimal data needed to toggle a garage door and the data that is expected after toggling it.
     */
    @Accessors(fluent = true)
    @Getter
    @EqualsAndHashCode
    public static final class OpeningData
    {
        private final String name;

        @Getter(AccessLevel.NONE)
        @EqualsAndHashCode.Exclude
        private final LazyValue<OpeningData> opposite;

        private final MovementDirection currentToggleDir;
        private final Vector3Di rotationPoint;
        private final Cuboid startCuboid;
        private final Cuboid endCuboid;

        /**
         * Private constructor used to create the opposite {@link OpeningData} of an existing one.
         * <p>
         * The provided {@code opposite} instance is used as the opposite of this {@link OpeningData}, so getting the
         * opposite of the opposite will return the same instance.
         */
        private OpeningData(
            String name,
            OpeningData opposite,
            MovementDirection currentToggleDir,
            Vector3Di rotationPoint,
            Cuboid startCuboid,
            Cuboid endCuboid)
        {
            this.name = name;
            this.opposite = new LazyValue<>(() -> opposite);
            this.currentToggleDir = currentToggleDir;
            this.rotationPoint = rotationPoint;
            this.startCuboid = startCuboid;
            this.endCuboid = endCuboid;
        }

        /**
         * @param name
         *     The name of the opening data. This is used to make the data more readable in test logs.
         * @param currentToggleDir
         *     The current toggle direction. See {@link GarageDoor#getCurrentToggleDir()}.
         * @param rotationPoint
         *     The rotation point. See {@link GarageDoor#getRotationPoint()}.
         * @param startCuboid
         *     The start cuboid. See {@link GarageDoor#getCuboid()}.
         * @param endCuboid
         *     The cuboid that will describe the structure after a potential toggle.
         */
        public OpeningData(
            String name,
            MovementDirection currentToggleDir,
            Vector3Di rotationPoint,
            Cuboid startCuboid,
            Cuboid endCuboid)
        {
            this.name = name;
            this.opposite = new LazyValue<>(this::createOpposite);
            this.currentToggleDir = currentToggleDir;
            this.rotationPoint = rotationPoint;
            this.startCuboid = startCuboid;
            this.endCuboid = endCuboid;
        }

        /**
         * @param name
         *     The name of the opening data. This is used to make the data more readable in test logs.
         * @param currentToggleDir
         *     The current toggle direction. See {@link GarageDoor#getCurrentToggleDir()}.
         * @param rotationPoint
         *     The rotation point. See {@link GarageDoor#getRotationPoint()}.
         * @param startMin
         *     The start minimum coordinates used to construct the cuboid. See {@link GarageDoor#getCuboid()}.
         * @param startMax
         *     The start maximum coordinates used to construct the cuboid. See {@link GarageDoor#getCuboid()}.
         * @param endMin
         *     The minimum coordinates of the new cuboid after the toggle.
         * @param endMax
         *     The maximum coordinates of the new cuboid after the toggle.
         */
        public OpeningData(
            String name,
            MovementDirection currentToggleDir,
            Vector3Di rotationPoint,
            Vector3Di startMin,
            Vector3Di startMax,
            Vector3Di endMin,
            Vector3Di endMax)
        {
            this(name, currentToggleDir, rotationPoint, new Cuboid(startMin, startMax), new Cuboid(endMin, endMax));
        }

        /**
         * Creates a new mocked GarageDoor with the given data.
         *
         * @return The new GarageDoor instance.
         */
        public GarageDoor createGarageDoor()
        {
            final GarageDoor garageDoor = Mockito.mock(GarageDoor.class, InvocationOnMock::callRealMethod);

            final var lock = new ReentrantReadWriteLock();
            ReflectionUtil.setField(GarageDoor.class, garageDoor, "lock", lock);
            ReflectionUtil.setField(AbstractStructure.class, garageDoor, "lock", lock);

            Mockito.doReturn(startCuboid).when(garageDoor).getCuboid();
            Mockito.doReturn(currentToggleDir).when(garageDoor).getCurrentToggleDir();
            Mockito.doReturn(rotationPoint).when(garageDoor).getRotationPoint();

            final LazyValue<StructureSnapshot> snapshot = new LazyValue<>(
                () -> UnitTestUtil.createStructureSnapshotForStructure(garageDoor));
            Mockito.doAnswer(invocation -> snapshot.get()).when(garageDoor).getSnapshot();

            return garageDoor;
        }

        /**
         * Gets the opposite of this OpeningData.
         * <p>
         * The opposite of an OpeningData describes the same garage door, but with mirrored values where applicable. In
         * essence, the opposite describes the same garage door, but after it was toggled.
         * <p>
         * The opposite of the opposite is the same instance, so calling this method 0 or any multiple of 2 times will
         * always return the same instance.
         *
         * @return The opposite of this OpeningData.
         */
        public OpeningData getOpposite()
        {
            return opposite.get();
        }

        private OpeningData createOpposite()
        {
            return new OpeningData(
                name + " (Opposite)",
                this,
                MovementDirection.getOpposite(currentToggleDir),
                rotationPoint,
                endCuboid,
                startCuboid);
        }

        public String toString()
        {
            return String.format(
                """
                OpeningData(
                  name             = '%s'
                  currentToggleDir = '%s'
                  rotationPoint    = '%s'
                  startCuboid      = '%s'
                  endCuboid        = '%s'
                )
                """,
                name, currentToggleDir, rotationPoint, startCuboid, endCuboid);
        }
    }
}
