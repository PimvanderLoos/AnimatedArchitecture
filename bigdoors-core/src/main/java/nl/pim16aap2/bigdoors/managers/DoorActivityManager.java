package nl.pim16aap2.bigdoors.managers;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Constants;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Keeps track of which doors are currently active.
 *
 * @author Pim
 */
public final class DoorActivityManager extends Restartable
{
    private final @NonNull Map<Long, Optional<BlockMover>> busyDoors = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@link DoorActivityManager}.
     *
     * @param holder The {@link IRestartableHolder} that manages this object.
     */
    public DoorActivityManager(final @NonNull IRestartableHolder holder)
    {
        super(holder);
    }

    /**
     * Checks if a {@link AbstractDoorBase} is 'busy', i.e. currently being animated.
     *
     * @param doorUID The UID of the {@link AbstractDoorBase}.
     * @return True if the {@link AbstractDoorBase} is busy.
     */
    public boolean isDoorBusy(final long doorUID)
    {
        return busyDoors.containsKey(doorUID);
    }

    /**
     * Attempts to register a door (as described by its UID) as busy. If the door was not previously registered as busy,
     * it will be registered now and this method will return <code>true</code>. If it was already registered as busy, it
     * will not touch it and return <code>false</code>.
     *
     * @param doorUID The UID of the door to register.
     * @return True if the door was not registered before (but is now), otherwise false.
     */
    public boolean attemptRegisterAsBusy(final long doorUID)
    {
        return busyDoors.putIfAbsent(doorUID, Optional.empty()) == null;
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
     * Processed a finished {@link BlockMover}.
     * <p>
     * The {@link AbstractDoorBase} that was being used by the {@link BlockMover} will be registered as inactive and any
     * scheduling that is required will be performed.
     *
     * @param blockMover      The {@link BlockMover} to postprocess.
     * @param allowReschedule Whether or not to allow rescheduling (e.g. autoclose).
     */
    public void processFinishedBlockMover(@NonNull BlockMover blockMover, boolean allowReschedule)
    {
        int delay = Math.max(Constants.MINIMUM_DOOR_DELAY,
                             BigDoors.get().getPlatform().getConfigLoader().coolDown() * 20);

        BigDoors.get().getPlatform().getPExecutor()
                .runSyncLater(() -> handleFinishedBlockMover(blockMover, allowReschedule), delay);
    }

    private void handleFinishedBlockMover(@NonNull BlockMover blockMover, boolean allowReschedule)
    {
        setDoorAvailable(blockMover.getDoor().getDoorUID());

        if (!allowReschedule)
            return;

        BigDoors.get().getPlatform().callDoorEvent(
            BigDoors.get().getPlatform().getDoorActionEventFactory()
                    .createToggleEndEvent(blockMover.getDoor(), blockMover.getCause(), blockMover.getActionType(),
                                          blockMover.getPlayer(), blockMover.getTime(),
                                          blockMover.isSkipAnimation()));

        if (blockMover.getDoor() instanceof ITimerToggleableArchetype)
            BigDoors.get().getAutoCloseScheduler()
                    .scheduleAutoClose(blockMover.getPlayer(),
                                       (AbstractDoorBase & ITimerToggleableArchetype) blockMover.getDoor(),
                                       blockMover.getTime(), blockMover.isSkipAnimation());
    }

    /**
     * Stores a {@link BlockMover} in the appropriate slot in {@link #busyDoors}
     *
     * @param mover The {@link BlockMover}.
     */
    public void addBlockMover(final @NonNull BlockMover mover)
    {
        busyDoors.replace(mover.getDoorUID(), Optional.of(mover));
    }

    /**
     * Gets all the currently active {@link BlockMover}s.
     *
     * @return All the currently active {@link BlockMover}s.
     */
    public @NonNull Stream<BlockMover> getBlockMovers()
    {
        return busyDoors.values().stream().filter(Optional::isPresent).map(Optional::get);
    }

    /**
     * Gets the {@link BlockMover} of a busy {@link AbstractDoorBase}, if it has been registered.
     *
     * @param doorUID The UID of the {@link AbstractDoorBase}.
     * @return The {@link BlockMover} of a busy {@link AbstractDoorBase}.
     */
    public @NonNull Optional<BlockMover> getBlockMover(final long doorUID)
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
        busyDoors.forEach((key, value) -> value.ifPresent(BlockMover::abort));
        emptyBusyDoors();
    }

    @Override
    public void restart()
    {
        busyDoors.forEach((key, value) -> value.ifPresent(BlockMover::restart));
        busyDoors.clear();
    }

    @Override
    public void shutdown()
    {
        busyDoors.forEach((key, value) -> value.ifPresent(BlockMover::shutdown));
        busyDoors.clear();
    }
}
