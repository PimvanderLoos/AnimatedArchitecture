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

class RevolvingDoorMover extends BlockMover
{
    private int tickRate;
    private static final double maxSpeed = 3;
    private static final double minSpeed = 0.1;
    private final BiFunction<MyBlockData, Double, Vector> getGoalPos;
    private final double time;

    public RevolvingDoorMover(final BigDoors plugin, final World world, final DoorBase door, final double time, final double multiplier,
        final RotateDirection rotateDirection)
    {
        super(plugin, world, door, 30, false, null, null, -1);
        this.time = time;

        double speed = 1 * multiplier;
        speed = speed > maxSpeed ? 3 : speed < minSpeed ? minSpeed : speed;
        tickRate = Util.tickRateFromSpeed(speed);
        tickRate = 3;

        switch (rotateDirection)
        {
        case CLOCKWISE:
            getGoalPos = this::getGoalPosClockwise;
            break;
        case COUNTERCLOCKWISE:
            getGoalPos = this::getGoalPosCounterClockwise;
            break;
        default:
            getGoalPos = null;
            plugin.getMyLogger().dumpStackTrace("Failed to open door \"" + getDoorUID()
                + "\". Reason: Invalid rotateDirection \"" + rotateDirection.toString() + "\"");
            return;
        }

        super.constructFBlocks();
    }

    // Implement this one first.
    private Vector getGoalPosClockwise(MyBlockData block, double stepSum)
    {
        double startAngle = block.getStartAngle();
        double posX = 0.5 + door.getEngine().getX() - block.getRadius() * Math.sin(startAngle + stepSum);
        double posY = block.getStartY();
        double posZ = 0.5 + door.getEngine().getZ() - block.getRadius() * Math.cos(startAngle + stepSum);
        return new Vector(posX, posY, posZ);
    }

    private Vector getGoalPosCounterClockwise(MyBlockData block, double stepSum)
    {
        double startAngle = block.getStartAngle();
        double posX = 0.5 + door.getEngine().getX() - block.getRadius() * Math.sin(startAngle - stepSum);
        double posY = block.getStartY();
        double posZ = 0.5 + door.getEngine().getZ() - block.getRadius() * Math.cos(startAngle - stepSum);
        return new Vector(posX, posY, posZ);
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
            int endCount = (int) (20 / tickRate * time);
            int totalTicks = (int) (endCount * 1.1);
            long startTime = System.nanoTime();
            long lastTime;
            long currentTime = System.nanoTime();

            double step = (Math.PI / 2) / endCount * -1;

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

                if (!plugin.getDatabaseManager().canGo() || counter > totalTicks || isAborted.get())
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
                            double stepSum = counter * step;
                            Vector vec = getGoalPos.apply(block, stepSum)
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
        double deltaA = (door.getEngine().getX() - xAxis);
        double deltaB = door.getEngine().getZ() - zAxis;
        return (float) Math.sqrt(Math.pow(deltaA, 2) + Math.pow(deltaB, 2));
    }

    @Override
    protected float getStartAngle(int xAxis, int yAxis, int zAxis)
    {
        return (float) Math.atan2(door.getEngine().getBlockX() - xAxis, door.getEngine().getBlockZ() - zAxis);
    }
}
