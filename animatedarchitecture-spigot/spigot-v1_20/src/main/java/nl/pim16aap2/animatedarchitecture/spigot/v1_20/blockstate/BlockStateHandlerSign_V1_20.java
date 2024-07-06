package nl.pim16aap2.animatedarchitecture.spigot.v1_20.blockstate;

import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.BlockStateHandler;
import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.BlockStateHandlerSign;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link BlockStateHandler} for {@link Sign}s.
 */
@Singleton
final class BlockStateHandlerSign_V1_20 extends BlockStateHandlerSign
{
    @Inject
    BlockStateHandlerSign_V1_20()
    {
        super();
    }

    @Override
    protected void applyBlockState(Sign source, Sign target, Block block)
    {
        super.applyBlockState(source, target, block);
        target.update(true, false);
    }
}
