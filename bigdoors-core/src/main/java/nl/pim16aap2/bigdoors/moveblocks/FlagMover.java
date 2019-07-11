package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.function.BiFunction;

class FlagMover extends BlockMover
{
    private static final double maxSpeed = 3;
    private static final double minSpeed = 0.1;
    private final BiFunction<PBlockData, Double, Vector> getGoalPos;
    private final boolean NS;
    private int tickRate;

    public FlagMover(final BigDoors plugin, final World world, final double time, final DoorBase door,
                     final double multiplier)
    {
        super(plugin, world, door, time, false, null, null, -1);

        int xLen = Math.abs(xMax - xMin) + 1;
        int zLen = Math.abs(zMax - zMin) + 1;
        NS = zLen > xLen;
        getGoalPos = NS ? this::getGoalPosNS : this::getGoalPosEW;

        double speed = 1 * multiplier;
        speed = speed > maxSpeed ? 3 : speed < minSpeed ? minSpeed : speed;
        this.time = time;
        tickRate = SpigotUtil.tickRateFromSpeed(speed);
        tickRate = 3;

        super.constructFBlocks();
    }

    private double getOffset(double counter, float distanceToEng, float radius)
    {
        double baseOffset = Math.sin(0.5 * Math.PI * (counter * tickRate / 20) + distanceToEng);
        double maxVal = 0.25 * radius;
        maxVal = maxVal > 0.75 ? 0.75 : maxVal;
        return baseOffset > maxVal ? maxVal : baseOffset;
    }

    private Vector getGoalPosNS(PBlockData block, double counter)
    {
        double xOff = 3 - 1 / (tickRate / 20); // WTF is this?
        if (block.getRadius() > 0)
            xOff = getOffset(counter, block.getRadius(), block.getRadius());
        return new Vector(block.getStartX() + xOff, block.getStartY(), block.getStartZ());
    }

    private Vector getGoalPosEW(PBlockData block, double counter)
    {
        double zOff = 3 - 1 / (tickRate / 20); // WTF is this?
        if (block.getRadius() > 0)
            zOff = getOffset(counter, block.getRadius(), block.getRadius());
        return new Vector(block.getStartX(), block.getStartY(), block.getStartZ() + zOff);
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
            boolean hasFinished = false;

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
                    for (PBlockData block : savedBlocks)
                        block.getFBlock().setVelocity(new Vector(0D, 0D, 0D));
                    Bukkit.getScheduler().callSyncMethod(plugin, () ->
                    {
                        if (!hasFinished)
                        {
                            putBlocks(false);
                            hasFinished = true;
                        }
                        return null;
                    });
                    cancel();
                }
                else
                    for (PBlockData block : savedBlocks)
                    {
                        Vector vec = getGoalPos.apply(block, counter)
                                               .subtract(block.getFBlock().getLocation().toVector());
                        vec.multiply(0.101);
                        block.getFBlock().setVelocity(vec);
                    }
            }
        }.runTaskTimerAsynchronously(plugin, 14, tickRate);
    }

    @Override
    protected float getRadius(int xAxis, int yAxis, int zAxis)
    {
        if (NS)
            return Math.abs(zAxis - door.getEngine().getBlockZ());
        return Math.abs(xAxis - door.getEngine().getBlockX());
    }
}
