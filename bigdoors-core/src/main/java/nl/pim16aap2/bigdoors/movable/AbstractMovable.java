package nl.pim16aap2.bigdoors.movable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.moveblocks.MovementRequestData;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.LazyValue;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.Rectangle;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Represents the abstract base all movable types should extend.
 *
 * @author Pim
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Flogger
public abstract class AbstractMovable implements IMovable
{
    /**
     * The lock as used by both the {@link MovableBase} and this class.
     */
    private final ReentrantReadWriteLock lock;
    private final MovableSerializer<?> serializer;

    @EqualsAndHashCode.Include
    private final MovableBase base;

    private final LazyValue<Rectangle> animationRange;
    private final LazyValue<Double> animationCycleDistance;

    private AbstractMovable(MovableBase base)
    {
        serializer = getType().getMovableSerializer();
        this.lock = base.getLock();
        this.base = Objects.requireNonNull(base);

        animationRange = newAnimationRangeVal(this);
        animationCycleDistance = newAnimationCycleDistanceVal(this);
    }

    protected AbstractMovable(MovableBaseHolder holder)
    {
        this(holder.base);
    }

    /**
     * @return The {@link MovableType} of this movable.
     */
    public abstract MovableType getType();

    /**
     * Gets the animation time of this movable.
     * <p>
     * This basically returns max(target, {@link #getMinimumAnimationTime()}), logging a message in case the target time
     * is too low.
     *
     * @param target
     *     The target time.
     * @return The target time if it is bigger than the minimum time, otherwise the minimum.
     */
    protected double calculateAnimationTime(double target)
    {
        final double minimum = getMinimumAnimationTime();
        if (target < minimum)
        {
            log.atFiner()
               .log("Target animation time of %.4f seconds is less than the minimum of %.4f seconds for movable: %s.",
                    target, minimum, getBasicInfo());
            return minimum;
        }
        return target;
    }

    /**
     * Gets the animation time for this movable.
     * <p>
     * The animation time is calculated using this movable's {@link #getBaseAnimationTime()}, its
     * {@link #getMinimumAnimationTime()}, and the multiplier for this type as described by
     * {@link IConfigLoader#getAnimationSpeedMultiplier(MovableType)}.
     * <p>
     * If the target is not null, the returned value will simply be the maximum value of the target and
     * {@link #getBaseAnimationTime()}. Note that the time multiplier is ignored in this case.
     *
     * @param target
     *     The target time. When null, {@link #getBaseAnimationTime()} is used.
     * @return The animation time for this movable in seconds.
     */
    final double getAnimationTime(@Nullable Double target)
    {
        final double realTarget = target != null ?
                                  target :
                                  base.getConfig().getAnimationSpeedMultiplier(getType()) * getBaseAnimationTime();
        return calculateAnimationTime(realTarget);
    }

    /**
     * Calculates the distance traveled per animation by the animated block that travels the furthest.
     * <p>
     * For example, for a circular object, this will be a block on the edge, as these blocks travel further per
     * revolution than the blocks closer to the center of the circle.
     * <p>
     * This method looks at the distance per animation cycle. The exact definition of a cycle depends on the
     * implementation of the movable. For a big door, for example, a cycle could be a single toggle, or a quarter
     * circle. To keep things easy, a revolving movable (which may not have a definitive end to its animation) could
     * define a cycle as quarter circle as well.
     * <p>
     * The distance value is used to calculate {@link #getMinimumAnimationTime()} and {@link #getBaseAnimationTime()}.
     *
     * @return The longest distance traveled by an animated block measured in blocks.
     */
    protected abstract double calculateAnimationCycleDistance();

    /**
     * See {@link #calculateAnimationRange()}.
     */
    protected final double getAnimationCycleDistance()
    {
        return animationCycleDistance.get();
    }

    /**
     * Calculates the rectangle describing the limits within an animation of this door takes place.
     * <p>
     * At no point during an animation will any animated block leave this cuboid, though not guarantees are given
     * regarding how tight the cuboid fits around the animated blocks.
     *
     * @return The animation range.
     */
    protected abstract Rectangle calculateAnimationRange();

    @Override
    @Locked.Read
    public final Rectangle getAnimationRange()
    {
        return animationRange.get();
    }

    /**
     * Certain actions may result in the animation range and animation cycle distance being changed. This method is
     * called when that may happen.
     */
    protected final void invalidateAnimationData()
    {
        animationRange.invalidate();
        animationCycleDistance.invalidate();
    }

    /**
     * The default speed of the animation in blocks/second, as measured by the fastest-moving block in the movable.
     */
    protected double getDefaultAnimationSpeed()
    {
        return 1.5D;
    }

    /**
     * Gets the lower time limit for an animation.
     * <p>
     * Because animated blocks have a speed limit, as determined by {@link IConfigLoader#maxBlockSpeed()}, there is also
     * a minimum amount of time for its animation.
     * <p>
     * The exact time limit depends on the shape, size, and type of movable.
     *
     * @return The lower animation time limit for this movable in seconds.
     */
    public double getMinimumAnimationTime()
    {
        return getAnimationCycleDistance() / base.getConfig().maxBlockSpeed();
    }

    /**
     * Gets the base animation time for this movable.
     * <p>
     * This is the animation time without any multipliers applied to it.
     *
     * @return The base animation time for this movable in seconds.
     */
    public double getBaseAnimationTime()
    {
        return getAnimationCycleDistance() /
            Math.min(getDefaultAnimationSpeed(), base.getConfig().maxBlockSpeed());
    }

    /**
     * Checks if this movable can be opened instantly (i.e. skip the animation).
     * <p>
     * This describes whether skipping the animation actually does anything. When a movable can skip its animation, it
     * means that the actual toggle has an effect on the world and as such toggling it without an animation does
     * something.
     * <p>
     * Movables that do not have any effect other than their animation because all their blocks start and stop at
     * exactly the same place (i.e. flags) cannot skip their animation because skipping the animation would mean
     * removing the blocks of the movable from their locations and then placing them back at exactly the same position.
     * This would be a waste of performance for both the server and the client and as such should be prevented.
     *
     * @return True, if this movable can skip its animation.
     */
    public boolean canSkipAnimation()
    {
        return false;
    }

    /**
     * Finds the new minimum and maximum coordinates (represented by a {@link Cuboid}) of this movable that would be the
     * result of toggling it.
     *
     * @return The {@link Cuboid} that would represent the movable if it was toggled right now.
     */
    public abstract Optional<Cuboid> getPotentialNewCoordinates();

    /**
     * Gets the direction the movable would go given its current state.
     *
     * @return The direction the movable would go if it were to be toggled.
     */
    public abstract MovementDirection getCurrentToggleDir();

    /**
     * Cycle the {@link MovementDirection} direction this {@link IMovable} will open in. By default, it'll set and
     * return the opposite direction of the current direction.
     * <p>
     * Note that this does not actually change the open direction; it merely tells you which direction comes next!
     *
     * @return The new {@link MovementDirection} direction this {@link IMovable} will open in.
     */
    @SuppressWarnings("unused")
    public MovementDirection cycleOpenDirection()
    {
        final Set<MovementDirection> validOpenDirections = getType().getValidOpenDirections();
        final MovementDirection currentDir = getOpenDir();

        @Nullable MovementDirection first = null;
        final Iterator<MovementDirection> it = validOpenDirections.iterator();
        while (it.hasNext())
        {
            final MovementDirection dir = it.next();
            if (first == null)
                first = dir;

            if (dir != currentDir)
                continue;

            if (it.hasNext())
                return it.next();
            break;
        }

        if (first != null)
        {
            invalidateAnimationData();
            return first;
        }

        log.atFine()
           .log(
               "Failed to cycle open direction for movable of type '%s' with open dir '%s' given valid directions '%s'",
               getType(), currentDir, validOpenDirections);
        return MovementDirection.NONE;
    }

    /**
     * @param data
     *     The data for the toggle request.
     * @return A new {@link BlockMover} for this type of movable.
     */
    protected abstract BlockMover constructBlockMover(MovementRequestData data)
        throws Exception;

    /**
     * Attempts to toggle a movable. Think twice before using this method. Instead, please look at
     * {@link MovableToggleRequestBuilder}.
     *
     * @param cause
     *     What caused the toggle action.
     * @param messageReceiver
     *     Who will receive any messages that have to be sent.
     * @param responsible
     *     Who is responsible for this movable. Either the player who directly toggled it (via a command or the GUI), or
     *     the prime owner when this data is not available.
     * @param targetTime
     *     The amount of time this {@link MovableBase} will try to use to move. When null, the default speed is used.
     * @param skipAnimation
     *     If the {@link MovableBase} should be opened instantly (i.e. skip animation) or not.
     * @param actionType
     *     The type of action.
     * @return The result of the attempt.
     */
    final MovableToggleResult toggle(
        MovableActionCause cause, IMessageable messageReceiver, IPPlayer responsible, @Nullable Double targetTime,
        boolean skipAnimation, MovableActionType actionType)
    {
        return base.getMovableOpeningHelper().toggle(
            this, cause, messageReceiver, responsible, targetTime, skipAnimation, actionType);
    }

    /**
     * Handles the chunk of the rotation point being loaded.
     */
    public void onChunkLoad()
    {
        // TODO: Implement this
    }

    /**
     * Handles a change in redstone current for this movable's powerblock.
     *
     * @param newCurrent
     *     The new current of the powerblock.
     */
    @SuppressWarnings("unused")
    public void onRedstoneChange(int newCurrent)
    {
        base.onRedstoneChange(this, newCurrent);
    }

    @Override
    @Locked.Read
    public final MovableSnapshot getSnapshot()
    {
        return new MovableSnapshot(this);
    }

    /**
     * Synchronizes this movable and its serialized type-specific data of an {@link AbstractMovable} with the database.
     *
     * @return The result of the synchronization.
     */
    @Locked.Read
    public final CompletableFuture<DatabaseManager.ActionResult> syncData()
    {
        try
        {
            return base.getDatabaseManager()
                       .syncMovableData(getSnapshot(), serializer.serialize(this))
                       .exceptionally(Util::exceptionally);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to sync data for movable: %s", getBasicInfo());
        }
        return CompletableFuture.completedFuture(DatabaseManager.ActionResult.FAIL);
    }

    /**
     * Ensures that the current thread can obtain a write lock.
     * <p>
     * For example, when a thread already holds a read lock, that thread may not (try to) obtain a write lock as well,
     * as this would result in a deadlock.
     *
     * @throws IllegalStateException
     *     When the current thread is not allowed to obtain a write lock.
     */
    private void assertWriteLockable()
    {
        if (lock.getReadHoldCount() > 0)
            throw new IllegalStateException(
                "Caught potential deadlock! Trying to obtain write lock while under read lock!");
    }

    /**
     * Executes a supplier under a write lock.
     *
     * @param supplier
     *     The supplier to execute under the write lock.
     * @param <T>
     *     The return type of the supplier.
     * @return The value returned by the supplier.
     *
     * @throws IllegalStateException
     *     When the current thread may is not allowed to obtain a write lock.
     */
    @SuppressWarnings("unused")
    public final <T> T withWriteLock(Supplier<T> supplier)
    {
        // Sadly, we cannot use Locked.Read, as we need to ensure that
        // we can obtain a write lock in the first place.
        assertWriteLockable();
        lock.writeLock().lock();
        try
        {
            return supplier.get();
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    /**
     * Executes a Runnable under a write lock.
     *
     * @throws IllegalStateException
     *     When the current thread may is not allowed to obtain a write lock.
     */
    @SuppressWarnings("unused")
    public final void withWriteLock(Runnable runnable)
    {
        // Sadly, we cannot use Locked.Read, as we need to ensure that
        // we can obtain a write lock in the first place.
        assertWriteLockable();
        lock.writeLock().lock();
        try
        {
            runnable.run();
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    /**
     * Executes a supplier under a read lock.
     *
     * @param supplier
     *     The supplier to execute under the read lock.
     * @param <T>
     *     The return type of the supplier.
     * @return The value returned by the supplier.
     */
    // Obtaining a read lock won't cause a deadlock,
    // so no need to check it's possible.
    @Locked.Read
    @SuppressWarnings("unused")
    public final <T> T withReadLock(Supplier<T> supplier)
    {
        return supplier.get();
    }

    /**
     * Executes a Runnable under a read lock.
     */
    // Obtaining a read lock won't cause a deadlock,
    // so no need to check it's possible.
    @Locked.Read
    @SuppressWarnings("unused")
    public final void withReadLock(Runnable runnable)
    {
        runnable.run();
    }

    @Locked.Read
    public String getBasicInfo()
    {
        return getUid() + " (" + getPrimeOwner() + ") - " + getType().getSimpleName() + ": " + getName();
    }

    @Override
    @Locked.Read
    public String toString()
    {
        String ret = base + "\n"
            + "Type-specific data:\n"
            + "type: " + getType() + "\n";

        ret += serializer.toString(this);

        return ret;
    }

    /**
     * @return A map-based view of the owners.
     */
    final Map<UUID, MovableOwner> getOwnersView()
    {
        return base.getOwnersView();
    }

    /**
     * @return The locking object used by both this class and its {@link MovableBase}.
     */
    protected final ReentrantReadWriteLock getLock()
    {
        return lock;
    }

    static LazyValue<Rectangle> newAnimationRangeVal(AbstractMovable movable)
    {
        return new LazyValue<>(movable::calculateAnimationRange);
    }

    static LazyValue<Double> newAnimationCycleDistanceVal(AbstractMovable movable)
    {
        return new LazyValue<>(movable::calculateAnimationCycleDistance);
    }

    @Override
    public Cuboid getCuboid()
    {
        return base.getCuboid();
    }

    @Override
    public Collection<MovableOwner> getOwners()
    {
        return base.getOwners();
    }

    @Override
    public Optional<MovableOwner> getOwner(UUID uuid)
    {
        return base.getOwner(uuid);
    }

    @Override
    public boolean isOwner(UUID player)
    {
        return base.isOwner(player);
    }

    @Override
    public void setCoordinates(Cuboid newCuboid)
    {
        assertWriteLockable();
        invalidateAnimationData();
        base.setCoordinates(newCuboid);
    }

    @Override
    public void setRotationPoint(Vector3Di pos)
    {
        assertWriteLockable();
        invalidateAnimationData();
        base.setRotationPoint(pos);
    }

    @Override
    public void setPowerBlock(Vector3Di pos)
    {
        assertWriteLockable();
        base.setPowerBlock(pos);
    }

    @Override
    public void setName(String name)
    {
        assertWriteLockable();
        base.setName(name);
    }

    @Override
    public void setOpen(boolean open)
    {
        assertWriteLockable();
        invalidateAnimationData();
        base.setOpen(open);
    }

    @Override
    public void setOpenDir(MovementDirection openDir)
    {
        assertWriteLockable();
        invalidateAnimationData();
        base.setOpenDir(openDir);
    }

    @Override
    public void setLocked(boolean locked)
    {
        assertWriteLockable();
        base.setLocked(locked);
    }

    @Override
    public long getUid()
    {
        return base.getUid();
    }

    @Override
    public IPWorld getWorld()
    {
        return base.getWorld();
    }

    @Override
    public Vector3Di getRotationPoint()
    {
        return base.getRotationPoint();
    }

    @Override
    public Vector3Di getPowerBlock()
    {
        return base.getPowerBlock();
    }

    @Override
    public String getName()
    {
        return base.getName();
    }

    @Override
    public boolean isOpen()
    {
        return base.isOpen();
    }

    @Override
    public MovementDirection getOpenDir()
    {
        return base.getOpenDir();
    }

    @Override
    public boolean isLocked()
    {
        return base.isLocked();
    }

    @Override
    public MovableOwner getPrimeOwner()
    {
        return base.getPrimeOwner();
    }

    @Locked.Write final @Nullable MovableOwner removeOwner(UUID ownerUUID)
    {
        return base.removeOwner(ownerUUID);
    }

    @Locked.Write final boolean addOwner(MovableOwner movableOwner)
    {
        return base.addOwner(movableOwner);
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE) @ToString @EqualsAndHashCode
    public static final class MovableBaseHolder
    {
        private final MovableBase base;

        MovableBase get()
        {
            return base;
        }
    }
}
