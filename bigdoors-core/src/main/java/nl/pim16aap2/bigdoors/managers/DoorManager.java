package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.IRestartableHolder;
import nl.pim16aap2.bigdoors.util.Restartable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Keeps track of which doors are currently active.
 *
 * @author Pim
 */
public final class DoorManager extends Restartable
{
    private static DoorManager instance;
    private final Map<Long, Optional<BlockMover>> busyDoors = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@link DoorManager}.
     *
     * @param holder The {@link IRestartableHolder} that manages this object.
     */
    private DoorManager(final @NotNull IRestartableHolder holder)
    {
        super(holder);
    }

    /**
     * Initializes the {@link DoorManager}. If it has already been initialized, it'll return that instance instead.
     *
     * @param holder The {@link IRestartableHolder} that manages this object.
     * @return The instance of this {@link DoorManager}.
     */
    public static DoorManager init(final @NotNull IRestartableHolder holder)
    {
        return (instance == null) ? instance = new DoorManager(holder) : instance;
    }

    /**
     * Gets the instance of the {@link DoorManager} if it exists.
     *
     * @return The instance of the {@link DoorManager}.
     */
    @Nullable
    public static DoorManager get()
    {
        return instance;
    }

    /**
     * Checks if a {@link DoorBase} is 'busy', i.e. currently being animated.
     *
     * @param doorUID The UID of the {@link DoorBase}.
     * @return True if the {@link DoorBase} is busy.
     */
    public boolean isDoorBusy(final long doorUID)
    {
        return busyDoors.containsKey(doorUID);
    }

    /**
     * Registers a {@link DoorBase} as busy.
     *
     * @param doorUID The UID of the {@link DoorBase}.
     */
    public void setDoorBusy(final long doorUID)
    {
        busyDoors.put(doorUID, Optional.empty());
    }

    /**
     * Register a door as available.
     *
     * @param doorUID The UID of the door.
     */
    public void setDoorAvailable(final long doorUID)
    {
        busyDoors.remove(doorUID);
    }

    /**
     * Stores a {@link BlockMover} in the appropriate slot in {@link #busyDoors}
     *
     * @param mover The {@link BlockMover}.
     */
    public void addBlockMover(final @NotNull BlockMover mover)
    {
        busyDoors.replace(mover.getDoorUID(), Optional.of(mover));
    }

    /**
     * Gets all the currently active {@link BlockMover}s.
     *
     * @return All the currently active {@link BlockMover}s.
     */
    public Stream<BlockMover> getBlockMovers()
    {
        return busyDoors.values().stream().filter(Optional::isPresent).map(Optional::get);
    }

    /**
     * Gets the {@link BlockMover} of a busy {@link DoorBase}, if it has been registered.
     *
     * @param doorUID The UID of the {@link DoorBase}.
     * @return The {@link BlockMover} of a busy {@link DoorBase}.
     */
    public Optional<BlockMover> getBlockMover(final long doorUID)
    {
        return busyDoors.containsKey(doorUID) ? busyDoors.get(doorUID) : Optional.empty();
    }

    /**
     * Clears all busy doors.
     */
    private void emptyBusyDoors()
    {
        busyDoors.clear();
    }

    /**
     * Stops all doors that are currently active.
     */
    public void stopDoors()
    {
        busyDoors.forEach((K, V) -> V.ifPresent(BlockMover::abort));
        emptyBusyDoors();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restart()
    {
        busyDoors.forEach((K, V) -> V.ifPresent(BlockMover::restart));
        busyDoors.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown()
    {
        busyDoors.forEach((K, V) -> V.ifPresent(BlockMover::shutdown));
        busyDoors.clear();
    }
}
