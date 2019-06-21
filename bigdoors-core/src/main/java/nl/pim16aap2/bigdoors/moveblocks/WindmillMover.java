package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.MyBlockData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.spigotutil.Util;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.function.BiFunction;

class WindmillMover extends BlockMover
{
    private final boolean NS;
    private int tickRate;
    private static final double maxSpeed = 3;
    private static final double minSpeed = 0.1;
    private final BiFunction<MyBlockData, Double, Vector> getVector;

    public WindmillMover(final BigDoors plugin, final World world, final DoorBase door, final double multiplier,
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
            getVector = this::getVectorNorth;
            break;
        case EAST:
            getVector = this::getVectorEast;
            break;
        case SOUTH:
            getVector = this::getVectorSouth;
            break;
        case WEST:
            getVector = this::getVectorWest;
            break;
        default:
            getVector = null;
            plugin.getMyLogger().dumpStackTrace("Failed to open door \"" + getDoorUID()
                + "\". Reason: Invalid rotateDirection \"" + rotateDirection.toString() + "\"");
            return;
        }

        super.constructFBlocks();
    }

    private Vector getVectorNorth(MyBlockData block, double stepSum)
    {
        double startAngle = block.getStartAngle();
        double posX = block.getFBlock().getLocation().getX();
        double posY = door.getEngine().getY() - block.getRadius() * Math.cos(startAngle - stepSum);
        double posZ = door.getEngine().getZ() - block.getRadius() * Math.sin(startAngle - stepSum);
        return new Vector(posX, posY, posZ + 0.5);
    }

    private Vector getVectorEast(MyBlockData block, double stepSum)
    {
        double startAngle = block.getStartAngle();
        double posX = door.getEngine().getX() - block.getRadius() * Math.sin(startAngle - stepSum);
        double posY = door.getEngine().getY() - block.getRadius() * Math.cos(startAngle - stepSum);
        double posZ = block.getFBlock().getLocation().getZ();
        return new Vector(posX + 0.5, posY, posZ);
    }

    private Vector getVectorSouth(MyBlockData block, double stepSum)
    {
        float startAngle = block.getStartAngle();
        double posX = block.getFBlock().getLocation().getX();
        double posY = door.getEngine().getY() - block.getRadius() * Math.cos(startAngle + stepSum);
        double posZ = door.getEngine().getZ() - block.getRadius() * Math.sin(startAngle + stepSum);
        return new Vector(posX, posY, posZ + 0.5);
    }

    private Vector getVectorWest(MyBlockData block, double stepSum)
    {
        float startAngle = block.getStartAngle();
        double posX = door.getEngine().getX() - block.getRadius() * Math.sin(startAngle + stepSum);
        double posY = door.getEngine().getY() - block.getRadius() * Math.cos(startAngle + stepSum);
        double posZ = block.getFBlock().getLocation().getZ();
        return new Vector(posX + 0.5, posY, posZ);
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

                if (!plugin.getDatabaseManager().canGo() || isAborted.get() || counter > totalTicks)
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
                            double stepSum = step * counter;
                            Vector vec = getVector.apply(block, stepSum)
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
    protected float getRadius(int xAxis, int yAxis, int zAxis)
    {
        double deltaA = (door.getEngine().getY() - yAxis);
        double deltaB = NS ? (door.getEngine().getZ() - zAxis) : (door.getEngine().getX() - xAxis);
        return (float) Math.sqrt(Math.pow(deltaA, 2) + Math.pow(deltaB, 2));
    }

    @Override
    protected float getStartAngle(int xAxis, int yAxis, int zAxis)
    {
        float deltaA = NS ? door.getEngine().getBlockZ() - zAxis : door.getEngine().getBlockX() - xAxis;
        float deltaB = door.getEngine().getBlockY() - yAxis;
        return (float) Math.atan2(deltaA, deltaB);
    }
}
