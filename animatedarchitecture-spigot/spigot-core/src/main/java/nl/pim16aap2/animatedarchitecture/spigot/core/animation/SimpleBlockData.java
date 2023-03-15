package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import com.google.common.flogger.StackSize;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockHook;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

@Flogger
public class SimpleBlockData implements IAnimatedBlockData
{
    private final IExecutor executor;
    @Getter
    private final BlockData blockData;
    private final AnimatedBlockDisplay animatedBlock;
    private final Vector3Di originalPosition;
    private final World bukkitWorld;

    SimpleBlockData(AnimatedBlockDisplay animatedBlock, IExecutor executor, World bukkitWorld, Vector3Di position)
    {
        this.executor = executor;
        this.animatedBlock = animatedBlock;
        this.originalPosition = position;
        this.bukkitWorld = bukkitWorld;
        this.blockData = bukkitWorld.getBlockAt(position.x(), position.y(), position.z()).getBlockData();
    }

    @Override
    public boolean canRotate()
    {
        return false;
    }

    @Override
    public boolean rotateBlock(MovementDirection movementDirection)
    {
        return false;
    }

    @Override
    public void putBlock(IVector3D loc)
    {
        if (!executor.isMainThread())
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Caught async block placement! THIS IS A BUG!");
            return;
        }

        animatedBlock.forEachHook("preBlockPlace", IAnimatedBlockHook::preBlockPlace);
        final Vector3Di loci = loc.floor().toInteger();
        this.bukkitWorld.getBlockAt(loci.x(), loci.y(), loci.z()).setBlockData(this.getBlockData());
        animatedBlock.forEachHook("postBlockPlace", IAnimatedBlockHook::postBlockPlace);
    }

    @Override
    public void deleteOriginalBlock(boolean applyPhysics)
    {
        if (!executor.isMainThread())
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Caught async block placement! THIS IS A BUG!");
            return;
        }

        animatedBlock.forEachHook("prePutBlock", IAnimatedBlockHook::preDeleteOriginalBlock);
        this.bukkitWorld.getBlockAt(originalPosition.x(), originalPosition.y(), originalPosition.z())
                        .setType(Material.AIR, applyPhysics);
        animatedBlock.forEachHook("postPutBlock", IAnimatedBlockHook::postDeleteOriginalBlock);
    }
}
