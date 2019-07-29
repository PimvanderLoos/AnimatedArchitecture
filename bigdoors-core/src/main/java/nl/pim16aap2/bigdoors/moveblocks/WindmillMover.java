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

class WindmillMover extends BlockMover
{
    private static final double maxSpeed = 3;
    private static final double minSpeed = 0.1;
    private final boolean NS;
    private final BiFunction<PBlockData, Double, Vector> getVector;
    private int tickRate;

    WindmillMover(final @NotNull BigDoors plugin, final @NotNull World world, final @NotNull DoorBase door,
                  final double time, final double multiplier, final @NotNull RotateDirection rotateDirection,
                  @Nullable final UUID playerUUID)
    {
        super(plugin, world, door, time, false, PBlockFace.UP, RotateDirection.NONE, -1, playerUUID);

        int xLen = Math.abs(xMax - xMin) + 1;
        int zLen = Math.abs(zMax - zMin) + 1;
        NS = zLen > xLen;

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
                plugin.getPLogger().dumpStackTrace("Failed to open door \"" + getDoorUID()
                                                       + "\". Reason: Invalid rotateDirection \"" +
                                                       rotateDirection.toString() + "\"");
                return;
        }

        super.constructFBlocks();
    }

    private Vector getVectorNorth(PBlockData block, double stepSum)
    {
        double startAngle = block.getStartAngle();
        double posX = block.getFBlock().getLocation().getX();
        double posY = door.getEngine().getY() - block.getRadius() * Math.cos(startAngle - stepSum);
        double posZ = door.getEngine().getZ() - block.getRadius() * Math.sin(startAngle - stepSum);
        return new Vector(posX, posY, posZ + 0.5);
    }

    private Vector getVectorEast(PBlockData block, double stepSum)
    {
        double startAngle = block.getStartAngle();
        double posX = door.getEngine().getX() - block.getRadius() * Math.sin(startAngle - stepSum);
        double posY = door.getEngine().getY() - block.getRadius() * Math.cos(startAngle - stepSum);
        double posZ = block.getFBlock().getLocation().getZ();
        return new Vector(posX + 0.5, posY, posZ);
    }

    private Vector getVectorSouth(PBlockData block, double stepSum)
    {
        float startAngle = block.getStartAngle();
        double posX = block.getFBlock().getLocation().getX();
        double posY = door.getEngine().getY() - block.getRadius() * Math.cos(startAngle + stepSum);
        double posZ = door.getEngine().getZ() - block.getRadius() * Math.sin(startAngle + stepSum);
        return new Vector(posX, posY, posZ + 0.5);
    }

    private Vector getVectorWest(PBlockData block, double stepSum)
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

                if (!plugin.getDatabaseManager().canGo() || isAborted.get() || counter > totalTicks)
                {
                    SpigotUtil.playSound(door.getEngine(), "bd.thud", 2f, 0.15f);
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
                {
                    for (PBlockData block : savedBlocks)
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
