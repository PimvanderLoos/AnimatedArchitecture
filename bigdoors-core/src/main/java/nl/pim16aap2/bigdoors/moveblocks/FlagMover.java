package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiFunction;

public class FlagMover extends BlockMover
{
    private static final double maxSpeed = 3;
    private static final double minSpeed = 0.1;
    private final BiFunction<PBlockData, Double, Vector> getGoalPos;
    private final boolean NS;
    private int tickRate;

    public FlagMover(final @NotNull BigDoors plugin, final @NotNull World world, final double time,
                     final @NotNull DoorBase door, final double multiplier, @Nullable UUID playerUUID)
    {
        super(plugin, world, door, time, false, PBlockFace.UP, RotateDirection.NONE, -1, playerUUID);

        int xLen = Math.abs(xMax - xMin) + 1;
        int zLen = Math.abs(zMax - zMin) + 1;
        NS = zLen > xLen;
        getGoalPos = NS ? this::getGoalPosNS : this::getGoalPosEW;

        double speed = 1 * multiplier;
        speed = speed > maxSpeed ? 3 : Math.max(speed, minSpeed);
        this.time = time;
        tickRate = SpigotUtil.tickRateFromSpeed(speed);
        tickRate = 3;

        super.constructFBlocks();
    }

    private double getOffset(double counter, float distanceToEng, float radius)
    {
        double baseOffset = Math.sin(0.5 * Math.PI * (counter * tickRate / 20) + distanceToEng);
        double maxVal = 0.25 * radius;
        maxVal = Math.min(maxVal, 0.75);
        return Math.min(baseOffset, maxVal);
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected Location getNewLocation(double radius, double xAxis, double yAxis, double zAxis)
    {
        return new Location(world, xAxis, yAxis, zAxis);
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

            @Override
            public void run()
            {
                lastTime = currentTime;
                currentTime = System.nanoTime();
                startTime += currentTime - lastTime;

                if (counter > totalTicks || isAborted.get())
                {
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected float getRadius(int xAxis, int yAxis, int zAxis)
    {
        if (NS)
            return Math.abs(zAxis - door.getEngine().getBlockZ());
        return Math.abs(xAxis - door.getEngine().getBlockX());
    }
}
