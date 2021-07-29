package nl.pim16aap2.bigdoors.doors;

import lombok.AllArgsConstructor;
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
    private final @NotNull ConcurrentHashMap<UUID, DoorOwner> doorOwners;

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

        final @NotNull ConcurrentHashMap<UUID, DoorOwner> doorOwnersTmp =
            new ConcurrentHashMap<>(doorOwners == null ? 1 : doorOwners.size());
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

    /**
     * Constructs a new {@link DoorBase}.
     */
    public DoorBase(final @NotNull DoorData doorData)
    {
        this(doorData.uid, doorData.name, doorData.cuboid, doorData.engine, doorData.powerBlock, doorData.world,
             doorData.isOpen, doorData.isLocked, doorData.openDirection, doorData.primeOwner, doorData.doorOwners);
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

    /**
     * Obtains a simple copy of the {@link DoorData} the describes this door.
     * <p>
     * Note that this creates a deep copy of the DoorData <u><b>excluding</u></b><b> its {@link DoorOwner}s</b>, so use
     * it sparingly.
     *
     * @return A copy of the {@link DoorData} the describes this door.
     */
    public synchronized @NotNull SimpleDoorData getSimpleDoorDataCopy()
    {
        return new SimpleDoorData(doorUID, name, cuboid, engine, powerBlock, world, isOpen, isLocked, openDir,
                                  primeOwner);
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

    /**
     * POD class that stores all the data needed for basic door initialization.
     * <p>
     * This type is called 'simple', as it doesn't include the list of all {@link DoorOwner}s. If you need that, use an
     * {@link DoorData} instead.
     *
     * @author Pim
     */
    @AllArgsConstructor
    @Getter
    public static class SimpleDoorData
    {
        /**
         * The UID of this door.
         */
        final long uid;

        /**
         * The name of this door.
         */
        final @NotNull String name;

        /**
         * The cuboid that describes this door.
         */
        final @NotNull Cuboid cuboid;

        /**
         * The location of the engine.
         */
        final @NotNull Vector3Di engine;

        /**
         * The location of the powerblock.
         */

        final @NotNull Vector3Di powerBlock;

        /**
         * The {@link IPWorld} this door is in.
         */
        final @NotNull IPWorld world;

        /**
         * Whether or not this door is currently open.
         */
        final boolean isOpen; // TODO: Use the bitflag here instead.

        /**
         * Whether or not this door is currently locked.
         */
        final boolean isLocked;

        /**
         * The open direction of this door.
         */

        final @NotNull RotateDirection openDirection;

        /**
         * The {@link DoorOwner} that originally created this door.
         */
        final @NotNull DoorOwner primeOwner;
    }

    /**
     * Represents a more complete picture of {@link SimpleDoorData}, as it includes a list of <u>all</u> {@link
     * DoorOwner}s of a door.
     * <p>
     * When no list of {@link DoorOwner}s is provided, it is assumed that the {@link SimpleDoorData#primeOwner} is the
     * only {@link DoorOwner}.
     *
     * @author Pim
     */
    public static class DoorData extends SimpleDoorData
    {
        /**
         * The list of {@link DoorOwner}s of this door.
         */
        @Getter
        private final @NotNull ConcurrentHashMap<@NotNull UUID, @NotNull DoorOwner> doorOwners;

        public DoorData(final long uid, final @NotNull String name, final @NotNull Cuboid cuboid,
                        final @NotNull Vector3Di engine, final @NotNull Vector3Di powerBlock,
                        final @NotNull IPWorld world, final boolean isOpen, final boolean isLocked,
                        final @NotNull RotateDirection openDirection, final @NotNull DoorOwner primeOwner)
        {
            super(uid, name, cuboid, engine, powerBlock, world, isOpen, isLocked, openDirection, primeOwner);
            doorOwners = new ConcurrentHashMap<>();
            doorOwners.put(primeOwner.pPlayerData().getUUID(), primeOwner);
        }

        public DoorData(final long uid, final @NotNull String name, final @NotNull Vector3Di min,
                        final @NotNull Vector3Di max, final @NotNull Vector3Di engine,
                        final @NotNull Vector3Di powerBlock, final @NotNull IPWorld world, final boolean isOpen,
                        final boolean isLocked, final @NotNull RotateDirection openDirection,
                        final @NotNull DoorOwner primeOwner)
        {
            this(uid, name, new Cuboid(min, max), engine, powerBlock, world, isOpen, isLocked, openDirection,
                 primeOwner);
        }

        public DoorData(final long uid, final @NotNull String name, final @NotNull Cuboid cuboid,
                        final @NotNull Vector3Di engine, final @NotNull Vector3Di powerBlock,
                        final @NotNull IPWorld world, final boolean isOpen, final boolean isLocked,
                        final @NotNull RotateDirection openDirection, final @NotNull DoorOwner primeOwner,
                        final @NotNull Map<@NotNull UUID, @NotNull DoorOwner> doorOwners)
        {
            super(uid, name, cuboid, engine, powerBlock, world, isOpen, isLocked, openDirection, primeOwner);
            this.doorOwners = new ConcurrentHashMap<>(doorOwners);
        }

        public DoorData(final long uid, final @NotNull String name, final @NotNull Vector3Di min,
                        final @NotNull Vector3Di max, final @NotNull Vector3Di engine,
                        final @NotNull Vector3Di powerBlock, final @NotNull IPWorld world, final boolean isOpen,
                        final boolean isLocked, final @NotNull RotateDirection openDirection,
                        final @NotNull DoorOwner primeOwner,
                        final @NotNull Map<@NotNull UUID, @NotNull DoorOwner> doorOwners)
        {
            this(uid, name, new Cuboid(min, max), engine, powerBlock, world, isOpen, isLocked, openDirection,
                 primeOwner, doorOwners);
        }
    }
}
