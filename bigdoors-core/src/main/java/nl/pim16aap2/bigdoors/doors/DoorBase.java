package nl.pim16aap2.bigdoors.doors;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.moveblocks.DoorActivityManager;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an unspecialized door.
 *
 * @author Pim
 */
@EqualsAndHashCode(callSuper = false)
public final class DoorBase extends DatabaseManager.FriendDoorAccessor implements IDoor
{
    @Getter
    private final long doorUID;

    @Getter
    private final IPWorld world;

    @Getter
    private Vector3Di engine;

    @Getter
    private Vector3Di powerBlock;

    @Getter
    @Setter
    private String name;

    private Cuboid cuboid;

    @Getter
    @Setter
    private boolean isOpen;

    @Getter
    @Setter
    private RotateDirection openDir;

    /**
     * Represents the locked status of this door. True = locked, False = unlocked.
     */
    @Getter
    @Setter
    private volatile boolean isLocked;

    @EqualsAndHashCode.Exclude
    // This is a ConcurrentHashMap to ensure serialization uses the correct type.
    private final Map<UUID, DoorOwner> doorOwners;

    @Getter
    private final DoorOwner primeOwner;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final IPLogger logger;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final ILocalizer localizer;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final DatabaseManager databaseManager;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final DoorOpener doorOpener;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final DoorRegistry doorRegistry;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final DoorActivityManager doorActivityManager;

    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PACKAGE)
    private final LimitsManager limitsManager;

    DoorBase(long doorUID, String name, Cuboid cuboid, Vector3Di engine, Vector3Di powerBlock, IPWorld world,
             boolean isOpen, boolean isLocked, RotateDirection openDir, DoorOwner primeOwner,
             @Nullable Map<UUID, DoorOwner> doorOwners, IPLogger logger, ILocalizer localizer,
             DatabaseManager databaseManager, DoorOpener doorOpener, DoorRegistry doorRegistry,
             DoorActivityManager doorActivityManager, LimitsManager limitsManager)
    {
        this.doorUID = doorUID;
        this.name = name;
        this.cuboid = cuboid;
        this.engine = engine;
        this.powerBlock = powerBlock;
        this.world = world;
        this.isOpen = isOpen;
        this.isLocked = isLocked;
        this.openDir = openDir;
        this.primeOwner = primeOwner;

        final int initSize = doorOwners == null ? 1 : doorOwners.size();
        final Map<UUID, DoorOwner> doorOwnersTmp = new ConcurrentHashMap<>(initSize);
        if (doorOwners == null)
            doorOwnersTmp.put(primeOwner.pPlayerData().getUUID(), primeOwner);
        else
            doorOwnersTmp.putAll(doorOwners);
        this.doorOwners = doorOwnersTmp;

        this.logger = logger;
        this.localizer = localizer;
        this.databaseManager = databaseManager;
        this.doorOpener = doorOpener;
        this.doorRegistry = doorRegistry;
        this.doorActivityManager = doorActivityManager;
        this.limitsManager = limitsManager;
    }

    // Copy constructor
    private DoorBase(DoorBase other, @Nullable Map<UUID, DoorOwner> doorOwners)
    {
        doorUID = other.doorUID;
        name = other.name;
        cuboid = other.cuboid;
        engine = other.engine;
        powerBlock = other.powerBlock;
        world = other.world;
        isOpen = other.isOpen;
        isLocked = other.isLocked;
        openDir = other.openDir;
        primeOwner = other.primeOwner;
        this.doorOwners = doorOwners == null ? new ConcurrentHashMap<>(0) : doorOwners;

        logger = other.logger;
        localizer = other.localizer;
        databaseManager = other.databaseManager;
        doorOpener = other.doorOpener;
        doorRegistry = other.doorRegistry;
        doorActivityManager = other.doorActivityManager;
        limitsManager = other.limitsManager;
    }

    /**
     * Gets a full copy of this {@link DoorBase}.
     * <p>
     * A full copy includes a full copy of {@link #doorOwners}. If this is not needed, consider using {@link
     * #getPartialSnapshot()} instead as it will be faster.
     *
     * @return A full copy of this {@link DoorBase}.
     */
    public synchronized DoorBase getFullSnapshot()
    {
        return new DoorBase(this, new HashMap<>(doorOwners));
    }

    /**
     * Gets a full copy of this {@link DoorBase}.
     * <p>
     * A partial copy does not include the {@link #doorOwners}. If these are needed, consider using {@link
     * #getFullSnapshot()} instead.
     *
     * @return A partial copy of this {@link DoorBase}.
     */
    public synchronized DoorBase getPartialSnapshot()
    {
        return new DoorBase(this, null);
    }

    @Override
    protected void addOwner(UUID uuid, DoorOwner doorOwner)
    {
        if (doorOwner.permission() == 0)
        {
            logger.logThrowable(new IllegalArgumentException(
                "Failed to add owner: " + doorOwner.pPlayerData() + " as owner to door: " +
                    getDoorUID() +
                    " because a permission level of 0 is not allowed!"));
            return;
        }
        doorOwners.put(uuid, doorOwner);
    }

    @Override
    protected boolean removeOwner(UUID uuid)
    {
        if (primeOwner.pPlayerData().getUUID().equals(uuid))
        {
            logger.logThrowable(new IllegalArgumentException(
                "Failed to remove owner: " + primeOwner.pPlayerData() + " as owner from door: " +
                    getDoorUID() + " because removing an owner with a permission level of 0 is not allowed!"));
            return false;
        }
        return doorOwners.remove(uuid) != null;
    }

    @Override
    public Cuboid getCuboid()
    {
        return cuboid;
    }

    @Override
    public boolean isOpenable()
    {
        return !isOpen;
    }

    @Override
    public boolean isCloseable()
    {
        return isOpen;
    }

    @Override
    public List<DoorOwner> getDoorOwners()
    {
        final List<DoorOwner> ret = new ArrayList<>(doorOwners.size());
        ret.addAll(doorOwners.values());
        return ret;
    }

    @Override
    public Optional<DoorOwner> getDoorOwner(IPPlayer player)
    {
        return getDoorOwner(player.getUUID());
    }

    @Override
    public Optional<DoorOwner> getDoorOwner(UUID uuid)
    {
        return Optional.ofNullable(doorOwners.get(uuid));
    }

    @Override
    public void setCoordinates(Cuboid newCuboid)
    {
        cuboid = newCuboid;
    }

    @Override
    public void setCoordinates(Vector3Di posA, Vector3Di posB)
    {
        cuboid = new Cuboid(posA, posB);
    }

    @Override
    public Vector3Di getMinimum()
    {
        return cuboid.getMin();
    }

    @Override
    public Vector3Di getMaximum()
    {
        return cuboid.getMax();
    }

    @Override
    public Vector3Di getDimensions()
    {
        return cuboid.getDimensions();
    }

    @Override
    public synchronized void setEngine(Vector3Di pos)
    {
        engine = pos;
    }

    @Override
    public void setPowerBlockPosition(Vector3Di pos)
    {
        powerBlock = pos;
    }

    @Override
    public int getBlockCount()
    {
        return cuboid.getVolume();
    }

    @Override
    public synchronized long getSimplePowerBlockChunkHash()
    {
        return Util.simpleChunkHashFromLocation(powerBlock.x(), powerBlock.z());
    }

    /**
     * @return String with (almost) all data of this door.
     */
    @Override
    public synchronized String toString()
    {
        return doorUID + ": " + name + "\n"
            + formatLine("Cuboid", getCuboid())
            + formatLine("Engine", getEngine())
            + formatLine("PowerBlock position: ", getPowerBlock())
            + formatLine("PowerBlock Hash: ", getSimplePowerBlockChunkHash())
            + formatLine("World", getWorld())
            + "This door is " + (isLocked ? "" : "NOT ") + "locked.\n"
            + "This door is " + (isOpen ? "open.\n" : "closed.\n")
            + formatLine("OpenDir", openDir.name());
    }

    private String formatLine(String name, @Nullable Object obj)
    {
        final String objString = obj == null ? "NULL" : obj.toString();
        return name + ": " + objString + "\n";
    }
}
