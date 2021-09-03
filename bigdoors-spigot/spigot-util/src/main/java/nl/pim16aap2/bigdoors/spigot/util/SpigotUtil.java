package nl.pim16aap2.bigdoors.spigot.util;

import lombok.Getter;
import lombok.Setter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * Represents various small and Spigot-specific utility functions.
 *
 * @author Pim
 */
public final class SpigotUtil
{
    private static final Map<PBlockFace, BlockFace> TO_BLOCK_FACE = new EnumMap<>(PBlockFace.class);
    private static final Map<BlockFace, PBlockFace> TO_PBLOCK_FACE = new EnumMap<>(BlockFace.class);
    @Getter
    @Setter
    private static boolean printDebugMessages = false;

    static
    {
        for (final PBlockFace pbf : PBlockFace.values())
        {
            final BlockFace mappedBlockFace;
            if (pbf.equals(PBlockFace.NONE))
                mappedBlockFace = BlockFace.SELF;
            else
                mappedBlockFace = BlockFace.valueOf(pbf.toString());
            TO_BLOCK_FACE.put(pbf, mappedBlockFace);
            TO_PBLOCK_FACE.put(mappedBlockFace, pbf);
        }
    }

    private static final Map<PColor, ChatColor> TO_BUKKIT_COLOR = new EnumMap<>(PColor.class);

    static
    {
        for (final PColor pColor : PColor.values())
            TO_BUKKIT_COLOR.put(pColor, ChatColor.valueOf(pColor.name()));
    }

    private SpigotUtil()
    {
        // Utility class
    }

    /**
     * Gets the bukkit version of a {@link PColor}.
     *
     * @param pColor
     *     The {@link PColor}.
     * @return The bukkit version of the {@link PColor}.
     */
    public static ChatColor toBukkitColor(PColor pColor)
    {
        return TO_BUKKIT_COLOR.getOrDefault(pColor, ChatColor.WHITE);
    }

    /**
     * Send a colored message to a specific player.
     *
     * @param player
     *     The player that will receive the message.
     * @param color
     *     Color of the message
     * @param msg
     *     The message to be sent.
     */
    public static void messagePlayer(Player player, ChatColor color, String msg)
    {
        player.sendMessage(color + msg);
    }

    /**
     * Get the {@link PBlockFace} parallel to the given {@link org.bukkit.block.BlockFace}.
     *
     * @param mbf
     *     {@link PBlockFace} that will be converted.
     * @return The parallel {@link org.bukkit.block.BlockFace}.
     */
    public static BlockFace getBukkitFace(PBlockFace mbf)
    {
        final BlockFace ret = TO_BLOCK_FACE.get(mbf);
        if (ret != null)
            return ret;

        final IllegalStateException e =
            new IllegalStateException("Failing to find spigot mapping for PBlockFace: " + mbf);
        BigDoors.get().getPLogger().logThrowable(e);
        return BlockFace.DOWN;
    }

    /**
     * Get the {@link org.bukkit.block.BlockFace} parallel to the given {@link PBlockFace}.
     *
     * @param bf
     *     {@link org.bukkit.block.BlockFace} that will be converted.
     * @return The parallel {@link PBlockFace}.
     */
    public static PBlockFace getPBlockFace(BlockFace bf)
    {
        final PBlockFace ret = TO_PBLOCK_FACE.get(bf);
        if (ret != null)
            return ret;

        final IllegalStateException e =
            new IllegalStateException("Failing to find mapping for lockFace: " + bf);
        BigDoors.get().getPLogger().logThrowable(e);
        return PBlockFace.NONE;
    }

    /**
     * Play a sound for all players in a range of 15 blocks around the provided location.
     *
     * @param loc
     *     The location of the sound.
     * @param sound
     *     The name of the sound.
     * @param volume
     *     The volume
     * @param pitch
     *     The pitch
     */
    public static void playSound(Location loc, String sound, float volume, float pitch)
    {
        if (loc.getWorld() == null)
            return;
        for (final Entity ent : loc.getWorld().getNearbyEntities(loc, 15, 15, 15))
            if (ent instanceof Player player)
                player.playSound(loc, sound, volume, pitch);
    }

    /**
     * Send a white message to a player.
     *
     * @param player
     *     Player to receive the message.
     * @param msg
     *     The message.
     */
    public static void messagePlayer(Player player, String msg)
    {
        messagePlayer(player, ChatColor.WHITE, msg);
    }

    /**
     * Certain material types need to be refreshed when being placed down.
     *
     * @param mat
     *     Material to be checked.
     * @return True if it needs to be refreshed.
     *
     * @deprecated I'm pretty sure this is no longer needed.
     */
    @Deprecated
    public static boolean needsRefresh(Material mat)
    {
        switch (mat)
        {
            case ACACIA_FENCE:
            case ACACIA_FENCE_GATE:
            case BIRCH_FENCE:
            case BIRCH_FENCE_GATE:
            case DARK_OAK_FENCE:
            case DARK_OAK_FENCE_GATE:
            case JUNGLE_FENCE:
            case JUNGLE_FENCE_GATE:
            case OAK_FENCE:
            case OAK_FENCE_GATE:
            case SPRUCE_FENCE:
            case SPRUCE_FENCE_GATE:
            case NETHER_BRICK_FENCE:

            case COBBLESTONE_WALL:
            case IRON_BARS:

            case WHITE_STAINED_GLASS_PANE:
            case YELLOW_STAINED_GLASS_PANE:
            case PURPLE_STAINED_GLASS_PANE:
            case LIGHT_BLUE_STAINED_GLASS_PANE:
            case MAGENTA_STAINED_GLASS_PANE:
            case GRAY_STAINED_GLASS_PANE:
            case GREEN_STAINED_GLASS_PANE:
            case BLACK_STAINED_GLASS_PANE:
            case LIME_STAINED_GLASS_PANE:
            case BLUE_STAINED_GLASS_PANE:
            case BROWN_STAINED_GLASS_PANE:
            case CYAN_STAINED_GLASS_PANE:
            case RED_STAINED_GLASS_PANE:
                return true;
            default:
                return false;
        }
    }
}
