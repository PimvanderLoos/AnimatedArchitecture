package nl.pim16aap2.animatedarchitecture.core.structures;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IPropertyManagerConst;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

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
    private Vector3Di rotationPoint;
    private Vector3Di powerBlock;
    private String name;
    private Cuboid cuboid;
    private boolean isOpen;
    private MovementDirection openDir;
    private boolean isLocked;
    private final StructureOwner primeOwner;
    private final Map<UUID, StructureOwner> ownersMap;
    private final StructureType type;
    private final IPropertyManagerConst propertyManager;

    @Getter(AccessLevel.NONE)
    private final Map<String, Object> persistentVariableMap;


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
            structure.getType(),
            structure.getPropertyManagerSnapshot(),
            getPersistentVariableMap(structure)
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

    /**
     * Gets the value of a property of this structure.
     * <p>
     * This can be used to retrieve type-specific properties of a structure.
     *
     * @param key
     *     The key of the property.
     * @return The value of the property, or {@link Optional#empty()} if the property does not exist.
     */
    public Optional<Object> getProperty(String key)
    {
        return Optional.ofNullable(persistentVariableMap.get(key));
    }

    /**
     * Gets the property map of a structure.
     *
     * @param structure
     *     The structure to get the property map of.
     * @return The property map of the structure.
     */
    @VisibleForTesting
    public static Map<String, Object> getPersistentVariableMap(AbstractStructure structure)
    {
        try
        {
            return structure.getType().getStructureSerializer().getPersistentVariableMap(structure);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
