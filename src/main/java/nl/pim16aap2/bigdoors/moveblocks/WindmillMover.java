package nl.pim16aap2.bigdoors.moveblocks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.util.MyBlockData;
import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.TriFunction;
import nl.pim16aap2.bigdoors.util.Util;

class WindmillMover extends BlockMover
{
    private final boolean NS;
    private int tickRate;
    private static final double maxSpeed = 3;
    private static final double minSpeed = 0.1;
    private final TriFunction<MyBlockData, Double, Double, Vector> getGoalPos;

    public WindmillMover(final BigDoors plugin, final World world, final Door door, final double multiplier,
        final RotateDirection rotateDirection)
    {
        super(plugin, world, door, 30, false, null, null, -1);

        int xLen = Math.abs(xMax - xMin) + 1;
        int zLen = Math.abs(zMax - zMin) + 1;
        NS = zLen > xLen ? true : false;

        double speed = 1 * multiplier;
        speed = speed > maxSpeed ? 3 : speed < minSpeed ? minSpeed : speed;
        tickRate = Util.tickRateFromSpeed(speed);
        tickRate = 3;

        switch (rotateDirection)
        {
        case NORTH:
            getGoalPos = this::getGoalPosNorth;
            break;
        case EAST:
            getGoalPos = this::getGoalPosEast;
            break;
        case SOUTH:
            getGoalPos = this::getGoalPosSouth;
            break;
        case WEST:
            getGoalPos = this::getGoalPosWest;
            break;
        default:
            getGoalPos = null;
            plugin.getMyLogger().dumpStackTrace("Failed to open door \"" + getDoorUID()
                + "\". Reason: Invalid rotateDirection \"" + rotateDirection.toString() + "\"");
            return;
        }

        super.constructFBlocks();
    }

    private double getStartAngleNS(MyBlockData block)
    {
        return Math.atan2(door.getEngine().getBlockZ() - block.getStartZ(), door.getEngine().getBlockY() - block.getStartY());
    }

    // Implement this one first.
    private Vector getGoalPosNorth(MyBlockData block, double counter, double step)
    {
        double startAngle = getStartAngleNS(block);
        double posX = block.getFBlock().getLocation().getX();
        double posY = door.getEngine().getY() + block.getRadius() * Math.cos(startAngle + step * counter);
        double posZ = door.getEngine().getZ() + block.getRadius() * Math.sin(startAngle + step * counter);
        return new Vector(posX, posY, posZ + 0.5);
    }

    private Vector getGoalPosEast(MyBlockData block, double counter, double step)
    {
        return null;
    }

    private Vector getGoalPosSouth(MyBlockData block, double counter, double step)
    {
        return null;
    }

    private Vector getGoalPosWest(MyBlockData block, double counter, double step)
    {
        return null;
    }

    @Override
    protected Location getNewLocation(double radius, double xAxis, double yAxis, double zAxis)
    {
        return new Location(world, xAxis, yAxis, zAxis);
    }

    // Method that takes care of the rotation aspect.
    @Override
    protected void animateEntities()
    {
        new BukkitRunnable()
        {
            double counter = 0;
            int endCount = (int) (20 / tickRate * time) * 4;
            int totalTicks = (int) (endCount * 1.1);
            long startTime = System.nanoTime();
            long lastTime;
            long currentTime = System.nanoTime();

            double step = (Math.PI / 2) / (endCount / 4) * -1 * 6;
//            double stepSum = 0.0d;

            @Override
            public void run()
            {
                lastTime = currentTime;
                currentTime = System.nanoTime();
                long msSinceStart = (currentTime - startTime) / 1000000;
                if (!plugin.getDatabaseManager().isPaused())
                    counter = msSinceStart / (50 * tickRate);
                else
                    startTime += currentTime - lastTime;

                if (!plugin.getDatabaseManager().canGo() || !door.canGo() || counter > totalTicks)
                {
                    Util.playSound(door.getEngine(), "bd.thud", 2f, 0.15f);
                    for (MyBlockData block : savedBlocks)
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
                    for (MyBlockData block : savedBlocks)
                    {
                        if (Math.abs(block.getRadius()) > 2 * Double.MIN_VALUE)
                        {
                            Vector vec = getGoalPos.apply(block, counter, step)
                                .subtract(block.getFBlock().getLocation().toVector());
                            vec.multiply(0.101);
                            block.getFBlock().setVelocity(vec);
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 14, tickRate);
    }

    @Override
    protected void updateCoords(Door door, MyBlockFace openDirection, RotateDirection upDown, int moved)
    {
        return;
    }

    @Override
    protected float getRadius(int xAxis, int yAxis, int zAxis)
    {
        double deltaA = (door.getEngine().getY() - yAxis);
        double deltaB = NS ? (door.getEngine().getZ() - zAxis) : (door.getEngine().getX() - xAxis);
        return (float) Math.sqrt(Math.pow(deltaA, 2) + Math.pow(deltaB, 2));
    }
}
