package nl.pim16aap2.bigDoors.moveBlocks;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory;
import nl.pim16aap2.bigDoors.NMS.NMSBlock;
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

import java.util.ArrayList;
import java.util.List;

public class SlidingMover extends BlockMover
{
    private FallingBlockFactory fabf;
    private Door door;
    private boolean NS;
    private double time;
    private World world;
    private BigDoors plugin;
    private int tickRate;
    private int moveX, moveZ;
    private int blocksToMove;
    private RotateDirection openDirection;
    private int xMin, xMax, yMin;
    private int yMax, zMin, zMax;
    private int endCount;
    private BukkitRunnable animationRunnable;

    @SuppressWarnings("deprecation")
    public SlidingMover(BigDoors plugin, World world, double time, Door door, boolean instantOpen, int blocksToMove,
        RotateDirection openDirection, double multiplier)
    {
        super(plugin, door, instantOpen);
        this.plugin = plugin;
        this.world = world;
        this.door = door;
        fabf = plugin.getFABF();

        // North and West direction move negatively along the Z/X axis.
        this.blocksToMove = (openDirection.equals(RotateDirection.NORTH) ||
                             openDirection.equals(RotateDirection.WEST)) ? -blocksToMove : blocksToMove;
        this.openDirection = openDirection;
        NS = openDirection.equals(RotateDirection.NORTH) || openDirection.equals(RotateDirection.SOUTH);

        xMin = door.getMinimum().getBlockX();
        yMin = door.getMinimum().getBlockY();
        zMin = door.getMinimum().getBlockZ();
        xMax = door.getMaximum().getBlockX();
        yMax = door.getMaximum().getBlockY();
        zMax = door.getMaximum().getBlockZ();

        moveX = NS ? 0 : this.blocksToMove;
        moveZ = NS ? this.blocksToMove : 0;

        double speed = 1;
        double pcMult = multiplier;
        pcMult = pcMult == 0.0 ? 1.0 : pcMult;
        int maxSpeed = 6;

        // If the time isn't default, calculate speed.
        if (time != 0.0)
        {
            speed = Math.abs(blocksToMove) / time;
            this.time = time;
        }

        // If the non-default exceeds the max-speed or isn't set, calculate default
        // speed.
        if (time == 0.0 || speed > maxSpeed)
        {
            speed = 1.4 * pcMult;
            speed = speed > maxSpeed ? maxSpeed : speed;
            this.time = Math.abs(blocksToMove) / speed;
        }
        tickRate = Util.tickRateFromSpeed(speed);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::createAnimatedBlocks, 2L);
    }

    private void createAnimatedBlocks()
    {
        savedBlocks.ensureCapacity(door.getBlockCount());

        // This will reserve a bit too much memory, but not enough to worry about.
        final List<NMSBlock> edges =
            new ArrayList<>(Math.min(door.getBlockCount(),
                                     (xMax - xMin + 1) * 2 + (yMax - yMin + 1) * 2 + (zMax - zMin + 1) * 2));

        int yAxis = yMin;
        do
        {
            int zAxis = zMin;
            do
            {
                for (int xAxis = xMin; xAxis <= xMax; xAxis++)
                {
                    Location startLocation = new Location(world, xAxis + 0.5, yAxis, zAxis + 0.5);
                    Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis, zAxis + 0.5);
                    Block vBlock = world.getBlockAt(xAxis, yAxis, zAxis);
                    Material mat = vBlock.getType();
                    if (Util.isAllowedBlock(mat))
                    {
                        byte matData = vBlock.getData();
                        BlockState bs = vBlock.getState();
                        MaterialData materialData = bs.getData();
                        NMSBlock block = fabf.nmsBlockFactory(world, xAxis, yAxis, zAxis);

                        if (!BigDoors.isOnFlattenedVersion())
                            vBlock.setType(Material.AIR);

                        CustomCraftFallingBlock fBlock = null;
                        if (!instantOpen)
                            fBlock = fabf.fallingBlockFactory(newFBlockLocation, block, matData, mat);
                        savedBlocks
                            .add(new MyBlockData(mat, matData, fBlock, 0, materialData, block, 0, startLocation));

                        if (xAxis == xMin || xAxis == xMax ||
                            yAxis == yMin || yAxis == yMax ||
                            zAxis == zMin || zAxis == zMax)
                            edges.add(block);
                    }
                }
                ++zAxis;
            }
            while (zAxis <= zMax);
            ++yAxis;
        }
        while (yAxis <= yMax);

        // This is only supported on 1.13
        if (BigDoors.isOnFlattenedVersion())
        {
            savedBlocks.forEach(myBlockData -> myBlockData.getBlock().deleteOriginalBlock(false));
            // Update the physics around the edges after we've removed all our blocks.
            edges.forEach(block -> block.deleteOriginalBlock(true));
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
                        (__, x, y, z) -> getNewLocation(x, y, z),
                        () -> updateCoords(door, null, openDirection, blocksToMove, NS, false));
    }

    private Location getNewLocation(double xAxis, double yAxis, double zAxis)
    {
        return new Location(world, xAxis + moveX, yAxis, zAxis + moveZ);
    }

    // Method that takes care of the rotation aspect.
    private void rotateEntities()
    {
        endCount = (int) (20.0f / tickRate * time);

        animationRunnable = new BukkitRunnable()
        {
            double counter = 0;
            double step = ((double) blocksToMove) / ((double) endCount);
            double stepSum = 0;
            int totalTicks = (int) (endCount * 1.1);
            long startTime = System.nanoTime();
            long lastTime;
            long currentTime = System.nanoTime();
            MyBlockData firstBlockData = savedBlocks.stream().filter(block -> !block.getMat().equals(Material.AIR))
                .findFirst().orElse(null);

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
                    stepSum = step * counter;
                else
                    stepSum = blocksToMove;

                if (!plugin.getCommander().canGo() || counter > totalTicks || firstBlockData == null)
                {
                    Util.playSound(door.getEngine(), "bd.thud", 2f, 0.15f);
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
                    Location loc = firstBlockData.getStartLocation();

                    if (NS)
                        loc.setZ(loc.getZ() + stepSum);
                    else
                        loc.setX(loc.getX() + stepSum);

                    Vector vec = loc.toVector().subtract(firstBlockData.getFBlock().getLocation().toVector());
                    vec.multiply(0.101);

                    for (MyBlockData block : savedBlocks)
                        if (!block.getMat().equals(Material.AIR))
                            block.getFBlock().setVelocity(vec);
                }
            }
        };
        animationRunnable.runTaskTimerAsynchronously(plugin, 14, tickRate);
    }

    // Update the coordinates of a door based on its location, direction it's
    // pointing in and rotation direction.
    public static void updateCoords(Door door, DoorDirection currentDirection, RotateDirection rotDirection, int moved,
                                    boolean NS, boolean shadow)
    {
        int xMin = door.getMinimum().getBlockX();
        int yMin = door.getMinimum().getBlockY();
        int zMin = door.getMinimum().getBlockZ();
        int xMax = door.getMaximum().getBlockX();
        int yMax = door.getMaximum().getBlockY();
        int zMax = door.getMaximum().getBlockZ();

        int movedX = NS ? 0 : moved;
        int movedZ = NS ? moved : 0;

        Location newMax = new Location(door.getWorld(), xMax + movedX, yMax, zMax + movedZ);
        Location newMin = new Location(door.getWorld(), xMin + movedX, yMin, zMin + movedZ);

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
