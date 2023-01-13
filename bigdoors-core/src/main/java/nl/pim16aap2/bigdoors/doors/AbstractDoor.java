package nl.pim16aap2.bigdoors.doors;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.moveblocks.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Represents the abstract base all door types should extend.
 *
 * @author Pim
 */
@EqualsAndHashCode
@Flogger
public abstract class AbstractDoor implements IDoor
{
    /**
     * The lock as used by both the {@link DoorBase} and this class.
     */
    @EqualsAndHashCode.Exclude
    private final ReentrantReadWriteLock lock;

    @EqualsAndHashCode.Exclude
    private final DoorSerializer<?> serializer;
    private final DoorRegistry doorRegistry;
    private final AutoCloseScheduler autoCloseScheduler;

    @Getter
    protected final DoorBase doorBase;
    protected final ILocalizer localizer;
    protected final IConfigLoader config;
    protected final DoorOpeningHelper doorOpeningHelper;

    protected AbstractDoor(
        DoorBase doorBase, ILocalizer localizer, DoorRegistry doorRegistry,
        AutoCloseScheduler autoCloseScheduler, DoorOpeningHelper doorOpeningHelper)
    {
        serializer = getDoorType().getDoorSerializer();
        this.lock = doorBase.getLock();
        this.doorBase = doorBase;
        this.localizer = localizer;
        this.doorRegistry = doorRegistry;
        this.config = doorBase.getConfig();
        this.autoCloseScheduler = autoCloseScheduler;
        this.doorOpeningHelper = doorOpeningHelper;

        log.at(Level.FINEST).log("Instantiating door: %d", doorBase.getDoorUID());
        if (doorBase.getDoorUID() > 0 && !doorRegistry.registerDoor(new Registrable()))
            throw new IllegalStateException("Tried to create new door \"" + doorBase.getDoorUID() +
                                                "\" while it is already registered!");
    }

    protected AbstractDoor(DoorBase doorBase)
    {
        this(doorBase, doorBase.getLocalizer(), doorBase.getDoorRegistry(),
             doorBase.getAutoCloseScheduler(), doorBase.getDoorOpeningHelper());
    }

    /**
     * Gets the {@link DoorType} of doorBase door.
     *
     * @return The {@link DoorType} of doorBase door.
     */
    public abstract DoorType getDoorType();

    /**
     * Gets the animation time of this door.
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
               .log("Target animation time of %.4f seconds is less than the minimum of %.4f seconds for door: %s.",
                    target, minimum, getBasicInfo());
            return minimum;
        }
        return target;
    }

    /**
     * Gets the animation time for this door.
     * <p>
     * The animation time is calculated using this door's {@link #getBaseAnimationTime()}, its
     * {@link #getMinimumAnimationTime()}, and the multiplier for this type as described by
     * {@link IConfigLoader#getAnimationSpeedMultiplier(DoorType)}.
     * <p>
     * If the target is not null, the returned value will simply be the maximum value of the target and
     * {@link #getBaseAnimationTime()}. Note that the time multiplier is ignored in this case.
     *
     * @param target
     *     The target time. When null, {@link #getBaseAnimationTime()} is used.
     * @return The animation time for this door in seconds.
     */
    final double getAnimationTime(@Nullable Double target)
    {
        final double realTarget = target != null ?
                                  target : config.getAnimationSpeedMultiplier(getDoorType()) * getBaseAnimationTime();
        return calculateAnimationTime(realTarget);
    }

    /**
     * Gets the distance traveled per animation by the animated block that travels the furthest.
     * <p>
     * For example, for a circular object, this will be a block on the edge, as these blocks travel further per
     * revolution than the blocks closer to the center of the circle.
     * <p>
     * This method looks at the distance per animation cycle. The exact definition of a cycle depends on the
     * implementation of the door. For a big door, for example, a cycle could be a single toggle, or a quarter circle.
     * To keep things easy, a revolving door (which may not have a definitive end to its animation) could define a cycle
     * as quarter circle as well.
     * <p>
     * The distance value is used to calculate {@link #getMinimumAnimationTime()} and {@link #getBaseAnimationTime()}.
     *
     * @return The longest distance traveled by an animated block measured in blocks.
     */
    protected abstract double getLongestAnimationCycleDistance();

    /**
     * The default speed of the animation in blocks/second, as measured by the fastest-moving block in the door.
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
     * The exact time limit depends on the shape, size, and type of door.
     *
     * @return The lower animation time limit for this door in seconds.
     */
    public double getMinimumAnimationTime()
    {
        return getLongestAnimationCycleDistance() / config.maxBlockSpeed();
    }

    /**
     * Gets the base animation time for this door.
     * <p>
     * This is the animation time without any multipliers applied to it.
     *
     * @return The base animation time for this door in seconds.
     */
    // TODO: This method should be abstract.
    public double getBaseAnimationTime()
    {
        return getLongestAnimationCycleDistance() / Math.min(getDefaultAnimationSpeed(), config.maxBlockSpeed());
    }

    /**
     * Checks if this door can be opened instantly (i.e. skip the animation).
     * <p>
     * This describes whether skipping the animation actually does anything. When a door can skip its animation, it
     * means that the actual toggle has an effect on the world and as such toggling it without an animation does
     * something.
     * <p>
     * Doors that do not have any effect other than their animation because all their blocks start and stop at exactly
     * the same place (i.e. flags) cannot skip their animation because skipping the animation would mean removing the
     * blocks of the door from their locations and then placing them back at exactly the same position. This would be a
     * waste of performance for both the server and the client and as such should be prevented.
     *
     * @return True, if the door can skip its animation.
     */
    public boolean canSkipAnimation()
    {
        return false;
    }

    /**
     * Finds the new minimum and maximum coordinates (represented by a {@link Cuboid}) of this door that would be the
     * result of toggling it.
     *
     * @return The {@link Cuboid} that would represent the door if it was toggled right now.
     */
    public abstract Optional<Cuboid> getPotentialNewCoordinates();

    /**
     * Gets the direction the door would go given its current state.
     *
     * @return The direction the door would go if it were to be toggled.
     */
    public abstract RotateDirection getCurrentToggleDir();

    /**
     * Cycle the {@link RotateDirection} direction this {@link IDoor} will open in. By default, it'll set and return the
     * opposite direction of the current direction.
     * <p>
     * Note that this does not actually change the open direction; it merely tells you which direction comes next!
     *
     * @return The new {@link RotateDirection} direction this {@link IDoor} will open in.
     */
    @SuppressWarnings("unused")
    public RotateDirection cycleOpenDirection()
    {
        final Set<RotateDirection> validOpenDirections = getDoorType().getValidOpenDirections();
        final RotateDirection currentDir = getOpenDir();

        @Nullable RotateDirection first = null;
        final Iterator<RotateDirection> it = validOpenDirections.iterator();
        while (it.hasNext())
        {
            final RotateDirection dir = it.next();
            if (first == null)
                first = dir;

            if (dir != currentDir)
                continue;

            if (it.hasNext())
                return it.next();
            break;
        }
        if (first != null)
            return first;
        log.at(Level.FINE)
           .log("Failed to cycle open direction for door of type '%s' with open dir '%s' given valid directions '%s'",
                getDoorType(), currentDir, validOpenDirections);
        return RotateDirection.NONE;
    }

    /**
     * Gets the {@link Supplier} for the {@link BlockMover} for doorBase type.
     * <p>
     * doorBase method MUST BE CALLED FROM THE MAIN THREAD! (Because of MC, spawning entities needs to happen
     * synchronously)
     *
     * @param cause
     *     What caused doorBase action.
     * @param time
     *     The amount of time doorBase {@link DoorBase} will try to use to move. The maximum speed is limited, so at a
     *     certain point lower values will not increase door speed.
     * @param skipAnimation
     *     If the {@link DoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param newCuboid
     *     The {@link Cuboid} representing the area the door will take up after the toggle.
     * @param responsible
     *     The {@link IPPlayer} responsible for the door action.
     * @param actionType
     *     The type of action that will be performed by the BlockMover.
     * @param context
     *     The {@link BlockMover.Context} to run the block mover in.
     * @return The {@link BlockMover} for doorBase class.
     */
    protected abstract BlockMover constructBlockMover(
        BlockMover.Context context, DoorActionCause cause, double time, boolean skipAnimation, Cuboid newCuboid,
        IPPlayer responsible, DoorActionType actionType)
        throws Exception;

    /**
     * Attempts to toggle a door. Think twice before using doorBase method. Instead, please look at
     * {@link DoorToggleRequestBuilder}.
     *
     * @param cause
     *     What caused doorBase action.
     * @param messageReceiver
     *     Who will receive any messages that have to be sent.
     * @param responsible
     *     Who is responsible for doorBase door. Either the player who directly toggled it (via a command or the GUI),
     *     or the prime owner when doorBase data is not available.
     * @param targetTime
     *     The amount of time doorBase {@link DoorBase} will try to use to move. When null, the default speed is used.
     * @param skipAnimation
     *     If the {@link DoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param actionType
     *     The type of action.
     * @return The result of the attempt.
     */
    final DoorToggleResult toggle(
        DoorActionCause cause, IMessageable messageReceiver, IPPlayer responsible, @Nullable Double targetTime,
        boolean skipAnimation, DoorActionType actionType)
    {
        return doorOpeningHelper.toggle(
            this, cause, messageReceiver, responsible, targetTime, skipAnimation, actionType);
    }

    /**
     * Handles a change in redstone current for this door's powerblock.
     *
     * @param newCurrent
     *     The new current of the powerblock.
     */
    @SuppressWarnings("unused")
    public final void onRedstoneChange(int newCurrent)
    {
        doorBase.onRedstoneChange(this, newCurrent);
    }

    @Override
    public DoorSnapshot getSnapshot()
    {
        return doorBase.getSnapshot();
    }

    /**
     * Synchronizes all data of this door with the database.
     *
     * @return True if the synchronization was successful.
     */
    @Locked.Read
    public final CompletableFuture<Boolean> syncData()
    {
        try
        {
            return doorBase.syncData(serializer.serialize(this)).exceptionally(Util::exceptionally);
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to sync data for door: %s", getBasicInfo());
        }
        return CompletableFuture.completedFuture(false);
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
        return getDoorUID() + " (" + getPrimeOwner() + ") - " + getDoorType().getSimpleName() + ": " + getName();
    }

    @Override
    @Locked.Read
    public String toString()
    {
        String ret = doorBase + "\n"
            + "Type-specific data:\n"
            + "type: " + getDoorType() + "\n";

        ret += serializer.toString(this);

        return ret;
    }

    /**
     * @return The locking object used by both this class and its {@link DoorBase}.
     */
    protected final ReentrantReadWriteLock getLock()
    {
        return lock;
    }

    @Override
    public Cuboid getCuboid()
    {
        return doorBase.getCuboid();
    }

    @Override
    public boolean isOpenable()
    {
        return doorBase.isOpenable();
    }

    @Override
    public boolean isCloseable()
    {
        return doorBase.isCloseable();
    }

    @Override
    public Collection<DoorOwner> getDoorOwners()
    {
        return doorBase.getDoorOwners();
    }

    @Override
    public Optional<DoorOwner> getDoorOwner(UUID uuid)
    {
        return doorBase.getDoorOwner(uuid);
    }

    @Override
    public boolean isDoorOwner(UUID player)
    {
        return doorBase.isDoorOwner(player);
    }

    @Override
    public void setCoordinates(Cuboid newCuboid)
    {
        assertWriteLockable();
        doorBase.setCoordinates(newCuboid);
    }

    @Override
    public void setRotationPoint(Vector3Di pos)
    {
        assertWriteLockable();
        doorBase.setRotationPoint(pos);
    }

    @Override
    public void setPowerBlock(Vector3Di pos)
    {
        assertWriteLockable();
        doorBase.setPowerBlock(pos);
    }

    @Override
    public void setName(String name)
    {
        assertWriteLockable();
        doorBase.setName(name);
    }

    @Override
    public void setOpen(boolean open)
    {
        assertWriteLockable();
        doorBase.setOpen(open);
    }

    @Override
    public void setOpenDir(RotateDirection openDir)
    {
        assertWriteLockable();
        doorBase.setOpenDir(openDir);
    }

    @Override
    public void setLocked(boolean locked)
    {
        assertWriteLockable();
        doorBase.setLocked(locked);
    }

    @Override
    public long getDoorUID()
    {
        return doorBase.getDoorUID();
    }

    @Override
    public IPWorld getWorld()
    {
        return doorBase.getWorld();
    }

    @Override
    public Vector3Di getRotationPoint()
    {
        return doorBase.getRotationPoint();
    }

    @Override
    public Vector3Di getPowerBlock()
    {
        return doorBase.getPowerBlock();
    }

    @Override
    public String getName()
    {
        return doorBase.getName();
    }

    @Override
    public boolean isOpen()
    {
        return doorBase.isOpen();
    }

    @Override
    public RotateDirection getOpenDir()
    {
        return doorBase.getOpenDir();
    }

    @Override
    public boolean isLocked()
    {
        return doorBase.isLocked();
    }

    @Override
    public DoorOwner getPrimeOwner()
    {
        return doorBase.getPrimeOwner();
    }

    public class Registrable
    {
        private Registrable()
        {
        }

        /**
         * Gets the {@link DoorBase} that is associated with this {@link Registrable}.
         *
         * @return The {@link DoorBase} that is associated with this {@link Registrable}.
         */
        public AbstractDoor getAbstractDoorBase()
        {
            return AbstractDoor.this;
        }
    }
}
