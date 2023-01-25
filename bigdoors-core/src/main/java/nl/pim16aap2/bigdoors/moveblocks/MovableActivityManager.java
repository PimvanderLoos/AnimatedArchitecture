package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.events.IBigDoorsEventCaller;
import nl.pim16aap2.bigdoors.managers.MovableDeletionManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.IMovableConst;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Keeps track of which movables are currently active.
 *
 * @author Pim
 */
@Singleton
public final class MovableActivityManager extends Restartable implements MovableDeletionManager.IDeletionListener
{
    private final Map<Long, Optional<BlockMover>> busyMovables = new ConcurrentHashMap<>();

    private final IBigDoorsEventFactory eventFactory;
    private final IBigDoorsEventCaller bigDoorsEventCaller;

    /**
     * Constructs a new {@link MovableActivityManager}.
     *
     * @param holder
     *     The {@link RestartableHolder} that manages this object.
     */
    @Inject
    public MovableActivityManager(
        RestartableHolder holder, IBigDoorsEventFactory eventFactory, IBigDoorsEventCaller bigDoorsEventCaller,
        MovableDeletionManager movableDeletionManager)
    {
        super(holder);
        movableDeletionManager.registerDeletionListener(this);
        this.eventFactory = eventFactory;
        this.bigDoorsEventCaller = bigDoorsEventCaller;
    }

    /**
     * Checks if a {@link AbstractMovable} is 'busy', i.e. currently being animated.
     *
     * @param movableUID
     *     The UID of the {@link AbstractMovable}.
     * @return True if the {@link AbstractMovable} is busy.
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
     * The {@link AbstractMovable} that was being used by the {@link BlockMover} will be registered as inactive and any
     * scheduling that is required will be performed.
     *
     * @param blockMover
     *     The {@link BlockMover} to post-process.
     */
    void processFinishedBlockMover(BlockMover blockMover)
    {
        handleFinishedBlockMover(blockMover);
    }

    private void handleFinishedBlockMover(BlockMover blockMover)
    {
        setMovableAvailable(blockMover.getMovable().getUid());

        bigDoorsEventCaller.callBigDoorsEvent(
            eventFactory.createToggleEndEvent(
                blockMover.getMovable(), blockMover.getSnapshot(), blockMover.getCause(),
                blockMover.getActionType(), blockMover.getPlayer(), blockMover.getTime(),
                blockMover.isSkipAnimation()));
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
     * Gets the {@link BlockMover} of a busy {@link AbstractMovable}, if it has been registered.
     *
     * @param movableUID
     *     The UID of the {@link AbstractMovable}.
     * @return The {@link BlockMover} of a busy {@link AbstractMovable}.
     */
    public Optional<BlockMover> getBlockMover(long movableUID)
    {
        return Objects.requireNonNullElse(busyMovables.get(movableUID), Optional.empty());
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
    public void onMovableDeletion(IMovableConst movable)
    {
        Objects.<Optional<BlockMover>>requireNonNullElse(busyMovables.remove(movable.getUid()), Optional.empty())
               .ifPresent(BlockMover::abort);
    }

    @Override
    public void shutDown()
    {
        stopMovables();
    }
}
