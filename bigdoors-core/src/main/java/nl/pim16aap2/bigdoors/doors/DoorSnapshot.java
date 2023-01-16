package nl.pim16aap2.bigdoors.doors;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a (read-only) snapshot of a door at a particular point in time.
 * <p>
 * All access to this class is thread safe and does not lock.
 *
 * @author Pim
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class DoorSnapshot implements IDoorConst
{
    private final long doorUID;
    private final IPWorld world;
    private Vector3Di rotationPoint;
    private Vector3Di powerBlock;
    private String name;
    private Cuboid cuboid;
    private boolean isOpen;
    private RotateDirection openDir;
    private boolean isLocked;
    private final DoorOwner primeOwner;
    private final Map<UUID, DoorOwner> doorOwners;

    DoorSnapshot(DoorBase doorBase)
    {
        this(
            doorBase.getDoorUID(),
            doorBase.getWorld(),
            doorBase.getRotationPoint(),
            doorBase.getPowerBlock(),
            doorBase.getName(),
            doorBase.getCuboid(),
            doorBase.isOpen(),
            doorBase.getOpenDir(),
            doorBase.isLocked(),
            doorBase.getPrimeOwner(),
            Map.copyOf(doorBase.getDoorOwnersView())
        );
    }

    @Override
    public Optional<DoorOwner> getDoorOwner(UUID player)
    {
        return Optional.ofNullable(doorOwners.get(player));
    }

    @Override
    public boolean isDoorOwner(UUID player)
    {
        return doorOwners.containsKey(player);
    }

    @Override
    public Collection<DoorOwner> getDoorOwners()
    {
        return doorOwners.values();
    }

    @Override
    public DoorSnapshot getSnapshot()
    {
        return this;
    }
}
