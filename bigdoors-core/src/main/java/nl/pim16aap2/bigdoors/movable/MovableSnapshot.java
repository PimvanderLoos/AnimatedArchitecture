package nl.pim16aap2.bigdoors.movable;

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
 * Represents a (read-only) snapshot of a movable at a particular point in time.
 * <p>
 * All access to this class is thread safe and does not lock.
 *
 * @author Pim
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class MovableSnapshot implements IMovableConst
{
    private final long uid;
    private final IPWorld world;
    private Vector3Di rotationPoint;
    private Vector3Di powerBlock;
    private String name;
    private Cuboid cuboid;
    private boolean isOpen;
    private RotateDirection openDir;
    private boolean isLocked;
    private final MovableOwner primeOwner;
    private final Map<UUID, MovableOwner> movableOwners;

    MovableSnapshot(MovableBase movable)
    {
        this(
            movable.getUid(),
            movable.getWorld(),
            movable.getRotationPoint(),
            movable.getPowerBlock(),
            movable.getName(),
            movable.getCuboid(),
            movable.isOpen(),
            movable.getOpenDir(),
            movable.isLocked(),
            movable.getPrimeOwner(),
            Map.copyOf(movable.getMovableOwnersView())
        );
    }

    @Override
    public Optional<MovableOwner> getOwner(UUID player)
    {
        return Optional.ofNullable(movableOwners.get(player));
    }

    @Override
    public boolean isOwner(UUID player)
    {
        return movableOwners.containsKey(player);
    }

    @Override
    public Collection<MovableOwner> getOwners()
    {
        return movableOwners.values();
    }

    @Override
    public MovableSnapshot getSnapshot()
    {
        return this;
    }
}
