package nl.pim16aap2.bigDoors.moveBlocks;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.ChunkUtils;
import nl.pim16aap2.bigDoors.util.ChunkUtils.ChunkLoadMode;
import nl.pim16aap2.bigDoors.util.ChunkUtils.ChunkLoadResult;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.DoorOpenResult;
import nl.pim16aap2.bigDoors.util.Pair;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;
import nl.pim16aap2.bigDoors.util.Vector2D;
import org.bukkit.Location;
import org.bukkit.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class DoorOpener implements Opener
{
    private static final List<RotateDirection> VALID_ROTATE_DIRECTIONS = Collections
        .unmodifiableList(Arrays.asList(RotateDirection.CLOCKWISE, RotateDirection.COUNTERCLOCKWISE));

    private final BigDoors plugin;

    public DoorOpener(BigDoors plugin)
    {
        this.plugin = plugin;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pair<Vector2D, Vector2D> getChunkRange(Door door)
    {
        return getChunkRange(door, getNewDirection(door));
    }

    @Override
    public @Nonnull List<RotateDirection> getValidRotateDirections()
    {
        return VALID_ROTATE_DIRECTIONS;
    }

    private Pair<Vector2D, Vector2D> getChunkRange(Door door, DoorDirection newDirection)
    {
        if (newDirection == null)
        {
            plugin.getMyLogger().warn("Failed to obtain good chunk range for door: " + door.toSimpleString()
                                          + "! Using current range instead!");
            return getCurrentChunkRange(door);
        }

        Pair<Location, Location> newLocations = getNewCoordinates(door, newDirection);
        return getChunkRange(door, newLocations.first, newLocations.second);
    }

    private Pair<Vector2D, Vector2D> getChunkRange(Door door, Location newMin, Location newMax)
    {
        return ChunkUtils.getChunkRangeBetweenSortedCoords(newMin, door.getMinimum(), newMax, door.getMaximum());
    }

    @Override
    public RotateDirection getRotateDirection(Door door)
    {
        if (isRotateDirectionValid(door))
            return door.getOpenDir();

        final RotateDirection defaultDirection = RotateDirection.CLOCKWISE;

        DoorDirection currentDirection = getCurrentDirection(door);
        if (currentDirection == null)
            return defaultDirection;

        @Nullable OpeningSpecification openingSpecification = getOpeningSpecification(door, getOpenDirection(door),
                                                                                      currentDirection);

        return openingSpecification == null ? defaultDirection : openingSpecification.rotateDirection;
    }

    private @Nullable DoorDirection getNewDirection(@Nonnull Door door)
    {
        final DoorDirection currentDir = getCurrentDirection(door);
        if (currentDir == null)
            return null;

        RotateDirection openDir = door.isOpen() ? RotateDirection.getOpposite(door.getOpenDir()) : door.getOpenDir();

        if (!isRotateDirectionValid(door))
            return null;

        return getNewDirection(currentDir, openDir);
    }

    private @Nonnull DoorDirection getNewDirection(@Nonnull DoorDirection currentDirection,
                                                   @Nonnull RotateDirection rotateDirection)
    {
        return rotateDirection.equals(RotateDirection.CLOCKWISE) ?
               DoorDirection.cycleCardinalDirection(currentDirection) :
               DoorDirection.cycleCardinalDirectionReverse(currentDirection);
    }

    private Pair<Location, Location> getNewCoordinates(Door door, DoorDirection newDirection)
    {
        Location engLoc = door.getEngine();
        int startX = engLoc.getBlockX();
        int startY = door.getMinimum().getBlockY();
        int startZ = engLoc.getBlockZ();

        int endX = engLoc.getBlockX();
        int endY = door.getMaximum().getBlockY();
        int endZ = engLoc.getBlockZ();

        int xLen = door.getMaximum().getBlockX() - door.getMinimum().getBlockX();
        int zLen = door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ();
        World world = door.getWorld();

        switch (newDirection)
        {
            case NORTH:
                startZ = engLoc.getBlockZ() - xLen;
                endZ = engLoc.getBlockZ() - 1;
                break;
            case EAST:
                startX = engLoc.getBlockX() + 1;
                endX = engLoc.getBlockX() + zLen;
                endZ = engLoc.getBlockZ();
                break;
            case SOUTH:
                startZ = engLoc.getBlockZ() + 1;
                endZ = engLoc.getBlockZ() + xLen;
                break;
            case WEST:
                startX = engLoc.getBlockX() - zLen;
                endX = engLoc.getBlockX() - 1;
                break;
        }
        return new Pair<>(new Location(world, startX, startY, startZ), new Location(world, endX, endY, endZ));
    }

    /**
     * Finds the rotation direction and the new coordinates for a door.
     * <p>
     * If the found location(s) are obstructed (see {@link #isPosFree(World, Location, Location)}), null is returned.
     * <p>
     * If the provided rotateDirection is not valid for this type, all rotation directions listed in {@link
     * #getValidRotateDirections()} either until a valid, unobstructed position is found or until we run out of
     * direction to check.
     *
     * @param door             The door for which to get the opening specification.
     * @param rotateDirection  The opening rotation direction to check.
     * @param currentDirection The current direction of the door.
     * @return The open direction and the new min/max coordinates of the door is a valid, unobstructed position could be
     * found.
     * <p>
     * The returned open direction is going to be the same as the input rotateDirection if the provided rotateDirection
     * is valid. When it is not valid, this value will be the {@link RotateDirection} used to get the found min/max
     * coordinates.
     */
    private @Nullable OpeningSpecification getOpeningSpecification(@Nonnull Door door,
                                                                   @Nullable RotateDirection rotateDirection,
                                                                   @Nonnull DoorDirection currentDirection)
    {
        if (isValidOpenDirection(rotateDirection))
        {
            final DoorDirection newDirection = getNewDirection(currentDirection, rotateDirection);
            final Pair<Location, Location> newCoordinates = getNewCoordinates(door, newDirection);
            return isPosFree(door.getWorld(), newCoordinates) ?
                   new OpeningSpecification(rotateDirection, newCoordinates) : null;
        }

        plugin.getMyLogger().info("Encountered invalid open direction " + rotateDirection + " for door " + door +
                                      "!\nWe will try to find a valid open direction now!");

        for (RotateDirection fallbackDirection : getValidRotateDirections())
        {
            final DoorDirection newDirection = getNewDirection(currentDirection, fallbackDirection);
            final Pair<Location, Location> newCoordinates = getNewCoordinates(door, newDirection);
            if (isPosFree(door.getWorld(), newCoordinates))
                return new OpeningSpecification(fallbackDirection, newCoordinates);
        }

        return null;
    }

    @Override
    public @Nonnull Optional<Pair<Location, Location>> getNewCoordinates(@Nonnull Door door)
    {
        final RotateDirection openDirection = getOpenDirection(door);
        if (!isValidOpenDirection(openDirection))
        {
            plugin.getMyLogger().warn("Invalid open direction " + openDirection + " for door: " + door);
            return Optional.empty();
        }

        final int maxDoorSize = getSizeLimit(door);
        if (maxDoorSize > 0 && door.getBlockCount() > maxDoorSize)
        {
            plugin.getMyLogger()
                  .warn("Size " + door.getBlockCount() + " exceeds limit of " + maxDoorSize + " for door: " + door);
            return Optional.empty();
        }

        DoorDirection currentDirection = getCurrentDirection(door);
        if (currentDirection == null)
        {
            plugin.getMyLogger().warn("Current direction is null for door " + door);
            return Optional.empty();
        }

        @Nullable OpeningSpecification openingSpecification = getOpeningSpecification(door, openDirection,
                                                                                      currentDirection);
        if (openingSpecification == null)
        {
            plugin.getMyLogger().info("Failed to find open direction for door " + door + " because it is obstructed!");
            return Optional.empty();
        }

        return Optional.of(new Pair<>(openingSpecification.min, openingSpecification.max));
    }

    private @Nullable RotateDirection getOpenDirection(Door door)
    {
        RotateDirection openDir = door.getOpenDir();
        if (openDir == null)
            return null;
        return openDir.equals(RotateDirection.CLOCKWISE) && door.isOpen() ? RotateDirection.COUNTERCLOCKWISE :
               openDir.equals(RotateDirection.COUNTERCLOCKWISE) && door.isOpen() ? RotateDirection.CLOCKWISE :
               openDir;

    }

    // Get the direction the door is currently facing as seen from the engine to the
    // end of the door.
    private DoorDirection getCurrentDirection(Door door)
    {
        // MinZ != EngineZ => North
        // MaxX != EngineX => East
        // MaxZ != EngineZ => South
        // MinX != EngineX => West
        return door.getEngine().getBlockZ() != door.getMinimum().getBlockZ() ? DoorDirection.NORTH :
               door.getEngine().getBlockX() != door.getMaximum().getBlockX() ? DoorDirection.EAST :
               door.getEngine().getBlockZ() != door.getMaximum().getBlockZ() ? DoorDirection.SOUTH :
               door.getEngine().getBlockX() != door.getMinimum().getBlockX() ? DoorDirection.WEST : null;
    }

    @Override
    public DoorOpenResult openDoor(Door door, double time)
    {
        return openDoor(door, time, false, false);
    }

    // Open a door.
    @Override
    public DoorOpenResult openDoor(Door door, double time, boolean instantOpen, boolean silent, ChunkLoadMode mode)
    {
        if (!plugin.getCommander().canGo())
        {
            plugin.getMyLogger()
                  .info("Failed to toggle: " + door.toSimpleString() + ", as door toggles are currently disabled!");
            return abort(DoorOpenResult.ERROR, door.getDoorUID());
        }

        if (plugin.getCommander().isDoorBusyRegisterIfNot(door.getDoorUID()))
        {
            if (!silent)
                plugin.getMyLogger().myLogger(Level.INFO,
                                              "Door " + door.toSimpleString() + " is not available right now!");
            return abort(DoorOpenResult.BUSY, door.getDoorUID());
        }

        final ChunkLoadResult chunkLoadResult = chunksLoaded(door, mode);
        if (chunkLoadResult == ChunkLoadResult.FAIL)
        {
            plugin.getMyLogger().logMessage("Chunks for door " + door.toSimpleString() + " are not loaded!", true,
                                            false);
            return abort(DoorOpenResult.CHUNKSNOTLOADED, door.getDoorUID());
        }
        if (chunkLoadResult == ChunkLoadResult.REQUIRED_LOAD)
            instantOpen = true;

        DoorDirection currentDirection = getCurrentDirection(door);
        if (currentDirection == null)
        {
            plugin.getMyLogger().logMessage("Current direction is null for door " + door.toSimpleString() + "!", true,
                                            false);
            return abort(DoorOpenResult.ERROR, door.getDoorUID());
        }

        final OpeningSpecification openingSpecification = getOpeningSpecification(door, getOpenDirection(door),
                                                                                  currentDirection);
        if (openingSpecification == null)
        {
            plugin.getMyLogger().warn("Could not determine opening direction for door: " + door + "!");
            return abort(DoorOpenResult.NODIRECTION, door.getDoorUID());
        }

        if (!isRotateDirectionValid(door))
        {
            // If the door is currently open, then the selected rotDirection is actually the
            // closing direction.
            // So, if the door is open, flip the direciton.
            RotateDirection newRotDirection = openingSpecification.rotateDirection;
            if (door.isOpen())
                newRotDirection = newRotDirection.equals(RotateDirection.CLOCKWISE) ? RotateDirection.COUNTERCLOCKWISE :
                                  RotateDirection.CLOCKWISE;

            plugin.getMyLogger().logMessage("Updating openDirection of door " + door.toSimpleString() + " to "
                                                + newRotDirection.name() +
                                                ". If this is undesired, change it via the GUI.", true, false);
            plugin.getCommander().updateDoorOpenDirection(door.getDoorUID(), newRotDirection);
        }

        int xOpposite, yOpposite, zOpposite;
        // If the xMax is not the same value as the engineX, then xMax is xOpposite.
        if (door.getMaximum().getBlockX() != door.getEngine().getBlockX())
            xOpposite = door.getMaximum().getBlockX();
        else
            xOpposite = door.getMinimum().getBlockX();

        // If the zMax is not the same value as the engineZ, then zMax is zOpposite.
        if (door.getMaximum().getBlockZ() != door.getEngine().getBlockZ())
            zOpposite = door.getMaximum().getBlockZ();
        else
            zOpposite = door.getMinimum().getBlockZ();

        // If the yMax is not the same value as the engineY, then yMax is yOpposite.
        if (door.getMaximum().getBlockY() != door.getEngine().getBlockY())
            yOpposite = door.getMaximum().getBlockY();
        else
            yOpposite = door.getMinimum().getBlockY();

        // Finalise the oppositePoint location.
        Location oppositePoint = new Location(door.getWorld(), xOpposite, yOpposite, zOpposite);

        // Make sure the doorSize does not exceed the total doorSize.
        // If it does, open the door instantly.
        int maxDoorSize = getSizeLimit(door);
        if (maxDoorSize > 0 && door.getBlockCount() > maxDoorSize)
        {
            plugin.getMyLogger().logMessage("Door " + door.toSimpleString() + " Exceeds the size limit: " + maxDoorSize,
                                            true, false);
            return abort(DoorOpenResult.ERROR, door.getDoorUID());
        }

        if (!hasAccessToLocations(door, openingSpecification.min, openingSpecification.max))
            return abort(DoorOpenResult.NOPERMISSION, door.getDoorUID());

        if (fireDoorEventTogglePrepare(door, instantOpen))
            return abort(DoorOpenResult.CANCELLED, door.getDoorUID());

        plugin.getCommander()
              .addBlockMover(
                  new CylindricalMover(plugin, oppositePoint.getWorld(), 1, openingSpecification.rotateDirection, time,
                                       oppositePoint, currentDirection, door, instantOpen,
                                       plugin.getConfigLoader().bdMultiplier()));
        fireDoorEventToggleStart(door, instantOpen);

        return DoorOpenResult.SUCCESS;
    }

    private static final class OpeningSpecification
    {
        public final RotateDirection rotateDirection;
        public final Location min;
        public final Location max;

        public OpeningSpecification(RotateDirection rotateDirection, Location min, Location max)
        {
            this.rotateDirection = rotateDirection;
            this.min = min;
            this.max = max;
        }

        public OpeningSpecification(RotateDirection rotateDirection, Pair<Location, Location> locations)
        {
            this(rotateDirection, locations.first, locations.second);
        }
    }
}
