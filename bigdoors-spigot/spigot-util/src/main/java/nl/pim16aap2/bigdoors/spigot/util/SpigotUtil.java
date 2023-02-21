package nl.pim16aap2.bigdoors.spigot.util;

import lombok.Getter;
import lombok.Setter;
import nl.pim16aap2.bigdoors.core.api.Color;
import nl.pim16aap2.bigdoors.core.util.BlockFace;
import nl.pim16aap2.bigdoors.core.util.Util;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/**
 * Represents various small and Spigot-specific utility functions.
 *
 * @author Pim
 */
public final class SpigotUtil
{
    private static final Map<BlockFace, org.bukkit.block.BlockFace> TO_BUKKIT_BLOCK_FACE =
        new EnumMap<>(BlockFace.class);

    private static final Map<org.bukkit.block.BlockFace, BlockFace> TO_BLOCK_FACE =
        new EnumMap<>(org.bukkit.block.BlockFace.class);

    @Getter
    @Setter
    private static boolean printDebugMessages = false;

    static
    {
        for (final BlockFace blockFace : BlockFace.values())
        {
            final org.bukkit.block.BlockFace mappedBlockFace;
            if (blockFace.equals(BlockFace.NONE))
                mappedBlockFace = org.bukkit.block.BlockFace.SELF;
            else
                mappedBlockFace = org.bukkit.block.BlockFace.valueOf(blockFace.toString());
            TO_BUKKIT_BLOCK_FACE.put(blockFace, mappedBlockFace);
            TO_BLOCK_FACE.put(mappedBlockFace, blockFace);
        }
    }

    private static final Map<Color, ChatColor> TO_BUKKIT_COLOR = new EnumMap<>(Color.class);

    static
    {
        for (final Color pColor : Color.values())
            TO_BUKKIT_COLOR.put(pColor, ChatColor.valueOf(pColor.name()));
    }

    private SpigotUtil()
    {
        // Utility class
    }

    /**
     * Gets the number of ticks required to cover a duration.
     * <p>
     * For example, given a tick duration of 50ms, any duration &gt; 0 ms and &lt;= 50ms will return 1 tick.
     * <p>
     * If the duration is negative or zero, 0 is returned.
     *
     * @param duration
     *     The duration.
     * @return The number of ticks required to cover the duration.
     */
    public static long durationToTicks(Duration duration)
    {
        if (duration.isNegative() || duration.isZero())
            return 0;

        final long millis = duration.toMillis();
        long ticks = millis / 50;
        if (millis % 50 != 0)
            ++ticks;
        return ticks;
    }

    /**
     * Gets the bukkit version of a {@link Color}.
     *
     * @param pColor
     *     The {@link Color}.
     * @return The bukkit version of the {@link Color}.
     */
    public static ChatColor toBukkitColor(Color pColor)
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
     * Get the {@link BlockFace} parallel to the given {@link org.bukkit.block.BlockFace}.
     *
     * @param blockFace
     *     {@link BlockFace} that will be converted.
     * @return The parallel {@link org.bukkit.block.BlockFace}.
     */
    public static org.bukkit.block.BlockFace getBukkitFace(BlockFace blockFace)
    {
        final org.bukkit.block.BlockFace ret = TO_BUKKIT_BLOCK_FACE.get(blockFace);
        if (ret != null)
            return ret;

        throw new IllegalStateException("Failing to find spigot mapping for BlockFace: " + blockFace);
    }

    /**
     * Get the {@link org.bukkit.block.BlockFace} parallel to the given {@link BlockFace}.
     *
     * @param blockFace
     *     {@link org.bukkit.block.BlockFace} that will be converted.
     * @return The parallel {@link BlockFace}.
     */
    public static BlockFace getBlockFace(org.bukkit.block.BlockFace blockFace)
    {
        final BlockFace ret = TO_BLOCK_FACE.get(blockFace);
        if (ret != null)
            return ret;

        throw new IllegalStateException("Failing to find mapping for Bukkit BlockFace: " + blockFace);
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

    public static int getPermissionSuffixValue(String permissionNode, String permissionBase)
    {
        if (permissionNode.startsWith(permissionBase))
        {
            final String[] parts = permissionNode.split(permissionBase);
            if (parts.length != 2)
                return -1;

            return Util.parseInt(parts[1]).orElse(-1);
        }
        return -1;
    }

    /**
     * Retrieves the highest integer value of a specific permission node, if it exists.
     * <p>
     * E.g., if a player has two permission nodes: "{@code example.permission.10}" and "{@code example.permission.20}",
     * then this method would return "20" for a permission base of "{@code example.permission.}".
     *
     * @param player
     *     The player whose permissions to analyze.
     * @param permissionBase
     *     The base permission node to find the suffix for. E.g. "{@code example.permission.}".
     * @return The highest integer suffix or -1 if none could be found.
     */
    public static int getHighestPermissionSuffix(Player player, String permissionBase)
    {
        return player.getEffectivePermissions().stream()
                     .map(PermissionAttachmentInfo::getPermission)
                     .mapToInt(node -> getPermissionSuffixValue(node, permissionBase))
                     .max().orElse(-1);
    }
}
