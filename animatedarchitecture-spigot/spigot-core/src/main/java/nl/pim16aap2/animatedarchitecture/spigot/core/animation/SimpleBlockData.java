package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import com.google.common.flogger.StackSize;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;

@Flogger
public class SimpleBlockData implements IAnimatedBlockData
{
    private final IExecutor executor;
    @Getter
    private final BlockData blockData;
    @Getter
    private final Display.Brightness brightness;
    private final Vector3Di originalPosition;
    private final World bukkitWorld;

    SimpleBlockData(IExecutor executor, World bukkitWorld, Vector3Di position)
    {
        this.executor = executor;
        this.originalPosition = position;
        this.bukkitWorld = bukkitWorld;

        final Block block = bukkitWorld.getBlockAt(position.x(), position.y(), position.z());
        this.blockData = block.getBlockData();
//        this.brightness = new Display.Brightness(block.getLightFromBlocks(), block.getLightFromSky());
        this.brightness = new Display.Brightness(block.getLightLevel(), block.getLightFromSky());
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

//        final Vector3Di loci = new Vector3Di(loc);
//        this.bukkitWorld.getBlockAt(loci.x(), loci.y(), loci.z()).setBlockData(this.getBlockData());
        this.bukkitWorld.getBlockAt(originalPosition.x(), originalPosition.y(), originalPosition.z())
                        .setBlockData(this.getBlockData());
    }

    @Override
    public void deleteOriginalBlock(boolean applyPhysics)
    {
        if (!executor.isMainThread())
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Caught async block placement! THIS IS A BUG!");
            return;
        }
        this.bukkitWorld.getBlockAt(originalPosition.x(), originalPosition.y(), originalPosition.z())
                        .setType(Material.AIR);
    }
}
