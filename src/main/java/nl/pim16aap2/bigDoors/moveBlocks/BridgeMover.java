package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_Vall;
import nl.pim16aap2.bigDoors.NMS.NMSBlock_Vall;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocation;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationEast;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationNorth;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationSouth;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationWest;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.MyBlockData;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;

public class BridgeMover implements BlockMover
{
    private World                    world;
    private BigDoors                plugin;
    private int                   tickRate;
    private double              multiplier;
    private int                     dx, dz;
    private double                    time;
    private FallingBlockFactory_Vall  fabf;
    private boolean                     NS;
    private GetNewLocation             gnl;
    private Door                      door;
    private RotateDirection         upDown;
    private DoorDirection       engineSide;
    private double              endStepSum;
    private boolean            instantOpen;
    private Location          turningPoint;
    private double            startStepSum;
    private DoorDirection    openDirection;
    private Location         pointOpposite;
    private int             stepMultiplier;
    private int           xMin, yMin, zMin;
    private int           xMax, yMax, zMax;
    private List<MyBlockData> savedBlocks = new ArrayList<MyBlockData>();

    @SuppressWarnings("deprecation")
    public BridgeMover(BigDoors plugin, World world, double time, Door door, RotateDirection upDown,
            DoorDirection openDirection, boolean instantOpen)
    {
        fabf = plugin.getFABF();
        engineSide = door.getEngSide();
        NS = engineSide == DoorDirection.NORTH || engineSide == DoorDirection.SOUTH;
        this.door = door;
        this.world = world;
        this.plugin = plugin;
        this.upDown = upDown;
        this.instantOpen = instantOpen;
        this.openDirection = openDirection;

        xMin = door.getMinimum().getBlockX();
        yMin = door.getMinimum().getBlockY();
        zMin = door.getMinimum().getBlockZ();
        xMax = door.getMaximum().getBlockX();
        yMax = door.getMaximum().getBlockY();
        zMax = door.getMaximum().getBlockZ();
        int xLen = Math.abs(door.getMaximum().getBlockX() - door.getMinimum().getBlockX());
        int yLen = Math.abs(door.getMaximum().getBlockY() - door.getMinimum().getBlockY());
        int zLen = Math.abs(door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ());
        int doorSize = Math.max(xLen, Math.max(yLen, zLen)) + 1;
        double vars[] = Util.calculateTimeAndTickRate(doorSize, time, plugin.getConfigLoader().dbMultiplier(), 5.2);
        this.time = vars[0];
        tickRate = (int) vars[1];
        multiplier = vars[2];

        // Regarding dx, dz. These variables determine whether loops get incremented (1) or decremented (-1)
        // When looking in the direction of the opposite point from the engine side, the blocks should get
        // Processed from left to right and from the engine to the opposite.
        /* Pointing:   Degrees:
         * UP            0 or 360
         * EAST         90
         * WEST        270
         * NORTH       270
         * SOUTH        90
         */
        startStepSum   = -1;
        stepMultiplier = -1;

        // Calculate turningpoint and pointOpposite.
        switch (engineSide)
        {
        case NORTH:
            // When EngineSide is North, x goes from low to high and z goes from low to high
            turningPoint = new Location(world, xMin, yMin, zMin);
            dx =  1;
            dz =  1;

            if (upDown.equals(RotateDirection.UP))
            {
                pointOpposite  = new Location(world, xMax, yMin, zMax);
                startStepSum   =  Math.PI / 2;
                stepMultiplier = -1;
            }
            else
            {
                pointOpposite  = new Location(world, xMax, yMax, zMin);
                if (openDirection.equals(DoorDirection.NORTH))
                    stepMultiplier = -1;
                else if (openDirection.equals(DoorDirection.SOUTH))
                    stepMultiplier =  1;
            }
            break;

        case SOUTH:
            // When EngineSide is South, x goes from high to low and z goes from high to low
            turningPoint = new Location(world, xMax, yMin, zMax);
            dx = -1;
            dz = -1;

            if (upDown.equals(RotateDirection.UP))
            {
                pointOpposite  = new Location(world, xMin, yMin, zMin);
                startStepSum   = -Math.PI / 2;
                stepMultiplier =  1;
            }
            else
            {
                pointOpposite  = new Location(world, xMin, yMax, zMax);
                if (openDirection.equals(DoorDirection.NORTH))
                    stepMultiplier = -1;
                else if (openDirection.equals(DoorDirection.SOUTH))
                    stepMultiplier =  1;
            }
            break;

        case EAST:
            // When EngineSide is East, x goes from high to low and z goes from low to high
            turningPoint = new Location(world, xMax, yMin, zMin);
            dx = -1;
            dz =  1;

            if (upDown.equals(RotateDirection.UP))
            {
                pointOpposite  = new Location(world, xMin, yMin, zMax);
                startStepSum   = -Math.PI / 2;
                stepMultiplier =  1;
            }
            else
            {
                pointOpposite  = new Location(world, xMax, yMax, zMax);
                if (openDirection.equals(DoorDirection.EAST))
                    stepMultiplier =  1;
                else if (openDirection.equals(DoorDirection.WEST))
                    stepMultiplier = -1;
            }
            break;

        case WEST:
            // When EngineSide is West, x goes from low to high and z goes from high to low
            turningPoint = new Location(world, xMin, yMin, zMax);
            dx =  1;
            dz = -1;

            if (upDown.equals(RotateDirection.UP))
            {
                pointOpposite  = new Location(world, xMax, yMin, zMin);
                startStepSum   =  Math.PI / 2;
                stepMultiplier = -1;
            }
            else
            {
                pointOpposite  = new Location(world, xMin, yMax, zMin);
                if (openDirection.equals(DoorDirection.EAST))
                    stepMultiplier =  1;
                else if (openDirection.equals(DoorDirection.WEST))
                    stepMultiplier = -1;
            }
            break;
        }

        endStepSum   = upDown.equals(RotateDirection.UP)   ? 0 : Math.PI / 2 * stepMultiplier;
        startStepSum = upDown.equals(RotateDirection.DOWN) ? 0 : startStepSum;

        int index = 0;
        double xAxis = turningPoint.getX();
        do
        {
            double zAxis = turningPoint.getZ();
            do
            {
                // Get the radius of this row.
                double radius = 0;
                if (upDown == RotateDirection.UP)
                {
                    if (NS)
                        radius = Math.abs(zAxis - turningPoint.getBlockZ());
                    else
                        radius = Math.abs(xAxis - turningPoint.getBlockX());
                }

                for (double yAxis = yMin; yAxis <= yMax; ++yAxis)
                {
                    Location startLocation = new Location(world, xAxis + 0.5, yAxis, zAxis + 0.5);
                    if (upDown == RotateDirection.DOWN)
                        radius = yAxis - turningPoint.getBlockY();

                    Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis - 0.020, zAxis + 0.5);
                    // Move the lowest blocks up a little, so the client won't predict they're touching through the ground,
                    // which would make them slower than the rest.
                    if (yAxis == yMin)
                        newFBlockLocation.setY(newFBlockLocation.getY() + .010001);

                    Block vBlock  = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis);
                    Material mat  = vBlock.getType();
                    if (!mat.equals(Material.AIR))
                    {
                        Byte matData  = vBlock.getData();
                        BlockState bs = vBlock.getState();
                        MaterialData materialData = bs.getData();

                        NMSBlock_Vall block  = fabf.nmsBlockFactory(world, (int) xAxis, (int) yAxis, (int) zAxis);
                        NMSBlock_Vall block2 = null;

                        int canRotate = 0;
                        Byte matByte  = matData;
                        // Certain blocks cannot be used the way normal blocks can (heads, (ender) chests etc).
                        if (Util.isAllowedBlock(mat))
                        {
                            canRotate        = Util.canRotate(mat);
                            // Rotate blocks here so they don't interrupt the rotation animation.
                            if (canRotate == 1 || canRotate == 2 || canRotate == 3 || canRotate == 6 || canRotate == 7)
                            {
                                matByte      = canRotate == 7 ? rotateEndRotBlockData(matData) : rotateBlockData(matData);
                                Block b      = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis);
                                materialData.setData(matByte);

                                if (plugin.is1_13())
                                {
                                    if (canRotate == 6)
                                    {
                                        block2 = fabf.nmsBlockFactory(world, (int) xAxis, (int) yAxis, (int) zAxis);
                                        block2.rotateBlockUpDown(NS);
                                    }
                                    else
                                    {
                                        b.setType(mat);
                                        BlockState bs2 = b.getState();
                                        bs2.setData(materialData);
                                        bs2.update();
                                        block2 = fabf.nmsBlockFactory(world, (int) xAxis, (int) yAxis, (int) zAxis);
                                    }
                                }
                            }
                            vBlock.setType(Material.AIR);
                        }
                        else
                        {
                            mat     = Material.AIR;
                            matData = 0;
                        }

                        CustomCraftFallingBlock_Vall fBlock = null;
                        if (!instantOpen)
                             fBlock = fallingBlockFactory(newFBlockLocation, mat, matData, block);

                        savedBlocks.add(index, new MyBlockData(mat, matByte, fBlock, radius, materialData, block2 == null ? block : block2, canRotate, startLocation));
                    }
                    else
                        savedBlocks.add(index, new MyBlockData(Material.AIR));
                    index++;
                }
                zAxis += dz;
            }
            while (zAxis >= pointOpposite.getBlockZ() && dz == -1 || zAxis <= pointOpposite.getBlockZ() && dz == 1);
            xAxis += dx;
        }
        while (xAxis >= pointOpposite.getBlockX() && dx == -1 || xAxis <= pointOpposite.getBlockX() && dx == 1);

        switch (openDirection)
        {
        case NORTH:
            gnl = new GetNewLocationNorth(world, xMin, xMax, yMin, yMax, zMin, zMax, upDown, openDirection);
            break;
        case EAST:
            gnl = new GetNewLocationEast (world, xMin, xMax, yMin, yMax, zMin, zMax, upDown, openDirection);
            break;
        case SOUTH:
            gnl = new GetNewLocationSouth(world, xMin, xMax, yMin, yMax, zMin, zMax, upDown, openDirection);
            break;
        case WEST:
            gnl = new GetNewLocationWest (world, xMin, xMax, yMin, yMax, zMin, zMax, upDown, openDirection);
            break;
        }

        if (!instantOpen)
            rotateEntities();
        else
            putBlocks(false);
    }

    // Put the door blocks back, but change their state now.
    @SuppressWarnings("deprecation")
    @Override
    public void putBlocks(boolean onDisable)
    {
        int index = 0;
        double xAxis = turningPoint.getX();
        do
        {
            double zAxis = turningPoint.getZ();
            do
            {
                for (double yAxis = yMin; yAxis <= yMax; yAxis++)
                {
                    /*
                     * 0-3: Vertical oak, spruce, birch, then jungle 4-7: East/west oak, spruce,
                     * birch, jungle 8-11: North/south oak, spruce, birch, jungle 12-15: Uses oak,
                     * spruce, birch, jungle bark texture on all six faces
                     */

                    Material mat = savedBlocks.get(index).getMat();

                    if (!mat.equals(Material.AIR))
                    {
                        Byte matByte;
                        matByte = savedBlocks.get(index).getBlockByte();
                        Location newPos = gnl.getNewLocation(savedBlocks.get(index).getRadius(), xAxis, yAxis, zAxis, index);

                        if (!instantOpen)
                            savedBlocks.get(index).getFBlock().remove();

                        if (!savedBlocks.get(index).getMat().equals(Material.AIR))
                        {
                            if (plugin.is1_13())
                            {
                                savedBlocks.get(index).getBlock().putBlock(newPos);
                                Block b = world.getBlockAt(newPos);
                                BlockState bs = b.getState();
                                bs.update();
                            }
                            else
                            {
                                Block b = world.getBlockAt(newPos);
                                MaterialData matData = savedBlocks.get(index).getMatData();
                                matData.setData(matByte);

                                b.setType(mat);
                                BlockState bs = b.getState();
                                bs.setData(matData);
                                bs.update();
                            }
                        }
                    }
                    index++;
                }
                zAxis += dz;
            }
            while (zAxis >= pointOpposite.getBlockZ() && dz == -1 || zAxis <= pointOpposite.getBlockZ() && dz == 1);
            xAxis += dx;
        }
        while (xAxis >= pointOpposite.getBlockX() && dx == -1 || xAxis <= pointOpposite.getBlockX() && dx == 1);
        savedBlocks.clear();

        // Tell the door object it has been opened and what its new coordinates are.
        updateCoords(door, openDirection, upDown, -1);
        toggleOpen  (door);
        if (!onDisable)
            plugin.removeBlockMover(this);

        // Change door availability to true, so it can be opened again.
        // Wait for a bit if instantOpen is enabled.
        int timer = onDisable   ?  0 :
                    instantOpen ? 40 : plugin.getConfigLoader().coolDown() * 20;

        if (timer > 0)
        {
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    plugin.getCommander().setDoorAvailable(door.getDoorUID());
                }
            }.runTaskLater(plugin, timer);
        }
        else
            plugin.getCommander().setDoorAvailable(door.getDoorUID());

        if (!onDisable)
            goAgain();
    }

    private void goAgain()
    {
        int autoCloseTimer = door.getAutoClose();
        if (autoCloseTimer < 0 || !door.isOpen())
            return;

        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                plugin.getCommander().setDoorAvailable(door.getDoorUID());
                plugin.getDoorOpener(door.getType()).openDoor(plugin.getCommander().getDoor(null, door.getDoorUID()), time, instantOpen, false);
            }
        }.runTaskLater(plugin, autoCloseTimer * 20);
    }

    // Method that takes care of the rotation aspect.
    private void rotateEntities()
    {
        new BukkitRunnable()
        {
            Location center   = new Location(world, turningPoint.getBlockX() + 0.5, yMin, turningPoint.getBlockZ() + 0.5);
            boolean replace   = false;
            double counter    = 0;
            int endCount      = (int) (20 / tickRate * time);
            double step       = (Math.PI / 2) / endCount * stepMultiplier;
            double stepSum    = startStepSum;
            int totalTicks    = (int) (endCount * multiplier);
            int replaceCount  = endCount / 2;
            long startTime    = System.nanoTime();
            long lastTime;
            long currentTime  = System.nanoTime();

            @Override
            public void run()
            {
                if (counter == 0 || (counter < endCount - 45 / tickRate && counter % (6 * tickRate / 4) == 0))
                    Util.playSound(door.getEngine(), "bd.drawbridge-rattling", 0.8f, 0.7f);

                lastTime = currentTime;
                currentTime = System.nanoTime();
                long msSinceStart = (currentTime - startTime) / 1000000;
                if (!plugin.getCommander().isPaused())
                    counter = msSinceStart / (50 * tickRate);
                else
                    startTime += currentTime - lastTime;

                if (counter < endCount - 1)
                    stepSum = startStepSum + step * counter;
                else
                    stepSum = endStepSum;

                replace = false;
                if (counter == replaceCount)
                    replace = true;

                if (!plugin.getCommander().canGo() || !door.canGo() || counter > totalTicks)
                {
                    Util.playSound(door.getEngine(), "bd.thud", 2f, 0.15f);
                    for (int idx = 0; idx < savedBlocks.size(); ++idx)
                        if (!savedBlocks.get(idx).getMat().equals(Material.AIR))
                            savedBlocks.get(idx).getFBlock().setVelocity(new Vector(0D, 0D, 0D));
                    Bukkit.getScheduler().callSyncMethod(plugin, () ->
                    {
                        putBlocks(false);
                        return null;
                    });
                    cancel();
                }
                else
                {
                    // It is not pssible to edit falling block blockdata (client won't update it), so delete the current fBlock and replace it by one that's been rotated.
                    // Also, this stuff needs to be done on the main thread.
                    if (replace)
                    {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
                        {
                            for (MyBlockData block : savedBlocks)
                            {
                                if (block.canRot() != 0 && block.canRot() != 4)
                                {
                                    Material mat = block.getMat();
                                    Location loc = block.getFBlock().getLocation();
                                    Byte matData = block.getBlockByte();
                                    Vector veloc = block.getFBlock().getVelocity();

                                    CustomCraftFallingBlock_Vall fBlock;
                                    // Because the block in savedBlocks is already rotated where applicable, just use that block now.
                                    fBlock = fallingBlockFactory(loc, mat, matData, block.getBlock());

                                    block.getFBlock().remove();
                                    block.setFBlock(fBlock);

                                    block.getFBlock().setVelocity(veloc);
                                }
                            }
                        }, 0);
                    }

                    for (MyBlockData block : savedBlocks)
                    {
                        if (!block.getMat().equals(Material.AIR))
                        {
                            double radius = block.getRadius();
                            if (radius != 0)
                            {
                                double posX, posY, posZ;
                                posY = center.getY() + radius * Math.cos(stepSum);
                                if (!NS)
                                {
                                    posX = center.getX() + radius * Math.sin(stepSum);
                                    posZ = block.getFBlock().getLocation().getZ();
                                }
                                else
                                {
                                    posX = block.getFBlock().getLocation().getX();
                                    posZ = center.getZ() + radius * Math.sin(stepSum);
                                }
                                Location loc = new Location(null, posX, posY, posZ);
                                Vector vec   = loc.toVector().subtract(block.getFBlock().getLocation().toVector());
                                vec.multiply(0.101);
                                block.getFBlock().setVelocity(vec);
                            }
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 14, tickRate);
    }

    // Rotate blocks such a logs by modifying its material data.
    private byte rotateBlockData(Byte matData)
    {
        if (!NS)
        {
            if (matData >= 0 && matData < 4)
                return (byte) (matData + 4);
            if (matData >= 4 && matData < 7)
                return (byte) (matData - 4);
            return matData;
        }
        else
        {
            if (matData >= 0 && matData < 4)
                return (byte) (matData + 8);
            if (matData >= 8 && matData < 12)
                return (byte) (matData - 8);
            return matData;
        }
    }

    // Rotate blocks such a logs by modifying its material data.
    private byte rotateEndRotBlockData(Byte matData)
    {
        /* 0: Pointing Down     (upside down (purple on top))
         * 1: Pointing Up       (normal)
         * 2: Pointing North
         * 3: Pointing South
         * 4: Pointing West
         * 5: Pointing East
         */
        if (!NS)
        {
            if (matData == 0)
                return (byte) (openDirection.equals(DoorDirection.EAST) ? 4 : 5);
            if (matData == 1)
                return (byte) (openDirection.equals(DoorDirection.EAST) ? 5 : 4);
            if (matData == 4)
                return (byte) (openDirection.equals(DoorDirection.EAST) ? 1 : 0);
            if (matData == 5)
                return (byte) (openDirection.equals(DoorDirection.EAST) ? 0 : 1);
            return matData;
        }
        else
        {
            if (matData == 0)
                return (byte) (openDirection.equals(DoorDirection.NORTH) ? 3 : 2);
            if (matData == 1)
                return (byte) (openDirection.equals(DoorDirection.NORTH) ? 2 : 3);
            if (matData == 2)
                return (byte) (openDirection.equals(DoorDirection.NORTH) ? 0 : 1);
            if (matData == 3)
                return (byte) (openDirection.equals(DoorDirection.NORTH) ? 1 : 0);
            return matData;
        }
    }

    // Toggle the open status of a drawbridge.
    private void toggleOpen(Door door)
    {
        door.setOpenStatus(!door.isOpen());
    }

    // Update the coordinates of a door based on its location, direction it's pointing in and rotation direction.
    private void updateCoords(Door door, DoorDirection openDirection, RotateDirection upDown, int moved)
    {
        int xMin = door.getMinimum().getBlockX();
        int yMin = door.getMinimum().getBlockY();
        int zMin = door.getMinimum().getBlockZ();
        int xMax = door.getMaximum().getBlockX();
        int yMax = door.getMaximum().getBlockY();
        int zMax = door.getMaximum().getBlockZ();
        int xLen = xMax - xMin;
        int yLen = yMax - yMin;
        int zLen = zMax - zMin;
        Location newMax = null;
        Location newMin = null;
        DoorDirection newEngSide = door.getEngSide();

        switch (openDirection)
        {
        case NORTH:
            if (upDown == RotateDirection.UP)
            {
                newEngSide = DoorDirection.NORTH;
                newMin = new Location(door.getWorld(), xMin, yMin,        zMin);
                newMax = new Location(door.getWorld(), xMax, yMin + zLen, zMin);
            }
            else
            {
                newEngSide = DoorDirection.SOUTH;
                newMin = new Location(door.getWorld(), xMin, yMin, zMin - yLen);
                newMax = new Location(door.getWorld(), xMax, yMin, zMin       );
            }
            break;


        case EAST:
            if (upDown == RotateDirection.UP)
            {
                newEngSide = DoorDirection.EAST;
                newMin = new Location(door.getWorld(), xMax, yMin,        zMin);
                newMax = new Location(door.getWorld(), xMax, yMin + xLen, zMax);
            }
            else
            {
                newEngSide = DoorDirection.WEST;
                newMin = new Location(door.getWorld(), xMax,        yMin, zMin);
                newMax = new Location(door.getWorld(), xMax + yLen, yMin, zMax);
            }
            break;


        case SOUTH:
            if (upDown == RotateDirection.UP)
            {
                newEngSide = DoorDirection.SOUTH;
                newMin = new Location(door.getWorld(), xMin, yMin,        zMax);
                newMax = new Location(door.getWorld(), xMax, yMin + zLen, zMax);
            }
            else
            {
                newEngSide = DoorDirection.NORTH;
                newMin = new Location(door.getWorld(), xMin, yMin, zMax       );
                newMax = new Location(door.getWorld(), xMax, yMin, zMax + yLen);
            }
            break;


        case WEST:
            if (upDown == RotateDirection.UP)
            {
                newEngSide = DoorDirection.WEST;
                newMin = new Location(door.getWorld(), xMin, yMin,        zMin);
                newMax = new Location(door.getWorld(), xMin, yMin + xLen, zMax);
            }
            else
            {
                newEngSide = DoorDirection.EAST;
                newMin = new Location(door.getWorld(), xMin - yLen, yMin, zMin);
                newMax = new Location(door.getWorld(), xMin,        yMin, zMax);
            }
            break;
        }
        door.setMaximum(newMax);
        door.setMinimum(newMin);
        door.setEngineSide(newEngSide);

        plugin.getCommander().updateDoorCoords(door.getDoorUID(), !door.isOpen(), newMin.getBlockX(), newMin.getBlockY(), newMin.getBlockZ(), newMax.getBlockX(), newMax.getBlockY(), newMax.getBlockZ(), newEngSide);
    }

    private CustomCraftFallingBlock_Vall fallingBlockFactory(Location loc, Material mat, byte matData, NMSBlock_Vall block)
    {
        CustomCraftFallingBlock_Vall entity = fabf.fallingBlockFactory(plugin, loc, block, matData, mat);
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