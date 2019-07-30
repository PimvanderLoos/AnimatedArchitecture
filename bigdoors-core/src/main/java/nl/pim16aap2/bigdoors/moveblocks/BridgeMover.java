package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICustomCraftFallingBlock;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLVerticalRotEast;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLVerticalRotNorth;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLVerticalRotSouth;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLVerticalRotWest;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.IGetNewLocation;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.TriFunction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

class BridgeMover extends BlockMover
{
    private final TriFunction<PBlockData, Double, Location, Vector> getDelta;
    private final IGetNewLocation gnl;
    private int tickRate;
    private double multiplier;
    private boolean NS;
    private PBlockFace engineSide;
    private double endStepSum;
    private Location turningPoint;
    private double startStepSum;
    private int stepMultiplier;

    /**
     * Constructs a {@link BlockMover}.
     *
     * @param plugin        The {@link BigDoors}.
     * @param world         The {@link World} in which the blocks will be moved.
     * @param door          The {@link DoorBase}.
     * @param time          The amount of time (in seconds) the door will try to toggle itself in.
     * @param instantOpen   If the door should be opened instantly (i.e. skip animation) or not.
     * @param upDown        Whether the {@link nl.pim16aap2.bigdoors.doors.DoorType#DRAWBRIDGE} should go up or down.
     * @param openDirection The direction the {@link DoorBase} will move.
     * @param multiplier    The speed multiplier.
     * @param playerUUID    The {@link UUID} of the player who opened this door.
     */
    BridgeMover(final @NotNull BigDoors plugin, final @NotNull World world, final double time,
                final @NotNull DoorBase door, final @NotNull PBlockFace upDown,
                final @NotNull RotateDirection openDirection, final boolean instantOpen, final double multiplier,
                @Nullable final UUID playerUUID)
    {
        super(plugin, world, door, time, instantOpen, upDown, openDirection, -1, playerUUID);

        engineSide = door.getEngineSide();
        NS = engineSide == PBlockFace.NORTH || engineSide == PBlockFace.SOUTH;

        int xLen = Math.abs(door.getMaximum().getBlockX() - door.getMinimum().getBlockX());
        int yLen = Math.abs(door.getMaximum().getBlockY() - door.getMinimum().getBlockY());
        int zLen = Math.abs(door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ());
        int doorSize = Math.max(xLen, Math.max(yLen, zLen)) + 1;
        double vars[] = SpigotUtil.calculateTimeAndTickRate(doorSize, time, multiplier, 5.2);
        this.time = vars[0];
        tickRate = (int) vars[1];
        this.multiplier = vars[2];

        // Pointing: Degrees:
        // UP__________0 or 360
        // EAST_______90
        // WEST______270
        // NORTH_____270
        // SOUTH______90

        startStepSum = -1;
        stepMultiplier = -1;

        // Calculate turningpoint and pointOpposite.
        switch (engineSide)
        {
            case NORTH:
                // When EngineSide is North, x goes from low to high and z goes from low to high
                turningPoint = new Location(world, xMin, yMin, zMin);
                if (upDown.equals(PBlockFace.UP))
                {
                    startStepSum = Math.PI / 2;
                    stepMultiplier = -1;
                }
                else
                {
                    if (openDirection.equals(RotateDirection.NORTH))
                        stepMultiplier = -1;
                    else if (openDirection.equals(RotateDirection.SOUTH))
                        stepMultiplier = 1;
                }
                break;

            case SOUTH:
                // When EngineSide is South, x goes from high to low and z goes from high to low
                turningPoint = new Location(world, xMax, yMin, zMax);

                if (upDown.equals(PBlockFace.UP))
                {
                    startStepSum = -Math.PI / 2;
                    stepMultiplier = 1;
                }
                else
                {
                    if (openDirection.equals(RotateDirection.NORTH))
                        stepMultiplier = -1;
                    else if (openDirection.equals(RotateDirection.SOUTH))
                        stepMultiplier = 1;
                }
                break;

            case EAST:
                // When EngineSide is East, x goes from high to low and z goes from low to high
                turningPoint = new Location(world, xMax, yMin, zMin);

                if (upDown.equals(PBlockFace.UP))
                {
                    startStepSum = -Math.PI / 2;
                    stepMultiplier = 1;
                }
                else
                {
                    if (openDirection.equals(RotateDirection.EAST))
                        stepMultiplier = 1;
                    else if (openDirection.equals(RotateDirection.WEST))
                        stepMultiplier = -1;
                }
                break;

            case WEST:
                // When EngineSide is West, x goes from low to high and z goes from high to low
                turningPoint = new Location(world, xMin, yMin, zMax);

                if (upDown.equals(PBlockFace.UP))
                {
                    startStepSum = Math.PI / 2;
                    stepMultiplier = -1;
                }
                else
                {
                    if (openDirection.equals(RotateDirection.EAST))
                        stepMultiplier = 1;
                    else if (openDirection.equals(RotateDirection.WEST))
                        stepMultiplier = -1;
                }
                break;
            default:
                plugin.getPLogger().dumpStackTrace("Invalid engine side for bridge mover: " + engineSide.toString());
                break;
        }

        endStepSum = upDown.equals(PBlockFace.UP) ? 0 : Math.PI / 2 * stepMultiplier;
        startStepSum = upDown.equals(PBlockFace.DOWN) ? 0 : startStepSum;

        switch (openDirection)
        {
            case NORTH:
                gnl = new GNLVerticalRotNorth(world, xMin, xMax, yMin, yMax, zMin, zMax, upDown, openDirection);
                getDelta = this::getDeltaNS;
                break;
            case EAST:
                gnl = new GNLVerticalRotEast(world, xMin, xMax, yMin, yMax, zMin, zMax, upDown, openDirection);
                getDelta = this::getDeltaEW;
                break;
            case SOUTH:
                gnl = new GNLVerticalRotSouth(world, xMin, xMax, yMin, yMax, zMin, zMax, upDown, openDirection);
                getDelta = this::getDeltaNS;
                break;
            case WEST:
                gnl = new GNLVerticalRotWest(world, xMin, xMax, yMin, yMax, zMin, zMax, upDown, openDirection);
                getDelta = this::getDeltaEW;
                break;
            default:
                plugin.getPLogger()
                      .warn("Failed to open door \"" + getDoorUID() + "\". Reason: Invalid rotateDirection \""
                                + openDirection.toString() + "\"");
                gnl = null;
                getDelta = null;
                return;
        }

        super.constructFBlocks();
    }

    private Vector getDeltaNS(PBlockData block, double stepSum, Location center)
    {
        double posX = block.getFBlock().getLocation().getX();
        double posY = center.getY() + block.getRadius() * Math.cos(stepSum);
        double posZ = center.getZ() + block.getRadius() * Math.sin(stepSum);
        return new Vector(posX, posY, posZ);
    }

    private Vector getDeltaEW(PBlockData block, double stepSum, Location center)
    {
        double posX = center.getX() + block.getRadius() * Math.sin(stepSum);
        double posY = center.getY() + block.getRadius() * Math.cos(stepSum);
        double posZ = block.getFBlock().getLocation().getZ();
        return new Vector(posX, posY, posZ);
    }

    // Method that takes care of the rotation aspect.
    @Override
    protected void animateEntities()
    {
        new BukkitRunnable()
        {
            Location center = new Location(world, turningPoint.getBlockX() + 0.5, yMin, turningPoint.getBlockZ() + 0.5);
            boolean replace = false;
            double counter = 0;
            int endCount = (int) (20 / tickRate * time);
            double step = (Math.PI / 2) / endCount * stepMultiplier;
            double stepSum = startStepSum;
            int totalTicks = (int) (endCount * multiplier);
            int replaceCount = endCount / 2;
            long startTime = System.nanoTime();
            long lastTime;
            long currentTime = System.nanoTime();

            @Override
            public void run()
            {
                if (counter == 0 || (counter < endCount - 45 / tickRate && counter % (6 * tickRate / 4) == 0))
                    SpigotUtil.playSound(door.getEngine(), "bd.drawbridge-rattling", 0.8f, 0.7f);

                lastTime = currentTime;
                currentTime = System.nanoTime();
                long msSinceStart = (currentTime - startTime) / 1000000;
                if (!plugin.getDatabaseManager().isPaused())
                    counter = msSinceStart / (50 * tickRate);
                else
                    startTime += currentTime - lastTime;

                if (counter < endCount - 1)
                    stepSum = startStepSum + step * counter;
                else
                    stepSum = endStepSum;
                replace = counter == replaceCount;

                if (!plugin.getDatabaseManager().canGo() || isAborted.get() || counter > totalTicks)
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
                    // It is not pssible to edit falling block blockdata (client won't update it),
                    // so delete the current fBlock and replace it by one that's been rotated.
                    // Also, this stuff needs to be done on the main thread.
                    if (replace)
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
                        {
                            for (PBlockData block : savedBlocks)
                                if (block.canRot())
                                {
                                    Location loc = block.getFBlock().getLocation();
                                    Vector veloc = block.getFBlock().getVelocity();

                                    ICustomCraftFallingBlock fBlock;
                                    // Because the block in savedBlocks is already rotated where applicable, just
                                    // use that block now.
                                    fBlock = fallingBlockFactory(loc, block.getBlock());

                                    block.getFBlock().remove();
                                    block.setFBlock(fBlock);

                                    block.getFBlock().setVelocity(veloc);
                                }
                        }, 0);

                    for (PBlockData block : savedBlocks)
                    {
                        double radius = block.getRadius();
                        if (radius != 0)
                        {
                            Vector vec = getDelta.apply(block, stepSum, center)
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
    protected Location getNewLocation(double radius, double xAxis, double yAxis, double zAxis)
    {
        return gnl.getNewLocation(radius, xAxis, yAxis, zAxis);
    }

    @Override
    protected float getRadius(int xAxis, int yAxis, int zAxis)
    {
        if (currentDirection == PBlockFace.UP)
        {
            if (NS)
                return Math.abs(zAxis - turningPoint.getBlockZ());
            return Math.abs(xAxis - turningPoint.getBlockX());
        }
        if (currentDirection == PBlockFace.DOWN)
            return yAxis - turningPoint.getBlockY();
        plugin.getPLogger().dumpStackTrace("Invalid BridgeMover direction \"" + currentDirection.toString() + "\"");
        return -1;
    }
}
