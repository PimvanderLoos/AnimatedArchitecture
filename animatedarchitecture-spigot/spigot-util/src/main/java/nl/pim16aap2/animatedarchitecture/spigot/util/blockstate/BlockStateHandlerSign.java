package nl.pim16aap2.animatedarchitecture.spigot.util.blockstate;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * The base handler for {@link Sign} block states.
 * <p>
 * This class is responsible for applying the block state of a source {@link Sign} to a target {@link Sign}.
 * <p>
 * This class is abstract because different versions of Minecraft have additional properties that need to be set.
 */
@Singleton
public abstract class BlockStateHandlerSign extends BlockStateHandler<Sign>
{
    private static final List<Side> SIDES = List.of(Side.values());

    @Inject
    protected BlockStateHandlerSign()
    {
        super(Sign.class);
    }

    /**
     * Applies the block state of the source block state to the target block state.
     * <p>
     * Note: This method does not update the block state of the target block state. This is left for subclasses to
     * implement.
     *
     * @param source
     *     The source block state to apply to the target block state.
     * @param target
     *     The target block state to apply the source block state to.
     * @param block
     */
    @Override
    protected void applyBlockState(Sign source, Sign target, Block block)
    {
        SIDES.forEach(side -> applyBlockState(source.getSide(side), target.getSide(side)));
    }

    private void applyBlockState(SignSide source, SignSide target)
    {
        final String[] lines = source.getLines();
        for (int i = 0; i < lines.length; i++)
            target.setLine(i, lines[i]);

        target.setGlowingText(source.isGlowingText());
    }
}
