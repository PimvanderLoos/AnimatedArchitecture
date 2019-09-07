package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLHorizontalRotEast;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLHorizontalRotNorth;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLHorizontalRotSouth;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLHorizontalRotWest;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.IGetNewLocation;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CylindricalMover extends BlockMover
{
    private final int tickRate;
    private final int stepMultiplier;
    private final Location turningPoint;
    private final IGetNewLocation gnl;
    private double endStepSum;
    private double multiplier;
    private double startStepSum;

    public CylindricalMover(final @NotNull RotateDirection rotDirection, final double time,
                            final @NotNull PBlockFace currentDirection, final @NotNull DoorBase door,
                            final boolean instantOpen, final double multiplier, @Nullable final UUID playerUUID,
                            final @NotNull Location finalMin, final @NotNull Location finalMax)
    {
        super(door, time, instantOpen, currentDirection, rotDirection, -1, playerUUID, finalMin, finalMax);

        turningPoint = door.getEngine();
        stepMultiplier = rotDirection == RotateDirection.CLOCKWISE ? -1 : 1;

        int xLen = Math.abs(door.getMaximum().getBlockX() - door.getMinimum().getBlockX());
        int zLen = Math.abs(door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ());
        int doorLength = Math.max(xLen, zLen) + 1;
        double vars[] = SpigotUtil.calculateTimeAndTickRate(doorLength, time, multiplier, 3.7);
        this.time = vars[0];
        tickRate = (int) vars[1];
        this.multiplier = vars[2];

        switch (currentDirection)
        {
            case NORTH:
                gnl = new GNLHorizontalRotNorth(world, xMin, xMax, zMin, zMax, rotDirection);
                startStepSum = Math.PI;
                endStepSum = rotDirection == RotateDirection.CLOCKWISE ? Math.PI / 2 : 3 * Math.PI / 2;
                break;
            case EAST:
                gnl = new GNLHorizontalRotEast(world, xMin, xMax, zMin, zMax, rotDirection);
                startStepSum = Math.PI / 2;
                endStepSum = rotDirection == RotateDirection.CLOCKWISE ? 0 : Math.PI;
                break;
            case SOUTH:
                gnl = new GNLHorizontalRotSouth(world, xMin, xMax, zMin, zMax, rotDirection);
                startStepSum = 0;
                endStepSum = rotDirection == RotateDirection.CLOCKWISE ? 3 * Math.PI / 2 : Math.PI / 2;
                break;
            case WEST:
                gnl = new GNLHorizontalRotWest(world, xMin, xMax, zMin, zMax, rotDirection);
                startStepSum = 3 * Math.PI / 2;
                endStepSum = rotDirection == RotateDirection.CLOCKWISE ? Math.PI : 0;
                break;
            default:
                plugin.getPLogger()
                      .dumpStackTrace("Invalid currentDirection for cylindrical mover: " + currentDirection.toString());
                gnl = null;
                break;
        }

        super.constructFBlocks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void animateEntities()
    {
        new BukkitRunnable()
        {
            IPLocation center = locationFactory.create(world, turningPoint.getBlockX() + 0.5, yMin,
                                                       turningPoint.getBlockZ() + 0.5);
            boolean replace = false;
            double counter = 0;
            int endCount = (int) (20 / tickRate * time);
            double step = (Math.PI / 2) / endCount * stepMultiplier;
            double stepSum = startStepSum;
            int totalTicks = (int) (endCount * multiplier);
            int replaceCount = endCount / 2;
            Long startTime = null; // Initialize on the first run, for better accuracy.
            long lastTime;
            long currentTime = System.nanoTime();

            @Override
            public void run()
            {
                if (startTime == null)
                    startTime = System.nanoTime();
                ++counter;

                if (counter == 0 || (counter < endCount - 27 / tickRate && counter % (5 * tickRate / 4) == 0))
                    SpigotUtil.playSound(door.getEngine(), "bd.dragging2", 0.5f, 0.6f);

                lastTime = currentTime;
                currentTime = System.nanoTime();
                startTime += currentTime - lastTime;

                if (counter < endCount - 1)
                    stepSum = startStepSum + step * counter;
                else
                    stepSum = endStepSum;

                replace = (counter == replaceCount);

                if (counter > totalTicks || isAborted.get())
                {
                    SpigotUtil.playSound(door.getEngine(), "bd.closing-vault-door", 0.2f, 1f);
                    for (PBlockData savedBlock : savedBlocks)
                        savedBlock.getFBlock().setVelocity(new Vector3Dd(0D, 0D, 0D));

                    Bukkit.getScheduler().callSyncMethod(plugin, () ->
                    {
                        putBlocks(false);
                        return null;
                    });
                    cancel();
                }
                else
                {
                    // It is not possible to edit falling block blockdata (client won't update it),
                    // so delete the current fBlock and replace it by one that's been rotated.
                    // Also, this stuff needs to be done on the main thread.
                    if (replace)
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> respawnBlocks(), 0);

                    double sin = Math.sin(stepSum);
                    double cos = Math.cos(stepSum);

                    for (PBlockData block : savedBlocks)
                    {
                        double radius = block.getRadius();
                        int yPos = block.getStartLocation().getBlockY();

                        if (radius != 0)
                        {
                            double addX = radius * sin;
                            double addZ = radius * cos;

                            Vector3Dd position = new Vector3Dd(center.getX() + addX, yPos, center.getZ() + addZ);
                            Vector3Dd vec = position.subtract(block.getFBlock().getPosition());
                            block.getFBlock().setVelocity(vec.multiply(0.101));
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
    protected IPLocation getNewLocation(double radius, double xAxis, double yAxis, double zAxis)
    {
        return gnl.getNewLocation(radius, xAxis, yAxis, zAxis);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected float getRadius(int xAxis, int yAxis, int zAxis)
    {
        // Get the radius of this pillar.
        return Math.max(Math.abs(xAxis - turningPoint.getBlockX()), Math.abs(zAxis - turningPoint.getBlockZ()));
    }
}
