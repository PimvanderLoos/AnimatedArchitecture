package nl.pim16aap2.bigdoors.doors;

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
import nl.pim16aap2.bigdoors.events.IDoorEventCaller;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.moveblocks.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.moveblocks.DoorActivityManager;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.Limit;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.GuardedBy;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * Represents an unspecialized door.
 *
 * @author Pim
 */
@EqualsAndHashCode(callSuper = false)
@Flogger
public final class DoorBase extends DatabaseManager.FriendDoorAccessor implements IDoor
{
    @Getter(AccessLevel.PACKAGE)
    @EqualsAndHashCode.Exclude @ToString.Exclude
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Getter
    private final long doorUID;

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
     * Represents the locked status of this door. True = locked, False = unlocked.
     */
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    private boolean isLocked;

    @Getter
    private final DoorOwner primeOwner;

    @EqualsAndHashCode.Exclude
    @GuardedBy("lock")
    private final Map<UUID, DoorOwner> doorOwners;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final IConfigLoader config;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final ILocalizer localizer;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final DoorRegistry doorRegistry;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final DoorOpeningHelper doorOpeningHelper;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final AutoCloseScheduler autoCloseScheduler;

    @EqualsAndHashCode.Exclude
    private final IDoorEventCaller doorEventCaller;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final IPExecutor executor;

    @EqualsAndHashCode.Exclude
    private final DatabaseManager databaseManager;

    @EqualsAndHashCode.Exclude
    private final DoorActivityManager doorActivityManager;

    @EqualsAndHashCode.Exclude
    private final LimitsManager limitsManager;

    @EqualsAndHashCode.Exclude
    private final DoorToggleRequestBuilder doorToggleRequestBuilder;

    @EqualsAndHashCode.Exclude
    private final IPPlayerFactory playerFactory;

    @EqualsAndHashCode.Exclude
    private final Provider<BlockMover.Context> blockMoverContextProvider;

    @AssistedInject //
    DoorBase(
        @Assisted long doorUID, @Assisted String name, @Assisted Cuboid cuboid,
        @Assisted("rotationPoint") Vector3Di rotationPoint, @Assisted("powerBlock") Vector3Di powerBlock,
        @Assisted IPWorld world, @Assisted("isOpen") boolean isOpen, @Assisted("isLocked") boolean isLocked,
        @Assisted RotateDirection openDir, @Assisted DoorOwner primeOwner,
        @Assisted @Nullable Map<UUID, DoorOwner> doorOwners, ILocalizer localizer,
        DatabaseManager databaseManager, DoorRegistry doorRegistry, DoorActivityManager doorActivityManager,
        LimitsManager limitsManager, AutoCloseScheduler autoCloseScheduler, DoorOpeningHelper doorOpeningHelper,
        DoorToggleRequestBuilder doorToggleRequestBuilder, IPPlayerFactory playerFactory,
        IDoorEventCaller doorEventCaller, Provider<BlockMover.Context> blockMoverContextProvider,
        IPExecutor executor, IConfigLoader config)
    {
        this.doorUID = doorUID;
        this.name = name;
        this.cuboid = cuboid;
        this.rotationPoint = rotationPoint;
        this.powerBlock = powerBlock;
        this.world = world;
        this.isOpen = isOpen;
        this.isLocked = isLocked;
        this.openDir = openDir;
        this.primeOwner = primeOwner;

        final int initSize = doorOwners == null ? 1 : doorOwners.size();
        final Map<UUID, DoorOwner> doorOwnersTmp = new HashMap<>(initSize);
        if (doorOwners == null)
            doorOwnersTmp.put(primeOwner.pPlayerData().getUUID(), primeOwner);
        else
            doorOwnersTmp.putAll(doorOwners);
        this.doorOwners = doorOwnersTmp;

        this.config = config;
        this.localizer = localizer;
        this.databaseManager = databaseManager;
        this.doorRegistry = doorRegistry;
        this.doorActivityManager = doorActivityManager;
        this.limitsManager = limitsManager;
        this.autoCloseScheduler = autoCloseScheduler;
        this.doorOpeningHelper = doorOpeningHelper;
        this.doorToggleRequestBuilder = doorToggleRequestBuilder;
        this.playerFactory = playerFactory;
        this.doorEventCaller = doorEventCaller;
        this.blockMoverContextProvider = blockMoverContextProvider;
        this.executor = executor;
    }

    // Copy constructor
    private DoorBase(DoorBase other, @Nullable Map<UUID, DoorOwner> doorOwners)
    {
        doorUID = other.doorUID;
        name = other.name;
        cuboid = other.cuboid;
        rotationPoint = other.rotationPoint;
        powerBlock = other.powerBlock;
        world = other.world;
        isOpen = other.isOpen;
        isLocked = other.isLocked;
        openDir = other.openDir;
        primeOwner = other.primeOwner;
        this.doorOwners = doorOwners == null ? new HashMap<>(0) : doorOwners;

        config = other.config;
        localizer = other.localizer;
        databaseManager = other.databaseManager;
        doorRegistry = other.doorRegistry;
        doorActivityManager = other.doorActivityManager;
        limitsManager = other.limitsManager;
        autoCloseScheduler = other.autoCloseScheduler;
        doorOpeningHelper = other.doorOpeningHelper;
        doorToggleRequestBuilder = other.doorToggleRequestBuilder;
        playerFactory = other.playerFactory;
        doorEventCaller = other.doorEventCaller;
        blockMoverContextProvider = other.blockMoverContextProvider;
        executor = other.executor;
    }

    /**
     * Gets a full copy of this {@link DoorBase}.
     * <p>
     * A full copy includes a full copy of {@link #doorOwners}. If this is not needed, consider using
     * {@link #getPartialSnapshot()} instead as it will be faster.
     *
     * @return A full copy of this {@link DoorBase}.
     */
    @SuppressWarnings("unused")
    @Locked.Read
    public DoorBase getFullSnapshot()
    {
        return new DoorBase(this, new HashMap<>(doorOwners));
    }

    /**
     * Gets a full copy of this {@link DoorBase}.
     * <p>
     * A partial copy does not include the {@link #doorOwners}. If these are needed, consider using
     * {@link #getFullSnapshot()} instead.
     *
     * @return A partial copy of this {@link DoorBase}.
     */
    @Locked.Read
    public DoorBase getPartialSnapshot()
    {
        return new DoorBase(this, null);
    }

    /**
     * Synchronizes this {@link DoorBase} and the serialized type-specific data of an {@link AbstractDoor} with the
     * database.
     *
     * @param typeData
     *     The type-specific data of an {@link AbstractDoor}.
     * @return Future true if the synchronization was successful.
     */
    @Locked.Read CompletableFuture<Boolean> syncData(byte[] typeData)
    {
        return databaseManager.syncDoorData(getPartialSnapshot(), typeData);
    }

    @Override
    @Locked.Write
    protected void addOwner(UUID uuid, DoorOwner doorOwner)
    {
        if (doorOwner.permission() == PermissionLevel.CREATOR)
        {
            log.at(Level.SEVERE).withStackTrace(StackSize.FULL)
               .log("Failed to add Owner '%s' as owner to door: %d because a permission level of 0 is not allowed!",
                    doorOwner.pPlayerData(), getDoorUID());
            return;
        }
        doorOwners.put(uuid, doorOwner);
    }

    /**
     * Checks if this door exceeds the size limit for the given player.
     * <p>
     * See {@link LimitsManager#exceedsLimit(IPPlayer, Limit, int)}.
     *
     * @param player
     *     The player whose limit to compare against this door's size.
     * @return True if {@link #getBlockCount()} exceeds the {@link Limit#DOOR_SIZE} for this door.
     */
    boolean exceedSizeLimit(IPPlayer player)
    {
        return limitsManager.exceedsLimit(player, Limit.DOOR_SIZE, getBlockCount());
    }

    @Locked.Read void onRedstoneChange(AbstractDoor abstractDoor, int newCurrent)
    {
        final @Nullable DoorActionType doorActionType;
        if (newCurrent == 0 && isCloseable())
            doorActionType = DoorActionType.CLOSE;
        else if (newCurrent > 0 && isOpenable())
            doorActionType = DoorActionType.CLOSE;
        else
            doorActionType = null;

        if (doorActionType != null)
            doorToggleRequestBuilder.builder()
                                    .door(abstractDoor)
                                    .doorActionCause(DoorActionCause.REDSTONE)
                                    .doorActionType(doorActionType)
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
            log.at(Level.SEVERE).withCause(new IllegalArgumentException(
                "Failed to remove owner: " + primeOwner.pPlayerData() + " as owner from door: " +
                    getDoorUID() + " because removing an owner with a permission level of 0 is not allowed!")).log();
            return false;
        }
        return doorOwners.remove(uuid) != null;
    }

    @Override
    @Locked.Read
    public boolean isOpenable()
    {
        return !isOpen;
    }

    @Override
    @Locked.Read
    public boolean isCloseable()
    {
        return isOpen;
    }

    @Override
    @Locked.Read
    public List<DoorOwner> getDoorOwners()
    {
        final List<DoorOwner> ret = new ArrayList<>(doorOwners.size());
        ret.addAll(doorOwners.values());
        return ret;
    }

    @Override
    @Locked.Read
    public Optional<DoorOwner> getDoorOwner(UUID uuid)
    {
        return Optional.ofNullable(doorOwners.get(uuid));
    }

    @Override
    @Locked.Read
    public boolean isDoorOwner(UUID uuid)
    {
        return doorOwners.containsKey(uuid);
    }

    @Override
    @Locked.Write
    public void setCoordinates(Cuboid newCuboid)
    {
        cuboid = newCuboid;
    }

    @Override
    @Locked.Read
    public Vector3Di getMinimum()
    {
        return cuboid.getMin();
    }

    @Override
    @Locked.Read
    public Vector3Di getMaximum()
    {
        return cuboid.getMax();
    }

    @Override
    @Locked.Read
    public Vector3Di getDimensions()
    {
        return cuboid.getDimensions();
    }

    @Override
    @Locked.Read
    public int getBlockCount()
    {
        return cuboid.getVolume();
    }

    @Override
    @Locked.Read
    public long getChunkId()
    {
        return Util.getChunkId(powerBlock);
    }

    /**
     * Registers a {@link BlockMover} with the {@link DoorActivityManager}.
     * <p>
     * doorBase method MUST BE CALLED FROM THE MAIN THREAD! (Because of MC, spawning entities needs to happen
     * synchronously)
     *
     * @param abstractDoor
     *     The {@link AbstractDoor} to use.
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
     * @return True when everything went all right, otherwise false.
     */
    // TODO: Move to DoorOpeningHelper.
    @Locked.Read
    boolean registerBlockMover(
        AbstractDoor abstractDoor, DoorActionCause cause, double time,
        boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible,
        DoorActionType actionType)
    {
        try
        {
            final BlockMover blockMover = abstractDoor.constructBlockMover(
                blockMoverContextProvider.get(), cause, time, skipAnimation, newCuboid, responsible, actionType);

            doorActivityManager.addBlockMover(blockMover);
            executor.scheduleOnMainThread(blockMover::startAnimation);
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log();
            return false;
        }
        return true;
    }

    /**
     * @return String with (almost) all data of this door.
     */
    @Override
    @Locked.Read
    public String toString()
    {
        return doorUID + ": " + name + "\n"
            + formatLine("Cuboid", getCuboid())
            + formatLine("Rotation Point", this.getRotationPoint())
            + formatLine("PowerBlock Position: ", getPowerBlock())
            + formatLine("PowerBlock Hash: ", getChunkId())
            + formatLine("World", getWorld())
            + "This door is " + (isLocked ? "" : "NOT ") + "locked.\n"
            + "This door is " + (isOpen ? "open.\n" : "closed.\n")
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
        DoorBase create(
            long doorUID, String name, Cuboid cuboid, @Assisted("rotationPoint") Vector3Di rotationPoint,
            @Assisted("powerBlock") Vector3Di powerBlock, @Assisted IPWorld world,
            @Assisted("isOpen") boolean isOpen, @Assisted("isLocked") boolean isLocked,
            RotateDirection openDir, DoorOwner primeOwner, @Nullable Map<UUID, DoorOwner> doorOwners);
    }
}
