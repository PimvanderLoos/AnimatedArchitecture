package nl.pim16aap2.animatedarchitecture.spigot.v1_21;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.IRestartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.BlockAnalyzerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IBlockAnalyzerConfig;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.CommandBlock;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * Represents a class that can perform analysis on blocks on the Spigot 1.21 platform.
 * <p>
 * See {@link BlockAnalyzerSpigot} for more information.
 */
@Flogger
@Singleton
final class BlockAnalyzer_V1_21 extends BlockAnalyzerSpigot implements IRestartable
{
    private static final List<Tag<Material>> BLOCKED_TAGS = List.of(
        Tag.AIR,
        Tag.BEDS,
        Tag.BEEHIVES,
        Tag.SHULKER_BOXES
    );

    @Inject
    BlockAnalyzer_V1_21(
        IBlockAnalyzerConfig config,
        RestartableHolder restartableHolder)
    {
        super(config, restartableHolder);
    }

    /**
     * Checks if a material is white-, grey-, or blacklisted.
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

        if (mat.isBlock())
        {
            try
            {
                final var blockData = mat.createBlockData();
                if (blockData instanceof Levelled || blockData instanceof CommandBlock)
                    return MaterialStatus.BLACKLISTED;
            }
            catch (Exception e)
            {
                log.atInfo().log(
                    "Encountered error parsing material '%s': %s - %s",
                    mat.name(),
                    e.getClass().getSimpleName(),
                    e.getMessage()
                );
            }
        }

        return switch (mat)
        {
            case COMMAND_BLOCK,
                 FROGSPAWN,
                 PISTON_HEAD,
                 REDSTONE,
                 STRUCTURE_BLOCK,
                 STRUCTURE_VOID,
                 TRIAL_SPAWNER,
                 VAULT -> MaterialStatus.BLACKLISTED;
            default -> MaterialStatus.WHITELISTED;
        };
    }
}
