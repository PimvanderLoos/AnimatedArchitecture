package nl.pim16aap2.bigDoors.moveBlocks;

import com.cryptomorin.xseries.XMaterial;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.MyBlockFace;
import nl.pim16aap2.bigDoors.util.Util;
import nl.pim16aap2.bigDoors.util.Vector3D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;

import javax.annotation.Nullable;

public abstract class BlockMover
{
    private final BigDoors plugin;
    private final @Nullable Door door;

    protected BlockMover(BigDoors plugin, @Nullable Door door)
    {
        this.plugin = plugin;
        this.door = door;
        if (door == null)
            return;

        plugin.getAutoCloseScheduler().cancelTimer(door.getDoorUID());
        preprocess();
    }

    private void preprocess()
    {
        if (this.door == null)
            return;

        final Location powerBlockLoc = door.getPowerBlockLoc();
        final Location min = door.getMinimum();
        final Location max = door.getMaximum();

        if (!Util.isPosInCuboid(powerBlockLoc, min.clone().add(-1, -1, -1), max.clone().add(1, 1, 1)))
            return;

        for (MyBlockFace blockFace : MyBlockFace.getValues())
        {
            final Vector3D vec = MyBlockFace.getDirection(blockFace);
            final Location newLocation = powerBlockLoc.clone().add(vec.getX(), vec.getY(), vec.getZ());

            if (!Util.isPosInCuboid(newLocation, min, max))
                continue;

            final Block block = newLocation.getBlock();
            if (block.getPistonMoveReaction() == PistonMoveReaction.BREAK)
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, block::breakNaturally, 1L);
        }

        for (Entity entity : powerBlockLoc.getWorld().getNearbyEntities(powerBlockLoc, 1.1, 1.1, 1.1))
        {
            if (entity instanceof ItemFrame && Util.isPosInCuboid(entity.getLocation(), min, max))
            {
                final ItemFrame itemFrame = (ItemFrame) entity;
                powerBlockLoc.getWorld().dropItemNaturally(itemFrame.getLocation(), itemFrame.getItem());
                powerBlockLoc.getWorld().dropItemNaturally(itemFrame.getLocation(), XMaterial.ITEM_FRAME.parseItem());
                itemFrame.remove();
            }
        }
    }

    // Put blocks in their final position.
    // Use onDisable = false to make it safe to use during onDisable().
    public abstract void putBlocks(boolean onDisable);

    public abstract long getDoorUID();

    public abstract Door getDoor();

    public abstract void cancel(boolean onDisable);

    /**
     * Gets the number of ticks the door should to the delay to make sure the second
     * toggle of a button doesn't toggle the door again.
     *
     * @param endCount The number of ticks the animation took.
     * @return The number of ticks to wait before a button cannot toggle the door
     *         again.
     */
    public int buttonDelay(final int endCount)
    {
        return Math.max(0, 17 - endCount);
    }
}
