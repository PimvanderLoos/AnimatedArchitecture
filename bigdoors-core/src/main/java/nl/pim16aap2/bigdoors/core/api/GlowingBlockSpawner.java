package nl.pim16aap2.bigdoors.core.api;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.structures.IStructureConst;
import nl.pim16aap2.bigdoors.core.util.IGlowingBlock;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.core.util.vector.IVector3D;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a glowing block used for highlights
 *
 * @author Pim
 */
public abstract class GlowingBlockSpawner
{
    protected abstract Optional<IGlowingBlock> spawnGlowingBlock(
        IPPlayer player, IPWorld world, Duration duration, double x, double y, double z, PColor pColor);

    /**
     * Spawns the glowing blocks required to highlight a structure.
     *
     * @param structure
     *     The structure to highlight.
     * @param player
     *     The {@link IPPlayer} for whom to highlight the structure.
     * @param duration
     *     The amount of time the glowing blocks should be visible for.
     * @return The list of {@link IGlowingBlock}s that were spawned.
     */
    public List<IGlowingBlock> spawnGlowingBlocks(IStructureConst structure, IPPlayer player, Duration duration)
    {
        final List<IGlowingBlock> ret = new ArrayList<>(4);
        final IPWorld world = structure.getWorld();

        spawnGlowingBlock(player, world, duration, structure.getPowerBlock().x() + 0.5,
                          structure.getPowerBlock().y(), structure.getPowerBlock().z() + 0.5, PColor.GOLD);
        spawnGlowingBlock(player, world, duration, structure.getRotationPoint().x() + 0.5,
                          structure.getRotationPoint().y(), structure.getRotationPoint().z() + 0.5,
                          PColor.DARK_PURPLE);
        spawnGlowingBlock(player, world, duration, structure.getMinimum().x() + 0.5, structure.getMinimum().y(),
                          structure.getMinimum().z() + 0.5, PColor.BLUE);
        spawnGlowingBlock(player, world, duration, structure.getMaximum().x() + 0.5, structure.getMaximum().y(),
                          structure.getMaximum().z() + 0.5, PColor.RED);
        return ret;
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

        private @Nullable IPPlayer player;
        private @Nullable IPWorld world;
        private double x;
        private double y;
        private double z;
        private Duration duration = Duration.ofSeconds(1);
        private PColor color = PColor.RED;

        private Builder(GlowingBlockSpawner glowingBlockSpawner)
        {
            this.glowingBlockSpawner = glowingBlockSpawner;
        }

        /**
         * @param player
         *     The player who will see the glowing block.
         */
        public Builder forPlayer(IPPlayer player)
        {
            this.player = player;
            return this;
        }

        /**
         * @param location
         *     The location where the glowing block will be spawned. This sets the coordinates and the world.
         */
        public Builder atLocation(IPLocation location)
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
        public Builder inWorld(IPWorld world)
        {
            this.world = world;
            return this;
        }

        /**
         * @param duration
         *     The amount of time the glowing block should exist for after it has been spawned.
         */
        public Builder forDuration(Duration duration)
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
            this.x = position.xD();
            this.y = position.yD();
            this.z = position.zD();
            return this;
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
         * @param color
         *     The color of the glowing block.
         */
        public Builder withColor(PColor color)
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
        public Optional<IGlowingBlock> build()
        {
            try
            {
                return glowingBlockSpawner.spawnGlowingBlock(
                    Util.requireNonNull(player, "Player"),
                    Util.requireNonNull(world, "World"),
                    Util.requireNonNull(duration, "Duration"),
                    x, y, z,
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
