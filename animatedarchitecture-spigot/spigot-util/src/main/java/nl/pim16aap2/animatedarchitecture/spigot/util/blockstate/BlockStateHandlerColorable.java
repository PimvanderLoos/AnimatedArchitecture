package nl.pim16aap2.animatedarchitecture.spigot.util.blockstate;

import org.bukkit.block.Block;
import org.bukkit.material.Colorable;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handler for {@link Colorable} block states.
 */
@Singleton
final class BlockStateHandlerColorable extends BlockStateHandler<Colorable>
{
    @Inject
    BlockStateHandlerColorable()
    {
        super(Colorable.class);
    }

    @Override
    protected void applyBlockState(Colorable source, Colorable target, Block block)
    {
        target.setColor(source.getColor());
    }
}
