package nl.pim16aap2.bigdoors.moveblocks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.nms.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigdoors.nms.FallingBlockFactory_Vall;
import nl.pim16aap2.bigdoors.nms.NMSBlock_Vall;
import nl.pim16aap2.bigdoors.util.MyBlockData;
import nl.pim16aap2.bigdoors.util.Util;

public class FlagMover implements BlockMover
{
    private boolean                     NS;
    private FallingBlockFactory_Vall  fabf;
    private Door                      door;
    private double                    time;
    private World                    world;
    private BigDoors                plugin;
    private int                   tickRate;
    private boolean            instantOpen;
    private int           xMin, xMax, yMin;
    private int           yMax, zMin, zMax;
    private List<MyBlockData> savedBlocks = new ArrayList<>();

    public FlagMover(BigDoors plugin, World world, double time, Door door)
    {
        this.plugin = plugin;
        this.world  = world;
        this.door   = door;
        fabf        = plugin.getFABF();

        xMin     = door.getMinimum().getBlockX();
        yMin     = door.getMinimum().getBlockY();
        zMin     = door.getMinimum().getBlockZ();
        xMax     = door.getMaximum().getBlockX();
        yMax     = door.getMaximum().getBlockY();
        zMax     = door.getMaximum().getBlockZ();
        int xLen = Math.abs(xMax - xMin) + 1;
        int zLen = Math.abs(zMax - zMin) + 1;
        NS       = zLen > xLen ? true : false;

        double speed = 1;
        this.time    = time;
        tickRate  = Util.tickRateFromSpeed(speed);
        tickRate  = 3;

        int index = 0;
        int yAxis = yMin;
        do
        {
            int zAxis = zMin;
            do
            {
                for (int xAxis = xMin; xAxis <= xMax; xAxis++)
                {
                    Location startLocation = new Location(world, xAxis + 0.5, yAxis, zAxis + 0.5);
                    Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis - 0.020, zAxis + 0.5);
                    // Move the lowest blocks up a little, so the client won't predict they're touching through the ground, which would make them slower than the rest.
                    if (yAxis == yMin)
                        newFBlockLocation.setY(newFBlockLocation.getY() + .010001);
                    Block vBlock  = world.getBlockAt(xAxis, yAxis, zAxis);
                    Material mat  = vBlock.getType();
                    if (!mat.equals(Material.AIR))
                    {

                        NMSBlock_Vall block  = fabf.nmsBlockFactory(world, xAxis, yAxis, zAxis);

                        // Certain blocks cannot be used the way normal blocks can (heads, (ender) chests etc).
                        if (Util.isAllowedBlock(vBlock))
                            vBlock.setType(Material.AIR);
                        else
                            mat = Material.AIR;

                        CustomCraftFallingBlock_Vall fBlock = null;
                        if (!instantOpen)
                             fBlock = fallingBlockFactory(newFBlockLocation, mat, block);
                        savedBlocks.add(index, new MyBlockData(mat, fBlock, 0, block, 0, startLocation));
                    }
                    else
                        savedBlocks.add(index, new MyBlockData(Material.AIR));
                    ++index;
                }
                ++zAxis;
            }
            while (zAxis <= zMax);
            ++yAxis;
        }
        while (yAxis <= yMax);

        if (!instantOpen)
            rotateEntities();
        else
            putBlocks(false);
    }

    // Put the door blocks back, but change their state now.
    @Override
    public void putBlocks(boolean onDisable)
    {
        int index = 0;
        double yAxis = yMin;
        do
        {
            double zAxis = zMin;
            do
            {
                for (int xAxis = xMin; xAxis <= xMax; ++xAxis)
                {
                    Material mat    = savedBlocks.get(index).getMat();
                    if (!mat.equals(Material.AIR))
                    {
                        Location newPos = getNewLocation(xAxis, yAxis, zAxis);

                        if (!instantOpen)
                            savedBlocks.get(index).getFBlock().remove();

                        if (!savedBlocks.get(index).getMat().equals(Material.AIR))
                        {
                            savedBlocks.get(index).getBlock().putBlock(newPos);

                            Block b = world.getBlockAt(newPos);
                            BlockState bs = b.getState();
                            bs.update();
                        }
                    }
                    ++index;
                }
                ++zAxis;
            }
            while (zAxis <= zMax);
            ++yAxis;
        }
        while (yAxis <= yMax);
        savedBlocks.clear();

        // Tell the door object it has been opened and what its new coordinates are.
        if (!onDisable)
            plugin.removeBlockMover(this);

        // Change door availability to true, so it can be opened again.
        // Wait for a bit if instantOpen is enabled.
        int timer = onDisable   ?  0 :
                    instantOpen ? 40 : plugin.getConfigLoader().coolDown() * 20;

        if (timer > 0)
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    plugin.getCommander().setDoorAvailable(door.getDoorUID());
                }
            }.runTaskLater(plugin, timer);
        else
            plugin.getCommander().setDoorAvailable(door.getDoorUID());

        if (!onDisable)
            goAgain();
    }

    private void goAgain()
    {
        return;
    }

    private Location getNewLocation(double xAxis, double yAxis, double zAxis)
    {
        return new Location(world, xAxis, yAxis, zAxis);
    }

    // Method that takes care of the rotation aspect.
    private void rotateEntities()
    {
        new BukkitRunnable()
        {
            double counter   = 0;
            int endCount     = (int) (20 / tickRate * time);
            int totalTicks   = (int) (endCount * 1.1);
            long startTime   = System.nanoTime();
            long lastTime;
            long currentTime = System.nanoTime();

            @Override
            public void run()
            {
//                if (counter == 0 || (counter < endCount - 27 / tickRate && counter % (5 * tickRate / 4) == 0))
//                    Util.playSound(door.getEngine(), "bd.dragging2", 0.5f, 0.6f);

                lastTime = currentTime;
                currentTime = System.nanoTime();
                long msSinceStart = (currentTime - startTime) / 1000000;
                if (!plugin.getCommander().isPaused())
                    counter = msSinceStart / (50 * tickRate);
                else
                    startTime += currentTime - lastTime;

                if (!plugin.getCommander().canGo() || !door.canGo() || counter > totalTicks)
                {
                    Util.playSound(door.getEngine(), "bd.thud", 2f, 0.15f);
                    for (int idx = 0; idx < savedBlocks.size(); ++idx)
                        if (!savedBlocks.get(idx).getMat().equals(Material.AIR))
                            savedBlocks.get(idx).getFBlock().setVelocity(new Vector(0D, 0D, 0D));
                    putBlocks(false);
                    cancel();
                }
                else
                    for (MyBlockData block : savedBlocks)
                        if (!block.getMat().equals(Material.AIR))
                        {
                            double xOff = 0;
                            double zOff = 0;
                            if (NS)
                            {
                                xOff = 3 - 1 / (tickRate / 20);
                                int distanceToEng = Math.abs(block.getStartLocation().getBlockZ() - door.getEngine().getBlockZ());
                                if (distanceToEng > 0)
                                {
                                    double offset = Math.sin(0.5 * Math.PI * (counter * tickRate / 20) + distanceToEng);
                                    double maxVal   = 0.25   *   distanceToEng;
                                    maxVal = maxVal > 0.75   ? 0.75   : maxVal;
                                    xOff   = offset > maxVal ? maxVal : offset;
                                }
                            }
                            else
                            {
                                int distanceToEng = Math.abs(block.getStartLocation().getBlockX() - door.getEngine().getBlockX());
                                if (distanceToEng > 0)
                                {
                                    double offset = Math.sin(0.5 * Math.PI * (counter * tickRate / 20) + distanceToEng);
                                    double maxVal   = 0.25   *   distanceToEng;
                                    maxVal = maxVal > 0.75   ? 0.75   : maxVal;
                                    zOff   = offset > maxVal ? maxVal : offset;
                                }
                            }
                            Location loc = block.getStartLocation();
                            loc.add(xOff, 0, zOff);
                            Vector vec   = loc.toVector().subtract(block.getFBlock().getLocation().toVector());
                            vec.multiply(0.101);
                            block.getFBlock().setVelocity(vec);
                        }
            }
        }.runTaskTimer(plugin, 14, tickRate);
    }

    private CustomCraftFallingBlock_Vall fallingBlockFactory(Location loc, Material mat, NMSBlock_Vall block)
    {
        CustomCraftFallingBlock_Vall entity = fabf.fallingBlockFactory(plugin, loc, block, mat);
        Entity bukkitEntity = (Entity) entity;
        bukkitEntity.setCustomName("BigDoorsEntity");
        bukkitEntity.setCustomNameVisible(false);
        return entity;
    }

    @Override
    public long getDoorUID()
    {
        return door.getDoorUID();
    }

    @Override
    public Door getDoor()
    {
        return door;
    }
}
