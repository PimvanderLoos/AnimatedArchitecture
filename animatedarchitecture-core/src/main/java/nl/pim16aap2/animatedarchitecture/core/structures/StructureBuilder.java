package nl.pim16aap2.animatedarchitecture.core.structures;

import lombok.RequiredArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.annotations.Initializer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IPropertyContainerConst;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyContainer;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Builder for {@link Structure} instances implemented as a guided builder.
 */
public final class StructureBuilder
{
    private final Structure.IFactory structureFactory;

    @Inject
    StructureBuilder(Structure.IFactory structureFactory)
    {
        this.structureFactory = structureFactory;
    }

    /**
     * Creates a new guided builder for a {@link Structure}.
     * <p>
     * This method is a shortcut for {@link #builder(StructureType, IStructureComponent)} with the component set to a
     * new instance provided by {@link StructureType#newComponent()}.
     *
     * @param type
     *     The type of structure to create.
     * @return A new guided builder.
     */
    public IBuilderUID builder(StructureType type)
    {
        return builder(Objects.requireNonNull(type), null);
    }

    /**
     * Creates a new guided builder for a {@link Structure}.
     *
     * @param type
     *     The type of structure to create.
     * @param component
     *     The component of the structure to create. This is likely to be {@link StructureType#newComponent()}.
     *     <p>
     *     If the component is {@code null}, the value will be provided by {@link StructureType#newComponent()}.
     * @return A new guided builder.
     */
    public IBuilderUID builder(StructureType type, @Nullable IStructureComponent component)
    {
        return new Builder(
            structureFactory,
            Objects.requireNonNull(type),
            Objects.requireNonNullElseGet(component, type::newComponent)
        );
    }

    @RequiredArgsConstructor
    private static final class Builder
        implements
        IBuilderUID,
        IBuilderName,
        IBuilderCuboid,
        IBuilderPowerBlock,
        IBuilderWorld,
        IBuilderIsLocked,
        IBuilderOpenDir,
        IBuilderPrimeOwner,
        IBuilderOwners,
        IBuilderProperties,
        IBuilder
    {
        private final Structure.IFactory structureFactory;
        private final StructureType structureType;
        private final IStructureComponent component;

        private long structureUID;
        private String name;
        private Cuboid cuboid;
        private Vector3Di powerBlock;
        private IWorld world;
        private boolean isLocked;
        private MovementDirection openDir;
        private StructureOwner primeOwner;
        private @Nullable Map<UUID, StructureOwner> owners;
        private PropertyContainer propertyContainer;

        @Override
        public long getUID()
        {
            return structureUID;
        }

        @Override
        public StructureType getStructureType()
        {
            return structureType;
        }

        @Override
        @Initializer
        public IBuilderName uid(long structureUID)
        {
            this.structureUID = structureUID;
            return this;
        }

        @Override
        @Initializer
        public IBuilderCuboid name(String name)
        {
            this.name = name;
            return this;
        }

        @Override
        @Initializer
        public IBuilderPowerBlock cuboid(Cuboid cuboid)
        {
            this.cuboid = cuboid;
            return this;
        }

        @Override
        @Initializer
        public IBuilderWorld powerBlock(Vector3Di powerBlock)
        {
            this.powerBlock = powerBlock;
            return this;
        }

        @Override
        @Initializer
        public IBuilderIsLocked world(IWorld world)
        {
            this.world = world;
            return this;
        }

        @Override
        @Initializer
        public IBuilderOpenDir isLocked(boolean isLocked)
        {
            this.isLocked = isLocked;
            return this;
        }

        @Override
        @Initializer
        public IBuilderPrimeOwner openDir(MovementDirection openDir)
        {
            this.openDir = openDir;
            return this;
        }

        @Override
        @Initializer
        public IBuilderOwners primeOwner(StructureOwner primeOwner)
        {
            this.primeOwner = primeOwner;
            return this;
        }

        @Override
        @Initializer
        public IBuilderProperties ownersOfStructure(@Nullable Map<UUID, StructureOwner> owners)
        {
            this.owners = owners;
            return this;
        }

        @Initializer
        private void setPropertyContainer(PropertyContainer propertyContainer)
        {
            this.propertyContainer = propertyContainer;
        }

        @Override
        public IBuilder propertiesOfStructure(IPropertyContainerConst propertyContainer)
        {
            setPropertyContainer(PropertyContainer.of(propertyContainer));
            return this;
        }

        @Override
        public IBuilder withDefaultProperties()
        {
            setPropertyContainer(PropertyContainer.forType(structureType));
            return this;
        }

        @Override
        public Structure build()
        {
            return structureFactory.create(
                structureUID,
                name,
                cuboid,
                powerBlock,
                world,
                isLocked,
                openDir,
                primeOwner,
                owners,
                propertyContainer,
                structureType,
                component
            );
        }
    }

    private interface IConstantsProvider
    {
        /**
         * Gets the UID of the structure to create.
         *
         * @return The UID.
         */
        long getUID();

        /**
         * Gets the type of structure to create.
         *
         * @return The type of structure.
         */
        StructureType getStructureType();
    }

    public interface IBuilderUID
    {
        /**
         * Provides the UID of the structure to create. If this structure hasn't been added to the database yet, this
         * value should be -1.
         *
         * @param structureUID
         *     The UID.
         * @return The next step of the guided builder process.
         */
        IBuilderName uid(long structureUID);
    }

    public interface IBuilderName extends IConstantsProvider
    {
        /**
         * Sets the name of the structure.
         *
         * @param name
         *     The name of the structure.
         * @return The next step of the guided builder process.
         */
        IBuilderCuboid name(String name);
    }

    public interface IBuilderCuboid extends IConstantsProvider
    {
        /**
         * Sets the cuboid of the structure. The cuboid refers to the 3d area defined by the min/max coordinate
         * combination of the structure.
         *
         * @param cuboid
         *     The cuboid.
         * @return The next step of the guided builder process.
         */
        IBuilderPowerBlock cuboid(Cuboid cuboid);

        /**
         * Sets the min/max coordinate-pair of the structure.
         *
         * @param min
         *     The minimum x/y/z coordinates of the structure.
         * @param max
         *     The maximum x/y/z coordinates of the structure.
         * @return The next step of the guided builder process.
         */
        default IBuilderPowerBlock cuboid(Vector3Di min, Vector3Di max)
        {
            return cuboid(new Cuboid(min, max));
        }
    }

    public interface IBuilderPowerBlock extends IConstantsProvider
    {
        /**
         * Sets the location of the power block of the structure.
         *
         * @param powerBlock
         *     The x/y/z coordinates of the structure's power block.
         * @return The next step of the guided builder process.
         */
        IBuilderWorld powerBlock(Vector3Di powerBlock);
    }

    public interface IBuilderWorld
    {
        /**
         * Sets the world the structure exists in.
         *
         * @param world
         *     The world.
         * @return The next step of the guided builder process.
         */
        IBuilderIsLocked world(IWorld world);
    }

    public interface IBuilderIsLocked
    {
        /**
         * Sets the locked-status of the structure.
         *
         * @param isLocked
         *     True if the structure is currently locked (true) or false if it is unlocked (false).
         * @return The next step of the guided builder process.
         */
        IBuilderOpenDir isLocked(boolean isLocked);
    }

    public interface IBuilderOpenDir
    {
        /**
         * Sets the open direction of the structure.
         *
         * @param openDir
         *     The rotation direction.
         * @return The next step of the guided builder process.
         */
        IBuilderPrimeOwner openDir(MovementDirection openDir);
    }

    public interface IBuilderPrimeOwner
    {
        /**
         * Sets the prime owner of the structure. This is the player who initially created the structure. This is the
         * player with permission level 0.
         *
         * @param primeOwner
         *     The prime owner of the structure.
         * @return The next step of the guided builder process.
         */
        IBuilderOwners primeOwner(StructureOwner primeOwner);
    }

    public interface IBuilderOwners
    {
        /**
         * Sets the (co-)owner(s) of the structure (including the prime owner).
         *
         * @param owners
         *     The (co-)owner(s) of the structure.
         * @return The next step of the guided builder process.
         */
        IBuilderProperties ownersOfStructure(@Nullable Map<UUID, StructureOwner> owners);
    }

    public interface IBuilderProperties extends IConstantsProvider
    {
        /**
         * Sets the properties of the structure to the default values.
         *
         * @param propertyContainer
         *     The properties of the structure.
         * @return The next step of the guided builder process.
         */
        IBuilder propertiesOfStructure(IPropertyContainerConst propertyContainer);

        /**
         * Sets the properties of the structure to the default values for the structure type.
         *
         * @return The next step of the guided builder process.
         */
        IBuilder withDefaultProperties();
    }

    public interface IBuilder
    {
        /**
         * Builds the {@link Structure} based on the provided input.
         *
         * @return The next step of the guided builder process.
         */
        Structure build();
    }
}
