package nl.pim16aap2.animatedarchitecture.spigot.util.blockstate;

import org.bukkit.block.Block;
import org.bukkit.block.Container;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link BlockStateHandler} for {@link Container}s.
 */
@Singleton
final class BlockStateHandlerContainer extends BlockStateHandler<Container>
{
    @Inject
    BlockStateHandlerContainer()
    {
        super(Container.class);
    }

    @Override
    protected void applyBlockState(Container source, Container target, Block block)
    {
        target.getInventory().setContents(source.getInventory().getContents());
    }
}
