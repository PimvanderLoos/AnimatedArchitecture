package nl.pim16aap2.animatedarchitecture.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class LimitsSection implements IConfigSection
{
    public static final String SECTION_TITLE = "limits";

    public static final String PATH_MAX_STRUCTURE_COUNT = "max_structure_count";
    public static final String PATH_MAX_BLOCKS_TO_MOVE = "max_blocks_to_move";
    public static final String PATH_MAX_STRUCTURE_SIZE = "max_structure_size";
    public static final String PATH_MAX_POWERBLOCK_DISTANCE = "max_powerblock_distance";
    public static final String PATH_MAX_BLOCK_SPEED = "max_block_speed";

    public static final int DEFAULT_MAX_STRUCTURE_COUNT = -1;
    public static final int DEFAULT_MAX_BLOCKS_TO_MOVE = 100;
    public static final int DEFAULT_MAX_STRUCTURE_SIZE = 1000;
    public static final int DEFAULT_MAX_POWERBLOCK_DISTANCE = -1;
    public static final double DEFAULT_MAX_BLOCK_SPEED = 5.0D;

    @Override
    public String getSectionTitle()
    {
        return SECTION_TITLE;
    }

    @Override
    public CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException
    {
        return CommentedConfigurationNode.root()
            .comment("""
                Global limits for structures and animated blocks.
                
                These limits apply to all players, including admins and OPs.
                You can use permissions to override most limits for individual players.
                
                A limit can be set to -1 to indicate no limit.
                """)
            .act(node ->
            {
                addInitialMaxStructureCount(node.node(PATH_MAX_STRUCTURE_COUNT));
                addInitialMaxStructureSize(node.node(PATH_MAX_STRUCTURE_SIZE));
                addInitialMaxBlocksToMove(node.node(PATH_MAX_BLOCKS_TO_MOVE));
                addInitialMaxPowerblockDistance(node.node(PATH_MAX_POWERBLOCK_DISTANCE));
                addInitialMaxBlockSpeed(node.node(PATH_MAX_BLOCK_SPEED));
            });
    }

    private void addInitialMaxStructureCount(CommentedConfigurationNode node)
        throws SerializationException
    {
        node.set(DEFAULT_MAX_STRUCTURE_COUNT)
            .comment("""
                Global maximum number of structures a player can own.
                
                You can use permissions if you need more finely grained control using this node:
                'animatedarchitecture.user.limit.structure_count.x', where 'x' can be any positive value.
                """);
    }

    private void addInitialMaxStructureSize(CommentedConfigurationNode node)
        throws SerializationException
    {
        node.set(DEFAULT_MAX_STRUCTURE_SIZE)
            .comment("""
                Global maximum size of a structure in blocks.
                If a structure exceeds this size, it will always skip the animation when it is toggled.
                
                You can use permissions if you need more finely grained control using this node:
                'animatedarchitecture.user.limit.structure_size.x', where 'x' can be any positive value.
                """);
    }

    private void addInitialMaxBlocksToMove(CommentedConfigurationNode node)
        throws SerializationException
    {
        node.set(DEFAULT_MAX_BLOCKS_TO_MOVE)
            .comment("""
                Global maximum number of structures a player can own.
                
                You can use permissions if you need more finely grained control using this node:
                'animatedarchitecture.user.limit.blocks_to_move.x', where 'x' can be any positive value.
                """);
    }

    private void addInitialMaxPowerblockDistance(CommentedConfigurationNode node)
        throws SerializationException
    {
        node.set(DEFAULT_MAX_POWERBLOCK_DISTANCE)
            .comment("""
                Global maximum distance between power blocks in blocks.
                If a structure exceeds this distance, it cannot be toggled.
                
                The distance is measured from the edge of the structure to the power block.
                As such, the distance may exceed this limit if the structure moves away
                from the power block after creation.
                
                You can use permissions if you need more finely grained control using this node:
                'animatedarchitecture.user.limit.powerblock_distance.x', where 'x' can be any positive value.
                """);
    }

    private void addInitialMaxBlockSpeed(CommentedConfigurationNode node)
        throws SerializationException
    {
        node.set(DEFAULT_MAX_BLOCK_SPEED)
            .comment("""
                Global maximum speed of blocks in blocks per second.
                
                Determines the global speed limit of animated blocks measured in blocks/second.
                Animated objects will slow down when necessary to avoid any of their animated blocks exceeding this limit
                Higher values may result in choppy/glitchy animations.
                
                This limit cannot be overridden by permissions.
                """);
    }
}
