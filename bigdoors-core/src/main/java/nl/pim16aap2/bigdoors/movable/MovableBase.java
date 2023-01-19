package nl.pim16aap2.bigdoors.movable;

import com.google.common.flogger.StackSize;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.managers.MovableRegistry;
import nl.pim16aap2.bigdoors.moveblocks.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.moveblocks.MovableActivityManager;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.Limit;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.GuardedBy;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents an unspecialized movable.
 *
 * @author Pim
 */
@EqualsAndHashCode(callSuper = false)
@Flogger
public final class MovableBase extends DatabaseManager.FriendMovableAccessor implements IMovable
{
    @Getter(AccessLevel.PACKAGE)
    @EqualsAndHashCode.Exclude @ToString.Exclude
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Getter
    private final long uid;

    @Getter
    private final IPWorld world;

    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    private Vector3Di rotationPoint;

    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    private Vector3Di powerBlock;

    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    private String name;

    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    private Cuboid cuboid;

    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    private boolean isOpen;

    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    private RotateDirection openDir;

    /**
     * Represents the locked status of this movable. True = locked, False = unlocked.
     */
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    private boolean isLocked;

    @Getter
    private final MovableOwner primeOwner;

    @EqualsAndHashCode.Exclude
    @GuardedBy("lock")
    private final Map<UUID, MovableOwner> owners;

    @EqualsAndHashCode.Exclude
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read, value = AccessLevel.PACKAGE)
    private final Map<UUID, MovableOwner> ownersView;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final IConfigLoader config;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final ILocalizer localizer;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final MovableRegistry movableRegistry;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final MovableOpeningHelper movableOpeningHelper;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final AutoCloseScheduler autoCloseScheduler;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final IPExecutor executor;

    @EqualsAndHashCode.Exclude
    private final DatabaseManager databaseManager;

    @EqualsAndHashCode.Exclude
    private final MovableActivityManager movableActivityManager;

    @EqualsAndHashCode.Exclude
    private final LimitsManager limitsManager;

    @EqualsAndHashCode.Exclude
    private final MovableToggleRequestBuilder movableToggleRequestBuilder;

    @EqualsAndHashCode.Exclude
    private final IPPlayerFactory playerFactory;

    @EqualsAndHashCode.Exclude
    private final Provider<BlockMover.Context> blockMoverContextProvider;

    @AssistedInject //
    MovableBase(
        @Assisted long uid, @Assisted String name, @Assisted Cuboid cuboid,
        @Assisted("rotationPoint") Vector3Di rotationPoint, @Assisted("powerBlock") Vector3Di powerBlock,
        @Assisted IPWorld world, @Assisted("isOpen") boolean isOpen, @Assisted("isLocked") boolean isLocked,
        @Assisted RotateDirection openDir, @Assisted MovableOwner primeOwner,
        @Assisted @Nullable Map<UUID, MovableOwner> owners, ILocalizer localizer,
        DatabaseManager databaseManager, MovableRegistry movableRegistry, MovableActivityManager movableActivityManager,
        LimitsManager limitsManager, AutoCloseScheduler autoCloseScheduler, MovableOpeningHelper movableOpeningHelper,
        MovableToggleRequestBuilder movableToggleRequestBuilder, IPPlayerFactory playerFactory,
        Provider<BlockMover.Context> blockMoverContextProvider,
        IPExecutor executor, IConfigLoader config)
    {
        this.uid = uid;
        this.name = name;
        this.cuboid = cuboid;
        this.rotationPoint = rotationPoint;
        this.powerBlock = powerBlock;
        this.world = world;
        this.isOpen = isOpen;
        this.isLocked = isLocked;
        this.openDir = openDir;
        this.primeOwner = primeOwner;

        final int initSize = owners == null ? 1 : owners.size();
        final Map<UUID, MovableOwner> movableOwnersTmp = new HashMap<>(initSize);
        if (owners == null)
            movableOwnersTmp.put(primeOwner.pPlayerData().getUUID(), primeOwner);
        else
            movableOwnersTmp.putAll(owners);
        this.owners = movableOwnersTmp;
        this.ownersView = Collections.unmodifiableMap(this.owners);

        this.config = config;
        this.localizer = localizer;
        this.databaseManager = databaseManager;
        this.movableRegistry = movableRegistry;
        this.movableActivityManager = movableActivityManager;
        this.limitsManager = limitsManager;
        this.autoCloseScheduler = autoCloseScheduler;
        this.movableOpeningHelper = movableOpeningHelper;
        this.movableToggleRequestBuilder = movableToggleRequestBuilder;
        this.playerFactory = playerFactory;
        this.blockMoverContextProvider = blockMoverContextProvider;
        this.executor = executor;
    }

    /**
     * @return A new {@link MovableSnapshot} of this {@link MovableBase}.
     */
    @Locked.Read
    @Override
    public MovableSnapshot getSnapshot()
    {
        return new MovableSnapshot(this);
    }

    /**
     * Synchronizes this {@link MovableBase} and the serialized type-specific data of an {@link AbstractMovable} with
     * the database.
     *
     * @param typeData
     *     The type-specific data of an {@link AbstractMovable}.
     * @return The result of the synchronization.
     */
    CompletableFuture<DatabaseManager.ActionResult> syncData(byte[] typeData)
    {
        return databaseManager.syncMovableData(getSnapshot(), typeData);
    }

    @Override
    @Locked.Write
    protected void addOwner(UUID uuid, MovableOwner movableOwner)
    {
        if (movableOwner.permission() == PermissionLevel.CREATOR)
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Failed to add Owner '%s' as owner to movable: %d because a permission level of 0 is not allowed!",
                    movableOwner.pPlayerData(), this.getUid());
            return;
        }
        owners.put(uuid, movableOwner);
    }

    /**
     * Checks if this movable exceeds the size limit for the given player.
     * <p>
     * See {@link LimitsManager#exceedsLimit(IPPlayer, Limit, int)}.
     *
     * @param player
     *     The player whose limit to compare against this movable's size.
     * @return True if {@link #getBlockCount()} exceeds the {@link Limit#MOVABLE_SIZE} for this movable.
     */
    boolean exceedSizeLimit(IPPlayer player)
    {
        return limitsManager.exceedsLimit(player, Limit.MOVABLE_SIZE, getBlockCount());
    }

    @Locked.Read void onRedstoneChange(AbstractMovable abstractMovable, int newCurrent)
    {
        final @Nullable MovableActionType movableActionType;
        if (newCurrent == 0 && isCloseable())
            movableActionType = MovableActionType.CLOSE;
        else if (newCurrent > 0 && isOpenable())
            movableActionType = MovableActionType.CLOSE;
        else
            movableActionType = null;

        if (movableActionType != null)
            movableToggleRequestBuilder.builder()
                                       .movable(abstractMovable)
                                       .movableActionCause(MovableActionCause.REDSTONE)
                                       .movableActionType(movableActionType)
                                       .messageReceiverServer()
                                       .responsible(playerFactory.create(getPrimeOwner().pPlayerData()))
                                       .build()
                                       .execute();
    }

    @Override
    @Locked.Write
    protected boolean removeOwner(UUID uuid)
    {
        if (primeOwner.pPlayerData().getUUID().equals(uuid))
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Failed to remove owner: '%s' as owner from movable: '%d'" +
                        " because removing an owner with a permission level of 0 is not allowed!",
                    primeOwner.pPlayerData(), this.getUid());
            return false;
        }
        return owners.remove(uuid) != null;
    }

    @Override
    @Locked.Read
    public Collection<MovableOwner> getOwners()
    {
        final List<MovableOwner> ret = new ArrayList<>(owners.size());
        ret.addAll(owners.values());
        return ret;
    }

    @Override
    @Locked.Read
    public Optional<MovableOwner> getOwner(UUID uuid)
    {
        return Optional.ofNullable(owners.get(uuid));
    }

    @Override
    @Locked.Read
    public boolean isOwner(UUID uuid)
    {
        return owners.containsKey(uuid);
    }

    @Override
    @Locked.Write
    public void setCoordinates(Cuboid newCuboid)
    {
        cuboid = newCuboid;
    }

    /**
     * Registers a {@link BlockMover} with the {@link MovableActivityManager}.
     * <p>
     * movableBase method MUST BE CALLED FROM THE MAIN THREAD! (Because of MC, spawning entities needs to happen
     * synchronously)
     *
     * @param abstractMovable
     *     The {@link AbstractMovable} to use.
     * @param cause
     *     What caused movableBase action.
     * @param time
     *     The amount of time movableBase {@link MovableBase} will try to use to move. The maximum speed is limited, so
     *     at a certain point lower values will not increase movable speed.
     * @param skipAnimation
     *     If the {@link MovableBase} should be opened instantly (i.e. skip animation) or not.
     * @param newCuboid
     *     The {@link Cuboid} representing the area the movable will take up after the toggle.
     * @param responsible
     *     The {@link IPPlayer} responsible for the movable action.
     * @param actionType
     *     The type of action that will be performed by the BlockMover.
     * @return True when everything went all right, otherwise false.
     */
    boolean registerBlockMover(
        AbstractMovable abstractMovable, MovableSnapshot snapshot, MovableActionCause cause, double time,
        boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible,
        MovableActionType actionType)
    {
        try
        {
            final BlockMover blockMover = abstractMovable.constructBlockMover(
                blockMoverContextProvider.get(), snapshot, cause, time, skipAnimation, newCuboid, responsible,
                actionType);

            movableActivityManager.addBlockMover(blockMover);
            executor.scheduleOnMainThread(blockMover::startAnimation);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log();
            return false;
        }
        return true;
    }

    /**
     * @return String with (almost) all data of this movable.
     */
    @Override
    @Locked.Read
    public String toString()
    {
        return uid + ": " + name + "\n"
            + formatLine("Cuboid", getCuboid())
            + formatLine("Rotation Point", this.getRotationPoint())
            + formatLine("PowerBlock Position: ", getPowerBlock())
            + formatLine("PowerBlock Hash: ", getChunkId())
            + formatLine("World", getWorld())
            + "This movable is " + (isLocked ? "" : "NOT ") + "locked.\n"
            + "This movable is " + (isOpen ? "open.\n" : "closed.\n")
            + formatLine("OpenDir", openDir.name());
    }

    @Locked.Read
    private String formatLine(String name, @Nullable Object obj)
    {
        final String objString = obj == null ? "NULL" : obj.toString();
        return name + ": " + objString + "\n";
    }

    /**
     * Checks if the current thread has either a read lock or a write lock.
     *
     * @return True if the thread from which this method is called has a read lock or a write lock on this object.
     */
    public boolean currentThreadHasLock()
    {
        return lock.isWriteLockedByCurrentThread() || lock.getReadHoldCount() > 0;
    }

    @AssistedFactory
    public interface IFactory
    {
        MovableBase create(
            long movableUID, String name, Cuboid cuboid, @Assisted("rotationPoint") Vector3Di rotationPoint,
            @Assisted("powerBlock") Vector3Di powerBlock, @Assisted IPWorld world,
            @Assisted("isOpen") boolean isOpen, @Assisted("isLocked") boolean isLocked,
            RotateDirection openDir, MovableOwner primeOwner, @Nullable Map<UUID, MovableOwner> movableOwners);
    }
}
