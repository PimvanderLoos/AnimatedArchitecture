package nl.pim16aap2.bigdoors.movable;

import lombok.RequiredArgsConstructor;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

/**
 * Builder for {@link MovableBase} instances implemented as a guided builder.
 *
 * @author Pim
 */
public final class MovableBaseBuilder
{
    private final MovableBase.IFactory doorBaseFactory;

    @Inject //
    public MovableBaseBuilder(MovableBase.IFactory doorBaseFactory)
    {
        this.doorBaseFactory = doorBaseFactory;
    }

    /**
     * Creates a new guided builder for a {@link MovableBase}.
     *
     * @return A new guided builder.
     */
    public IBuilderUID builder()
    {
        return new Builder(doorBaseFactory);
    }

    @RequiredArgsConstructor
    private static final class Builder
        implements IBuilderUID, IBuilderName, IBuilderCuboid, IBuilderRotationPoint, IBuilderPowerBlock, IBuilderWorld,
        IBuilderIsOpen, IBuilderIsLocked, IBuilderOpenDir, IBuilderPrimeOwner, IBuilderDoorOwners, IBuilder
    {
        private final MovableBase.IFactory doorBaseFactory;

        private long movableUID;
        private String name;
        private Cuboid cuboid;
        private Vector3Di rotationPoint;
        private Vector3Di powerBlock;
        private IPWorld world;
        private boolean isOpen;
        private boolean isLocked;
        private MovementDirection openDir;
        private MovableOwner primeOwner;
        private @Nullable Map<UUID, MovableOwner> doorOwners;

        @Override
        @Initializer
        public IBuilderName uid(long movableUID)
        {
            this.movableUID = movableUID;
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
        public IBuilderIsOpen world(IPWorld world)
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
        public IBuilderDoorOwners primeOwner(MovableOwner primeOwner)
        {
            this.primeOwner = primeOwner;
            return this;
        }

        @Override
        public IBuilder ownersOfMovable(@Nullable Map<UUID, MovableOwner> doorOwners)
        {
            this.doorOwners = doorOwners;
            return this;
        }

        @Override
        public AbstractMovable.MovableBaseHolder build()
        {
            return new AbstractMovable.MovableBaseHolder(
                doorBaseFactory.create(movableUID, name, cuboid, rotationPoint, powerBlock, world, isOpen,
                                       isLocked, openDir, primeOwner, doorOwners));
        }
    }

    public interface IBuilderUID
    {
        /**
         * Provides the UID of the movable to create. If this movable hasn't been added to the database yet, this value
         * should be -1.
         *
         * @param movableUID
         *     The UID.
         * @return The next step of the guided builder process.
         */
        IBuilderName uid(long movableUID);
    }

    public interface IBuilderName
    {
        /**
         * Sets the name of the movable.
         *
         * @param name
         *     The name of the movable.
         * @return The next step of the guided builder process.
         */
        IBuilderCuboid name(String name);
    }

    public interface IBuilderCuboid
    {
        /**
         * Sets the cuboid of the movable. The cuboid refers to the 3d area defined by the min/max coordinate
         * combination of the movable.
         *
         * @param cuboid
         *     The cuboid.
         * @return The next step of the guided builder process.
         */
        IBuilderRotationPoint cuboid(Cuboid cuboid);

        /**
         * Sets the min/max coordinate-pair of the movable.
         *
         * @param min
         *     The minimum x/y/z coordinates of the movable.
         * @param max
         *     The maximum x/y/z coordinates of the movable.
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
         * Sets the point around which the movable will rotate.
         *
         * @param rotationPoint
         *     The x/y/z coordinates of the rotation point of the movable.
         * @return The next step of the guided builder process.
         */
        IBuilderPowerBlock rotationPoint(Vector3Di rotationPoint);
    }

    public interface IBuilderPowerBlock
    {
        /**
         * Sets the location of the power block of the movable.
         *
         * @param powerBlock
         *     The x/y/z coordinates of the movable's power block.
         * @return The next step of the guided builder process.
         */
        IBuilderWorld powerBlock(Vector3Di powerBlock);
    }

    public interface IBuilderWorld
    {
        /**
         * Sets the world the movable exists in.
         *
         * @param world
         *     The world.
         * @return The next step of the guided builder process.
         */
        IBuilderIsOpen world(IPWorld world);
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
         * Sets the open-status of the movable.
         *
         * @param isLocked
         *     Whether the movable is currently locked (true) or unlocked (false).
         * @return The next step of the guided builder process.
         */
        IBuilderOpenDir isLocked(boolean isLocked);
    }

    public interface IBuilderOpenDir
    {
        /**
         * Sets the open direction of the movable.
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
         * Sets the prime owner of the movable. This is the player who initially created the movable. This is the player
         * with permission level 0.
         *
         * @param primeOwner
         *     The prime owner of the movable.
         * @return The next step of the guided builder process.
         */
        IBuilderDoorOwners primeOwner(MovableOwner primeOwner);
    }

    public interface IBuilderDoorOwners extends IBuilder
    {
        /**
         * Sets the (co-)owner(s) of the movable (including the prime owner).
         *
         * @param doorOwners
         *     The (co-)owner(s) of the movable.
         * @return The next step of the guided builder process.
         */
        IBuilder ownersOfMovable(@Nullable Map<UUID, MovableOwner> doorOwners);
    }

    public interface IBuilder
    {
        /**
         * Builds the {@link MovableBase} based on the provided input.
         *
         * @return The next step of the guided builder process.
         */
        AbstractMovable.MovableBaseHolder build();
    }
}
