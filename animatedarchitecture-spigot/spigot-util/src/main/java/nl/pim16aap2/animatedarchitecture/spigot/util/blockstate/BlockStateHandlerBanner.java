package nl.pim16aap2.animatedarchitecture.spigot.util.blockstate;

import org.bukkit.block.Banner;
import org.bukkit.block.Block;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handler for {@link Banner} block states.
 */
@Singleton
final class BlockStateHandlerBanner extends BlockStateHandler<Banner>
{
    @Inject
    BlockStateHandlerBanner()
    {
        super(Banner.class);
    }

    @Override
    protected void applyBlockState(Banner source, Banner target, Block block)
    {
        target.setBaseColor(source.getBaseColor());
        target.setPatterns(source.getPatterns());
        target.update(true, false);
    }
}
