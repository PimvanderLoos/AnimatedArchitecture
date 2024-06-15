package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.util.Limit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.OptionalInt;

class LimitContainerTest
{
    @Test
    void testConstructorWithNullableIntegers()
    {
        final var limitContainer = new LimitContainer(
            null,
            10,
            null,
            20
        );

        Assertions.assertTrue(limitContainer.structureSizeLimit().isEmpty());
        assertEquals(10, limitContainer.structureCountLimit());
        Assertions.assertTrue(limitContainer.powerBlockDistanceLimit().isEmpty());
        assertEquals(20, limitContainer.blocksToMoveLimit());
    }

    @Test
    void testConstructorWithPrimitiveIntegers()
    {
        final var limitContainer = new LimitContainer(
            5,
            10,
            15,
            20
        );

        assertEquals(5, limitContainer.structureSizeLimit());
        assertEquals(10, limitContainer.structureCountLimit());
        assertEquals(15, limitContainer.powerBlockDistanceLimit());
        assertEquals(20, limitContainer.blocksToMoveLimit());
    }

    @Test
    void testGetLimit()
    {
        final var limitContainer = new LimitContainer(
            5,
            10,
            15,
            20
        );

        assertEquals(5, limitContainer.getLimit(Limit.STRUCTURE_SIZE));
        assertEquals(10, limitContainer.getLimit(Limit.STRUCTURE_COUNT));
        assertEquals(15, limitContainer.getLimit(Limit.POWERBLOCK_DISTANCE));
        assertEquals(20, limitContainer.getLimit(Limit.BLOCKS_TO_MOVE));
    }

    @Test
    void testGetLimitWithNullValues()
    {
        final var limitContainer = new LimitContainer(
            (Integer) null,
            (Integer) null,
            (Integer) null,
            (Integer) null
        );

        Assertions.assertTrue(limitContainer.getLimit(Limit.STRUCTURE_SIZE).isEmpty());
        Assertions.assertTrue(limitContainer.getLimit(Limit.STRUCTURE_COUNT).isEmpty());
        Assertions.assertTrue(limitContainer.getLimit(Limit.POWERBLOCK_DISTANCE).isEmpty());
        Assertions.assertTrue(limitContainer.getLimit(Limit.BLOCKS_TO_MOVE).isEmpty());
    }

    @Test
    void testStaticOfMethodWithIPlayer()
    {
        final IPlayer player = newDataContainer(IPlayer.class);
        final var limitContainer = LimitContainer.of(player);

        assertEquals(1, limitContainer.structureSizeLimit());
        assertEquals(2, limitContainer.structureCountLimit());
        assertEquals(3, limitContainer.powerBlockDistanceLimit());
        Assertions.assertTrue(limitContainer.blocksToMoveLimit().isEmpty());
    }

    @Test
    void testStaticOfMethodWithPlayerData()
    {
        final PlayerData playerData = newDataContainer(PlayerData.class);
        final var limitContainer = LimitContainer.of(playerData);

        assertEquals(1, limitContainer.structureSizeLimit());
        assertEquals(2, limitContainer.structureCountLimit());
        assertEquals(3, limitContainer.powerBlockDistanceLimit());
        Assertions.assertTrue(limitContainer.blocksToMoveLimit().isEmpty());
    }

    /**
     * Creates a new instance of the specified class and mocks the {@link IPlayerDataContainer#getLimit(Limit)} method.
     * <p>
     * This method will return the following values:
     * <ul>
     *     <li>{@link Limit#STRUCTURE_SIZE} -> 1</li>
     *     <li>{@link Limit#STRUCTURE_COUNT} -> 2</li>
     *     <li>{@link Limit#POWERBLOCK_DISTANCE} -> 3</li>
     *     <li>{@link Limit#BLOCKS_TO_MOVE} -> empty</li>
     * </ul>
     *
     * @param clz
     *     The class to mock.
     * @param <T>
     *     The type of the class to mock. Has to be a subclass of {@link IPlayerDataContainer}.
     * @return The mocked instance of the class.
     */
    private <T extends IPlayerDataContainer> T newDataContainer(Class<T> clz)
    {
        final T mock = Mockito.mock(clz);

        Mockito.when(mock.getLimit(Mockito.any(Limit.class)))
            .thenAnswer(invocation ->
                switch (invocation.getArgument(0, Limit.class))
                {
                    case STRUCTURE_SIZE -> OptionalInt.of(1);
                    case STRUCTURE_COUNT -> OptionalInt.of(2);
                    case POWERBLOCK_DISTANCE -> OptionalInt.of(3);
                    case BLOCKS_TO_MOVE -> OptionalInt.empty();
                });

        return mock;
    }

    /**
     * Asserts that the given {@link OptionalInt} is present and equals the expected value.
     * <p>
     * This method is a shortcut for {@link Assertions#assertTrue(boolean)} with {@link OptionalInt#isPresent()} and
     * {@link Assertions#assertEquals(Object, Object)} with {@link OptionalInt#getAsInt()}.
     *
     * @param expected
     *     The expected value.
     * @param actual
     *     The actual value.
     */
    private void assertEquals(int expected, OptionalInt actual)
    {
        Assertions.assertTrue(actual.isPresent());
        Assertions.assertEquals(expected, actual.getAsInt());
    }
}
