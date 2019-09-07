package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiFunction;

public class RevolvingDoorMover extends BlockMover
{
    private static final double maxSpeed = 3;
    private static final double minSpeed = 0.1;
    private final BiFunction<PBlockData, Double, Vector3Dd> getGoalPos;
    private final double time;
    private int tickRate;

    public RevolvingDoorMover(final @NotNull DoorBase door, final double time, final double multiplier,
                              final @NotNull RotateDirection rotateDirection, @Nullable final UUID playerUUID)
    {
        super(door, 30, false, PBlockFace.UP, RotateDirection.NONE, -1, playerUUID, door.getMinimum(),
              door.getMaximum());
        this.time = time;

        double speed = 1 * multiplier;
        speed = speed > maxSpeed ? 3 : Math.max(speed, minSpeed);
        tickRate = SpigotUtil.tickRateFromSpeed(speed);
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
                plugin.getPLogger().dumpStackTrace("Failed to open door \"" + getDoorUID()
                                                       + "\". Reason: Invalid rotateDirection \"" +
                                                       rotateDirection.toString() + "\"");
                return;
        }

        super.constructFBlocks();
    }

    // Implement this one first.
    private Vector3Dd getGoalPosClockwise(PBlockData block, double stepSum)
    {
        double startAngle = block.getStartAngle();
        double posX = 0.5 + door.getEngine().getX() - block.getRadius() * Math.sin(startAngle + stepSum);
        double posY = block.getStartY();
        double posZ = 0.5 + door.getEngine().getZ() - block.getRadius() * Math.cos(startAngle + stepSum);
        return new Vector3Dd(posX, posY, posZ);
    }

    private Vector3Dd getGoalPosCounterClockwise(PBlockData block, double stepSum)
    {
        double startAngle = block.getStartAngle();
        double posX = 0.5 + door.getEngine().getX() - block.getRadius() * Math.sin(startAngle - stepSum);
        double posY = block.getStartY();
        double posZ = 0.5 + door.getEngine().getZ() - block.getRadius() * Math.cos(startAngle - stepSum);
        return new Vector3Dd(posX, posY, posZ);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IPLocation getNewLocation(double radius, double xAxis, double yAxis, double zAxis)
    {
        return locationFactory.create(world, xAxis, yAxis, zAxis);
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
            double step = (Math.PI / 2) / endCount * -1;

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
                        block.getFBlock().setVelocity(new Vector3Dd(0D, 0D, 0D));
                    Bukkit.getScheduler().callSyncMethod(plugin, () ->
                    {
                        putBlocks(false);
                        return null;
                    });
                    cancel();
                }
                else
                {
                    for (PBlockData block : savedBlocks)
                    {
                        if (Math.abs(block.getRadius()) > 2 * Double.MIN_VALUE)
                        {
                            double stepSum = counter * step;
                            Vector3Dd vec = getGoalPos.apply(block, stepSum)
                                                      .subtract(block.getFBlock().getPosition());
                            vec.multiply(0.101);
                            block.getFBlock().setVelocity(vec);
                        }
                    }
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
        double deltaA = (door.getEngine().getX() - xAxis);
        double deltaB = door.getEngine().getZ() - zAxis;
        return (float) Math.sqrt(Math.pow(deltaA, 2) + Math.pow(deltaB, 2));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected float getStartAngle(int xAxis, int yAxis, int zAxis)
    {
        return (float) Math.atan2(door.getEngine().getBlockX() - xAxis, door.getEngine().getBlockZ() - zAxis);
    }
}
