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
 * Factory for {@link DoorBase} instances using a guided builder.
 *
 * @author Pim
 */
public final class DoorBaseFactory
{
    private final DoorBase.Factory doorBaseFactory;

    @Inject //
    DoorBaseFactory(DoorBase.Factory doorBaseFactory)
    {
        this.doorBaseFactory = doorBaseFactory;
    }

    /**
     * Creates a new guided builder for a {@link DoorBase}.
     *
     * @return A new guided builder.
     */
    public BuilderUID builder()
    {
        return new DoorBaseBuilder(doorBaseFactory);
    }

    @RequiredArgsConstructor
    private static final class DoorBaseBuilder
        implements BuilderUID, BuilderName, BuilderCuboid, BuilderEngine, BuilderPowerBlock, BuilderWorld,
        BuilderIsOpen, BuilderIsLocked, BuilderOpenDir, BuilderPrimeOwner, BuilderDoorOwners, Builder
    {
        private final DoorBase.Factory doorBaseFactory;

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
        public BuilderName uid(long doorUID)
        {
            this.doorUID = doorUID;
            return this;
        }

        @Override
        @Initializer
        public BuilderCuboid name(String name)
        {
            this.name = name;
            return this;
        }

        @Override
        @Initializer
        public BuilderEngine cuboid(Cuboid cuboid)
        {
            this.cuboid = cuboid;
            return this;
        }

        @Override
        @Initializer
        public BuilderPowerBlock engine(Vector3Di engine)
        {
            this.engine = engine;
            return this;
        }

        @Override
        @Initializer
        public BuilderWorld powerBlock(Vector3Di powerBlock)
        {
            this.powerBlock = powerBlock;
            return this;
        }

        @Override
        @Initializer
        public BuilderIsOpen world(IPWorld world)
        {
            this.world = world;
            return this;
        }

        @Override
        @Initializer
        public BuilderIsLocked isOpen(boolean isOpen)
        {
            this.isOpen = isOpen;
            return this;
        }

        @Override
        @Initializer
        public BuilderOpenDir isLocked(boolean isLocked)
        {
            this.isLocked = isLocked;
            return this;
        }

        @Override
        @Initializer
        public BuilderPrimeOwner openDir(RotateDirection openDir)
        {
            this.openDir = openDir;
            return this;
        }

        @Override
        @Initializer
        public BuilderDoorOwners primeOwner(DoorOwner primeOwner)
        {
            this.primeOwner = primeOwner;
            return this;
        }

        @Override
        public Builder doorOwners(@Nullable Map<UUID, DoorOwner> doorOwners)
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

    public interface BuilderUID
    {
        BuilderName uid(long doorUID);
    }

    public interface BuilderName
    {
        BuilderCuboid name(String name);
    }

    public interface BuilderCuboid
    {
        BuilderEngine cuboid(Cuboid cuboid);
    }

    public interface BuilderEngine
    {
        BuilderPowerBlock engine(Vector3Di engine);
    }

    public interface BuilderPowerBlock
    {
        BuilderWorld powerBlock(Vector3Di powerBlock);
    }

    public interface BuilderWorld
    {
        BuilderIsOpen world(IPWorld world);
    }

    public interface BuilderIsOpen
    {
        BuilderIsLocked isOpen(boolean isOpen);
    }

    public interface BuilderIsLocked
    {
        BuilderOpenDir isLocked(boolean isLocked);
    }

    public interface BuilderOpenDir
    {
        BuilderPrimeOwner openDir(RotateDirection openDir);
    }

    public interface BuilderPrimeOwner
    {
        BuilderDoorOwners primeOwner(DoorOwner primeOwner);
    }

    public interface BuilderDoorOwners extends Builder
    {
        Builder doorOwners(@Nullable Map<UUID, DoorOwner> doorOwners);
    }

    public interface Builder
    {
        DoorBase build();
    }
}
