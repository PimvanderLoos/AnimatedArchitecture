package nl.pim16aap2.animatedarchitecture.spigot.v1_21;

import nl.pim16aap2.animatedarchitecture.spigot.util.api.BlockAnalyzerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.ISpigotSubPlatform;
import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.BlockStateManipulator;
import nl.pim16aap2.animatedarchitecture.spigot.v1_21.blockstate.BlockStateManipulator_V1_21;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents a sub-platform for Spigot 1.21.
 */
@Singleton
public final class SubPlatform_V1_21 implements ISpigotSubPlatform
{
    private final BlockAnalyzer_V1_21 blockAnalyzer;
    private final BlockStateManipulator_V1_21 blockStateManipulator;

    @Inject
    SubPlatform_V1_21(
        BlockAnalyzer_V1_21 blockAnalyzer,
        BlockStateManipulator_V1_21 blockStateManipulator)
    {
        this.blockAnalyzer = blockAnalyzer;
        this.blockStateManipulator = blockStateManipulator;
    }

    @Override
    public BlockAnalyzerSpigot getBlockAnalyzer()
    {
        return blockAnalyzer;
    }

    @Override
    public BlockStateManipulator getBlockStateManipulator()
    {
        return blockStateManipulator;
    }
}
