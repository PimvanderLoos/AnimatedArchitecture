package nl.pim16aap2.animatedarchitecture.core.structures;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Delegate;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IPropertyContainerConst;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a (read-only) snapshot of a structure at a particular point in time.
 * <p>
 * All access to this class is thread safe and does not lock.
 * <p>
 * Please read the documentation of {@link nl.pim16aap2.animatedarchitecture.core.structures} for more information about
 * the structure system.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ThreadSafe
public final class StructureSnapshot implements IStructureConst
{
    private final long uid;
    private final IWorld world;
    private final Rectangle animationRange;
    private Vector3Di powerBlock;
    private String name;
    private Cuboid cuboid;
    private MovementDirection openDirection;
    private boolean isLocked;
    private final StructureOwner primeOwner;
    private final Map<UUID, StructureOwner> ownersMap;
    private final StructureType type;
    private final MovementDirection cycledOpenDirection;
    private final double minimumAnimationTime;

    @Delegate
    @Getter
    private final IPropertyContainerConst propertyContainerSnapshot;

    StructureSnapshot(Structure structure)
    {
        this(
            structure.getUid(),
            structure.getWorld(),
            structure.getAnimationRange(),
            structure.getPowerBlock(),
            structure.getName(),
            structure.getCuboid(),
            structure.getOpenDirection(),
            structure.isLocked(),
            structure.getPrimeOwner(),
            Map.copyOf(structure.getOwnersView()),
            structure.getType(),
            structure.getCycledOpenDirection(),
            structure.getMinimumAnimationTime(),
            structure.getPropertyContainerSnapshot()
        );
    }

    @Override
    public Optional<StructureOwner> getOwner(UUID player)
    {
        return Optional.ofNullable(getOwnersMap().get(player));
    }

    @Override
    public boolean isOwner(UUID player)
    {
        return getOwnersMap().containsKey(player);
    }

    @Override
    public boolean isOwner(UUID uuid, PermissionLevel permissionLevel)
    {
        final @Nullable StructureOwner owner = getOwnersMap().get(uuid);
        return owner != null && owner.permission().isLowerThanOrEquals(permissionLevel);
    }

    @Override
    public Collection<StructureOwner> getOwners()
    {
        return getOwnersMap().values();
    }

    @Override
    public StructureSnapshot getSnapshot()
    {
        return this;
    }
}
