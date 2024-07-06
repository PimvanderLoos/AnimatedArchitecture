package nl.pim16aap2.animatedarchitecture.spigot.v1_21.blockstate;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.BlockStateHandler;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link BlockStateHandler} for {@link TileState}s for Spigot 1.21.
 */
@Flogger
@Singleton
final class BlockStateHandlerTileState_V1_21 extends BlockStateHandler<TileState>
{
    @Inject
    BlockStateHandlerTileState_V1_21()
    {
        super(TileState.class);
    }

    @Override
    protected void applyBlockState(TileState source, TileState target, Block block)
    {
        source.getPersistentDataContainer().copyTo(target.getPersistentDataContainer(), true);
        target.update(true, false);
    }
}
