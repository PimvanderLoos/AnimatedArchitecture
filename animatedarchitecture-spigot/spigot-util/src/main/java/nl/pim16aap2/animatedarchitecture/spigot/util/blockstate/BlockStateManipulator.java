package nl.pim16aap2.animatedarchitecture.spigot.util.blockstate;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class that handles block states.
 * <p>
 * This class contains all the {@link BlockStateHandler}s and applies the correct handler a given block state.
 */
@Singleton
@Flogger
// TODO: Write tests for this class. Use MockBukkit to create a fake server to test manipulating block states.
public abstract class BlockStateManipulator implements IDebuggable
{
    private final Set<BlockStateHandler<?>> blockStateHandlers;

    protected BlockStateManipulator(
        DebuggableRegistry debuggableRegistry,
        DefaultBlockStateHandlers defaultBlockStateHandlers,
        BlockStateHandler<?>... handlers)
    {
        blockStateHandlers = getSortedBlockStateHandlers(defaultBlockStateHandlers, handlers);

        debuggableRegistry.registerDebuggable(this);
    }

    /**
     * Combines the default block state handlers with the provided handlers.
     * <p>
     * The handlers will be sorted by the order of the hierarchy of the block state classes. See
     * {@link #getSortedBlockStateHandlers(Stream)}.
     *
     * @param defaultBlockStateHandlers
     *     The default block state handlers.
     * @param handlers
     *     The additional handlers.
     * @return A set of block state handlers sorted by the hierarchy of the block state classes.
     */
    static Set<BlockStateHandler<?>> getSortedBlockStateHandlers(
        DefaultBlockStateHandlers defaultBlockStateHandlers,
        BlockStateHandler<?>... handlers)
    {
        return getSortedBlockStateHandlers(
            Stream.concat(
                defaultBlockStateHandlers.getDefaultBlockStateTypeHandlers().stream(),
                Stream.of(handlers))
        );
    }

    /**
     * Sorts a stream of block state handlers by the hierarchy of the block state classes.
     * <p>
     * The final set will be sorted by the order of the hierarchy of {@link BlockStateHandler#getBlockStateClass()}. The
     * handlers will be sorted from the most general to the most specific block state classes.
     * <p>
     * For example, given the following block state classes:
     * <ul>
     *     <li>InterfaceA</li>
     *     <li>InterfaceB extends InterfaceA</li>
     *     <li>InterfaceC extends InterfaceA</li>
     *     <li>InterfaceD extends InterfaceB</li>
     * </ul>
     * <p>
     * The order of the handlers will be:
     * <ol>
     *     <li>InterfaceA</li>
     *     <li>InterfaceB, InterfaceC</li>
     *     <li>InterfaceD</li>
     * </ol>
     * <p>
     * If multiple handlers exist on the same level, such as InterfaceB and InterfaceC, they will be sorted
     * first by the depth of the interface in the class hierarchy (see {@link #getInterfaceDepth(Class)}), and then, if
     * the depth is equal, by the (simple) name of the class.
     *
     * @param handlers
     *     The stream of block state handlers to sort and return as a set.
     * @return A set of block state handlers sorted by the hierarchy of the block state classes.
     */
    static Set<BlockStateHandler<?>> getSortedBlockStateHandlers(
        Stream<BlockStateHandler<?>> handlers)
    {
        final TreeSet<BlockStateHandler<?>> set = new TreeSet<>(
            (a, b) ->
            {
                if (a.equals(b))
                    return 0;

                if (a.getBlockStateClass().isAssignableFrom(b.getBlockStateClass()))
                    return -1;

                if (b.getBlockStateClass().isAssignableFrom(a.getBlockStateClass()))
                    return 1;

                final int aDepth = getInterfaceDepth(a.getBlockStateClass());
                final int bDepth = getInterfaceDepth(b.getBlockStateClass());

                if (aDepth < bDepth)
                    return -1;

                if (aDepth > bDepth)
                    return 1;

                return a.getBlockStateClass().getSimpleName().compareTo(b.getBlockStateClass().getSimpleName());
            });

        handlers.forEach(set::add);

        return Collections.unmodifiableSet(new LinkedHashSet<>(set));
    }

    /**
     * Gets the depth of the interface in the class hierarchy.
     * <p>
     * For example, if a class "Impl" implements an interface "Interface0", which extends "Interface1", which extends
     * "Interface2", the depth of "Interface2" in the class hierarchy of "Impl" is 2 and the depth of "Interface1" is
     * 1.
     *
     * @param baseClass
     *     The base class to start from.
     * @return The depth of the interface in the class hierarchy of the base class.
     */
    static int getInterfaceDepth(Class<?> baseClass)
    {
        return findInterfaceDepth(baseClass, 0);
    }

    private static int findInterfaceDepth(@Nullable Class<?> currentClass, int currentDepth)
    {
        if (currentClass == null)
            return currentDepth;

        final Class<?>[] interfaces = currentClass.getInterfaces();
        if (interfaces.length == 0)
            return currentDepth;

        return Stream.of(interfaces).mapToInt(clz -> findInterfaceDepth(clz, currentDepth + 1)).max().orElseThrow();
    }

    /**
     * Applies the block state to the block.
     *
     * @param block
     *     The block to apply the block state to.
     * @param source
     *     The block state to apply.
     */
    public BlockState applyBlockState(BlockState source, Block block)
    {
        return applyBlockState(source, block.getState(), block);
    }

    protected BlockState applyBlockState(BlockState source, BlockState target, Block block)
    {
        // The block state should be applied to the same type of block (as we simply moved the block).
        // Therefore, the classes should be equal.
        if (!target.getClass().equals(source.getClass()))
            throw new IllegalArgumentException(
                "Expected the target block state to be of type " + source.getClass().getSimpleName() +
                    ", but got " + target.getClass().getSimpleName() + " instead.");

        return getBlockStateHandlers(source.getClass())
            .map(handler -> (BlockStateHandler<?>) handler)
            .reduce(
                target,
                (intermediateTarget, handler) -> applyBlockState(handler, source, intermediateTarget, block),
                (a, b) -> b);
    }

    /**
     * Applies the block state to the block.
     *
     * @param handler
     *     The block state handler that will be used to apply the block state.
     *     <p>
     *     It is assumed that it has already been checked that the handler can handle the block state.
     *     <p>
     *     See {@link #getBlockStateHandlers(Class)}.
     * @param source
     *     The source block state to apply to the target block state.
     * @param target
     *     The target block state to apply the source block state to.
     * @param block
     *     The block that is being manipulated. This is used for logging purposes.
     */
    private BlockState applyBlockState(BlockStateHandler<?> handler, BlockState source, BlockState target, Block block)
    {
        try
        {
            return handler.applyBlockState(source, target, block);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log(
                "Failed to apply source block state %s to target block state %s for block %s.",
                source,
                target,
                block
            );
        }
        return target;
    }

    /**
     * Gets an unordered stream of block state handlers that can handle the given block state class.
     * <p>
     * See {@link BlockStateHandler#getBlockStateClass()}.
     *
     * @param blockStateClass
     *     The block state class to get the handlers for.
     * @return An unordered stream of block state handlers that can handle the given block state class.
     */
    Stream<BlockStateHandler<?>> getBlockStateHandlers(Class<?> blockStateClass)
    {
        return blockStateHandlers.stream()
            .filter(handler -> handler.getBlockStateClass().isAssignableFrom(blockStateClass));
    }

    @Override
    public String getDebugInformation()
    {
        return "Registered Handlers:\n" +
            blockStateHandlers
                .stream()
                .map(handler -> handler.getClass().getSimpleName())
                .collect(Collectors.joining("\n- ", "- ", ""));
    }
}
