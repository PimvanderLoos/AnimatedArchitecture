package nl.pim16aap2.animatedarchitecture.spigot.util.blockstate;

import org.bukkit.block.Block;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockStateManipulatorTest
{
    @Test
    void testGetSortedBlockStateHandlers()
    {
        final List<BlockStateHandler<?>> handlers = List.of(
            createBlockStateHandler(InterfaceB.class),
            createBlockStateHandler(InterfaceA.class),
            createBlockStateHandler(Interface3.class),
            createBlockStateHandler(Interface2.class),
            createBlockStateHandler(Interface1.class)
        );

        final var sortedHandlers = BlockStateManipulator
            .getSortedBlockStateHandlers(handlers.stream())
            .stream()
            .map(BlockStateHandler::getBlockStateClass)
            .toList();

        final List<Class<?>> expectedOrder = List.of(
            Interface1.class,
            Interface2.class,
            Interface3.class,
            InterfaceA.class,
            InterfaceB.class
        );

        Assertions.assertIterableEquals(expectedOrder, sortedHandlers);
    }

    @Test
    void testGetInterfaceDepth()
    {
        assertEquals(0, BlockStateManipulator.getInterfaceDepth(Interface1.class));
        assertEquals(1, BlockStateManipulator.getInterfaceDepth(Interface2.class));
        assertEquals(2, BlockStateManipulator.getInterfaceDepth(Interface3.class));
        assertEquals(3, BlockStateManipulator.getInterfaceDepth(InterfaceA.class));
        assertEquals(3, BlockStateManipulator.getInterfaceDepth(InterfaceB.class));
    }

    /**
     * Create a {@link BlockStateHandler} for the given class.
     *
     * @param clz
     *     The class to create a {@link BlockStateHandler} for.
     * @param <T>
     *     The type of the class.
     * @return The created {@link BlockStateHandler}.
     */
    private static <T> BlockStateHandler<T> createBlockStateHandler(Class<T> clz)
    {
        return new BlockStateHandler<>(clz)
        {
            @Override
            public Class<T> getBlockStateClass()
            {
                return clz;
            }

            @Override
            protected void applyBlockState(T source, T target, Block block)
            {
            }
        };
    }

    private interface Interface1
    {
    }

    private interface Interface2 extends Interface1
    {
    }

    private interface Interface3 extends Interface2
    {
    }

    private interface InterfaceA extends Interface1, Interface3, Interface2
    {
    }

    private interface InterfaceB extends Interface3
    {
    }
}
