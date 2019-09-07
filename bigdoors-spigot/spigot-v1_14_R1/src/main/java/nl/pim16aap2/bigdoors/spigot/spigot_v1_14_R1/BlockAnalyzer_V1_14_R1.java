package nl.pim16aap2.bigdoors.spigot.spigot_v1_14_R1;

import nl.pim16aap2.bigdoors.api.PBlockData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a class that can perform basic analysis on blocks, such as if they're empty, blacklisted, and/or
 * rotatable.
 *
 * @author Pim
 */
public final class BlockAnalyzer_V1_14_R1
{
    /**
     * Checks if placement of this block should be deferred to the second pass or not.
     * <p>
     * See {@link PBlockData#deferPlacement()}
     * <p>
     * This method assume
     *
     * @param block The block.
     * @return True if this block should be placed on the second pass, false otherwise.
     */
    public static boolean placeOnSecondPass(final @Nullable Block block)
    {
        if (block == null || isAirOrLiquid(block))
            return false;

        BlockData blockData = block.getBlockData();
        if (
            blockData instanceof org.bukkit.block.data.type.Bed ||
                blockData instanceof org.bukkit.block.data.Bisected ||
                blockData instanceof org.bukkit.block.data.type.Ladder ||
                blockData instanceof org.bukkit.block.data.type.Sapling ||
                blockData instanceof org.bukkit.block.data.type.Sign ||
                blockData instanceof org.bukkit.block.data.type.WallSign ||
                blockData instanceof org.bukkit.block.data.type.RedstoneWire ||
                blockData instanceof org.bukkit.block.data.type.RedstoneWallTorch ||
                blockData instanceof org.bukkit.block.data.type.Tripwire ||
                blockData instanceof org.bukkit.block.data.type.TripwireHook ||
                blockData instanceof org.bukkit.block.data.type.Repeater ||
                blockData instanceof org.bukkit.block.data.type.Switch ||
                blockData instanceof org.bukkit.block.data.type.Comparator
        )
            return true;

        Material mat = block.getType();
        switch (mat)
        {
            case WALL_TORCH:

            case PAINTING:

            case ATTACHED_MELON_STEM:
            case ATTACHED_PUMPKIN_STEM:
            case WHITE_TULIP:
            case DANDELION:
            case SUGAR_CANE:
            case NETHER_WART:
            case CHORUS_FLOWER:
            case CHORUS_FRUIT:
            case SEAGRASS:
            case POPPY:
            case OXEYE_DAISY:
            case LILY_OF_THE_VALLEY:
            case LILY_PAD:
            case VINE:
                return true;
            default:
                break;
        }

        String matName = mat.toString();
        // Potted stuff will always work.
        if (matName.startsWith("POTTED"))
            return false;
        if (matName.endsWith("TULIP") || matName.endsWith("BANNER") || matName.endsWith("CARPET") ||
            matName.endsWith("HEAD"))
            return true;

        return false;
    }


    /**
     * Check if a block if air or liquid (water, lava).
     *
     * @param block The block to be checked.
     * @return True if it is air or liquid.
     */
    public static boolean isAirOrLiquid(final @NotNull Block block)
    {
        // Empty means it's air.
        return block.isLiquid() || block.isEmpty();
    }

    /**
     * Check if a block is on the blacklist of types/materials that is not allowed for animations.
     *
     * @param block The block to be checked
     * @return True if the block can be used for animations.
     */
    public static boolean isAllowedBlock(final @Nullable Block block)
    {
        if (block == null || isAirOrLiquid(block))
            return false;

        BlockData blockData = block.getBlockData();
        BlockState blockState = block.getState();

        if (blockData instanceof org.bukkit.block.data.type.Stairs ||
            blockData instanceof org.bukkit.block.data.type.Gate)
            return true;
        /*





         */


        if (blockState instanceof org.bukkit.inventory.InventoryHolder ||
            // Door, Stairs, TrapDoor, sunflower, tall grass, tall seagrass, large fern,
            // peony, rose bush, lilac,
            blockData instanceof org.bukkit.block.data.Bisected ||
            blockData instanceof org.bukkit.block.data.Rail ||
            // Cauldron, Composter, Water, Lava
            blockData instanceof org.bukkit.block.data.Levelled ||

            blockData instanceof org.bukkit.block.data.type.Bed ||
            blockData instanceof org.bukkit.block.data.type.BrewingStand ||
            blockData instanceof org.bukkit.block.data.type.Cake ||
            blockData instanceof org.bukkit.block.data.type.CommandBlock ||
            blockData instanceof org.bukkit.block.data.type.EnderChest ||
            blockData instanceof org.bukkit.block.data.type.Ladder ||
            blockData instanceof org.bukkit.block.data.type.Sapling ||
            blockData instanceof org.bukkit.block.data.type.Sign ||
            blockData instanceof org.bukkit.block.data.type.TechnicalPiston ||
            blockData instanceof org.bukkit.block.data.type.WallSign ||
            blockData instanceof org.bukkit.block.data.type.RedstoneWire ||
            blockData instanceof org.bukkit.block.data.type.RedstoneWallTorch ||
            blockData instanceof org.bukkit.block.data.type.Tripwire ||
            blockData instanceof org.bukkit.block.data.type.TripwireHook ||
            blockData instanceof org.bukkit.block.data.type.Repeater ||
            blockData instanceof org.bukkit.block.data.type.Switch ||
            blockData instanceof org.bukkit.block.data.type.Comparator)
            return false;

        Material mat = block.getType();
        switch (mat)
        {
            case WALL_TORCH:

            case PAINTING:

            case ATTACHED_MELON_STEM:
            case ATTACHED_PUMPKIN_STEM:
            case WHITE_TULIP:
            case DANDELION:
            case SUGAR_CANE:
            case NETHER_WART:
            case CHORUS_FLOWER:
            case CHORUS_FRUIT:
            case SEAGRASS:
            case POPPY:
            case OXEYE_DAISY:
            case LILY_OF_THE_VALLEY:
            case LILY_PAD:
            case VINE:
                return false;
            default:
                break;
        }

        String matName = mat.toString();
        // Potted stuff will always work.
        if (matName.startsWith("POTTED"))
            return true;
        if (matName.endsWith("TULIP") || matName.endsWith("BANNER") || matName.endsWith("CARPET") ||
            matName.endsWith("HEAD"))
            return false;
        return true;
    }

}
