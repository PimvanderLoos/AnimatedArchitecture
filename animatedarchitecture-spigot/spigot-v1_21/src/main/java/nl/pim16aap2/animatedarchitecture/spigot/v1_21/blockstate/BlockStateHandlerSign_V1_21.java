package nl.pim16aap2.animatedarchitecture.spigot.v1_21.blockstate;

import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.BlockStateHandler;
import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.BlockStateHandlerSign;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import javax.inject.Inject;

/**
 * Implementation of {@link BlockStateHandler} for {@link Sign}s.
 */
public class BlockStateHandlerSign_V1_21 extends BlockStateHandlerSign
{
    @Inject
    BlockStateHandlerSign_V1_21()
    {
        super();
    }

    @Override
    protected void applyBlockState(Sign source, Sign target, Block block)
    {
        super.applyBlockState(source, target, block);
        target.setWaxed(source.isWaxed());
        target.update(true, false);
    }
}
