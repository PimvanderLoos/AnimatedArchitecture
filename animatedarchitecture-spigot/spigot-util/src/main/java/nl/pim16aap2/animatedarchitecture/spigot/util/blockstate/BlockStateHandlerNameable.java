package nl.pim16aap2.animatedarchitecture.spigot.util.blockstate;

import org.bukkit.Nameable;
import org.bukkit.block.Block;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handler for {@link Nameable} block states.
 */
@Singleton
final class BlockStateHandlerNameable extends BlockStateHandler<Nameable>
{
    @Inject
    BlockStateHandlerNameable()
    {
        super(Nameable.class);
    }

    @Override
    protected void applyBlockState(Nameable source, Nameable target, Block block)
    {
        target.setCustomName(source.getCustomName());
    }
}
