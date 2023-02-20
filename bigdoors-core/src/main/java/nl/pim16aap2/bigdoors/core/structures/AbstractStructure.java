package nl.pim16aap2.bigdoors.core.structures;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.IConfig;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.IWorld;
import nl.pim16aap2.bigdoors.core.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.core.moveblocks.AnimationRequestData;
import nl.pim16aap2.bigdoors.core.moveblocks.Animator;
import nl.pim16aap2.bigdoors.core.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import nl.pim16aap2.bigdoors.core.util.LazyValue;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.Rectangle;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;
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
 * Represents the abstract base all structure types should extend.
 *
 * @author Pim
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Flogger
public abstract class AbstractStructure implements IStructure
{
    /**
     * The lock as used by both the {@link StructureBase} and this class.
     */
    private final ReentrantReadWriteLock lock;
    private final StructureSerializer<?> serializer;

    @EqualsAndHashCode.Include
    private final StructureBase base;

    private final StructureType type;

    private final LazyValue<Rectangle> lazyAnimationRange;
    private final LazyValue<Double> lazyAnimationCycleDistance;
    private final LazyValue<StructureSnapshot> lazyStructureSnapshot;

    private AbstractStructure(StructureBase base, StructureType type)
    {
        this.type = type;
        serializer = getType().getStructureSerializer();
        this.lock = base.getLock();
        this.base = Objects.requireNonNull(base);

        lazyAnimationRange = new LazyValue<>(this::calculateAnimationRange);
        lazyAnimationCycleDistance = new LazyValue<>(this::calculateAnimationCycleDistance);
        lazyStructureSnapshot = new LazyValue<>(this::createNewSnapshot);
    }

    protected AbstractStructure(BaseHolder holder, StructureType type)
    {
        this(holder.base, type);
    }

    @Override
    public final StructureType getType()
    {
        return type;
    }

    /**
     * Gets the animation time of this structure.
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
               .log("Target animation time of %.4f seconds is less than the minimum of %.4f seconds for structure: %s.",
                    target, minimum, getBasicInfo());
            return minimum;
        }
        return target;
    }

    /**
     * Gets the animation time for this structure.
     * <p>
     * The animation time is calculated using this structure's {@link #getBaseAnimationTime()}, its
     * {@link #getMinimumAnimationTime()}, and the multiplier for this type as described by
     * {@link IConfig#getAnimationTimeMultiplier(StructureType)}.
     * <p>
     * If the target is not null, the returned value will simply be the maximum value of the target and
     * {@link #getBaseAnimationTime()}. Note that the time multiplier is ignored in this case.
     *
     * @param target
     *     The target time. When null, {@link #getBaseAnimationTime()} is used.
     * @return The animation time for this structure in seconds.
     */
    final double getAnimationTime(@Nullable Double target)
    {
        final double realTarget = target != null ?
                                  target :
                                  base.getConfig().getAnimationTimeMultiplier(getType()) * getBaseAnimationTime();
        return calculateAnimationTime(realTarget);
    }

    /**
     * Calculates the distance traveled per animation by the animated block that travels the furthest.
     * <p>
     * For example, for a circular object, this will be a block on the edge, as these blocks travel further per
     * revolution than the blocks closer to the center of the circle.
     * <p>
     * This method looks at the distance per animation cycle. The exact definition of a cycle depends on the
     * implementation of the structure. For a big door, for example, a cycle could be a single toggle, or a quarter
     * circle. To keep things easy, a revolving structure (which may not have a definitive end to its animation) could
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
        return lazyAnimationCycleDistance.get();
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
        return lazyAnimationRange.get();
    }

    /**
     * Invalidates all cached animation data.
     * <p>
     * Certain actions may result in the animation range and animation cycle distance being changed. This method has to
     * be called when that may happen.
     */
    protected final void invalidateAnimationData()
    {
        lazyAnimationRange.invalidate();
        lazyAnimationCycleDistance.invalidate();
        invalidateBasicData();
    }

    /**
     * Has to be called when any "basic data" is changed.
     * <p>
     * "Basic data" here means any data that does not affect the animation in any way that only exists in this class.
     * The name of the structure is one such example. Any type of "basic data" also refers to "All data", but not
     * necessarily the other way round.
     * <p>
     * If the animation may be affected in any way, use {@link #invalidateAnimationData()} instead.
     */
    private void invalidateBasicData()
    {
        lazyStructureSnapshot.invalidate();
    }

    /**
     * The default speed of the animation in blocks/second, as measured by the fastest-moving block in the structure.
     */
    protected double getDefaultAnimationSpeed()
    {
        return 1.5D;
    }

    /**
     * Gets the lower time limit for an animation.
     * <p>
     * Because animated blocks have a speed limit, as determined by {@link IConfig#maxBlockSpeed()}, there is also a
     * minimum amount of time for its animation.
     * <p>
     * The exact time limit depends on the shape, size, and type of structure.
     *
     * @return The lower animation time limit for this structure in seconds.
     */
    public double getMinimumAnimationTime()
    {
        return getAnimationCycleDistance() / base.getConfig().maxBlockSpeed();
    }

    /**
     * Gets the base animation time for this structure.
     * <p>
     * This is the animation time without any multipliers applied to it.
     *
     * @return The base animation time for this structure in seconds.
     */
    public double getBaseAnimationTime()
    {
        return getAnimationCycleDistance() /
            Math.min(getDefaultAnimationSpeed(), base.getConfig().maxBlockSpeed());
    }

    /**
     * Checks if this structure can be opened instantly (i.e. skip the animation).
     * <p>
     * This describes whether skipping the animation actually does anything. When a structure can skip its animation, it
     * means that the actual toggle has an effect on the world and as such toggling it without an animation does
     * something.
     * <p>
     * Structures that do not have any effect other than their animation because all their blocks start and stop at
     * exactly the same place (i.e. flags) cannot skip their animation because skipping the animation would mean
     * removing the blocks of the structure from their locations and then placing them back at exactly the same
     * position. This would be a waste of performance for both the server and the client and as such should be
     * prevented.
     *
     * @return True, if this structure can skip its animation.
     */
    public boolean canSkipAnimation()
    {
        return false;
    }

    /**
     * Finds the new minimum and maximum coordinates (represented by a {@link Cuboid}) of this structure that would be
     * the result of toggling it.
     *
     * @return The {@link Cuboid} that would represent the structure if it was toggled right now.
     */
    public abstract Optional<Cuboid> getPotentialNewCoordinates();

    /**
     * Gets the direction the structure would go given its current state.
     *
     * @return The direction the structure would go if it were to be toggled.
     */
    public abstract MovementDirection getCurrentToggleDir();

    /**
     * Cycle the {@link MovementDirection} direction this {@link IStructure} will open in. By default, it'll set and
     * return the opposite direction of the current direction.
     * <p>
     * Note that this does not actually change the open direction; it merely tells you which direction comes next!
     *
     * @return The new {@link MovementDirection} direction this {@link IStructure} will open in.
     */
    @SuppressWarnings("unused")
    @Locked.Write
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
           .log("Failed to cycle open direction for structure of type '%s' with open dir '%s' " +
                    "given valid directions '%s'",
                getType(), currentDir, validOpenDirections);
        return MovementDirection.NONE;
    }

    /**
     * @param data
     *     The data for the toggle request.
     * @return A new {@link Animator} for this type of structure.
     */
    protected abstract IAnimationComponent constructAnimationComponent(AnimationRequestData data);

    /**
     * Attempts to toggle a structure. Think twice before using this method. Instead, please look at
     * {@link StructureAnimationRequestBuilder}.
     *
     * @param request
     *     The toggle request to process.
     * @param responsible
     *     Who is responsible for this structure. Either the player who directly toggled it (via a command or the GUI),
     *     or the prime owner when this data is not available.
     * @return The result of the attempt.
     */
    final StructureToggleResult toggle(StructureAnimationRequest request, IPlayer responsible)
    {
        return base.getStructureOpeningHelper().toggle(this, request, responsible);
    }

    /**
     * Handles the chunk of the rotation point being loaded.
     */
    public void onChunkLoad()
    {
        base.onChunkLoad(this);
    }

    public void verifyRedstoneState()
    {
        base.verifyRedstoneState(this);
    }

    /**
     * Handles a change in redstone current for this structure's powerblock.
     *
     * @param isPowered
     *     True if the powerblock is now powered.
     */
    public void onRedstoneChange(boolean isPowered)
    {
        base.onRedstoneChange(this, isPowered);
    }

    @Locked.Read
    private StructureSnapshot createNewSnapshot()
    {
        return new StructureSnapshot(this);
    }

    @Override
    @Locked.Read
    public StructureSnapshot getSnapshot()
    {
        return lazyStructureSnapshot.get();
    }

    /**
     * Synchronizes this structure and its serialized type-specific data of an {@link AbstractStructure} with the
     * database.
     *
     * @return The result of the synchronization.
     */
    @Locked.Read
    public final CompletableFuture<DatabaseManager.ActionResult> syncData()
    {
        try
        {
            return base.getDatabaseManager()
                       .syncStructureData(getSnapshot(), serializer.serialize(this))
                       .exceptionally(Util::exceptionally);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to sync data for structure: %s", getBasicInfo());
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
            final T result = supplier.get();
            invalidateAnimationData();
            return result;
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
            invalidateAnimationData();
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
            + "type: " + getType() + "\n"
            + "Type data: ";

        try
        {
            ret += serializer.serialize(this);
        }
        catch (Exception e)
        {
            ret += "NULL";
            log.atSevere().withCause(e).log("Failed to get serialized data for structure %d", getUid());
        }

        return ret;
    }

    /**
     * @return A map-based view of the owners.
     */
    final Map<UUID, StructureOwner> getOwnersView()
    {
        return base.getOwnersView();
    }

    /**
     * @return The locking object used by both this class and its {@link StructureBase}.
     */
    protected final ReentrantReadWriteLock getLock()
    {
        return lock;
    }

    @Override
    public Cuboid getCuboid()
    {
        return base.getCuboid();
    }

    @Override
    public Collection<StructureOwner> getOwners()
    {
        return base.getOwners();
    }

    @Override
    public Optional<StructureOwner> getOwner(UUID uuid)
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
        invalidateBasicData();
        base.setPowerBlock(pos);
        verifyRedstoneState();
    }

    @Override
    public void setName(String name)
    {
        assertWriteLockable();
        invalidateBasicData();
        base.setName(name);
    }

    @Override
    public void setOpen(boolean open)
    {
        assertWriteLockable();
        invalidateAnimationData();
        base.setOpen(open);
        verifyRedstoneState();
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
        invalidateBasicData();
        base.setLocked(locked);
        verifyRedstoneState();
    }

    @Override
    public long getUid()
    {
        return base.getUid();
    }

    @Override
    public IWorld getWorld()
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
    public StructureOwner getPrimeOwner()
    {
        return base.getPrimeOwner();
    }

    @Locked.Write final @Nullable StructureOwner removeOwner(UUID ownerUUID)
    {
        final @Nullable StructureOwner removed = base.removeOwner(ownerUUID);
        if (removed != null)
            invalidateBasicData();
        return removed;
    }

    @Locked.Write final boolean addOwner(StructureOwner structureOwner)
    {
        final boolean result = base.addOwner(structureOwner);
        if (result)
            invalidateBasicData();
        return result;
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @EqualsAndHashCode
    public static final class BaseHolder
    {
        private final StructureBase base;

        StructureBase get()
        {
            return base;
        }

        @Override
        public String toString()
        {
            return "Holder for structure base: " + base.getUid();
        }
    }
}
