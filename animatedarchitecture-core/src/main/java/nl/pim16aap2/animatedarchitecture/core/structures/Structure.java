package nl.pim16aap2.animatedarchitecture.core.structures;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IPropertyContainerConst;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IPropertyHolder;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IPropertyValue;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IStructureComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyContainerSnapshot;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyScope;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.FutureUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.util.LazyValue;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents the abstract base all structure types should extend.
 * <p>
 * Please read the documentation of {@link nl.pim16aap2.animatedarchitecture.core.structures} for more information about
 * the structure system.
 * <p>
 * Changes made to this class will not automatically be updated in the database. To update the state in the database,
 * you can use either the {@link #syncData()} method or the {@link #syncDataAsync()} method.
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Flogger
@ThreadSafe
public final class Structure implements IStructureConst, IPropertyHolder
{
    private static final double DEFAULT_ANIMATION_SPEED = 1.5D;

    /**
     * The lock as used by both the {@link StructureBase} and this class.
     */
    private final ReentrantReadWriteLock lock;
    private final StructureSerializer<?> serializer;

    @EqualsAndHashCode.Include
    private final StructureBase base;

    @EqualsAndHashCode.Include
    @GuardedBy("lock")
    private final IStructureComponent component;

    private final StructureType type;

    private final LazyValue<Rectangle> lazyAnimationRange;
    private final LazyValue<Double> lazyAnimationCycleDistance;
    private final LazyValue<StructureSnapshot> lazyStructureSnapshot;
    private final LazyValue<PropertyContainerSnapshot> lazyPropertyContainerSnapshot;

    private Structure(
        StructureBase base,
        StructureType type,
        IStructureComponent component
    )
    {
        this.type = type;
        this.serializer = getType().getStructureSerializer();
        this.lock = base.getLock();
        this.base = Objects.requireNonNull(base);
        this.component = Objects.requireNonNull(component);

        lazyAnimationRange = new LazyValue<>(this::calculateAnimationRange);
        lazyAnimationCycleDistance = new LazyValue<>(this::calculateAnimationCycleDistance);

        lazyStructureSnapshot = new LazyValue<>(this::createNewSnapshot);
        lazyPropertyContainerSnapshot = new LazyValue<>(this.base::newPropertyContainerSnapshot);
    }

    @Override
    public StructureType getType()
    {
        return type;
    }

    @Override
    @Locked.Read("lock")
    public MovementDirection getCycledOpenDirection()
    {
        return component.getCycledOpenDirection(this);
    }

    /**
     * Gets the animation time (in seconds) of this structure.
     * <p>
     * This basically returns max(target, {@link #getMinimumAnimationTime()}), logging a message in case the target time
     * is too low.
     *
     * @param target
     *     The target time.
     * @return The target time if it is bigger than the minimum time, otherwise the minimum.
     */
    @Locked.Read("lock")
    private double calculateAnimationTime(double target)
    {
        return component.calculateAnimationTime(this, target);
    }

    @Locked.Read("lock")
    private Rectangle calculateAnimationRange()
    {
        return component.calculateAnimationRange(this);
    }

    @Locked.Read("lock")
    private double calculateAnimationCycleDistance()
    {
        return component.calculateAnimationCycleDistance(this);
    }

    /**
     * Constructs a new animation component for this structure.
     *
     * @param data
     *     The data to construct the animation component with.
     * @return The constructed animation component.
     */
    @Locked.Read("lock")
    public IAnimationComponent constructAnimationComponent(AnimationRequestData data)
    {
        return this.component.constructAnimationComponent(this, data);
    }

    /**
     * Finds the new minimum and maximum coordinates (represented by a {@link Cuboid}) of this structure that would be
     * the result of toggling it.
     *
     * @return The {@link Cuboid} that would represent the structure if it was toggled right now.
     */
    @Locked.Read("lock")
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        return component.getPotentialNewCoordinates(this);
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
    @Locked.Read("lock")
    double getAnimationTime(@Nullable Double target)
    {
        final double realTarget = target != null ?
            target :
            base.getConfig().getAnimationTimeMultiplier(getType()) * getBaseAnimationTime();
        return calculateAnimationTime(realTarget);
    }

    /**
     * Gets the distance traveled per animation by the animated block that travels the furthest.
     * <p>
     * For example, for a circular object, this will be a block on the edge, as these blocks travel further per
     * revolution than the blocks closer to the center of the circle.
     * <p>
     * This method looks at the distance per animation cycle. The exact definition of a cycle depends on the
     * implementation of the structure. For a big door, for example, a cycle could be a single toggle, or a quarter
     * circle. To keep things easy, a revolving structure (which may not have a definitive end to its animation) could
     * define a cycle as quarter circle as well.
     * <p>
     * The distance value is used to calculate {@link Structure#getMinimumAnimationTime()} and
     * {@link Structure#getBaseAnimationTime()}.
     *
     * @return The longest distance traveled by an animated block measured in blocks.
     */
    public double getAnimationCycleDistance()
    {
        return lazyAnimationCycleDistance.get();
    }

    /**
     * Gets the rectangle describing the limits within an animation of this door takes place.
     * <p>
     * At no point during an animation will any animated block leave this cuboid, though not guarantees are given
     * regarding how tight the cuboid fits around the animated blocks.
     *
     * @return The animation range.
     */
    @Override
    public Rectangle getAnimationRange()
    {
        return lazyAnimationRange.get();
    }

    /**
     * Invalidates all cached animation data.
     * <p>
     * Certain actions may result in the animation range and animation cycle distance being changed. This method has to
     * be called when that may happen.
     */
    private void invalidateAnimationData()
    {
        lazyAnimationRange.reset();
        lazyAnimationCycleDistance.reset();
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
        lazyStructureSnapshot.reset();
    }

    /**
     * The default speed of the animation in blocks/second, as measured by the fastest-moving block in the structure.
     */
    private double getDefaultAnimationSpeed()
    {
        return DEFAULT_ANIMATION_SPEED;
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
    @Override
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
    @Locked.Read("lock")
    public boolean canSkipAnimation()
    {
        return component.canSkipAnimation(this);
    }

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
    final CompletableFuture<StructureToggleResult> toggle(StructureAnimationRequest request, IPlayer responsible)
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

    @Locked.Read("lock")
    private StructureSnapshot createNewSnapshot()
    {
        return new StructureSnapshot(this);
    }

    @Override
    @Locked.Read("lock")
    public StructureSnapshot getSnapshot()
    {
        return lazyStructureSnapshot.get();
    }

    /**
     * Synchronizes this structure and its serialized type-specific data of an {@link Structure} with the database.
     * <p>
     * Unlike {@link #syncData()}, this method will not block the current thread to wait for 1) a read lock to be
     * obtained, 2) the snapshot to be created, and 3) the data to be serialized.
     */
    public CompletableFuture<DatabaseManager.ActionResult> syncDataAsync()
    {
        return CompletableFuture
            .supplyAsync(this::syncData)
            .thenCompose(Function.identity())
            .exceptionally(ex -> FutureUtil.exceptionally(ex, DatabaseManager.ActionResult.FAIL));
    }

    /**
     * Synchronizes this structure and its serialized type-specific data of an {@link Structure} with the database.
     *
     * @return The result of the synchronization.
     */
    @Locked.Read("lock")
    public CompletableFuture<DatabaseManager.ActionResult> syncData()
    {
        try
        {
            return base
                .getDatabaseManager()
                .syncStructureData(getSnapshot(), serializer.serializeTypeData(this))
                .exceptionally(ex -> FutureUtil.exceptionally(ex, DatabaseManager.ActionResult.FAIL));
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
     * Use {@link #withWriteLock(Supplier)} instead.
     */
    @Locked.Write("lock")
    private <T> T withWriteLock0(Supplier<T> supplier)
    {
        final T result = supplier.get();
        invalidateAnimationData();
        return result;
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
    public <T> T withWriteLock(Supplier<T> supplier)
    {
        assertWriteLockable();
        return withWriteLock0(supplier);
    }

    /**
     * Use {@link #withWriteLock(Runnable)} instead.
     */
    @Locked.Write("lock")
    private void withWriteLock0(Runnable runnable)
    {
        runnable.run();
        invalidateAnimationData();
    }

    /**
     * Executes a Runnable under a write lock.
     *
     * @throws IllegalStateException
     *     When the current thread may is not allowed to obtain a write lock.
     */
    @SuppressWarnings("unused")
    public void withWriteLock(Runnable runnable)
    {
        assertWriteLockable();
        withWriteLock0(runnable);
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
    @Locked.Read("lock")
    @SuppressWarnings("unused")
    public <T> T withReadLock(Supplier<T> supplier)
    {
        return supplier.get();
    }

    /**
     * Executes a Runnable under a read lock.
     */
    // Obtaining a read lock won't cause a deadlock,
    // so no need to check it's possible.
    @Locked.Read("lock")
    @SuppressWarnings("unused")
    public void withReadLock(Runnable runnable)
    {
        runnable.run();
    }

    @Override
    @Locked.Read("lock")
    public String getBasicInfo()
    {
        return IStructureConst.super.getBasicInfo();
    }

    @Override
    @Locked.Read("lock")
    public String toString()
    {
        String ret = base + "\n"
            + "Type-specific data:\n"
            + "type: " + getType() + "\n"
            + "Type data: ";

        try
        {
            ret += serializer.serializeTypeData(this);
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
    public boolean isOwner(UUID uuid, PermissionLevel permissionLevel)
    {
        return base.isOwner(uuid, permissionLevel);
    }

    /**
     * Changes the position of this {@link Structure}. The min/max order of the positions doesn't matter.
     *
     * @param newCuboid
     *     The {@link Cuboid} representing the area the structure will take up from now on.
     */
    public void setCoordinates(Cuboid newCuboid)
    {
        assertWriteLockable();
        invalidateAnimationData();
        base.setCoordinates(newCuboid);
    }

    /**
     * Updates the position of the powerblock.
     *
     * @param pos
     *     The new position.
     */
    public void setPowerBlock(Vector3Di pos)
    {
        assertWriteLockable();
        invalidateBasicData();
        base.setPowerBlock(pos);
        verifyRedstoneState();
    }

    /**
     * Changes the name of the structure.
     *
     * @param name
     *     The new name of this structure.
     */
    public void setName(String name)
    {
        assertWriteLockable();
        invalidateBasicData();
        base.setName(name);
    }

    /**
     * Sets the {@link MovementDirection} this {@link Structure} will open if currently closed.
     * <p>
     * Note that if it's currently in the open status, it is supposed go in the opposite direction, as the closing
     * direction is the opposite of the opening direction.
     *
     * @param openDir
     *     The {@link MovementDirection} this {@link Structure} will open in.
     */
    public void setOpenDir(MovementDirection openDir)
    {
        assertWriteLockable();
        invalidateAnimationData();
        base.setOpenDir(openDir);
    }

    /**
     * Changes the lock status of this structure. Locked structures cannot be opened.
     *
     * @param locked
     *     New lock status.
     */
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
    public MovementDirection getOpenDirection()
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

    @Locked.Write("lock")
    final @Nullable StructureOwner removeOwner(UUID ownerUUID)
    {
        final @Nullable StructureOwner removed = base.removeOwner(ownerUUID);
        if (removed != null)
            invalidateBasicData();
        return removed;
    }

    @Locked.Write("lock")
    final boolean addOwner(StructureOwner structureOwner)
    {
        final boolean result = base.addOwner(structureOwner);
        if (result)
            invalidateBasicData();
        return result;
    }

    /**
     * Changes the position of this {@link Structure}. The min/max order of the positions does not matter.
     *
     * @param posA
     *     The first new position.
     * @param posB
     *     The second new position.
     */
    public void setCoordinates(Vector3Di posA, Vector3Di posB)
    {
        setCoordinates(new Cuboid(posA, posB));
    }

    @Override
    public IPropertyContainerConst getPropertyContainerSnapshot()
    {
        return lazyPropertyContainerSnapshot.get();
    }

    @Override
    @Locked.Read("lock")
    public <T> IPropertyValue<T> getPropertyValue(Property<T> property)
    {
        return base.getPropertyValue(property);
    }

    @Override
    @Locked.Read("lock")
    public boolean hasProperty(Property<?> property)
    {
        return base.hasProperty(property);
    }

    @Override
    @Locked.Read("lock")
    public boolean hasProperties(Collection<Property<?>> properties)
    {
        return base.hasProperties(properties);
    }

    @Override
    public <T> IPropertyValue<T> setPropertyValue(Property<T> property, @Nullable T value)
    {
        assertWriteLockable();
        return setPropertyValue0(property, value);
    }

    @Locked.Write("lock")
    private <T> IPropertyValue<T> setPropertyValue0(Property<T> property, @Nullable T value)
    {
        final var ret = base.setPropertyValue(property, value);
        handlePropertyChange(property);
        return ret;
    }

    /**
     * Handles a change in a property.
     * <p>
     * This will invalidate any cached data in the scope of the property.
     *
     * @param property
     *     The property that changed.
     */
    @GuardedBy("lock")
    private void handlePropertyChange(Property<?> property)
    {
        lazyPropertyContainerSnapshot.reset();
        property.getPropertyScopes().forEach(this::handlePropertyScopeChange);
        invalidateBasicData();
    }

    /**
     * Handles a change in a property scope.
     *
     * @param scope
     *     The scope that changed.
     */
    @GuardedBy("lock")
    private void handlePropertyScopeChange(PropertyScope scope)
    {
        switch (scope)
        {
            case REDSTONE -> verifyRedstoneState();
            case ANIMATION -> invalidateAnimationData();
            default -> throw new IllegalArgumentException("Unknown property scope: '" + scope + "'!");
        }
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @EqualsAndHashCode
    public static final class BaseHolder
    {
        private final StructureBase base;

        /**
         * Gets the UID of the structure this holder is for.
         *
         * @return The UID of the structure.
         */
        public long getUid()
        {
            return base.getUid();
        }

        @Override
        public String toString()
        {
            return "Holder for structure base: " + base.getUid();
        }
    }
}
