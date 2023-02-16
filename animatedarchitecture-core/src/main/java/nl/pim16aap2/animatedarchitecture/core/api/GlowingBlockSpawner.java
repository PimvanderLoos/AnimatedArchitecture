package nl.pim16aap2.animatedarchitecture.core.api;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.IGlowingBlock;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;

/**
 * Represents a glowing block used for highlights
 *
 * @author Pim
 */
public abstract class GlowingBlockSpawner
{
    protected abstract Optional<IGlowingBlock> spawnGlowingBlock(
        IPlayer player, IWorld world, @Nullable Duration duration, RotatedPosition rotatedPosition, Color color);

    private Optional<IGlowingBlock> spawnGlowingBlock(
        IPlayer player, IWorld world, @Nullable Duration duration, Vector3Dd position, Color color)
    {
        return spawnGlowingBlock(player, world, duration, new RotatedPosition(position, new Vector3Dd(0, 0, 0)), color);
    }

    private Optional<IGlowingBlock> spawnGlowingBlock(
        IPlayer player, IWorld world, @Nullable Duration duration, double x, double y, double z, Color color)
    {
        return spawnGlowingBlock(player, world, duration, new Vector3Dd(x, y, z), color);
    }

    /**
     * Spawns the glowing blocks required to highlight a structure.
     *
     * @param structure
     *     The structure to highlight.
     * @param player
     *     The {@link IPlayer} for whom to highlight the structure.
     * @param duration
     *     The amount of time the glowing blocks should be visible for.
     */
    public void spawnGlowingBlocks(IStructureConst structure, IPlayer player, @Nullable Duration duration)
    {
        final IWorld world = structure.getWorld();

        spawnGlowingBlock(player, world, duration,
                          structure.getPowerBlock().x() + 0.5,
                          structure.getPowerBlock().y(),
                          structure.getPowerBlock().z() + 0.5, Color.GOLD);

        spawnGlowingBlock(player, world, duration,
                          structure.getRotationPoint().x() + 0.5,
                          structure.getRotationPoint().y(),
                          structure.getRotationPoint().z() + 0.5, Color.DARK_PURPLE);

        spawnGlowingBlock(player, world, duration,
                          structure.getMinimum().x() + 0.5,
                          structure.getMinimum().y(),
                          structure.getMinimum().z() + 0.5, Color.BLUE);

        spawnGlowingBlock(player, world, duration,
                          structure.getMaximum().x() + 0.5,
                          structure.getMaximum().y(),
                          structure.getMaximum().z() + 0.5, Color.RED);
    }

    /**
     * Spawns the glowing blocks required to highlight a structure.
     *
     * @param structure
     *     The structure to highlight.
     * @param player
     *     The {@link IPlayer} for whom to highlight the structure.
     * @param duration
     *     The amount of time the glowing blocks should be visible for.
     */
    public void spawnGlowingBlocks(AbstractStructure structure, IPlayer player, @Nullable Duration duration)
    {
        spawnGlowingBlocks((IStructureConst) structure, player, duration);

        final IWorld world = structure.getWorld();
        final Optional<Cuboid> cuboidOptional = structure.getPotentialNewCoordinates();
        if (cuboidOptional.isEmpty())
            return;
        final Cuboid cuboid = cuboidOptional.get();

        spawnGlowingBlock(player, world, duration,
                          cuboid.getMin().x() + 0.5,
                          cuboid.getMin().y(),
                          cuboid.getMin().z() + 0.5, Color.DARK_AQUA);

        spawnGlowingBlock(player, world, duration,
                          cuboid.getMax().x() + 0.5,
                          cuboid.getMax().y(),
                          cuboid.getMax().z() + 0.5, Color.DARK_RED);
    }

    /**
     * @return A new builder for a glowing block.
     */
    public Builder builder()
    {
        return new Builder(this);
    }

    /**
     * The default builder implementation for glowing blocks.
     */
    @SuppressWarnings("unused")
    @Flogger
    public static class Builder
    {
        private final GlowingBlockSpawner glowingBlockSpawner;

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

        private Builder(GlowingBlockSpawner glowingBlockSpawner)
        {
            this.glowingBlockSpawner = glowingBlockSpawner;
        }

        /**
         * @param player
         *     The player who will see the glowing block.
         */
        public Builder forPlayer(IPlayer player)
        {
            this.player = player;
            return this;
        }

        /**
         * @param location
         *     The location where the glowing block will be spawned. This sets the coordinates and the world.
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
         *     The world the glowing block will be spawned in.
         */
        public Builder inWorld(IWorld world)
        {
            this.world = world;
            return this;
        }

        /**
         * @param duration
         *     The amount of time the glowing block should exist for after it has been spawned.
         */
        public Builder forDuration(@Nullable Duration duration)
        {
            this.duration = duration;
            return this;
        }

        /**
         * @param position
         *     The position to spawn the glowing block at. This only sets the coordinates; not the world.
         */
        public Builder atPosition(IVector3D position)
        {
            return atPosition(position.xD(), position.yD(), position.zD());
        }

        /**
         * Sets the x, y, and z coordinates to spawn the glowing block at.
         */
        public Builder atPosition(double x, double y, double z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        /**
         * Sets the rotation components of the glowing block.
         */
        public Builder withRotation(double x, double y, double z)
        {
            this.rotX = x;
            this.rotY = y;
            this.rotZ = z;
            return this;
        }

        /**
         * Sets the rotation components of the glowing block.
         */
        public Builder withRotation(IVector3D rotation)
        {
            return withRotation(rotation.xD(), rotation.yD(), rotation.zD());
        }

        /**
         * Sets the rotation components of the glowing block.
         */
        public Builder atPosition(RotatedPosition rotatedPosition)
        {
            atPosition(rotatedPosition.position());
            withRotation(rotatedPosition.rotation());
            return this;
        }

        /**
         * @param color
         *     The color of the glowing block.
         */
        public Builder withColor(Color color)
        {
            this.color = color;
            return this;
        }

        /**
         * Creates and spawns the new glowing block using the provided configuration.
         *
         * @return The {@link IGlowingBlock} that was spawned. The optional will be empty if it could not be spawned for
         * some reason.
         */
        public Optional<IGlowingBlock> spawn()
        {
            try
            {
                return glowingBlockSpawner.spawnGlowingBlock(
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
                log.atSevere().withCause(e).log("Failed to spawn glowing block!");
                return Optional.empty();
            }
        }
    }
}
