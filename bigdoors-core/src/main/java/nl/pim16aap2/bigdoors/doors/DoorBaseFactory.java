package nl.pim16aap2.bigdoors.doors;

import lombok.RequiredArgsConstructor;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

/**
 * IFactory for {@link DoorBase} instances using a guided builder.
 *
 * @author Pim
 */
public final class DoorBaseFactory
{
    private final DoorBase.IFactory doorBaseFactory;

    @Inject //
    DoorBaseFactory(DoorBase.IFactory doorBaseFactory)
    {
        this.doorBaseFactory = doorBaseFactory;
    }

    /**
     * Creates a new guided builder for a {@link DoorBase}.
     *
     * @return A new guided builder.
     */
    public IBuilderUID builder()
    {
        return new Builder(doorBaseFactory);
    }

    @RequiredArgsConstructor
    private static final class Builder
        implements IBuilderUID, IBuilderName, IBuilderCuboid, IBuilderEngine, IBuilderPowerBlock, IBuilderWorld,
        IBuilderIsOpen, IBuilderIsLocked, IBuilderOpenDir, IBuilderPrimeOwner, IBuilderDoorOwners, IBuilder
    {
        private final DoorBase.IFactory doorBaseFactory;

        private long doorUID;
        private String name;
        private Cuboid cuboid;
        private Vector3Di engine;
        private Vector3Di powerBlock;
        private IPWorld world;
        private boolean isOpen;
        private boolean isLocked;
        private RotateDirection openDir;
        private DoorOwner primeOwner;
        private @Nullable Map<UUID, DoorOwner> doorOwners;

        @Override
        @Initializer
        public IBuilderName uid(long doorUID)
        {
            this.doorUID = doorUID;
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
        public IBuilderEngine cuboid(Cuboid cuboid)
        {
            this.cuboid = cuboid;
            return this;
        }

        @Override
        @Initializer
        public IBuilderPowerBlock engine(Vector3Di engine)
        {
            this.engine = engine;
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
        public IBuilderPrimeOwner openDir(RotateDirection openDir)
        {
            this.openDir = openDir;
            return this;
        }

        @Override
        @Initializer
        public IBuilderDoorOwners primeOwner(DoorOwner primeOwner)
        {
            this.primeOwner = primeOwner;
            return this;
        }

        @Override
        public IBuilder doorOwners(@Nullable Map<UUID, DoorOwner> doorOwners)
        {
            this.doorOwners = doorOwners;
            return this;
        }

        @Override
        public DoorBase build()
        {
            return doorBaseFactory.create(doorUID, name, cuboid, engine, powerBlock, world, isOpen,
                                          isLocked, openDir, primeOwner, doorOwners);
        }
    }

    public interface IBuilderUID
    {
        /**
         * Provides the UID of the door to create. If this door hasn't been added to the database yet, this value should
         * be -1.
         *
         * @param doorUID
         *     The UID.
         * @return The next step of the guided builder process.
         */
        IBuilderName uid(long doorUID);
    }

    public interface IBuilderName
    {
        /**
         * Sets the name of the door.
         *
         * @param name
         *     The name of the door.
         * @return The next step of the guided builder process.
         */
        IBuilderCuboid name(String name);
    }

    public interface IBuilderCuboid
    {
        /**
         * Sets the cuboid of the door. The cuboid refers to the 3d area defined by the min/max coordinate combination
         * of the door.
         *
         * @param cuboid
         *     The cuboid.
         * @return The next step of the guided builder process.
         */
        IBuilderEngine cuboid(Cuboid cuboid);

        /**
         * Sets the min/max coordinate pair of the door.
         *
         * @param min
         *     The minimum x/y/z coordinates of the door.
         * @param max
         *     The maximum x/y/z coordinates of the door.
         * @return The next step of the guided builder process.
         */
        default IBuilderEngine cuboid(Vector3Di min, Vector3Di max)
        {
            return cuboid(new Cuboid(min, max));
        }
    }

    public interface IBuilderEngine
    {
        /**
         * Sets the point around which the door will rotate.
         *
         * @param engine
         *     The x/y/z coordinates of the rotation point of the door.
         * @return The next step of the guided builder process.
         */
        IBuilderPowerBlock engine(Vector3Di engine);
    }

    public interface IBuilderPowerBlock
    {
        /**
         * Sets the location of the power block of the door.
         *
         * @param powerBlock
         *     The x/y/z coordinates of the door's power block.
         * @return The next step of the guided builder process.
         */
        IBuilderWorld powerBlock(Vector3Di powerBlock);
    }

    public interface IBuilderWorld
    {
        /**
         * Sets the world the door exists in.
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
         * Sets the open-status of the door.
         *
         * @param isLocked
         *     Whether the door is currently locked (true) or unlocked (false).
         * @return The next step of the guided builder process.
         */
        IBuilderOpenDir isLocked(boolean isLocked);
    }

    public interface IBuilderOpenDir
    {
        /**
         * Sets the open direction of the door.
         *
         * @param openDir
         *     The rotation direction.
         * @return The next step of the guided builder process.
         */
        IBuilderPrimeOwner openDir(RotateDirection openDir);
    }

    public interface IBuilderPrimeOwner
    {
        /**
         * Sets the prime owner of the door. This is the player who initially created the door. This is the player with
         * permission level 0.
         *
         * @param primeOwner
         *     The prime owner of the door.
         * @return The next step of the guided builder process.
         */
        IBuilderDoorOwners primeOwner(DoorOwner primeOwner);
    }

    public interface IBuilderDoorOwners extends IBuilder
    {
        /**
         * Sets the (co-)owner(s) of the door (including the prime owner).
         *
         * @param doorOwners
         *     The (co-)owner(s) of the door.
         * @return The next step of the guided builder process.
         */
        IBuilder doorOwners(@Nullable Map<UUID, DoorOwner> doorOwners);
    }

    public interface IBuilder
    {
        /**
         * Builds the {@link DoorBase} based on the provided input.
         *
         * @return The next step of the guided builder process.
         */
        DoorBase build();
    }
}
