package nl.pim16aap2.bigDoors.moveBlocks;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory;
import nl.pim16aap2.bigDoors.NMS.NMSBlock;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocation;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationEast;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationNorth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationSouth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationWest;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.MyBlockData;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CylindricalMover extends BlockMover
{
    private GetNewLocation gnl;
    private final FallingBlockFactory fabf;
    private final double time;
    private final Door door;
    private final World world;
    private final int dx, dz;
    private final BigDoors plugin;
    private final int tickRate;
    private double endStepSum;
    private double multiplier;
    private double startStepSum;
    private final RotateDirection rotDirection;
    private final Location pointOpposite;
    private final int stepMultiplier;
    private final int xMin, xMax, yMin;
    private final int yMax, zMin, zMax;
    private final DoorDirection currentDirection;
    private final Location turningPoint;
    private int endCount = 0;
    private BukkitRunnable animationRunnable;

    @SuppressWarnings("deprecation")
    public CylindricalMover(BigDoors plugin, World world, int qCircleLimit, RotateDirection rotDirection, double time,
        Location pointOpposite, DoorDirection currentDirection, Door door, boolean instantOpen, double multiplier)
    {
        super(plugin, door, instantOpen);
        this.rotDirection = rotDirection;
        this.currentDirection = currentDirection;
        this.plugin = plugin;
        this.world = world;
        this.door = door;
        this.pointOpposite = pointOpposite;
        turningPoint = door.getEngine();
        fabf = plugin.getFABF();
        stepMultiplier = rotDirection == RotateDirection.CLOCKWISE ? -1 : 1;

        xMin = Math.min(turningPoint.getBlockX(), pointOpposite.getBlockX());
        yMin = Math.min(turningPoint.getBlockY(), pointOpposite.getBlockY());
        zMin = Math.min(turningPoint.getBlockZ(), pointOpposite.getBlockZ());
        xMax = Math.max(turningPoint.getBlockX(), pointOpposite.getBlockX());
        yMax = Math.max(turningPoint.getBlockY(), pointOpposite.getBlockY());
        zMax = Math.max(turningPoint.getBlockZ(), pointOpposite.getBlockZ());

        int xLen = Math.abs(door.getMaximum().getBlockX() - door.getMinimum().getBlockX());
        int zLen = Math.abs(door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ());
        int doorSize = Math.max(xLen, zLen) + 1;
        double[] vars = Util.calculateTimeAndTickRate(doorSize, time, multiplier, 3.7);
        this.time = vars[0];
        tickRate = (int) vars[1];
        this.multiplier = vars[2];

        dx = pointOpposite.getBlockX() > turningPoint.getBlockX() ? 1 : -1;
        dz = pointOpposite.getBlockZ() > turningPoint.getBlockZ() ? 1 : -1;
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::createAnimatedBlocks, 2L);
    }

    private void createAnimatedBlocks()
    {
        savedBlocks.ensureCapacity(door.getBlockCount());

        int xAxis = turningPoint.getBlockX();
        do
        {
            int zAxis = turningPoint.getBlockZ();
            do
            {
                final double xRadius = Math.abs(xAxis - turningPoint.getBlockX());
                final double zRadius = Math.abs(zAxis - turningPoint.getBlockZ());

                // Get the radius of this pillar.
                final double radius = Math.max(xRadius, zRadius);

                for (int yAxis = yMin; yAxis <= yMax; yAxis++)
                {
                    Location startLocation = new Location(world, xAxis + 0.5, yAxis, zAxis + 0.5);
                    Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis, zAxis + 0.5);

                    final Block vBlock = world.getBlockAt(xAxis, yAxis, zAxis);
                    final Material mat = vBlock.getType();

                    if (Util.isAllowedBlock(mat))
                    {
                        final byte matData = vBlock.getData();
                        final BlockState bs = vBlock.getState();
                        final MaterialData materialData = bs.getData();

                        final NMSBlock block = fabf.nmsBlockFactory(world, xAxis, yAxis, zAxis);
                        NMSBlock block2 = null;

                        byte matByte = matData;
                        final int canRotate = Util.canRotate(mat);

                        // Rotate blocks here so they don't interrupt the rotation animation.
                        if (canRotate != 0)
                        {
                            Location pos = new Location(world, xAxis, yAxis, zAxis);
                            if (canRotate == 1 || canRotate == 3)
                                matByte = rotateBlockDataLog(matData);
                            else if (canRotate == 2)
                                matByte = rotateBlockDataStairs(matData);
                            else if (canRotate == 4)
                                matByte = rotateBlockDataAnvil(matData);
                            else if (canRotate == 7)
                                matByte = rotateBlockDataEndRod(matData);

                            Block b = world.getBlockAt(pos);
                            materialData.setData(matByte);

                            if (BigDoors.isOnFlattenedVersion())
                            {
                                if (canRotate == 6 || canRotate == 8)
                                {
                                    block2 = fabf.nmsBlockFactory(world, xAxis, yAxis, zAxis);
                                    block2.rotateCylindrical(this.rotDirection);
                                }
                                else
                                {
                                    b.setType(mat);
                                    BlockState bs2 = b.getState();
                                    bs2.setData(materialData);
                                    bs2.update();
                                    block2 = fabf.nmsBlockFactory(world, xAxis, yAxis, zAxis);
                                }
                            }
                        }
                        if (!BigDoors.isOnFlattenedVersion())
                            vBlock.setType(Material.AIR);

                        CustomCraftFallingBlock fBlock = null;
                        if (!instantOpen)
                            fBlock = fabf.fallingBlockFactory(newFBlockLocation, block, matData, mat);

                        savedBlocks.add(new MyBlockData(mat, matByte, fBlock, radius, materialData,
                                                        block2 == null ? block : block2, canRotate, startLocation));
                    }
                }
                zAxis += dz;
            }
            while (zAxis >= pointOpposite.getBlockZ() && dz == -1 || zAxis <= pointOpposite.getBlockZ() && dz == 1);
            xAxis += dx;
        }
        while (xAxis >= pointOpposite.getBlockX() && dx == -1 || xAxis <= pointOpposite.getBlockX() && dx == 1);

        switch (currentDirection)
        {
        case NORTH:
            gnl = new GetNewLocationNorth(world, xMin, xMax, zMin, zMax, rotDirection);
            startStepSum = Math.PI;
            endStepSum = rotDirection == RotateDirection.CLOCKWISE ? Math.PI / 2 : 3 * Math.PI / 2;
            break;
        case EAST:
            gnl = new GetNewLocationEast(world, xMin, xMax, zMin, zMax, rotDirection);
            startStepSum = Math.PI / 2;
            endStepSum = rotDirection == RotateDirection.CLOCKWISE ? 0 : Math.PI;
            break;
        case SOUTH:
            gnl = new GetNewLocationSouth(world, xMin, xMax, zMin, zMax, rotDirection);
            startStepSum = 0;
            endStepSum = rotDirection == RotateDirection.CLOCKWISE ? 3 * Math.PI / 2 : Math.PI / 2;
            break;
        case WEST:
            gnl = new GetNewLocationWest(world, xMin, xMax, zMin, zMax, rotDirection);
            startStepSum = 3 * Math.PI / 2;
            endStepSum = rotDirection == RotateDirection.CLOCKWISE ? Math.PI : 0;
            break;
        }

        // This is only supported on 1.13
        if (BigDoors.isOnFlattenedVersion())
        {
            savedBlocks.forEach(myBlockData -> myBlockData.getBlock().deleteOriginalBlock(false));
        }

        savedBlocks.trimToSize();

        if (!instantOpen)
            rotateEntities();
        else
            putBlocks(false);
    }

    @Override
    public synchronized void cancel(boolean onDisable)
    {
        if (this.animationRunnable == null)
            return;
        this.animationRunnable.cancel();
        this.putBlocks(onDisable);
    }

    @Override
    public synchronized void putBlocks(boolean onDisable)
    {
        super.putBlocks(onDisable, time, endCount,
                        gnl::getNewLocation,
                        () -> updateCoords(door, currentDirection, rotDirection, -1, false));
    }

    // Method that takes care of the rotation aspect.
    private void rotateEntities()
    {
        endCount = (int) (20.0f / tickRate * time);

        animationRunnable = new BukkitRunnable()
        {
            final Location center = new Location(world, turningPoint.getBlockX() + 0.5, yMin, turningPoint.getBlockZ() + 0.5);
            boolean replace = false;
            double counter = 0;
            final double step = (Math.PI / 2) / endCount * stepMultiplier;
            double stepSum = startStepSum;
            final int totalTicks = (int) (endCount * multiplier);
            final int replaceCount = endCount / 2;
            long startTime = System.nanoTime();
            long lastTime;
            long currentTime = System.nanoTime();

            @Override
            public void run()
            {
                if (counter == 0 || (counter < endCount - 27 / tickRate && counter % (5 * tickRate / 4) == 0))
                    Util.playSound(door.getEngine(), "bd.dragging2", 0.5f, 0.6f);

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

                replace = counter == replaceCount;

                if (!plugin.getCommander().canGo() || counter > totalTicks)
                {
                    Util.playSound(door.getEngine(), "bd.closing-vault-door", 0.2f, 1f);
                    for (MyBlockData savedBlock : savedBlocks)
                        if (!savedBlock.getMat().equals(Material.AIR))
                            savedBlock.getFBlock().setVelocity(new Vector(0D, 0D, 0D));

                    Bukkit.getScheduler().callSyncMethod(plugin, () ->
                    {
                        putBlocks(false);
                        return null;
                    });
                    cancel();
                }
                else
                {
                    // It is not pssible to edit falling block blockdata (client won't update it),
                    // so delete the current fBlock and replace it by one that's been rotated.
                    // Also, this stuff needs to be done on the main thread.
                    if (replace)
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
                        {
                            for (MyBlockData block : savedBlocks)
                                if (block.canRot() != 0 && block.canRot() != 5)
                                {
                                    Material mat = block.getMat();
                                    Location loc = block.getFBlock().getLocation();
                                    byte matData = block.getBlockByte();
                                    Vector veloc = block.getFBlock().getVelocity();

                                    CustomCraftFallingBlock fBlock;
                                    // Because the block in savedBlocks is already rotated where applicable, just
                                    // use that block now.
                                    fBlock = fabf.fallingBlockFactory(loc, block.getBlock(), matData, mat);

                                    block.getFBlock().remove();
                                    block.setFBlock(fBlock);
                                    block.getFBlock().setVelocity(veloc);
                                }
                        }, 0);

                    double sin = Math.sin(stepSum);
                    double cos = Math.cos(stepSum);

                    for (MyBlockData block : savedBlocks)
                        if (!block.getMat().equals(Material.AIR))
                        {
                            double radius = block.getRadius();
                            double yPos = block.getStartLocation().getBlockY();

                            if (radius != 0)
                            {
                                Location loc;
                                double addX = radius * sin;
                                double addZ = radius * cos;

                                loc = new Location(null, center.getX() + addX, yPos, center.getZ() + addZ);

                                Vector vec = loc.toVector().subtract(block.getFBlock().getLocation().toVector());
                                vec.multiply(0.101);
                                block.getFBlock().setVelocity(vec);
                            }
                        }
                }
            }
        };
        animationRunnable.runTaskTimerAsynchronously(plugin, 14, tickRate);
    }

    // Rotate logs by modifying its material data.
    private byte rotateBlockDataLog(byte matData)
    {
        if (matData >= 4 && matData <= 7)
            matData = (byte) (matData + 4);
        else if (matData >= 7 && matData <= 11)
            matData = (byte) (matData - 4);
        return matData;
    }

    private byte rotateBlockDataEndRod(byte matData)
    {
        /*
         * 0: Pointing Down (upside down (purple on top))
         * 1: Pointing Up (normal)
         * 2: Pointing North
         * 3: Pointing South
         * 4: Pointing West
         * 5: Pointing East
         */
        if (matData == 0 || matData == 1)
            return matData;

        if (rotDirection == RotateDirection.CLOCKWISE)
        {
            switch (matData)
            {
                case 2: return 5; // North -> East
                case 3: return 4; // South -> West
                case 4: return 2; // West  -> North
                case 5: return 3; // East  -> South
                default: return matData;
            }
        }

        switch (matData)
        {
            case 2: return 5; // North -> West
            case 3: return 4; // South -> East
            case 4: return 3; // West  -> South
            case 5: return 2; // East  -> North
            default: return matData;
        }
    }

    private byte rotateBlockDataAnvil(byte matData)
    {
        if (rotDirection == RotateDirection.CLOCKWISE)
        {
            if (matData == 0 || matData == 4 || matData == 8)
                matData = (byte) (matData + 1);
            else if (matData == 1 || matData == 5 || matData == 9)
                matData = (byte) (matData + 1);
            else if (matData == 2 || matData == 6 || matData == 10)
                matData = (byte) (matData + 1);
            else if (matData == 3 || matData == 7 || matData == 11)
                matData = (byte) (matData - 3);
        }
        else if (matData == 0 || matData == 4 || matData == 8)
            matData = (byte) (matData + 3);
        else if (matData == 1 || matData == 5 || matData == 9)
            matData = (byte) (matData - 1);
        else if (matData == 2 || matData == 6 || matData == 10)
            matData = (byte) (matData - 1);
        else if (matData == 3 || matData == 7 || matData == 11)
            matData = (byte) (matData - 1);
        return matData;
    }

    // Rotate stairs by modifying its material data.
    private byte rotateBlockDataStairs(byte matData)
    {
        if (rotDirection == RotateDirection.CLOCKWISE)
        {
            if (matData == 0 || matData == 4)
                matData = (byte) (matData + 2);
            else if (matData == 1 || matData == 5)
                matData = (byte) (matData + 2);
            else if (matData == 2 || matData == 6)
                matData = (byte) (matData - 1);
            else if (matData == 3 || matData == 7)
                matData = (byte) (matData - 3);
        }
        else if (matData == 0 || matData == 4)
            matData = (byte) (matData + 3);
        else if (matData == 1 || matData == 5)
            matData = (byte) (matData + 1);
        else if (matData == 2 || matData == 6)
            matData = (byte) (matData - 2);
        else if (matData == 3 || matData == 7)
            matData = (byte) (matData - 2);
        return matData;
    }

    // Update the coordinates of a door based on its location, direction it's
    // pointing in and rotation direction.
    @SuppressWarnings("null")
    public static void updateCoords(Door door, DoorDirection currentDirection, RotateDirection rotDirection, int moved,
                                    boolean shadow)
    {
        int xMin = door.getMinimum().getBlockX();
        int yMin = door.getMinimum().getBlockY();
        int zMin = door.getMinimum().getBlockZ();
        int xMax = door.getMaximum().getBlockX();
        int yMax = door.getMaximum().getBlockY();
        int zMax = door.getMaximum().getBlockZ();
        int xLen = xMax - xMin;
        int zLen = zMax - zMin;
        Location newMax = null;
        Location newMin = null;

        switch (currentDirection)
        {
        case NORTH:
            if (rotDirection == RotateDirection.CLOCKWISE)
            {
                newMin = new Location(door.getWorld(), xMin, yMin, zMax);
                newMax = new Location(door.getWorld(), (xMin + zLen), yMax, zMax);
            }
            else
            {
                newMin = new Location(door.getWorld(), (xMin - zLen), yMin, zMax);
                newMax = new Location(door.getWorld(), xMax, yMax, zMax);
            }
            break;

        case EAST:
            if (rotDirection == RotateDirection.CLOCKWISE)
            {
                newMin = new Location(door.getWorld(), xMin, yMin, zMin);
                newMax = new Location(door.getWorld(), xMin, yMax, (zMax + xLen));
            }
            else
            {
                newMin = new Location(door.getWorld(), xMin, yMin, (zMin - xLen));
                newMax = new Location(door.getWorld(), xMin, yMax, zMin);
            }
            break;

        case SOUTH:
            if (rotDirection == RotateDirection.CLOCKWISE)
            {
                newMin = new Location(door.getWorld(), (xMin - zLen), yMin, zMin);
                newMax = new Location(door.getWorld(), xMax, yMax, zMin);
            }
            else
            {
                newMin = new Location(door.getWorld(), xMin, yMin, zMin);
                newMax = new Location(door.getWorld(), (xMin + zLen), yMax, zMin);
            }
            break;

        case WEST:
            if (rotDirection == RotateDirection.CLOCKWISE)
            {
                newMin = new Location(door.getWorld(), xMax, yMin, (zMin - xLen));
                newMax = new Location(door.getWorld(), xMax, yMax, zMax);
            }
            else
            {
                newMin = new Location(door.getWorld(), xMax, yMin, zMin);
                newMax = new Location(door.getWorld(), xMax, yMax, (zMax + xLen));
            }
            break;
        }
        door.setMaximum(newMax);
        door.setMinimum(newMin);

        boolean isOpen = shadow ? door.isOpen() : !door.isOpen();
        BigDoors.get().getCommander().updateDoorCoords(door.getDoorUID(), isOpen, newMin.getBlockX(),
                                                       newMin.getBlockY(), newMin.getBlockZ(), newMax.getBlockX(),
                                                       newMax.getBlockY(), newMax.getBlockZ());
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
