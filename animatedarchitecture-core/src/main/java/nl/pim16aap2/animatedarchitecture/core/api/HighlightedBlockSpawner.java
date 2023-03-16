package nl.pim16aap2.animatedarchitecture.core.api;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;

/**
 * Represents a highlighted animated block. These can be useful for highlighting locations and structures.
 *
 * @author Pim
 */
public abstract class HighlightedBlockSpawner
{
    protected abstract Optional<IHighlightedBlock> spawnHighlightedBlock(
        IPlayer player, IWorld world, @Nullable Duration duration, RotatedPosition rotatedPosition, Color color);

    private Optional<IHighlightedBlock> spawnHighlightedBlock(
        IPlayer player, IWorld world, @Nullable Duration duration, Vector3Dd position, Color color)
    {
        return spawnHighlightedBlock(
            player, world, duration, new RotatedPosition(position, new Vector3Dd(0, 0, 0)), color);
    }

    private Optional<IHighlightedBlock> spawnHighlightedBlock(
        IPlayer player, IWorld world, @Nullable Duration duration, double x, double y, double z, Color color)
    {
        return spawnHighlightedBlock(player, world, duration, new Vector3Dd(x, y, z), color);
    }

    /**
     * Spawns the highlighted blocks required to highlight a structure.
     *
     * @param structure
     *     The structure to highlight.
     * @param player
     *     The {@link IPlayer} for whom to highlight the structure.
     * @param duration
     *     The amount of time the highlighted blocks should be visible for.
     */
    public void spawnHighlightedBlocks(IStructureConst structure, IPlayer player, @Nullable Duration duration)
    {
        getExecutor().runOnMainThread(() -> spawnHighlightedBlocks0(structure, player, duration));
    }

    private void spawnHighlightedBlocks0(IStructureConst structure, IPlayer player, @Nullable Duration duration)
    {
        final IWorld world = structure.getWorld();

        spawnHighlightedBlock(
            player, world, duration,
            structure.getPowerBlock().x() + 0.5,
            structure.getPowerBlock().y(),
            structure.getPowerBlock().z() + 0.5, Color.GOLD);

        spawnHighlightedBlock(
            player, world, duration,
            structure.getRotationPoint().x() + 0.5,
            structure.getRotationPoint().y(),
            structure.getRotationPoint().z() + 0.5, Color.DARK_PURPLE);

        spawnHighlightedBlock(
            player, world, duration,
            structure.getMinimum().x() + 0.5,
            structure.getMinimum().y(),
            structure.getMinimum().z() + 0.5, Color.BLUE);

        spawnHighlightedBlock(
            player, world, duration,
            structure.getMaximum().x() + 0.5,
            structure.getMaximum().y(),
            structure.getMaximum().z() + 0.5, Color.RED);
    }

    /**
     * Spawns the highlighted blocks required to highlight a structure.
     *
     * @param structure
     *     The structure to highlight.
     * @param player
     *     The {@link IPlayer} for whom to highlight the structure.
     * @param duration
     *     The amount of time the highlighted blocks should be visible for.
     */
    public void spawnHighlightedBlocks(AbstractStructure structure, IPlayer player, @Nullable Duration duration)
    {
        getExecutor().runOnMainThread(() -> spawnHighlightedBlocks0(structure, player, duration));
    }

    protected abstract IExecutor getExecutor();

    private void spawnHighlightedBlocks0(AbstractStructure structure, IPlayer player, @Nullable Duration duration)
    {
        spawnHighlightedBlocks((IStructureConst) structure, player, duration);

        final IWorld world = structure.getWorld();
        final Optional<Cuboid> cuboidOptional = structure.getPotentialNewCoordinates();
        if (cuboidOptional.isEmpty())
            return;
        final Cuboid cuboid = cuboidOptional.get();

        spawnHighlightedBlock(
            player, world, duration,
            cuboid.getMin().x() + 0.5,
            cuboid.getMin().y(),
            cuboid.getMin().z() + 0.5, Color.DARK_AQUA);

        spawnHighlightedBlock(
            player, world, duration,
            cuboid.getMax().x() + 0.5,
            cuboid.getMax().y(),
            cuboid.getMax().z() + 0.5, Color.DARK_RED);
    }

    /**
     * @return A new builder for a highlighted block.
     */
    public Builder builder()
    {
        return new Builder(this);
    }

    /**
     * The default builder implementation for highlighted blocks.
     */
    @SuppressWarnings("unused")
    @Flogger
    public static class Builder
    {
        private final HighlightedBlockSpawner highlightedBlockSpawner;

        private @Nullable IPlayer player;
        private @Nullable IWorld world;
        private double x;
        private double y;
        private double z;
        private double rotX;
        private double rotY;
        private double rotZ;
        private @Nullable Duration duration;
        private Color color = Color.RED;

        private Builder(HighlightedBlockSpawner highlightedBlockSpawner)
        {
            this.highlightedBlockSpawner = highlightedBlockSpawner;
        }

        /**
         * @param player
         *     The player who will see the highlighted block.
         */
        public Builder forPlayer(IPlayer player)
        {
            this.player = player;
            return this;
        }

        /**
         * @param location
         *     The location where the highlighted block will be spawned. This sets the coordinates and the world.
         */
        public Builder atLocation(ILocation location)
        {
            this.world = location.getWorld();
            this.x = location.getX();
            this.y = location.getY();
            this.z = location.getZ();
            return this;
        }

        /**
         * @param world
         *     The world the highlighted block will be spawned in.
         */
        public Builder inWorld(IWorld world)
        {
            this.world = world;
            return this;
        }

        /**
         * @param duration
         *     The amount of time the highlighted block should exist for after it has been spawned.
         */
        public Builder forDuration(@Nullable Duration duration)
        {
            this.duration = duration;
            return this;
        }

        /**
         * @param position
         *     The position to spawn the highlighted block at. This only sets the coordinates; not the world.
         */
        public Builder atPosition(IVector3D position)
        {
            return atPosition(position.xD(), position.yD(), position.zD());
        }

        /**
         * Sets the x, y, and z coordinates to spawn the highlighted block at.
         */
        public Builder atPosition(double x, double y, double z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        /**
         * Sets the rotation components of the highlighted block.
         */
        public Builder withRotation(double x, double y, double z)
        {
            this.rotX = x;
            this.rotY = y;
            this.rotZ = z;
            return this;
        }

        /**
         * Sets the rotation components of the highlighted block.
         */
        public Builder withRotation(IVector3D rotation)
        {
            return withRotation(rotation.xD(), rotation.yD(), rotation.zD());
        }

        /**
         * Sets the rotation components of the highlighted block.
         */
        public Builder atPosition(RotatedPosition rotatedPosition)
        {
            atPosition(rotatedPosition.position());
            withRotation(rotatedPosition.rotation());
            return this;
        }

        /**
         * @param color
         *     The color of the highlighted block.
         */
        public Builder withColor(Color color)
        {
            this.color = color;
            return this;
        }

        /**
         * Creates and spawns the new highlighted block using the provided configuration.
         *
         * @return The {@link IHighlightedBlock} that was spawned. The optional will be empty if it could not be spawned
         * for some reason.
         */
        public Optional<IHighlightedBlock> spawn()
        {
            try
            {
                return highlightedBlockSpawner.spawnHighlightedBlock(
                    Util.requireNonNull(player, "Player"),
                    Util.requireNonNull(world, "World"),
                    duration,
                    new RotatedPosition(
                        new Vector3Dd(x, y, z), new Vector3Dd(rotX, rotY, rotZ)),
                    Util.requireNonNull(color, "Color")
                );
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log("Failed to spawn highlighted block!");
                return Optional.empty();
            }
        }
    }
}
