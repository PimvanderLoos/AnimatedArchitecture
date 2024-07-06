package nl.pim16aap2.animatedarchitecture.spigot.v1_20.blockstate;

import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.BlockStateManipulator;
import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.DefaultBlockStateHandlers;

import javax.inject.Inject;

/**
 * Represents a {@link BlockStateManipulator} for Spigot 1.20.
 */
public class BlockStateManipulator_V1_20 extends BlockStateManipulator
{
    @Inject
    BlockStateManipulator_V1_20(
        DebuggableRegistry debuggableRegistry,
        DefaultBlockStateHandlers defaultBlockStateHandlers,
        BlockStateHandlerSign_V1_20 blockStateHandlerSign)
    {
        super(
            debuggableRegistry,
            defaultBlockStateHandlers,
            blockStateHandlerSign
        );
    }
}
