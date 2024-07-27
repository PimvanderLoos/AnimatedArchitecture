package nl.pim16aap2.animatedarchitecture.spigot.v1_21.blockstate;

import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.BlockStateManipulator;
import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.DefaultBlockStateHandlers;

import javax.inject.Inject;

/**
 * Represents a {@link BlockStateManipulator} for Spigot 1.21.
 */
public final class BlockStateManipulator_V1_21 extends BlockStateManipulator
{
    @Inject
    BlockStateManipulator_V1_21(
        DebuggableRegistry debuggableRegistry,
        DefaultBlockStateHandlers defaultBlockStateHandlers,
        BlockStateHandlerSign_V1_21 blockStateHandlerSign,
        BlockStateHandlerTileState_V1_21 blockStateHandlerTileState)
    {
        super(
            debuggableRegistry,
            defaultBlockStateHandlers,
            // Add this one manually instead of using Dagger because Dagger gets confused about the BaseSpawner class.
            new BlockStateHandlerBaseSpawner_V1_21(),
            blockStateHandlerSign,
            blockStateHandlerTileState
        );
    }
}
