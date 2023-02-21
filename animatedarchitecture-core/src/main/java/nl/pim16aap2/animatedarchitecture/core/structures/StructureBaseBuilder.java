package nl.pim16aap2.animatedarchitecture.core.structures;

import lombok.RequiredArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.annotations.Initializer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

/**
 * Builder for {@link StructureBase} instances implemented as a guided builder.
 *
 * @author Pim
 */
public final class StructureBaseBuilder
{
    private final StructureBase.IFactory baseFactory;

    @Inject //
    public StructureBaseBuilder(StructureBase.IFactory baseFactory)
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
        implements IBuilderUID, IBuilderName, IBuilderCuboid, IBuilderRotationPoint, IBuilderPowerBlock, IBuilderWorld,
        IBuilderIsOpen, IBuilderIsLocked, IBuilderOpenDir, IBuilderPrimeOwner, IBuilderOwners, IBuilder
    {
        private final StructureBase.IFactory baseFactory;

        private long structureUID;
        private String name;
        private Cuboid cuboid;
        private Vector3Di rotationPoint;
        private Vector3Di powerBlock;
        private IWorld world;
        private boolean isOpen;
        private boolean isLocked;
        private MovementDirection openDir;
        private StructureOwner primeOwner;
        private @Nullable Map<UUID, StructureOwner> owners;

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
        public IBuilderRotationPoint cuboid(Cuboid cuboid)
        {
            this.cuboid = cuboid;
            return this;
        }

        @Override
        @Initializer
        public IBuilderPowerBlock rotationPoint(Vector3Di rotationPoint)
        {
            this.rotationPoint = rotationPoint;
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
        public IBuilderIsOpen world(IWorld world)
        {
            this.world = world;
            return this;
        }

        @Override
        @Initializer
        public IBuilderIsLocked isOpen(boolean isOpen)
        {
            this.isOpen = isOpen;
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
        public IBuilder ownersOfStructure(@Nullable Map<UUID, StructureOwner> owners)
        {
            this.owners = owners;
            return this;
        }

        @Override
        public AbstractStructure.BaseHolder build()
        {
            return new AbstractStructure.BaseHolder(
                baseFactory.create(structureUID, name, cuboid, rotationPoint, powerBlock, world, isOpen,
                                   isLocked, openDir, primeOwner, owners));
        }
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

    public interface IBuilderName
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

    public interface IBuilderCuboid
    {
        /**
         * Sets the cuboid of the structure. The cuboid refers to the 3d area defined by the min/max coordinate
         * combination of the structure.
         *
         * @param cuboid
         *     The cuboid.
         * @return The next step of the guided builder process.
         */
        IBuilderRotationPoint cuboid(Cuboid cuboid);

        /**
         * Sets the min/max coordinate-pair of the structure.
         *
         * @param min
         *     The minimum x/y/z coordinates of the structure.
         * @param max
         *     The maximum x/y/z coordinates of the structure.
         * @return The next step of the guided builder process.
         */
        default IBuilderRotationPoint cuboid(Vector3Di min, Vector3Di max)
        {
            return cuboid(new Cuboid(min, max));
        }
    }

    public interface IBuilderRotationPoint
    {
        /**
         * Sets the point around which the structure will rotate.
         *
         * @param rotationPoint
         *     The x/y/z coordinates of the rotation point of the structure.
         * @return The next step of the guided builder process.
         */
        IBuilderPowerBlock rotationPoint(Vector3Di rotationPoint);
    }

    public interface IBuilderPowerBlock
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
        IBuilderIsOpen world(IWorld world);
    }

    public interface IBuilderIsOpen
    {
        /**
         * a
         *
         * @param isOpen
         * @return The next step of the guided builder process.
         */
        IBuilderIsLocked isOpen(boolean isOpen);
    }

    public interface IBuilderIsLocked
    {
        /**
         * Sets the open-status of the structure.
         *
         * @param isLocked
         *     Whether the structure is currently locked (true) or unlocked (false).
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

    public interface IBuilderOwners extends IBuilder
    {
        /**
         * Sets the (co-)owner(s) of the structure (including the prime owner).
         *
         * @param owners
         *     The (co-)owner(s) of the structure.
         * @return The next step of the guided builder process.
         */
        IBuilder ownersOfStructure(@Nullable Map<UUID, StructureOwner> owners);
    }

    public interface IBuilder
    {
        /**
         * Builds the {@link StructureBase} based on the provided input.
         *
         * @return The next step of the guided builder process.
         */
        AbstractStructure.BaseHolder build();
    }
}
