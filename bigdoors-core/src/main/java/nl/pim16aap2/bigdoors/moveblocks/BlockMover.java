package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICustomCraftFallingBlock;
import nl.pim16aap2.bigdoors.api.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.INMSBlock;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.Mutable;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BlockMover
{
    protected final BigDoors plugin;
    protected final World world;
    protected final DoorBase door;
    protected final IFallingBlockFactory fabf;
    protected double time;
    protected boolean instantOpen;
    protected RotateDirection openDirection;
    protected List<PBlockData> savedBlocks;
    protected AtomicBoolean isAborted = new AtomicBoolean(false);
    protected PBlockFace currentDirection;
    protected int blocksMoved;
    protected int xMin, xMax, yMin;
    protected int yMax, zMin, zMax;


    protected BlockMover(final BigDoors plugin, final World world, final DoorBase door, final double time,
                         final boolean instantOpen, final PBlockFace currentDirection,
                         final RotateDirection openDirection,
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

    public void abort()
    {
        isAborted.set(true);
    }

    protected void constructFBlocks()
    {
        for (int xAxis = xMin; xAxis <= xMax; ++xAxis)
            for (int yAxis = yMin; yAxis <= yMax; ++yAxis)
                for (int zAxis = zMin; zAxis <= zMax; ++zAxis)
                {
                    Location startLocation = new Location(world, xAxis + 0.5, yAxis, zAxis + 0.5);
                    Block vBlock = world.getBlockAt(xAxis, yAxis, zAxis);

                    if (SpigotUtil.isAllowedBlock(vBlock))
                    {
                        Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis - 0.020, zAxis + 0.5);
                        // Move the lowest blocks up a little, so the client won't predict they're
                        // touching through the ground, which would make them slower than the rest.
                        if (yAxis == yMin)
                            newFBlockLocation.setY(newFBlockLocation.getY() + .010001);

                        INMSBlock block = fabf.nmsBlockFactory(world, xAxis, yAxis, zAxis);
                        INMSBlock block2 = null;
                        boolean canRotate = false;

                        if (openDirection != null && block.canRotate())
                        {
                            block2 = fabf.nmsBlockFactory(world, xAxis, yAxis, zAxis);
                            block2.rotateBlock(openDirection);
                            canRotate = true;
                        }

                        float radius = getRadius(xAxis, yAxis, zAxis);
                        float startAngle = getStartAngle(xAxis, yAxis, zAxis);

                        ICustomCraftFallingBlock fBlock = instantOpen ? null :
                                                          fallingBlockFactory(newFBlockLocation, block);
                        savedBlocks.add(new PBlockData(fBlock, radius, block2 == null ? block : block2, canRotate,
                                                       startLocation, startAngle));
                    }
                }
        for (PBlockData mbd : savedBlocks)
            if (mbd.getBlock() != null)
                mbd.getBlock().deleteOriginalBlock();

        if (instantOpen)
            putBlocks(false);
        else
            animateEntities();
    }

    protected abstract void animateEntities();

    // Can be overridden to get the radius of the block at the given coordinates.
    protected float getRadius(int xAxis, int yAxis, int zAxis)
    {
        return -1;
    }

    // Can be overridden to get the start angle of the block at the given coordinates.
    protected float getStartAngle(int xAxis, int yAxis, int zAxis)
    {
        return -1;
    }

    // Put blocks in their final position.
    // Use onDisable = false to make it safe to use during onDisable().
    // Put the door blocks back, but change their state now.
    public final void putBlocks(boolean onDisable)
    {
        for (PBlockData savedBlock : savedBlocks)
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

        if (!onDisable)
        {
            int delay = Math.max(plugin.getMinimumDoorDelay(), plugin.getConfigLoader().coolDown() * 20);
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    plugin.getDatabaseManager().setDoorAvailable(door.getDoorUID());
                    plugin.getAutoCloseScheduler().scheduleAutoClose(door, time, instantOpen);
                }
            }.runTaskLater(plugin, delay);
        }
        else
            plugin.getDatabaseManager().setDoorAvailable(door.getDoorUID());
    }

    private void updateCoords(DoorBase door, PBlockFace openDirection, RotateDirection rotateDirection,
                              int moved)
    {
        Location newMin = new Location(world, 0, 0, 0);
        Location newMax = new Location(world, 0, 0, 0);
        Mutable<PBlockFace> newEngineSide = new Mutable<>(null);

        door.getNewLocations(openDirection, rotateDirection, newMin, newMax, moved, newEngineSide);

        if (newMin.equals(door.getMinimum()) && newMax.equals(door.getMaximum()))
            return;

        door.setMaximum(newMax);
        door.setMinimum(newMin);

        if (newEngineSide.getVal() != null)
            door.setEngineSide(newEngineSide.getVal());

        toggleOpen(door);
        plugin.getDatabaseManager().updateDoorCoords(door.getDoorUID(), door.isOpen(), newMin.getBlockX(),
                                                     newMin.getBlockY(), newMin.getBlockZ(), newMax.getBlockX(),
                                                     newMax.getBlockY(), newMax.getBlockZ(), newEngineSide.getVal());
    }

    protected abstract Location getNewLocation(double radius, double xAxis, double yAxis, double zAxis);

    // Toggle the open status of a drawbridge.
    private void toggleOpen(DoorBase door)
    {
        door.setOpenStatus(!door.isOpen());
    }

    protected final ICustomCraftFallingBlock fallingBlockFactory(Location loc, INMSBlock block)
    {
        ICustomCraftFallingBlock entity = fabf.fallingBlockFactory(loc, block);
        Entity bukkitEntity = (Entity) entity;
        bukkitEntity.setCustomName("BigDoorsEntity");
        bukkitEntity.setCustomNameVisible(false);
        return entity;
    }

    public final long getDoorUID()
    {
        return door.getDoorUID();
    }

    public final DoorBase getDoor()
    {
        return door;
    }
}
