package nl.pim16aap2.bigdoors.doors;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
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
    private final @NotNull IPWorld world;

    @Getter
    private @NotNull Vector3Di engine;

    @Getter
    private @NotNull Vector3Di powerBlock;

    @Getter
    @Setter
    private @NotNull String name;

    private @NotNull Cuboid cuboid;

    @Getter
    @Setter
    private boolean isOpen;

    @Getter
    @Setter
    private @NotNull RotateDirection openDir;

    /**
     * Represents the locked status of this door. True = locked, False = unlocked.
     */
    @Getter
    @Setter
    private volatile boolean isLocked;

    @EqualsAndHashCode.Exclude
    // This is a ConcurrentHashMap to ensure serialization uses the correct type.
    private final @NotNull Map<UUID, DoorOwner> doorOwners;

    @Getter
    private final @NotNull DoorOwner primeOwner;

    public DoorBase(long doorUID, @NotNull String name, @NotNull Cuboid cuboid, @NotNull Vector3Di engine,
                    @NotNull Vector3Di powerBlock, @NotNull IPWorld world, boolean isOpen, boolean isLocked,
                    @NotNull RotateDirection openDir, @NotNull DoorOwner primeOwner,
                    @Nullable Map<UUID, DoorOwner> doorOwners)
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
        final @NotNull Map<UUID, DoorOwner> doorOwnersTmp = new ConcurrentHashMap<>(initSize);
        if (doorOwners == null)
            doorOwnersTmp.put(primeOwner.pPlayerData().getUUID(), primeOwner);
        else
            doorOwnersTmp.putAll(doorOwners);
        this.doorOwners = doorOwnersTmp;
    }

    public DoorBase(long doorUID, @NotNull String name, @NotNull Cuboid cuboid, @NotNull Vector3Di engine,
                    @NotNull Vector3Di powerBlock, @NotNull IPWorld world, boolean isOpen, boolean isLocked,
                    @NotNull RotateDirection openDir, @NotNull DoorOwner primeOwner)
    {
        this(doorUID, name, cuboid, engine, powerBlock, world, isOpen, isLocked, openDir, primeOwner, null);
    }

    // Copy constructor
    private DoorBase(@NotNull DoorBase other, @Nullable Map<UUID, DoorOwner> doorOwners)
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
    }

    /**
     * Gets a full copy of this {@link DoorBase}.
     * <p>
     * A full copy includes a full copy of {@link #doorOwners}. If this is not needed, consider using {@link
     * #getPartialSnapshot()} instead as it will be faster.
     *
     * @return A full copy of this {@link DoorBase}.
     */
    public synchronized @NotNull DoorBase getFullSnapshot()
    {
        return new DoorBase(this, getDoorOwnersCopy());
    }

    /**
     * Gets a full copy of this {@link DoorBase}.
     * <p>
     * A partial copy does not include the {@link #doorOwners}. If these are needed, consider using {@link
     * #getFullSnapshot()} instead.
     *
     * @return A partial copy of this {@link DoorBase}.
     */
    public synchronized @NotNull DoorBase getPartialSnapshot()
    {
        return new DoorBase(this, null);
    }

    @Override
    protected void addOwner(final @NotNull UUID uuid, final @NotNull DoorOwner doorOwner)
    {
        if (doorOwner.permission() == 0)
        {
            BigDoors.get().getPLogger().logThrowable(new IllegalArgumentException(
                "Failed to add owner: " + doorOwner.pPlayerData() + " as owner to door: " +
                    getDoorUID() +
                    " because a permission level of 0 is not allowed!"));
            return;
        }
        doorOwners.put(uuid, doorOwner);
    }

    @Override
    protected boolean removeOwner(final @NotNull UUID uuid)
    {
        if (primeOwner.pPlayerData().getUUID().equals(uuid))
        {
            BigDoors.get().getPLogger().logThrowable(new IllegalArgumentException(
                "Failed to remove owner: " + primeOwner.pPlayerData() + " as owner from door: " +
                    getDoorUID() + " because removing an owner with a permission level of 0 is not allowed!"));
            return false;
        }
        return doorOwners.remove(uuid) != null;
    }

    @Override
    public @NotNull Cuboid getCuboid()
    {
        return cuboid;
    }

    @Deprecated
    @Override
    public boolean isPowerBlockActive()
    {
        // FIXME: Cleanup
        Vector3Di powerBlockChunkSpaceCoords = Util.getChunkSpacePosition(getPowerBlock());
        Vector2Di powerBlockChunk = Util.getChunkCoords(getPowerBlock());
        if (BigDoors.get().getPlatform().getChunkManager().load(getWorld(), powerBlockChunk) ==
            IChunkManager.ChunkLoadResult.FAIL)
        {
            BigDoors.get().getPLogger()
                    .logThrowable(new IllegalStateException("Failed to load chunk at: " + powerBlockChunk));
            return false;
        }

        // TODO: Make sure that all corners around the block are also loaded (to check redstone).
        //       Might have to load up to 3 chunks.
        return BigDoors.get().getPlatform().getPowerBlockRedstoneManager()
                       .isBlockPowered(getWorld(), getPowerBlock());
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
    public @NotNull List<DoorOwner> getDoorOwners()
    {
        final List<DoorOwner> ret = new ArrayList<>(doorOwners.size());
        ret.addAll(doorOwners.values());
        return ret;
    }

    protected @NotNull Map<UUID, DoorOwner> getDoorOwnersCopy()
    {
        final @NotNull Map<UUID, DoorOwner> copy = new HashMap<>(doorOwners.size());
        doorOwners.forEach(copy::put);
        return copy;
    }

    @Override
    public @NotNull Optional<DoorOwner> getDoorOwner(final @NotNull IPPlayer player)
    {
        return getDoorOwner(player.getUUID());
    }

    @Override
    public @NotNull Optional<DoorOwner> getDoorOwner(final @NotNull UUID uuid)
    {
        return Optional.ofNullable(doorOwners.get(uuid));
    }

    @Override
    public void setCoordinates(final @NotNull Cuboid newCuboid)
    {
        cuboid = newCuboid;
    }

    @Override
    public void setCoordinates(final @NotNull Vector3Di posA, final @NotNull Vector3Di posB)
    {
        cuboid = new Cuboid(posA, posB);
    }

    @Override
    public @NotNull Vector3Di getMinimum()
    {
        return cuboid.getMin();
    }

    @Override
    public @NotNull Vector3Di getMaximum()
    {
        return cuboid.getMax();
    }

    @Override
    public @NotNull Vector3Di getDimensions()
    {
        return cuboid.getDimensions();
    }

    @Override
    public synchronized void setEngine(final @NotNull Vector3Di pos)
    {
        engine = pos;
    }

    @Override
    public void setPowerBlockPosition(final @NotNull Vector3Di pos)
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
    public synchronized @NotNull String toString()
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

    private @NotNull String formatLine(@NotNull String name, @Nullable Object obj)
    {
        final @NotNull String objString = obj == null ? "NULL" : obj.toString();
        return name + ": " + objString + "\n";
    }
}
