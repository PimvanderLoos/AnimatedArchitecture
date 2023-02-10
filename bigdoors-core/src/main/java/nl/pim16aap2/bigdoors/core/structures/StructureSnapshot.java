package nl.pim16aap2.bigdoors.core.structures;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.bigdoors.core.api.IWorld;
import nl.pim16aap2.bigdoors.core.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.Rectangle;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a (read-only) snapshot of a structure at a particular point in time.
 * <p>
 * All access to this class is thread safe and does not lock.
 *
 * @author Pim
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class StructureSnapshot implements IStructureConst
{
    private final long uid;
    private final IWorld world;
    private final Rectangle animationRange;
    private Vector3Di rotationPoint;
    private Vector3Di powerBlock;
    private String name;
    private Cuboid cuboid;
    private boolean isOpen;
    private MovementDirection openDir;
    private boolean isLocked;
    private final StructureOwner primeOwner;
    private final Map<UUID, StructureOwner> owners;
    private final StructureType type;

    StructureSnapshot(AbstractStructure structure)
    {
        this(
            structure.getUid(),
            structure.getWorld(),
            structure.getAnimationRange(),
            structure.getRotationPoint(),
            structure.getPowerBlock(),
            structure.getName(),
            structure.getCuboid(),
            structure.isOpen(),
            structure.getOpenDir(),
            structure.isLocked(),
            structure.getPrimeOwner(),
            Map.copyOf(structure.getOwnersView()),
            structure.getType()
        );
    }

    @Override
    public Optional<StructureOwner> getOwner(UUID player)
    {
        return Optional.ofNullable(owners.get(player));
    }

    @Override
    public boolean isOwner(UUID player)
    {
        return owners.containsKey(player);
    }

    @Override
    public Collection<StructureOwner> getOwners()
    {
        return owners.values();
    }

    @Override
    public StructureSnapshot getSnapshot()
    {
        return this;
    }
}
