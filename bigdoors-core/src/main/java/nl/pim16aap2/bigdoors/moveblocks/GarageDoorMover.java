package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector3D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiFunction;

public class GarageDoorMover extends BlockMover
{
    private static final double maxSpeed = 3;
    private static final double minSpeed = 0.1;
    private final double time;
    private final double resultHeight;
    private final Vector3D directionVec;
    private int tickRate;
    private BiFunction<PBlockData, Double, Vector> getVector;
    private int xLen, yLen, zLen;

    public GarageDoorMover(final @NotNull BigDoors plugin, final @NotNull World world, final @NotNull DoorBase door,
                           final double time, final double multiplier, final boolean instantOpen,
                           final @NotNull PBlockFace currentDirection, final @NotNull RotateDirection rotateDirection,
                           @Nullable final UUID playerUUID)
    {
        super(plugin, world, door, 30, instantOpen, currentDirection, rotateDirection, -1, playerUUID);
        this.time = time;

        double speed = 1 * multiplier;
        speed = speed > maxSpeed ? 3 : Math.max(speed, minSpeed);
        tickRate = SpigotUtil.tickRateFromSpeed(speed);
        tickRate = 3;

        resultHeight = door.getMaximum().getBlockY() + 1;

        BiFunction<PBlockData, Double, Vector> getVectorTmp;
        switch (rotateDirection)
        {
            case NORTH:
                directionVec = PBlockFace.getDirection(PBlockFace.NORTH);
                getVectorTmp = this::getVectorDownNorth;
                break;
            case EAST:
                directionVec = PBlockFace.getDirection(PBlockFace.EAST);
                getVectorTmp = this::getVectorDownEast;
                break;
            case SOUTH:
                directionVec = PBlockFace.getDirection(PBlockFace.SOUTH);
                getVectorTmp = this::getVectorDownSouth;
                break;
            case WEST:
                directionVec = PBlockFace.getDirection(PBlockFace.WEST);
                getVectorTmp = this::getVectorDownWest;
                break;
            default:
                directionVec = null;
                plugin.getPLogger().dumpStackTrace("Failed to open garage door \"" + getDoorUID()
                                                       + "\". Reason: Invalid rotateDirection \"" +
                                                       rotateDirection.toString() + "\"");
                return;
        }

        xLen = xMax - xMin;
        yLen = yMax - yMin;
        zLen = zMax - zMin;

        if (currentDirection.equals(PBlockFace.UP))
        {
            super.blocksMoved = yLen + 1;
            getVector = this::getVectorUp;
        }
        else
        {
            super.blocksMoved = (xLen + 1) * directionVec.getX() + (yLen + 1) * directionVec.getY()
                + (zLen + 1) * directionVec.getZ();
            getVector = getVectorTmp;
        }

        super.constructFBlocks();
    }

    private Vector getVectorUp(final @NotNull PBlockData block, final double stepSum)
    {
        double currentHeight = Math.min(resultHeight, block.getStartY() + stepSum);
        double xMod = 0;
        double yMod = stepSum;
        double zMod = 0;

        if (currentHeight >= door.getMaximum().getBlockY())
        {
            double horizontal = Math.max(0, stepSum - block.getRadius() - 0.5);
            xMod = directionVec.getX() * horizontal;
            yMod = Math.min(resultHeight - block.getStartY(), stepSum);
            zMod = directionVec.getZ() * horizontal;
        }
        return new Vector(block.getStartX() + xMod, block.getStartY() + yMod, block.getStartZ() + zMod);
    }

    private Vector getVectorDownNorth(final @NotNull PBlockData block, final double stepSum)
    {
        double goalZ = door.getEngine().getBlockZ() - 0.5;
        double pivotZ = goalZ + 1.5;
        double currentZ = Math.max(goalZ, block.getStartZ() - stepSum);

        double xMod = 0;
        double yMod = 0;
        double zMod = -stepSum;

        if (currentZ <= pivotZ)
        {
            yMod = -Math.max(0, stepSum - block.getRadius() + 0.5);
            zMod = Math.max(goalZ - block.getStartLocation().getBlockZ() + 0.5, zMod);
        }

        return new Vector(block.getStartX() + xMod, block.getStartY() + yMod, block.getStartZ() + zMod);
    }

    private Vector getVectorDownSouth(final @NotNull PBlockData block, final double stepSum)
    {
        double goalZ = door.getEngine().getBlockZ() - 0.5;
        double pivotZ = goalZ - 1.5;
        double currentZ = Math.min(goalZ, block.getStartZ() + stepSum);

        double xMod = 0;
        double yMod = 0;
        double zMod = stepSum;

        if (currentZ >= pivotZ)
        {
            yMod = -Math.max(0, stepSum - block.getRadius() + 0.5);
            zMod = Math.min(goalZ - block.getStartLocation().getBlockZ() + 0.5, zMod);
        }
        return new Vector(block.getStartX() + xMod, block.getStartY() + yMod, block.getStartZ() + zMod);
    }

    private Vector getVectorDownEast(final @NotNull PBlockData block, final double stepSum)
    {
        double goalX = door.getEngine().getBlockX() - 0.5;
        double pivotX = goalX - 1.5;
        double currentX = Math.min(goalX, block.getStartX() + stepSum);

        double xMod = stepSum;
        double yMod = 0;
        double zMod = 0;

        if (currentX >= pivotX)
        {
            xMod = Math.min(goalX - block.getStartLocation().getBlockX() + 0.5, xMod);
            yMod = -Math.max(0, stepSum - block.getRadius() + 0.5);
        }
        return new Vector(block.getStartX() + xMod, block.getStartY() + yMod, block.getStartZ() + zMod);
    }

    private Vector getVectorDownWest(final @NotNull PBlockData block, final double stepSum)
    {

        double goalX = door.getEngine().getBlockX() - 0.5;
        double pivotX = goalX + 1.5;
        double currentX = Math.max(goalX, block.getStartX() - stepSum);

        double xMod = -stepSum;
        double yMod = 0;
        double zMod = 0;
        boolean test = false;

        if (currentX <= pivotX)
        {
            test = true;

            xMod = Math.max(goalX - block.getStartLocation().getBlockX() + 0.5, xMod);
            yMod = -Math.max(0, stepSum - block.getRadius() + 0.5);
        }

        SpigotUtil.broadcastMessage(String.format(
            "%.5b: goalX: %.2f, pivotX: %.2f, currentX: %.2f, stepSum: %.2f, "
                + "yMod: %.2f, xMod: %.2f, radius: %.2f, startX: %.2f, BTM: %.1f, test: %.2f",
            test, goalX, pivotX, currentX, stepSum, yMod, xMod, block.getRadius(),
            block.getStartX(), getBlocksMoved(), (goalX - block.getStartLocation().getBlockX() + 0.5)));

        return new Vector(block.getStartX() + xMod, block.getStartY() + yMod, block.getStartZ() + zMod);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Location getNewLocation(final double radius, final double xAxis, final double yAxis, final double zAxis)
    {
        double newX, newY, newZ;

        if (currentDirection.equals(PBlockFace.UP))
        {
            newX = xAxis + (1 + yLen - radius) * directionVec.getX();
            newY = resultHeight;
            newZ = zAxis + (1 + yLen - radius) * directionVec.getZ();
        }
        else
        {
            if (directionVec.getX() == 0)
            {
                newX = xAxis;
                newY = door.getMaximum().getY() - (zLen - radius);
                newZ = door.getEngine().getZ();
            }
            else
            {
                newX = door.getEngine().getX();
                newY = door.getMaximum().getY() - (xLen - radius);
                newZ = zAxis;
            }
            newY -= 2;
        }
        return new Location(world, newX, newY, newZ);


//        Location newLoc = new Location(world, newX, newY, newZ);
//        newLoc.getBlock().setType(Material.LAPIS_BLOCK);
//        SpigotUtil.broadcastMessage("newLoc: " + SpigotUtil.locIntToString(newLoc));
//        return new Location(world, xAxis, yAxis, zAxis);
    }

    private double getBlocksMoved()
    {
        return Math.abs(blocksMoved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void animateEntities()
    {
        new BukkitRunnable()
        {
            double counter = 0;
            int endCount = (int) (20 / tickRate * time);
            int totalTicks = (int) (endCount * 1.1);
            long startTime = System.nanoTime();
            long lastTime;
            long currentTime = System.nanoTime();
            double step = getBlocksMoved() / endCount;
            double stepSum = 0;

            @Override
            public void run()
            {
                ++counter;
                lastTime = currentTime;
                currentTime = System.nanoTime();
                startTime += currentTime - lastTime;

                if (counter > totalTicks || isAborted.get())
                {
                    SpigotUtil.playSound(door.getEngine(), "bd.thud", 2f, 0.15f);
                    for (PBlockData block : savedBlocks)
                        block.getFBlock().setVelocity(new Vector(0D, 0D, 0D));
                    Bukkit.getScheduler().callSyncMethod(plugin, () ->
                    {
                        putBlocks(false);
                        return null;
                    });
                    cancel();
                }
                else
                {
                    stepSum = counter * step;
                    for (PBlockData block : savedBlocks)
                    {
                        Vector vec = getVector.apply(block, stepSum)
                                              .subtract(block.getFBlock().getLocation().toVector());
                        vec.multiply(0.101);
                        block.getFBlock().setVelocity(vec);
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 14, tickRate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected float getRadius(final int xAxis, final int yAxis, final int zAxis)
    {
        if (currentDirection.equals(PBlockFace.UP))
        {
            float height = door.getMaximum().getBlockY();
            return height - yAxis;
        }

        int dX = Math.abs(xAxis - door.getEngine().getBlockX());
        int dZ = Math.abs(zAxis - door.getEngine().getBlockZ());
        return Math.abs(dX * directionVec.getX() + dZ * directionVec.getZ());
    }
}
