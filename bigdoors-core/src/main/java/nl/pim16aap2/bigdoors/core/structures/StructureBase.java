package nl.pim16aap2.bigdoors.core.structures;

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
import nl.pim16aap2.bigdoors.core.api.IChunkLoader;
import nl.pim16aap2.bigdoors.core.api.IConfig;
import nl.pim16aap2.bigdoors.core.api.IExecutor;
import nl.pim16aap2.bigdoors.core.api.IRedstoneManager;
import nl.pim16aap2.bigdoors.core.api.IWorld;
import nl.pim16aap2.bigdoors.core.api.factories.IPlayerFactory;
import nl.pim16aap2.bigdoors.core.events.structureaction.StructureActionCause;
import nl.pim16aap2.bigdoors.core.events.structureaction.StructureActionType;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.core.moveblocks.StructureActivityManager;
import nl.pim16aap2.bigdoors.core.structures.structurearchetypes.IPerpetualMover;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.core.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;
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
 * Represents an unspecialized structure.
 *
 * @author Pim
 */
@EqualsAndHashCode(callSuper = false)
@Flogger final class StructureBase
{
    @Getter(AccessLevel.PACKAGE)
    @EqualsAndHashCode.Exclude @ToString.Exclude
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Getter
    private final long uid;

    @Getter
    private final IWorld world;

    @EqualsAndHashCode.Exclude @ToString.Exclude
    private final StructureToggleRequestBuilder structureToggleRequestBuilder;

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
     * Represents the locked status of this structure. True = locked, False = unlocked.
     */
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    private boolean isLocked;

    @Getter
    private final StructureOwner primeOwner;
    private final IRedstoneManager redstoneManager;
    private final StructureActivityManager structureActivityManager;
    private final IChunkLoader chunkLoader;

    @EqualsAndHashCode.Exclude
    @GuardedBy("lock")
    private final Map<UUID, StructureOwner> owners;

    @EqualsAndHashCode.Exclude
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read, value = AccessLevel.PACKAGE)
    private final Map<UUID, StructureOwner> ownersView;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final IConfig config;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final ILocalizer localizer;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final StructureRegistry structureRegistry;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final StructureOpeningHelper structureOpeningHelper;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final IExecutor executor;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final DatabaseManager databaseManager;

    @EqualsAndHashCode.Exclude
    private final IPlayerFactory playerFactory;

    @AssistedInject StructureBase(
        @Assisted long uid,
        @Assisted String name,
        @Assisted Cuboid cuboid,
        @Assisted("rotationPoint") Vector3Di rotationPoint,
        @Assisted("powerBlock") Vector3Di powerBlock,
        @Assisted IWorld world,
        @Assisted("isOpen") boolean isOpen,
        @Assisted("isLocked") boolean isLocked,
        @Assisted MovementDirection openDir,
        @Assisted StructureOwner primeOwner,
        @Assisted @Nullable Map<UUID, StructureOwner> owners,
        ILocalizer localizer,
        DatabaseManager databaseManager,
        StructureRegistry structureRegistry,
        StructureOpeningHelper structureOpeningHelper,
        StructureToggleRequestBuilder structureToggleRequestBuilder,
        IPlayerFactory playerFactory,
        IExecutor executor,
        IRedstoneManager redstoneManager,
        StructureActivityManager structureActivityManager,
        IChunkLoader chunkLoader,
        IConfig config)
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
        this.structureActivityManager = structureActivityManager;
        this.chunkLoader = chunkLoader;

        this.owners = new HashMap<>();
        if (owners == null)
            this.owners.put(primeOwner.playerData().getUUID(), primeOwner);
        else
            this.owners.putAll(owners);
        this.ownersView = Collections.unmodifiableMap(this.owners);

        this.config = config;
        this.localizer = localizer;
        this.databaseManager = databaseManager;
        this.structureRegistry = structureRegistry;
        this.structureOpeningHelper = structureOpeningHelper;
        this.structureToggleRequestBuilder = structureToggleRequestBuilder;
        this.playerFactory = playerFactory;
        this.executor = executor;
    }

    @Locked.Write
    boolean addOwner(StructureOwner structureOwner)
    {
        if (structureOwner.permission() == PermissionLevel.CREATOR)
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log(
                   "Failed to add Owner '%s' as owner to structure: %d because a permission level of 0 is not allowed!",
                   structureOwner.playerData(), this.getUid());
            return false;
        }
        owners.put(structureOwner.playerData().getUUID(), structureOwner);
        return true;
    }

    @Locked.Write
    @Nullable StructureOwner removeOwner(UUID uuid)
    {
        if (primeOwner.playerData().getUUID().equals(uuid))
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Failed to remove owner: '%s' as owner from structure: '%d'" +
                        " because removing an owner with a permission level of 0 is not allowed!",
                    primeOwner.playerData(), this.getUid());
            return null;
        }
        return owners.remove(uuid);
    }

    private boolean isChunkLoaded(IVector3D position)
    {
        return chunkLoader.checkChunk(world, position, IChunkLoader.ChunkLoadMode.VERIFY_LOADED) ==
            IChunkLoader.ChunkLoadResult.PASS;
    }

    @Locked.Read
    public void onChunkLoad(AbstractStructure structure)
    {
        if (!config.isRedstoneEnabled())
            return;

        final Vector3Di powerBlock = getPowerBlock();
        if (!isChunkLoaded(powerBlock) || !isChunkLoaded(getRotationPoint()))
            return;
        verifyRedstoneState(structure, powerBlock);
    }

    private void verifyRedstoneState(AbstractStructure structure, Vector3Di powerBlock)
    {
        if (!config.isRedstoneEnabled())
            return;

        final var result = redstoneManager.isBlockPowered(world, powerBlock);
        if (result == IRedstoneManager.RedstoneStatus.DISABLED)
            return;

        if (result == IRedstoneManager.RedstoneStatus.UNPOWERED &&
            structure instanceof IPerpetualMover perpetualMover &&
            perpetualMover.isPerpetual())
        {
            structureActivityManager.stopAnimatorsWithWriteAccess(structure.getUid());
            return;
        }

        onRedstoneChange(structure, result == IRedstoneManager.RedstoneStatus.POWERED);
    }

    public void verifyRedstoneState(AbstractStructure structure)
    {
        verifyRedstoneState(structure, getPowerBlock());
    }

    @Locked.Read void onRedstoneChange(AbstractStructure structure, boolean isPowered)
    {
        if (!config.isRedstoneEnabled())
            return;

        if (isPowered && !structure.isOpenable() || !isPowered && !structure.isCloseable())
            return;

        final StructureActionType type = isPowered ? StructureActionType.OPEN : StructureActionType.CLOSE;
        structureToggleRequestBuilder
            .builder()
            .structure(structure)
            .structureActionCause(StructureActionCause.REDSTONE)
            .structureActionType(type)
            .messageReceiverServer()
            .responsible(playerFactory.create(getPrimeOwner().playerData()))
            .build()
            .execute();
    }

    @Locked.Read
    public Collection<StructureOwner> getOwners()
    {
        final List<StructureOwner> ret = new ArrayList<>(owners.size());
        ret.addAll(owners.values());
        return ret;
    }

    @Locked.Read
    public Optional<StructureOwner> getOwner(UUID uuid)
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
     * @return String with (almost) all data of this structure.
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
            + formatLine("This structure is ", (isLocked ? "locked" : "unlocked"))
            + formatLine("This structure is ", (isOpen ? "open" : "closed"))
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
        StructureBase create(
            long structureUID, String name, Cuboid cuboid, @Assisted("rotationPoint") Vector3Di rotationPoint,
            @Assisted("powerBlock") Vector3Di powerBlock, @Assisted IWorld world,
            @Assisted("isOpen") boolean isOpen, @Assisted("isLocked") boolean isLocked,
            MovementDirection openDir, StructureOwner primeOwner, @Nullable Map<UUID, StructureOwner> structureOwners);
    }
}
