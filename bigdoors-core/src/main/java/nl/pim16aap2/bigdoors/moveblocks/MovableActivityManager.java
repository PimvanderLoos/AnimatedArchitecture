package nl.pim16aap2.bigdoors.moveblocks;

import dagger.Lazy;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.events.IBigDoorsEventCaller;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.util.Constants;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Keeps track of which movables are currently active.
 *
 * @author Pim
 */
@Singleton
public final class MovableActivityManager extends Restartable
{
    private final Map<Long, Optional<BlockMover>> busyMovables = new ConcurrentHashMap<>();

    private final Lazy<AutoCloseScheduler> autoCloseScheduler;
    private final IConfigLoader config;
    private final IPExecutor executor;
    private final IBigDoorsEventFactory eventFactory;
    private final IBigDoorsEventCaller bigDoorsEventCaller;

    /**
     * Constructs a new {@link MovableActivityManager}.
     *
     * @param holder
     *     The {@link RestartableHolder} that manages this object.
     * @param autoCloseScheduler
     *     The {@link AutoCloseScheduler} to use for scheduling auto close actions when required.
     */
    @Inject
    public MovableActivityManager(
        RestartableHolder holder, Lazy<AutoCloseScheduler> autoCloseScheduler,
        IConfigLoader config, IPExecutor executor, IBigDoorsEventFactory eventFactory,
        IBigDoorsEventCaller bigDoorsEventCaller)
    {
        super(holder);
        this.autoCloseScheduler = autoCloseScheduler;
        this.config = config;
        this.executor = executor;
        this.eventFactory = eventFactory;
        this.bigDoorsEventCaller = bigDoorsEventCaller;
    }

    /**
     * Checks if a {@link MovableBase} is 'busy', i.e. currently being animated.
     *
     * @param movableUID
     *     The UID of the {@link MovableBase}.
     * @return True if the {@link MovableBase} is busy.
     */
    @SuppressWarnings("unused")
    public boolean isMovableBusy(long movableUID)
    {
        return busyMovables.containsKey(movableUID);
    }

    /**
     * Attempts to register a movable (as described by its UID) as busy. If the movable was not previously registered as
     * busy, it will be registered now and this method will return <code>true</code>. If it was already registered as
     * busy, it will not touch it and return <code>false</code>.
     *
     * @param movableUID
     *     The UID of the movable to register.
     * @return True if the movable was not registered before (but is now), otherwise false.
     */
    // The busyMovables map stores the values as optional and here we just want to know if a value exists for the key.
    // If it doesn't, the value will be null, but both IntelliJ and SonarLint will complain about comparing an
    // optional to null. Because that is _exactly_ what we want to do here, we ignore the warnings.
    @SuppressWarnings({"OptionalAssignedToNull", "squid:S2789"})
    public boolean attemptRegisterAsBusy(long movableUID)
    {
        return busyMovables.putIfAbsent(movableUID, Optional.empty()) == null;
    }

    /**
     * Register a movable as available.
     *
     * @param movableUID
     *     The UID of the movable.
     */
    public void setMovableAvailable(long movableUID)
    {
        busyMovables.remove(movableUID);
    }

    /**
     * Processed a finished {@link BlockMover}.
     * <p>
     * The {@link MovableBase} that was being used by the {@link BlockMover} will be registered as inactive and any
     * scheduling that is required will be performed.
     *
     * @param blockMover
     *     The {@link BlockMover} to post-process.
     * @param allowReschedule
     *     Whether to allow rescheduling (e.g. autoClose).
     */
    void processFinishedBlockMover(BlockMover blockMover, boolean allowReschedule)
    {
        final int delay = Math.max(Constants.MINIMUM_MOVABLE_DELAY, config.coolDown() * 20);

        executor.runSyncLater(() -> handleFinishedBlockMover(blockMover, allowReschedule), delay);
    }

    private void handleFinishedBlockMover(BlockMover blockMover, boolean allowReschedule)
    {
        setMovableAvailable(blockMover.getMovable().getUID());

        if (!allowReschedule)
            return;

        bigDoorsEventCaller.callBigDoorsEvent(
            eventFactory.createToggleEndEvent(
                blockMover.getMovable(), blockMover.getSnapshot(), blockMover.getCause(),
                blockMover.getActionType(), blockMover.getPlayer(), blockMover.getTime(),
                blockMover.isSkipAnimation()));

        if (blockMover.getMovable() instanceof ITimerToggleable)
            autoCloseScheduler.get().scheduleAutoClose(blockMover.getPlayer(),
                                                       (AbstractMovable & ITimerToggleable) blockMover.getMovable(),
                                                       blockMover.getTime(), blockMover.isSkipAnimation());
    }

    /**
     * Stores a {@link BlockMover} in the appropriate slot in {@link #busyMovables}
     *
     * @param mover
     *     The {@link BlockMover}.
     */
    public void addBlockMover(BlockMover mover)
    {
        busyMovables.replace(mover.getMovableUID(), Optional.of(mover));
    }

    /**
     * Gets all the currently active {@link BlockMover}s.
     *
     * @return All the currently active {@link BlockMover}s.
     */
    @SuppressWarnings("unused")
    public Stream<BlockMover> getBlockMovers()
    {
        return busyMovables.values().stream().filter(Optional::isPresent).map(Optional::get);
    }

    /**
     * Gets the {@link BlockMover} of a busy {@link MovableBase}, if it has been registered.
     *
     * @param movableUID
     *     The UID of the {@link MovableBase}.
     * @return The {@link BlockMover} of a busy {@link MovableBase}.
     */
    public Optional<BlockMover> getBlockMover(long movableUID)
    {
        return busyMovables.containsKey(movableUID) ? busyMovables.get(movableUID) : Optional.empty();
    }

    /**
     * Clears all busy movables.
     */
    private void emptyBusyMovables()
    {
        busyMovables.clear();
    }

    /**
     * Stops all block movers that are currently active.
     */
    public void stopMovables()
    {
        busyMovables.forEach((key, value) -> value.ifPresent(BlockMover::abort));
        emptyBusyMovables();
    }

    @Override
    public void shutDown()
    {
        stopMovables();
    }
}
