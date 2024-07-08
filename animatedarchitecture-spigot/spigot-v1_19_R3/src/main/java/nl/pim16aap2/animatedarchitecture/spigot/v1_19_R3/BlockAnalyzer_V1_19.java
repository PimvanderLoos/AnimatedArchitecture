package nl.pim16aap2.animatedarchitecture.spigot.v1_19_R3;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.BlockAnalyzerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IBlockAnalyzerConfig;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.CommandBlock;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * Represents a class that can perform analysis on blocks on the Spigot 1.19 platform.
 * <p>
 * See {@link BlockAnalyzerSpigot} for more information.
 */
@Flogger
@Singleton
final class BlockAnalyzer_V1_19 extends BlockAnalyzerSpigot
{
    private static final List<Tag<Material>> BLOCKED_TAGS = List.of(
        Tag.ALL_SIGNS,
        Tag.BANNERS,
        Tag.BEDS,
        Tag.BEEHIVES,
        Tag.SHULKER_BOXES
    );

    @Inject
    BlockAnalyzer_V1_19(
        IBlockAnalyzerConfig config,
        RestartableHolder restartableHolder)
    {
        super(config, restartableHolder);
    }

    /**
     * Checks if a material is blacklisted.
     *
     * @param mat
     *     The material.
     * @return The listing status of the material.
     */
    @Override
    protected MaterialStatus getDefaultMaterialStatus(Material mat)
    {
        if (!mat.isBlock())
            return MaterialStatus.BLACKLISTED;

        for (final Tag<Material> tag : BLOCKED_TAGS)
            if (tag.isTagged(mat))
                return MaterialStatus.BLACKLISTED;

        if (mat.isAir())
            return MaterialStatus.BLACKLISTED;

        final @Nullable var blockData = safeCreateBlockData(mat);
        if (blockData instanceof Levelled || blockData instanceof CommandBlock)
            return MaterialStatus.BLACKLISTED;

        return switch (mat)
        {
            //noinspection UnstableApiUsage
            case BARREL,
                 BLAST_FURNACE,
                 BREWING_STAND,
                 CHEST,
                 CHISELED_BOOKSHELF,
                 DECORATED_POT,
                 COMMAND_BLOCK,
                 CREEPER_HEAD,
                 CREEPER_WALL_HEAD,
                 DISPENSER,
                 DRAGON_EGG,
                 DRAGON_HEAD,
                 DRAGON_WALL_HEAD,
                 DROPPER,
                 FROGSPAWN,
                 FURNACE,
                 HOPPER,
                 JUKEBOX,
                 LECTERN,
                 PIGLIN_HEAD,
                 PIGLIN_WALL_HEAD,
                 PISTON_HEAD,
                 PLAYER_HEAD,
                 PLAYER_WALL_HEAD,
                 REDSTONE,
                 SHULKER_BOX,
                 SKELETON_SKULL,
                 SKELETON_WALL_SKULL,
                 SMOKER,
                 SPAWNER,
                 STONECUTTER,
                 STRUCTURE_BLOCK,
                 STRUCTURE_VOID,
                 TRAPPED_CHEST,
                 WITHER_SKELETON_SKULL,
                 WITHER_SKELETON_WALL_SKULL,
                 ZOMBIE_HEAD,
                 ZOMBIE_WALL_HEAD -> MaterialStatus.BLACKLISTED;
            default -> MaterialStatus.WHITELISTED;
        };
    }
}
