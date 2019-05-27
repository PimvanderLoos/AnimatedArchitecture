package nl.pim16aap2.bigdoors.moveblocks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.nms.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigdoors.nms.FallingBlockFactory_Vall;
import nl.pim16aap2.bigdoors.nms.NMSBlock_Vall;
import nl.pim16aap2.bigdoors.util.MyBlockData;
import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;

public abstract class BlockMover
{
    protected final BigDoors plugin;
    protected final FallingBlockFactory_Vall fabf;
    protected final World world;
    protected List<MyBlockData> savedBlocks;
    protected final Door door;
    protected double time;
    protected boolean instantOpen;
    protected MyBlockFace currentDirection;
    protected RotateDirection openDirection;
    protected int blocksMoved;
    protected int xMin, xMax, yMin;
    protected int yMax, zMin, zMax;

    protected BlockMover(final BigDoors plugin, final World world, final Door door, final double time,
        final boolean instantOpen, final MyBlockFace currentDirection, final RotateDirection openDirection,
        final int blocksMoved)
    {
        plugin.getAutoCloseScheduler().cancelTimer(door.getDoorUID());
        this.plugin = plugin;
        this.world = world;
        this.door = door;
        this.time = time;
        this.instantOpen = instantOpen;
        this.currentDirection = currentDirection;
        this.openDirection = openDirection;
        this.blocksMoved = blocksMoved;
        fabf = plugin.getFABF();
        savedBlocks = new ArrayList<>();

        xMin = door.getMinimum().getBlockX();
        yMin = door.getMinimum().getBlockY();
        zMin = door.getMinimum().getBlockZ();
        xMax = door.getMaximum().getBlockX();
        yMax = door.getMaximum().getBlockY();
        zMax = door.getMaximum().getBlockZ();
    }

    protected void constructFBlocks()
    {
        for (int xAxis = xMin; xAxis <= xMax; ++xAxis)
            for (int yAxis = yMin; yAxis <= yMax; ++yAxis)
                for (int zAxis = zMin; zAxis <= zMax; ++zAxis)
                {
                    Location startLocation = new Location(world, xAxis + 0.5, yAxis, zAxis + 0.5);
                    Block vBlock = world.getBlockAt(xAxis, yAxis, zAxis);

                    if (Util.isAllowedBlock(vBlock))
                    {
                        Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis - 0.020, zAxis + 0.5);
                        // Move the lowest blocks up a little, so the client won't predict they're
                        // touching through the ground, which would make them slower than the rest.
                        if (yAxis == yMin)
                            newFBlockLocation.setY(newFBlockLocation.getY() + .010001);

                        NMSBlock_Vall block = fabf.nmsBlockFactory(world, xAxis, yAxis, zAxis);
                        NMSBlock_Vall block2 = null;
                        boolean canRotate = false;

                        if (openDirection != null && block.canRotate())
                        {
                            block2 = fabf.nmsBlockFactory(world, xAxis, yAxis, zAxis);
                            block2.rotateBlock(openDirection);
                            canRotate = true;
                        }

                        float radius = getRadius(xAxis, yAxis, zAxis);

                        CustomCraftFallingBlock_Vall fBlock = instantOpen ? null :
                            fallingBlockFactory(newFBlockLocation, block);
                        savedBlocks.add(new MyBlockData(fBlock, radius, block2 == null ? block : block2, canRotate,
                                                        startLocation));
                    }
                }
        for (MyBlockData mbd : savedBlocks)
            if (mbd.getBlock() != null)
                mbd.getBlock().deleteOriginalBlock();

        if (instantOpen)
            putBlocks(false);
        else
            animateEntities();
    }

    protected abstract void animateEntities();

    protected abstract float getRadius(int xAxis, int yAxis, int zAxis);

    // Put blocks in their final position.
    // Use onDisable = false to make it safe to use during onDisable().
    // Put the door blocks back, but change their state now.
    public final void putBlocks(boolean onDisable)
    {
        for (MyBlockData savedBlock : savedBlocks)
        {
            Location newPos = getNewLocation(savedBlock.getRadius(), savedBlock.getStartX(), savedBlock.getStartY(),
                                             savedBlock.getStartZ());
            savedBlock.killFBlock();
            savedBlock.getBlock().putBlock(newPos);
            Block b = world.getBlockAt(newPos);
            BlockState bs = b.getState();
            bs.update();
        }

        // Tell the door object it has been opened and what its new coordinates are.
        updateCoords(door, currentDirection, openDirection, blocksMoved);
        if (!onDisable)
            plugin.removeBlockMover(this);

        savedBlocks.clear();

        // Change door availability to true, so it can be opened again.
        // Wait for a bit if instantOpen is enabled.
        int timer = onDisable ? 0 : instantOpen ? 40 : 10 + plugin.getConfigLoader().coolDown() * 20;

        if (timer > 0)
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    plugin.getDatabaseManager().setDoorAvailable(door.getDoorUID());
                }
            }.runTaskLater(plugin, timer);
        else
            plugin.getDatabaseManager().setDoorAvailable(door.getDoorUID());

        if (!onDisable)
            plugin.getAutoCloseScheduler().scheduleAutoClose(door, time, onDisable);
    }

    protected abstract void updateCoords(Door door, MyBlockFace openDirection, RotateDirection upDown, int moved);

    protected abstract Location getNewLocation(double radius, double xAxis, double yAxis, double zAxis);

    // Toggle the open status of a drawbridge.
    protected final void toggleOpen(Door door)
    {
        door.setOpenStatus(!door.isOpen());
    }

    protected final CustomCraftFallingBlock_Vall fallingBlockFactory(Location loc, NMSBlock_Vall block)
    {
        CustomCraftFallingBlock_Vall entity = fabf.fallingBlockFactory(plugin, loc, block);
        Entity bukkitEntity = (Entity) entity;
        bukkitEntity.setCustomName("BigDoorsEntity");
        bukkitEntity.setCustomNameVisible(false);
        return entity;
    }

    public final long getDoorUID()
    {
        return door.getDoorUID();
    }

    public final Door getDoor()
    {
        return door;
    }
}