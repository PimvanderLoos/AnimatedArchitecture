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
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IRedstoneManager;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents an unspecialized movable.
 *
 * @author Pim
 */
@EqualsAndHashCode(callSuper = false)
@Flogger final class MovableBase
{
    @Getter(AccessLevel.PACKAGE)
    @EqualsAndHashCode.Exclude @ToString.Exclude
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Getter
    private final long uid;

    @Getter
    private final IPWorld world;

    @EqualsAndHashCode.Exclude @ToString.Exclude
    private final MovableToggleRequestBuilder movableToggleRequestBuilder;

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
    private MovementDirection openDir;

    /**
     * Represents the locked status of this movable. True = locked, False = unlocked.
     */
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    private boolean isLocked;

    @Getter
    private final MovableOwner primeOwner;
    private final IRedstoneManager redstoneManager;

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
    private final IPExecutor executor;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final DatabaseManager databaseManager;

    @EqualsAndHashCode.Exclude
    private final IPPlayerFactory playerFactory;

    @AssistedInject MovableBase(
        @Assisted long uid,
        @Assisted String name,
        @Assisted Cuboid cuboid,
        @Assisted("rotationPoint") Vector3Di rotationPoint,
        @Assisted("powerBlock") Vector3Di powerBlock,
        @Assisted IPWorld world,
        @Assisted("isOpen") boolean isOpen,
        @Assisted("isLocked") boolean isLocked,
        @Assisted MovementDirection openDir,
        @Assisted MovableOwner primeOwner,
        @Assisted @Nullable Map<UUID, MovableOwner> owners,
        ILocalizer localizer,
        DatabaseManager databaseManager,
        MovableRegistry movableRegistry,
        MovableOpeningHelper movableOpeningHelper,
        MovableToggleRequestBuilder movableToggleRequestBuilder,
        IPPlayerFactory playerFactory,
        IPExecutor executor,
        IRedstoneManager redstoneManager,
        IConfigLoader config)
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
        this.redstoneManager = redstoneManager;

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
        this.movableOpeningHelper = movableOpeningHelper;
        this.movableToggleRequestBuilder = movableToggleRequestBuilder;
        this.playerFactory = playerFactory;
        this.executor = executor;
    }

    @Locked.Write
    boolean addOwner(MovableOwner movableOwner)
    {
        if (movableOwner.permission() == PermissionLevel.CREATOR)
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Failed to add Owner '%s' as owner to movable: %d because a permission level of 0 is not allowed!",
                    movableOwner.pPlayerData(), this.getUid());
            return false;
        }
        owners.put(movableOwner.pPlayerData().getUUID(), movableOwner);
        return true;
    }

    @Locked.Write
    @Nullable MovableOwner removeOwner(UUID uuid)
    {
        if (primeOwner.pPlayerData().getUUID().equals(uuid))
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Failed to remove owner: '%s' as owner from movable: '%d'" +
                        " because removing an owner with a permission level of 0 is not allowed!",
                    primeOwner.pPlayerData(), this.getUid());
            return null;
        }
        return owners.remove(uuid);
    }

    private void verifyRedstoneState(AbstractMovable movable, Vector3Di powerBlock)
    {
        final var result = redstoneManager.isBlockPowered(world, powerBlock);
        if (result == IRedstoneManager.RedstoneStatus.DISABLED)
            return;
        onRedstoneChange(movable, result == IRedstoneManager.RedstoneStatus.POWERED);
    }

    public void verifyRedstoneState(AbstractMovable movable)
    {
        verifyRedstoneState(movable, getPowerBlock());
    }

    @Locked.Read void onRedstoneChange(AbstractMovable movable, boolean isPowered)
    {
        if (isPowered && movable.isOpen() || !isPowered && !movable.isOpen())
            return;

        final MovableActionType type = isPowered ? MovableActionType.OPEN : MovableActionType.CLOSE;
        movableToggleRequestBuilder
            .builder()
            .movable(movable)
            .movableActionCause(MovableActionCause.REDSTONE)
            .movableActionType(type)
            .messageReceiverServer()
            .responsible(playerFactory.create(getPrimeOwner().pPlayerData()))
            .build()
            .execute();
    }

    @Locked.Read
    public Collection<MovableOwner> getOwners()
    {
        final List<MovableOwner> ret = new ArrayList<>(owners.size());
        ret.addAll(owners.values());
        return ret;
    }

    @Locked.Read
    public Optional<MovableOwner> getOwner(UUID uuid)
    {
        return Optional.ofNullable(owners.get(uuid));
    }

    @Locked.Read
    public boolean isOwner(UUID uuid)
    {
        return owners.containsKey(uuid);
    }

    @Locked.Write
    public void setCoordinates(Cuboid newCuboid)
    {
        cuboid = newCuboid;
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
            + formatLine("PowerBlock Hash: ", Util.getChunkId(getPowerBlock()))
            + formatLine("World", getWorld())
            + formatLine("This movable is ", (isLocked ? "locked" : "unlocked"))
            + formatLine("This movable is ", (isOpen ? "open" : "closed"))
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
    @SuppressWarnings("unused")
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
            MovementDirection openDir, MovableOwner primeOwner, @Nullable Map<UUID, MovableOwner> movableOwners);
    }
}
