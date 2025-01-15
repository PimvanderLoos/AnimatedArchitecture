package nl.pim16aap2.animatedarchitecture.core.structures;

import com.google.common.flogger.StackSize;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Locked;
import lombok.Setter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.StructureActivityManager;
import nl.pim16aap2.animatedarchitecture.core.api.IChunkLoader;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IRedstoneManager;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IPlayerFactory;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IPropertyValue;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IStructureWithOpenStatus;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IStructureWithRotationPoint;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyContainer;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyContainerSnapshot;
import nl.pim16aap2.animatedarchitecture.core.structures.structurearchetypes.IPerpetualMover;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.FutureUtil;
import nl.pim16aap2.animatedarchitecture.core.util.LocationUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

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
 * Represents the base of a structure.
 * <p>
 * Please read the documentation of {@link nl.pim16aap2.animatedarchitecture.core.structures} for more information about
 * the structure system.
 */
@EqualsAndHashCode(callSuper = false)
@Flogger
final class StructureBase
{
    @Getter(AccessLevel.PACKAGE)
    @EqualsAndHashCode.Exclude
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Getter
    private final long uid;

    @Getter
    private final IWorld world;

    @GuardedBy("lock")
    private final PropertyContainer propertyContainer;

    @EqualsAndHashCode.Exclude
    private final StructureAnimationRequestBuilder structureToggleRequestBuilder;

    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read("lock"))
    @Setter(onMethod_ = @Locked.Write("lock"))
    private Vector3Di powerBlock;

    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read("lock"))
    @Setter(onMethod_ = @Locked.Write("lock"))
    private String name;

    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read("lock"))
    private Cuboid cuboid;

    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read("lock"))
    @Setter(onMethod_ = @Locked.Write("lock"))
    private MovementDirection openDir;

    /**
     * Represents the locked status of this structure. True = locked, False = unlocked.
     */
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read("lock"))
    @Setter(onMethod_ = @Locked.Write("lock"))
    private boolean isLocked;

    @Getter
    @EqualsAndHashCode.Exclude
    private final StructureOwner primeOwner;

    @EqualsAndHashCode.Exclude
    private final IRedstoneManager redstoneManager;

    @EqualsAndHashCode.Exclude
    private final StructureActivityManager structureActivityManager;

    @EqualsAndHashCode.Exclude
    private final IChunkLoader chunkLoader;

    @EqualsAndHashCode.Exclude
    @GuardedBy("lock")
    private final Map<UUID, StructureOwner> owners;

    @EqualsAndHashCode.Exclude
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read("lock"), value = AccessLevel.PACKAGE)
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
    private final StructureToggleHelper structureOpeningHelper;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final IExecutor executor;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final DatabaseManager databaseManager;

    @EqualsAndHashCode.Exclude
    private final IPlayerFactory playerFactory;

    @AssistedInject
    StructureBase(
        @Assisted long uid,
        @Assisted String name,
        @Assisted Cuboid cuboid,
        @Assisted("powerBlock") Vector3Di powerBlock,
        @Assisted IWorld world,
        @Assisted("isLocked") boolean isLocked,
        @Assisted MovementDirection openDir,
        @Assisted StructureOwner primeOwner,
        @Assisted @Nullable Map<UUID, StructureOwner> owners,
        @Assisted PropertyContainer propertyContainer,
        ILocalizer localizer,
        DatabaseManager databaseManager,
        StructureRegistry structureRegistry,
        StructureToggleHelper structureOpeningHelper,
        StructureAnimationRequestBuilder structureToggleRequestBuilder,
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
        this.powerBlock = powerBlock;
        this.world = world;
        this.isLocked = isLocked;
        this.openDir = openDir;
        this.primeOwner = primeOwner;
        this.propertyContainer = propertyContainer;
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

    @Locked.Write("lock")
    boolean addOwner(StructureOwner structureOwner)
    {
        if (structureOwner.permission() == PermissionLevel.CREATOR)
        {
            log.atSevere().withStackTrace(StackSize.FULL).log(
                "Failed to add Owner '%s' as owner to structure: %d because a permission level of 0 is not allowed!",
                structureOwner.playerData(),
                this.getUid()
            );
            return false;
        }
        owners.put(structureOwner.playerData().getUUID(), structureOwner);
        return true;
    }

    @Locked.Write("lock")
    @Nullable
    StructureOwner removeOwner(UUID uuid)
    {
        if (primeOwner.playerData().getUUID().equals(uuid))
        {
            log.atSevere().withStackTrace(StackSize.FULL).log(
                "Failed to remove owner: '%s' as owner from structure: '%d'" +
                    " because removing an owner with a permission level of 0 is not allowed!",
                primeOwner.playerData(),
                this.getUid()
            );
            return null;
        }
        return owners.remove(uuid);
    }

    private boolean isChunkLoaded(IVector3D position)
    {
        return chunkLoader.checkChunk(world, position, IChunkLoader.ChunkLoadMode.VERIFY_LOADED) ==
            IChunkLoader.ChunkLoadResult.PASS;
    }

    /**
     * @return True if this structure base should ignore all redstone interaction.
     */
    private boolean shouldIgnoreRedstone()
    {
        return uid < 1 || !config.isRedstoneEnabled();
    }

    @Locked.Read("lock")
    public void onChunkLoad(Structure structure)
    {
        if (shouldIgnoreRedstone())
            return;

        final Vector3Di powerBlock = getPowerBlock();
        if (!isChunkLoaded(powerBlock))
            return;

        if (structure instanceof IStructureWithRotationPoint withRotationPoint &&
            !isChunkLoaded(withRotationPoint.getRotationPoint()))
            return;

        verifyRedstoneState(structure, powerBlock);
    }

    private void verifyRedstoneState(Structure structure, Vector3Di powerBlock)
    {
        if (shouldIgnoreRedstone())
            return;

        final var result = redstoneManager.isBlockPowered(world, powerBlock);
        if (result == IRedstoneManager.RedstoneStatus.DISABLED)
            return;

        if (result == IRedstoneManager.RedstoneStatus.UNPOWERED && structure instanceof IPerpetualMover)
        {
            structureActivityManager.stopAnimatorsWithWriteAccess(structure.getUid());
            return;
        }

        onRedstoneChange(structure, result == IRedstoneManager.RedstoneStatus.POWERED);
    }

    public void verifyRedstoneState(Structure structure)
    {
        verifyRedstoneState(structure, getPowerBlock());
    }

    @Locked.Read("lock")
    void onRedstoneChange(Structure structure, boolean isPowered)
    {
        if (shouldIgnoreRedstone())
            return;

        final StructureActionType actionType;
        if (structure instanceof IPerpetualMover)
        {
            if (!isPowered)
            {
                structureActivityManager.stopAnimatorsWithWriteAccess(structure.getUid());
                return;
            }
            actionType = StructureActionType.TOGGLE;
        }
        else if (structure instanceof IStructureWithOpenStatus openable)
        {
            if (isPowered && openable.isOpenable())
                actionType = StructureActionType.OPEN;
            else if (!isPowered && openable.isCloseable())
                actionType = StructureActionType.CLOSE;
            else
            {
                log.atFinest().log("Aborted toggle attempt with %s redstone for openable structure: %s",
                    (isPowered ? "powered" : "unpowered"), structure);
                return;
            }
        }
        else
        {
            log.atFinest().log("Aborted toggle attempt with %s redstone for structure: %s",
                (isPowered ? "powered" : "unpowered"), structure);
            return;
        }

        structureToggleRequestBuilder
            .builder()
            .structure(structure)
            .structureActionCause(StructureActionCause.REDSTONE)
            .structureActionType(actionType)
            .messageReceiverServer()
            .responsible(playerFactory.create(getPrimeOwner().playerData()))
            .build()
            .execute()
            .exceptionally(FutureUtil::exceptionally);
    }

    @Locked.Read("lock")
    public Collection<StructureOwner> getOwners()
    {
        final List<StructureOwner> ret = new ArrayList<>(owners.size());
        ret.addAll(owners.values());
        return ret;
    }

    @Locked.Read("lock")
    public Optional<StructureOwner> getOwner(UUID uuid)
    {
        return Optional.ofNullable(owners.get(uuid));
    }

    @Locked.Read("lock")
    public boolean isOwner(UUID uuid)
    {
        return owners.containsKey(uuid);
    }

    @Locked.Read("lock")
    public boolean isOwner(UUID uuid, PermissionLevel permissionLevel)
    {
        final @Nullable StructureOwner owner = owners.get(uuid);
        return owner != null && owner.permission().isLowerThanOrEquals(permissionLevel);
    }

    @Locked.Write("lock")
    public void setCoordinates(Cuboid newCuboid)
    {
        cuboid = newCuboid;
    }

    @Locked.Read("lock")
    public PropertyContainerSnapshot newPropertyContainerSnapshot()
    {
        return propertyContainer.snapshot();
    }

    @Locked.Read("lock")
    public <T> IPropertyValue<T> getPropertyValue(Property<T> property)
    {
        return propertyContainer.getPropertyValue(property);
    }

    @Locked.Read("lock")
    public boolean hasProperty(Property<?> property)
    {
        return propertyContainer.hasProperty(property);
    }

    @Locked.Read("lock")
    public boolean hasProperties(Collection<Property<?>> properties)
    {
        return propertyContainer.hasProperties(properties);
    }

    @Locked.Write("lock")
    public <T> IPropertyValue<T> setPropertyValue(Property<T> property, @Nullable T value)
    {
        return propertyContainer.setPropertyValue(property, value);
    }

    /**
     * @return String with (almost) all data of this structure.
     */
    @Override
    @Locked.Read("lock")
    public String toString()
    {
        return uid + ": " + name + "\n"
            + formatLine("Cuboid", getCuboid())
            + formatLine("PowerBlock Position: ", getPowerBlock())
            + formatLine("PowerBlock Hash: ", LocationUtil.getChunkId(getPowerBlock()))
            + formatLine("World", getWorld())
            + formatLine("This structure is ", (isLocked ? "locked" : "unlocked"))
            + formatLine("OpenDir", openDir.name()
            + formatLine("Properties", propertyContainer)
        );
    }

    @Locked.Read("lock")
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
    interface IFactory
    {
        StructureBase create(
            long structureUID,
            String name,
            Cuboid cuboid,
            @Assisted("powerBlock") Vector3Di powerBlock,
            @Assisted IWorld world,
            @Assisted("isLocked") boolean isLocked,
            MovementDirection openDir,
            StructureOwner primeOwner,
            @Nullable Map<UUID, StructureOwner> structureOwners,
            PropertyContainer propertyContainer
        );
    }
}
