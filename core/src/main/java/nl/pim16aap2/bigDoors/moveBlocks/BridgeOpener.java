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

public class BridgeOpener implements Opener
{
    private static final List<RotateDirection> VALID_ROTATE_DIRECTIONS = Collections
        .unmodifiableList(Arrays.asList(RotateDirection.NORTH, RotateDirection.EAST,
                                        RotateDirection.SOUTH, RotateDirection.WEST));

    private final BigDoors plugin;

    public BridgeOpener(BigDoors plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public @Nonnull List<RotateDirection> getValidRotateDirections()
    {
        return VALID_ROTATE_DIRECTIONS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pair<Vector2D, Vector2D> getChunkRange(Door door)
    {
        if (!door.isOpen())
            return getCurrentChunkRange(door);

        DoorDirection doorDirection = getOpenDirection(door);
        if (doorDirection == null)
        {
            plugin.getMyLogger().warn("Failed to obtain good chunk range for door: " + door.getDoorUID()
                                          + "! Using current range instead!");
            return getCurrentChunkRange(door);
        }

        return getChunkRange(door, RotateDirection.DOWN, doorDirection);
    }

    private Pair<Vector2D, Vector2D> getChunkRange(Door door, RotateDirection upDown, DoorDirection currentDirection)
    {
        final Pair<Location, Location> newCoordinates = getNewCoordinates(door, upDown, currentDirection);
        return ChunkUtils.getChunkRangeBetweenCoords(newCoordinates.first, newCoordinates.second);
    }

    @Override
    public boolean isRotateDirectionValid(@Nonnull Door door)
    {
        // When rotation point is positioned along the NORTH/SOUTH axis, it can only
        // rotate EAST or WEST. Or the other way round.
        boolean NS = door.getEngSide().equals(DoorDirection.EAST) || door.getEngSide().equals(DoorDirection.WEST);
        if (NS)
            return door.getOpenDir().equals(RotateDirection.EAST) || door.getOpenDir().equals(RotateDirection.WEST);
        return door.getOpenDir().equals(RotateDirection.NORTH) || door.getOpenDir().equals(RotateDirection.SOUTH);
    }

    @Override
    public RotateDirection getRotateDirection(Door door)
    {
        if (isRotateDirectionValid(door))
            return door.getOpenDir();
        return Util.getRotateDirection(DoorDirection.getOpposite(door.getEngSide()));
    }

    @Override
    public @Nonnull Optional<Pair<Location, Location>> getNewCoordinates(@Nonnull Door door)
    {
        final int maxDoorSize = getSizeLimit(door);
        if (maxDoorSize > 0 && door.getBlockCount() > maxDoorSize)
        {
            plugin.getMyLogger().warn("Size " + door.getBlockCount() + " exceeds limit of " +
                                          maxDoorSize + " for drawbridge: " + door);
            return Optional.empty();
        }

        RotateDirection upDown = getUpDown(door);
        if (upDown == null)
        {
            plugin.getMyLogger().warn("Found null open direction for drawbridge: " + door);
            return Optional.empty();
        }

        DoorDirection currentDirection = getCurrentDirection(door);
        if (currentDirection == null)
        {
            plugin.getMyLogger().warn("Current direction is null for drawbridge " + door + "!");
            return Optional.empty();
        }

        @Nullable OpeningSpecification openingSpecification = getOpeningSpecification(door, upDown, currentDirection);
        if (openingSpecification == null)
        {
            plugin.getMyLogger()
                  .info("Failed to find open direction for drawbridge " + door + " because it is obstructed!");
            return Optional.empty();
        }

        return Optional.of(new Pair<>(openingSpecification.min, openingSpecification.max));
    }

    private @Nonnull Pair<Location, Location> getNewCoordinates(@Nonnull Door door, @Nonnull RotateDirection upDown,
                                                                @Nonnull DoorDirection currentDirection)
    {
        int startX = 0, startY = 0, startZ = 0;
        int endX = 0, endY = 0, endZ = 0;
        World world = door.getWorld();

        if (upDown.equals(RotateDirection.UP))
            switch (currentDirection)
            {
                // North West = Min X, Min Z
                // South West = Min X, Max Z
                // North East = Max X, Min Z
                // South East = Max X, Max X
                case NORTH:
                    startX = door.getMinimum().getBlockX();
                    endX = door.getMaximum().getBlockX();

                    startY = door.getMinimum().getBlockY() + 1;
                    endY =
                        door.getMinimum().getBlockY() + door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ();

                    startZ = door.getMinimum().getBlockZ();
                    endZ = door.getMinimum().getBlockZ();
                    break;

                case SOUTH:
                    startX = door.getMinimum().getBlockX();
                    endX = door.getMaximum().getBlockX();

                    startY = door.getMinimum().getBlockY() + 1;
                    endY =
                        door.getMinimum().getBlockY() + door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ();

                    startZ = door.getMaximum().getBlockZ();
                    endZ = door.getMaximum().getBlockZ();
                    break;

                case EAST:
                    startX = door.getMaximum().getBlockX();
                    endX = door.getMaximum().getBlockX();

                    startY = door.getMinimum().getBlockY() + 1;
                    endY =
                        door.getMinimum().getBlockY() + door.getMaximum().getBlockX() - door.getMinimum().getBlockX();

                    startZ = door.getMinimum().getBlockZ();
                    endZ = door.getMaximum().getBlockZ();
                    break;

                case WEST:
                    startX = door.getMinimum().getBlockX();
                    endX = door.getMinimum().getBlockX();

                    startY = door.getMinimum().getBlockY() + 1;
                    endY =
                        door.getMinimum().getBlockY() + door.getMaximum().getBlockX() - door.getMinimum().getBlockX();

                    startZ = door.getMinimum().getBlockZ();
                    endZ = door.getMaximum().getBlockZ();
                    break;
            }
        else
            switch (currentDirection)
            {
                // North West = Min X, Min Z
                // South West = Min X, Max Z
                // North East = Max X, Min Z
                // South East = Max X, Max X
                case NORTH:
                    startX = door.getMinimum().getBlockX();
                    endX = door.getMaximum().getBlockX();

                    startY = door.getMinimum().getBlockY();
                    endY = door.getMinimum().getBlockY();

                    startZ =
                        door.getMinimum().getBlockZ() - door.getMaximum().getBlockY() + door.getMinimum().getBlockY();
                    endZ = door.getMinimum().getBlockZ() - 1;
                    break;

                case SOUTH:
                    startX = door.getMinimum().getBlockX();
                    endX = door.getMaximum().getBlockX();

                    startY = door.getMinimum().getBlockY();
                    endY = door.getMinimum().getBlockY();

                    startZ = door.getMinimum().getBlockZ() + 1;
                    endZ =
                        door.getMinimum().getBlockZ() + door.getMaximum().getBlockY() - door.getMinimum().getBlockY();
                    break;

                case EAST:
                    startX = door.getMinimum().getBlockX() + 1;
                    endX =
                        door.getMaximum().getBlockX() + door.getMaximum().getBlockY() - door.getMinimum().getBlockY();

                    startY = door.getMinimum().getBlockY();
                    endY = door.getMinimum().getBlockY();

                    startZ = door.getMinimum().getBlockZ();
                    endZ = door.getMaximum().getBlockZ();
                    break;

                case WEST:
                    startX =
                        door.getMinimum().getBlockX() - door.getMaximum().getBlockY() + door.getMinimum().getBlockY();
                    endX = door.getMinimum().getBlockX() - 1;

                    startY = door.getMinimum().getBlockY();
                    endY = door.getMinimum().getBlockY();

                    startZ = door.getMinimum().getBlockZ();
                    endZ = door.getMaximum().getBlockZ();
                    break;
            }

        return new Pair<>(new Location(world, startX, startY, startZ), new Location(world, endX, endY, endZ));
    }

    // Check if the bridge should go up or down.
    public RotateDirection getUpDown(Door door)
    {
        int height = Math.abs(door.getMinimum().getBlockY() - door.getMaximum().getBlockY());
        if (height > 0)
            return RotateDirection.DOWN;
        return RotateDirection.UP;
    }

    private @Nullable DoorDirection getOpenDirection(Door door)
    {
        if (door.getOpenDir() == null)
            return null;

        RotateDirection upDown = getUpDown(door);
        DoorDirection cDir = getCurrentDirection(door);
        if (upDown == null || cDir == null)
            return null;

        if (upDown.equals(RotateDirection.UP))
            return door.getEngSide();

        if (isRotateDirectionValid(door))
        {
            Optional<DoorDirection> newDir = Util.getDoorDirection(door.getOpenDir())
                                                 .map(dir -> door.isOpen() ? DoorDirection.getOpposite(dir) : dir);
            if (newDir.isPresent())
                return newDir.get();
        }

        boolean NS = cDir == DoorDirection.NORTH || cDir == DoorDirection.SOUTH;
        if (door.getOpenDir().equals(RotateDirection.CLOCKWISE) && !door.isOpen() ||
            door.getOpenDir().equals(RotateDirection.COUNTERCLOCKWISE) && door.isOpen())
            return NS ? DoorDirection.SOUTH : DoorDirection.EAST;
        if (door.getOpenDir().equals(RotateDirection.CLOCKWISE) && door.isOpen() ||
            door.getOpenDir().equals(RotateDirection.COUNTERCLOCKWISE) && !door.isOpen())
            return NS ? DoorDirection.NORTH : DoorDirection.WEST;

        return null;
    }

    private @Nullable OpeningSpecification findFirstValidSpecification(@Nonnull Door door,
                                                                       @Nonnull RotateDirection upDown,
                                                                       @Nonnull DoorDirection... directions)
    {
        for (DoorDirection fallbackDirection : directions)
        {
            final Pair<Location, Location> newCoordinates = getNewCoordinates(door, upDown, fallbackDirection);
            if (isPosFree(door.getWorld(), newCoordinates))
                return new OpeningSpecification(fallbackDirection, newCoordinates);
        }
        return null;
    }

    private @Nullable OpeningSpecification getOpeningSpecification(@Nonnull Door door,
                                                                   @Nonnull RotateDirection upDown,
                                                                   @Nonnull DoorDirection currentDirection)
    {
        final @Nullable DoorDirection openDirection = getOpenDirection(door);
        if (isValidOpenDirection(Util.getRotateDirection(openDirection)))
        {
            final Pair<Location, Location> newCoordinates = getNewCoordinates(door, upDown, openDirection);
            return isPosFree(door.getWorld(), newCoordinates) ?
                   new OpeningSpecification(openDirection, newCoordinates) : null;
        }

        final boolean NS = currentDirection == DoorDirection.NORTH || currentDirection == DoorDirection.SOUTH;
        if (NS)
            return findFirstValidSpecification(door, upDown, DoorDirection.NORTH, DoorDirection.SOUTH);
        else
            return findFirstValidSpecification(door, upDown, DoorDirection.EAST, DoorDirection.WEST);
    }

    // Get the "current direction". In this context this means on which side of the
    // drawbridge the engine is.
    private DoorDirection getCurrentDirection(Door door)
    {
        return door.getEngSide();
    }

    @Override
    public @Nonnull DoorOpenResult openDoor(@Nonnull Door door, double time, boolean instantOpen, boolean silent,
                                            @Nonnull ChunkLoadMode mode, boolean bypassProtectionHooks)
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
                                              "Bridge " + door.toSimpleString() + " is not available right now!");
            return abort(DoorOpenResult.BUSY, door.getDoorUID());
        }

        final ChunkLoadResult chunkLoadResult = chunksLoaded(door, mode);
        if (chunkLoadResult == ChunkLoadResult.FAIL)
        {
            plugin.getMyLogger().logMessage("Chunks for bridge " + door.toSimpleString() + " are not loaded!", true,
                                            false);
            return abort(DoorOpenResult.CHUNKSNOTLOADED, door.getDoorUID());
        }
        if (chunkLoadResult == ChunkLoadResult.REQUIRED_LOAD)
            instantOpen = true;

        DoorDirection currentDirection = getCurrentDirection(door);
        if (currentDirection == null)
        {
            plugin.getMyLogger().logMessage("Current direction is null for bridge " + door.toSimpleString() + "!", true,
                                            false);
            return abort(DoorOpenResult.ERROR, door.getDoorUID());
        }

        RotateDirection upDown = getUpDown(door);
        if (upDown == null)
        {
            plugin.getMyLogger().logMessage("UpDown direction is null for bridge " + door.toSimpleString() + "!", true,
                                            false);
            return abort(DoorOpenResult.ERROR, door.getDoorUID());
        }

        final OpeningSpecification openingSpecification = getOpeningSpecification(door, upDown, currentDirection);
        if (openingSpecification == null)
        {
            plugin.getMyLogger().warn("Could not determine opening direction for door: " + door + "!");
            return abort(DoorOpenResult.NODIRECTION, door.getDoorUID());
        }

        if (!isRotateDirectionValid(door))
        {
            // If the door is currently open, then the selected rotDirection is actually the
            // closing direction. So, if the door is open, flip the direciton.
            RotateDirection newRotDir = Util.getRotateDirection(openingSpecification.openDirection);
            if (door.isOpen())
                newRotDir = RotateDirection.getOpposite(newRotDir);

            plugin.getMyLogger().logMessage(
                "Updating openDirection of drawbridge " + door.toSimpleString() + " to "
                    + newRotDir + ". If this is undesired, change it via the GUI.",
                true, false);
            plugin.getCommander().updateDoorOpenDirection(door.getDoorUID(), newRotDir);
        }

        // Make sure the doorSize does not exceed the total doorSize.
        // If it does, open the door instantly.
        int maxDoorSize = getSizeLimit(door);
        if (maxDoorSize > 0 && door.getBlockCount() > maxDoorSize)
        {
            plugin.getMyLogger().logMessage("Door " + door.toSimpleString() + " Exceeds the size limit: " + maxDoorSize,
                                            true, false);
            return abort(DoorOpenResult.ERROR, door.getDoorUID());
        }

        if (!bypassProtectionHooks && !hasAccessToLocations(door, openingSpecification.min, openingSpecification.max))
            return abort(DoorOpenResult.NOPERMISSION, door.getDoorUID());

        if (fireDoorEventTogglePrepare(door, instantOpen))
            return abort(DoorOpenResult.CANCELLED, door.getDoorUID());

        plugin.getCommander().addBlockMover(new BridgeMover(plugin, door.getWorld(), time, door, upDown,
                                                            openingSpecification.openDirection, instantOpen,
                                                            plugin.getConfigLoader().dbMultiplier()));
        fireDoorEventToggleStart(door, instantOpen);

        return DoorOpenResult.SUCCESS;
    }

    private static final class OpeningSpecification
    {
        public final DoorDirection openDirection;
        public final Location min;
        public final Location max;

        public OpeningSpecification(DoorDirection openDirection,
                                    Location min, Location max)
        {
            this.openDirection = openDirection;
            this.min = min;
            this.max = max;
        }

        public OpeningSpecification(DoorDirection openDirection,
                                    Pair<Location, Location> locations)
        {
            this(openDirection, locations.first, locations.second);
        }
    }
}
