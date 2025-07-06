package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.config.IConfigSectionResult;
import nl.pim16aap2.animatedarchitecture.core.config.LimitsSection;
import nl.pim16aap2.animatedarchitecture.core.util.Limit;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.function.Consumer;

/**
 * Represents a section in the configuration file that governs {@link Limit}s on the Spigot platform.
 * <p>
 * This section is used to configure various limits, such as the maximum number of animations.
 */
@AllArgsConstructor
public class LimitsSectionSpigot extends LimitsSection<LimitsSectionSpigot.Result>
{
    @Getter
    private final @Nullable Consumer<Result> resultConsumer;

    @Override
    protected Result getResult(ConfigurationNode sectionNode)
    {
        return new Result(
            getMaxStructureCount(sectionNode),
            getMaxBlocksToMove(sectionNode),
            getMaxStructureSize(sectionNode),
            getMaxPowerblockDistance(sectionNode),
            getMaxBlockSpeed(sectionNode)
        );
    }

    private int getMaxStructureCount(ConfigurationNode sectionNode)
    {
        return sectionNode.node(PATH_MAX_STRUCTURE_COUNT).getInt(DEFAULT_MAX_STRUCTURE_COUNT);
    }

    private int getMaxBlocksToMove(ConfigurationNode sectionNode)
    {
        return sectionNode.node(PATH_MAX_BLOCKS_TO_MOVE).getInt(DEFAULT_MAX_BLOCKS_TO_MOVE);
    }

    private int getMaxStructureSize(ConfigurationNode sectionNode)
    {
        return sectionNode.node(PATH_MAX_STRUCTURE_SIZE).getInt(DEFAULT_MAX_STRUCTURE_SIZE);
    }

    private int getMaxPowerblockDistance(ConfigurationNode sectionNode)
    {
        return sectionNode.node(PATH_MAX_POWERBLOCK_DISTANCE).getInt(DEFAULT_MAX_POWERBLOCK_DISTANCE);
    }

    private double getMaxBlockSpeed(ConfigurationNode sectionNode)
    {
        return sectionNode.node(PATH_MAX_BLOCK_SPEED).getDouble(DEFAULT_MAX_BLOCK_SPEED);
    }

    /**
     * Represents the result of the LimitsSectionSpigot configuration.
     *
     * @param maxStructureCount
     *     The maximum number of structures a player can have.
     * @param maxBlocksToMove
     *     The maximum number of blocks that can be moved in a single animation.
     * @param maxStructureSize
     *     The maximum size of a structure in terms of blocks.
     * @param maxPowerblockDistance
     *     The maximum distance between power blocks in a structure.
     * @param maxBlockSpeed
     *     The maximum speed at which blocks can move in an animation.
     */
    public record Result(
        int maxStructureCount,
        int maxBlocksToMove,
        int maxStructureSize,
        int maxPowerblockDistance,
        double maxBlockSpeed
    ) implements IConfigSectionResult {}
}
