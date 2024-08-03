package nl.pim16aap2.animatedarchitecture.core.structures;

import lombok.RequiredArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.annotations.Initializer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyManager;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
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
        private PropertyManager propertyManager;

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
        public IBuilder propertiesOfStructure(PropertyManager propertyManager)
        {
            this.propertyManager = propertyManager;
            return this;
        }

        @Override
        public AbstractStructure.BaseHolder build()
        {
            return new AbstractStructure.BaseHolder(
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
                    propertyManager
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
         * Sets the properties of the structure.
         *
         * @param propertyManager
         *     The properties of the structure.
         * @return The next step of the guided builder process.
         */
        IBuilder propertiesOfStructure(PropertyManager propertyManager);
    }

    public interface IBuilder extends IUIDProvider
    {
        /**
         * Builds the {@link StructureBase} based on the provided input.
         *
         * @return The next step of the guided builder process.
         */
        AbstractStructure.BaseHolder build();
    }
}
