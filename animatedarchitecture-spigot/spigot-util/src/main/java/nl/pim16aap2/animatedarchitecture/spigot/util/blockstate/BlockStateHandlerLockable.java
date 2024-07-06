package nl.pim16aap2.animatedarchitecture.spigot.util.blockstate;

import org.bukkit.block.Block;
import org.bukkit.block.Lockable;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link BlockStateHandler} for {@link Lockable}s.
 */
@Singleton
final class BlockStateHandlerLockable extends BlockStateHandler<Lockable>
{
    @Inject
    BlockStateHandlerLockable()
    {
        super(Lockable.class);
    }

    @Override
    protected void applyBlockState(Lockable source, Lockable target, Block block)
    {
        target.setLock(source.getLock());
    }
}
