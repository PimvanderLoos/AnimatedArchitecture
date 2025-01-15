package nl.pim16aap2.animatedarchitecture.core.structures;

import lombok.RequiredArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.annotations.Initializer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyContainer;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

/**
 * Builder for {@link StructureBase} instances implemented as a guided builder.
 */
public final class StructureBaseBuilder
{
    private final StructureBase.IFactory baseFactory;

    @Inject
    StructureBaseBuilder(StructureBase.IFactory baseFactory)
    {
        this.baseFactory = baseFactory;
    }

    /**
     * Creates a new guided builder for a {@link StructureBase}.
     *
     * @return A new guided builder.
     */
    public IBuilderUID builder()
    {
        return new Builder(baseFactory);
    }

    @RequiredArgsConstructor
    private static final class Builder
        implements IBuilderUID, IBuilderName, IBuilderCuboid, IBuilderPowerBlock, IBuilderWorld,
        IBuilderIsLocked, IBuilderOpenDir, IBuilderPrimeOwner, IBuilderOwners, IBuilderProperties,
        IBuilder
    {
        private final StructureBase.IFactory baseFactory;

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

        @Override
        @Initializer
        public IBuilder propertiesOfStructure(PropertyContainer propertyContainer)
        {
            this.propertyContainer = propertyContainer;
            return this;
        }

        @Override
        public Structure.BaseHolder build()
        {
            return new Structure.BaseHolder(
                baseFactory.create(
                    structureUID,
                    name,
                    cuboid,
                    powerBlock,
                    world,
                    isLocked,
                    openDir,
                    primeOwner,
                    owners,
                    propertyContainer
                ));
        }
    }

    public interface IUIDProvider
    {
        /**
         * Gets the UID of the structure to create.
         *
         * @return The UID.
         */
        long getUID();
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

    public interface IBuilderName extends IUIDProvider
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

    public interface IBuilderCuboid extends IUIDProvider
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

    public interface IBuilderPowerBlock extends IUIDProvider
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

    public interface IBuilderWorld extends IUIDProvider
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

    public interface IBuilderIsLocked extends IUIDProvider
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

    public interface IBuilderOpenDir extends IUIDProvider
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

    public interface IBuilderPrimeOwner extends IUIDProvider
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

    public interface IBuilderOwners extends IUIDProvider
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

    public interface IBuilderProperties extends IUIDProvider
    {
        /**
         * Sets the properties of the structure to the default values.
         *
         * @param propertyContainer
         *     The properties of the structure.
         * @return The next step of the guided builder process.
         */
        IBuilder propertiesOfStructure(PropertyContainer propertyContainer);

        /**
         * Sets the properties of the structure to the default values.
         *
         * @param structureType
         *     The type of structure to set the properties for.
         * @return The next step of the guided builder process.
         */
        default IBuilder propertiesOfStructure(StructureType structureType)
        {
            return propertiesOfStructure(PropertyContainer.forType(structureType));
        }

        /**
         * Sets the properties of the structure to the provided values.
         * <p>
         * Any properties not provided will be set to their default values.
         * <p>
         * Only properties that are supported by the structure type will be set. See
         * {@link StructureType#getProperties()}. Trying to set an unsupported property will result in an exception.
         *
         * @param structureType
         *     The type of structure to set the properties for.
         * @param properties
         *     The properties to set. These should be provided in pairs of 2. The first element of the pair should be
         *     the property, the second element should be the value to set. If the property is nullable, the value may
         *     be {@code null}.
         * @return The next step of the guided builder process.
         *
         * @throws IllegalArgumentException
         *     If the properties are not provided in pairs of 2.
         *     <p>
         *     If the property is not valid for the structure type this property container was created for.
         */
        default IBuilder propertiesOfStructure(StructureType structureType, @Nullable Object @Nullable ... properties)
        {
            if (properties == null)
                return propertiesOfStructure(structureType);

            if (properties.length % 2 != 0)
                throw new IllegalArgumentException("Properties must be provided in pairs of 2.");

            final var propertyContainer = PropertyContainer.forType(structureType);
            for (int idx = 0; idx < properties.length; idx += 2)
            {
                final Property<?> property = (Property<?>) Util.requireNonNull(
                    properties[idx],
                    "Property at index " + idx
                );

                final @Nullable Object value = properties[idx + 1];
                propertyContainer.setUntypedPropertyValue(property, value);
            }

            return propertiesOfStructure(propertyContainer);
        }

        /**
         * Type-safe version of {@link #propertiesOfStructure(StructureType, Object...)} for 1 property.
         */
        default <T> IBuilder propertiesOfStructure(
            StructureType structureType,
            Property<T> property0, @Nullable T value0)
        {
            return propertiesOfStructure(
                structureType,
                property0, (Object) value0
            );
        }

        /**
         * Type-safe version of {@link #propertiesOfStructure(StructureType, Object...)} for 2 properties.
         */
        default <T, U> IBuilder propertiesOfStructure(
            StructureType structureType,
            Property<T> property0, @Nullable T value0,
            Property<U> property1, @Nullable U value1)
        {
            return propertiesOfStructure(
                structureType,
                property0, value0,
                property1, (Object) value1
            );
        }

        /**
         * Type-safe version of {@link #propertiesOfStructure(StructureType, Object...)} for 3 properties.
         */
        default <T, U, V> IBuilder propertiesOfStructure(
            StructureType structureType,
            Property<T> property0, @Nullable T value0,
            Property<U> property1, @Nullable U value1,
            Property<V> property2, @Nullable V value2)
        {
            return propertiesOfStructure(
                structureType,
                property0, value0,
                property1, value1,
                property2, (Object) value2
            );
        }

        /**
         * Type-safe version of {@link #propertiesOfStructure(StructureType, Object...)} for 4 properties.
         */
        default <T, U, V, W> IBuilder propertiesOfStructure(
            StructureType structureType,
            Property<T> property0, @Nullable T value0,
            Property<U> property1, @Nullable U value1,
            Property<V> property2, @Nullable V value2,
            Property<W> property3, @Nullable W value3)
        {
            return propertiesOfStructure(
                structureType,
                property0, value0,
                property1, value1,
                property2, value2,
                property3, (Object) value3
            );
        }
    }

    public interface IBuilder extends IUIDProvider
    {
        /**
         * Builds the {@link StructureBase} based on the provided input.
         *
         * @return The next step of the guided builder process.
         */
        Structure.BaseHolder build();
    }
}
