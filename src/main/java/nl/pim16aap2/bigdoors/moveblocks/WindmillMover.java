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
import nl.pim16aap2.bigdoors.util.Util;

class WindmillMover extends BlockMover
{
    private final boolean NS;
    private int tickRate;
    private static final double maxSpeed = 3;
    private static final double minSpeed = 0.1;

    public WindmillMover(final BigDoors plugin, final World world, final double time, final Door door,
        final double multiplier)
    {
        super(plugin, world, door, time, false, null, null, -1);

        int xLen = Math.abs(xMax - xMin) + 1;
        int zLen = Math.abs(zMax - zMin) + 1;
        NS = zLen > xLen ? true : false;

        double speed = 1 * multiplier;
        speed = speed > maxSpeed ? 3 : speed < minSpeed ? minSpeed : speed;
        this.time = time;
        tickRate = Util.tickRateFromSpeed(speed);
        tickRate = 3;

        super.constructFBlocks();
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
            double counter   = 0;
            int endCount     = (int) (20 / tickRate * time);
            int totalTicks   = (int) (endCount * 1.1);
            long startTime   = System.nanoTime();
            long lastTime;
            long currentTime = System.nanoTime();

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
                    for (int idx = 0; idx < savedBlocks.size(); ++idx)
                        savedBlocks.get(idx).getFBlock().setVelocity(new Vector(0D, 0D, 0D));
                    Bukkit.getScheduler().callSyncMethod(plugin, () ->
                    {
                        putBlocks(false);
                        return null;
                    });
                    cancel();
                }
                else
                    for (MyBlockData block : savedBlocks)
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
        return -1;
    }
}
