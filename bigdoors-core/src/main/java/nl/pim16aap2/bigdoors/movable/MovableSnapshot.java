package nl.pim16aap2.bigdoors.movable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.Rectangle;
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
    private final Rectangle animationRange;
    private Vector3Di rotationPoint;
    private Vector3Di powerBlock;
    private String name;
    private Cuboid cuboid;
    private boolean isOpen;
    private MovementDirection openDir;
    private boolean isLocked;
    private final MovableOwner primeOwner;
    private final Map<UUID, MovableOwner> owners;
    private final MovableType type;

    MovableSnapshot(AbstractMovable movable)
    {
        this(
            movable.getUid(),
            movable.getWorld(),
            movable.getAnimationRange(),
            movable.getRotationPoint(),
            movable.getPowerBlock(),
            movable.getName(),
            movable.getCuboid(),
            movable.isOpen(),
            movable.getOpenDir(),
            movable.isLocked(),
            movable.getPrimeOwner(),
            Map.copyOf(movable.getOwnersView()),
            movable.getType()
        );
    }

    @Override
    public Optional<MovableOwner> getOwner(UUID player)
    {
        return Optional.ofNullable(owners.get(player));
    }

    @Override
    public boolean isOwner(UUID player)
    {
        return owners.containsKey(player);
    }

    @Override
    public Collection<MovableOwner> getOwners()
    {
        return owners.values();
    }

    @Override
    public MovableSnapshot getSnapshot()
    {
        return this;
    }
}
