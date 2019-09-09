package nl.pim16aap2.bigdoors.spigotutil;

import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents various small and Spigot-specific utility functions.
 *
 * @author Pim
 */
public final class SpigotUtil
{
    private static final Map<PBlockFace, BlockFace> toBlockFace = new EnumMap<>(PBlockFace.class);
    private static final Map<BlockFace, PBlockFace> toPBlockFace = new EnumMap<>(BlockFace.class);
    public static boolean printDebugMessages = false;

    static
    {
        for (PBlockFace pbf : PBlockFace.values())
        {
            BlockFace mappedBlockFace;
            if (pbf.equals(PBlockFace.NONE))
                mappedBlockFace = BlockFace.SELF;
            else
                mappedBlockFace = BlockFace.valueOf(pbf.toString());
            toBlockFace.put(pbf, mappedBlockFace);
            toPBlockFace.put(mappedBlockFace, pbf);
        }
    }

    private static final Map<PColor, ChatColor> toBukkitColor = new EnumMap<>(PColor.class);

    static
    {
        for (PColor pColor : PColor.values())
            toBukkitColor.put(pColor, ChatColor.valueOf(pColor.name()));
    }

    /**
     * Gets the bukkit version of a {@link PColor}.
     *
     * @param pColor The {@link PColor}.
     * @return The bukkit version of the {@link PColor}.
     */
    @NotNull
    public static ChatColor toBukkitColor(final @NotNull PColor pColor)
    {
        return toBukkitColor.get(pColor);
    }

    /**
     * Gets an offline player from a playerUUID.
     *
     * @param playerUUID The UUID of the player.
     */
    @NotNull
    public static OfflinePlayer getOfflinePlayer(final @NotNull UUID playerUUID)
    {
        @Nullable final Player onlinePlayer = Bukkit.getPlayer(playerUUID);
        return onlinePlayer != null ? onlinePlayer : Bukkit.getOfflinePlayer(playerUUID);
    }

    /**
     * Gets the number are available (i.e. either air or liquid blocks) in a given direction for a certain area. Note
     * that the result may be negative depending on the direction.
     * <p>
     * For example, when checking how many blocks are available in downwards direction, it will return -5 if 5 blocks
     * under the area are available.
     *
     * @param min       The minimum coordinates of the area.
     * @param max       The maximum coordinates of the area.
     * @param maxDist   The maximum number of blocks to check.
     * @param direction The direction to check.
     * @param world     The world in which to check.
     * @return The number are available in a given direction. Can be negative depending on the direction.
     */
    public static int getBlocksInDir(final @NotNull Location min, final @NotNull Location max, int maxDist,
                                     final @NotNull PBlockFace direction, final @NotNull World world)
    {
        int startX, startY, startZ, endX, endY, endZ, countX = 0, countY = 0, countZ = 0;
        Vector3Di vec = PBlockFace.getDirection(direction);
        maxDist = Math.abs(maxDist);

        startX = vec.getX() == 0 ? min.getBlockX() : vec.getX() == 1 ? max.getBlockX() + 1 : min.getBlockX() - 1;
        startY = vec.getY() == 0 ? min.getBlockY() : vec.getY() == 1 ? max.getBlockY() + 1 : min.getBlockY() - 1;
        startZ = vec.getZ() == 0 ? min.getBlockZ() : vec.getZ() == 1 ? max.getBlockZ() + 1 : min.getBlockZ() - 1;

        endX = vec.getX() == 0 ? max.getBlockX() : startX + vec.getX() * maxDist;
        endY = vec.getY() == 0 ? max.getBlockY() : startY + vec.getY() * maxDist;
        endZ = vec.getZ() == 0 ? max.getBlockZ() : startZ + vec.getZ() * maxDist;

        int stepX = vec.getX() == 0 ? 1 : vec.getX();
        int stepY = vec.getY() == 0 ? 1 : vec.getY();
        int stepZ = vec.getZ() == 0 ? 1 : vec.getZ();

        int ret = 0;
        if (vec.getX() != 0)
            for (int xAxis = startX; xAxis != endX + 1; ++xAxis)
            {
                for (int zAxis = startZ; zAxis != endZ; zAxis += stepZ)
                    for (int yAxis = startY; yAxis != endY + 1; ++yAxis)
                        if (!SpigotUtil.isAirOrLiquid(world.getBlockAt(xAxis, yAxis, zAxis)))
                            return ret;
                ret += stepX;
            }
        else if (vec.getY() != 0)
            for (int yAxis = startY; yAxis != endY + 1; ++yAxis)
            {
                for (int zAxis = startZ; zAxis != endZ; zAxis += stepZ)
                    for (int xAxis = startX; xAxis != endX + 1; ++xAxis)
                        if (!SpigotUtil.isAirOrLiquid(world.getBlockAt(xAxis, yAxis, zAxis)))
                            return ret;
                ret += stepY;
            }
        else if (vec.getZ() != 0)
        {
            for (int zAxis = startZ; zAxis != endZ; zAxis += stepZ)
            {
                for (int xAxis = startX; xAxis != endX + 1; ++xAxis)
                    for (int yAxis = startY; yAxis != endY + 1; ++yAxis)
                        if (!SpigotUtil.isAirOrLiquid(world.getBlockAt(xAxis, yAxis, zAxis)))
                            return ret;
                ret += stepZ;
            }
        }
        else
            ret = 0;
        return ret;
    }

    /**
     * Send a colored message to a specific player.
     *
     * @param player The player that will receive the message.
     * @param color  Color of the message
     * @param msg    The message to be sent.
     */
    public static void messagePlayer(final @NotNull Player player, final @NotNull ChatColor color,
                                     final @NotNull String msg)
    {
        player.sendMessage(color + msg);
    }

    /**
     * Convert a command and its explanation to the help format.
     *
     * @param command     Name of the command.
     * @param explanation Explanation of how to use the command.
     * @return String in the helperformat.
     */
    @NotNull
    public static String helpFormat(final @NotNull String command, final @NotNull String explanation)
    {
        return String.format(ChatColor.GREEN + "/%s: " + ChatColor.BLUE + "%s\n", command, explanation);
    }

    /**
     * Get the {@link PBlockFace} parallel to the given {@link org.bukkit.block.BlockFace}.
     *
     * @param mbf {@link PBlockFace} that will be converted.
     * @return The parallel {@link org.bukkit.block.BlockFace}.
     */
    @NotNull
    public static BlockFace getBukkitFace(final @NotNull PBlockFace mbf)
    {
        return toBlockFace.get(mbf);
    }

    /**
     * Get the {@link org.bukkit.block.BlockFace} parallel to the given {@link PBlockFace}.
     *
     * @param bf {@link org.bukkit.block.BlockFace} that will be converted.
     * @return The parallel {@link PBlockFace}.
     */
    @NotNull
    public static PBlockFace getPBlockFace(final @NotNull BlockFace bf)
    {
        return toPBlockFace.get(bf);
    }

    /**
     * Convert a location to a nicely formatted string of x:y:z using integers.
     *
     * @param loc The location to convert to a string.
     * @return A string of the coordinates of the location.
     */
    @NotNull
    public static String locIntToString(final @NotNull Location loc)
    {
        return String.format("(%d;%d;%d)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    /**
     * Convert a location to a nicely formatted string of x:y:z using doubles rounded to 2 decimals.
     *
     * @param loc The location to convert to a string.
     * @return A string of the coordinates of the location.
     */
    @NotNull
    public static String locDoubleToString(final @NotNull Location loc)
    {
        return String.format("(%.2f;%.2f;%.2f)", loc.getX(), loc.getY(), loc.getZ());
    }

    @NotNull
    public static Optional<String> nameFromUUID(final @NotNull UUID playerUUID)
    {
        Player player = Bukkit.getPlayer(playerUUID);
        return Optional
            .ofNullable(player != null ? player.getName() : Bukkit.getOfflinePlayer(playerUUID).getName());
    }

    /**
     * Try to get a player's UUID from a given name.
     *
     * @param playerName Name of the player.
     * @return UUID of the player if one was found, otherwise null.
     */
    /*
     * First try to get the UUID from an online player, then try an offline player;
     * the first option is faster.
     */
    @NotNull
    public static Optional<UUID> playerUUIDFromString(final @NotNull String playerName)
    {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null)
            try
            {
                player = Bukkit.getPlayer(UUID.fromString(playerName));
            }
            catch (Exception dontcare)
            {
                // Ignored, because it doesn't matter.
            }
        if (player != null)
            /*
             * Check if the resulting player's name is a match to the provided playerName,
             * because player retrieval from a name is not exact. "pim" would match
             * "pim16aap2", for example.
             */
            return Optional.ofNullable(playerName.equals(player.getName()) ? player.getUniqueId() : null);

        OfflinePlayer offPlayer = null;
        try
        {
            offPlayer = Bukkit.getOfflinePlayer(UUID.fromString(playerName));
        }
        catch (Exception dontcare)
        {
            // Ignored, because it doesn't matter.
        }
        return Optional.ofNullable(
            offPlayer == null ? null : playerName.equals(offPlayer.getName()) ? offPlayer.getUniqueId() : null);
    }

    /**
     * Play a sound for all players in a range of 15 blocks around the provided location.
     *
     * @param loc    The location of the sound.
     * @param sound  The name of the sound.
     * @param volume The volume
     * @param pitch  The pitch
     */
    public static void playSound(final @NotNull Location loc, final @NotNull String sound, final float volume,
                                 final float pitch)
    {
        if (loc.getWorld() == null)
            return;
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, 15, 15, 15))
            if (ent instanceof Player)
                ((Player) ent).playSound(loc, sound, volume, pitch);
    }

    /**
     * Retrieve the number of doors a given player is allowed to won.
     *
     * @param player The player for whom to retrieve the limit.
     * @return The limit if one was found, or -1 if unlimited.
     */
    public static CompletableFuture<Integer> getMaxDoorsForPlayer(final @NotNull Player player)
    {
        if (player.isOp())
            return CompletableFuture.completedFuture(-1);

        return getHighestPermissionSuffix(player.getEffectivePermissions(), "bigdoors.own.");
    }

    /**
     * Retrieve the limit of the door size (measured in blocks) a given player can own.
     *
     * @param player The player for whom to retrieve the limit.
     * @return The limit if one was found, or -1 if unlimited.
     */
    public static CompletableFuture<Integer> getMaxDoorSizeForPlayer(final @NotNull Player player)
    {
        if (player.isOp())
            return CompletableFuture.completedFuture(-1);
        return getHighestPermissionSuffix(player.getEffectivePermissions(), "bigdoors.maxsize.");
    }

    /**
     * Get the highest value of a variable in a permission node of a player.
     * <p>
     * For example, retrieve '8' from 'permission.node.8'.
     *
     * @param permissions    The list of permissions of this player to check as obtained from {@link
     *                       Player#getEffectivePermissions()}.
     * @param permissionNode The base permission node.
     * @return The highest value of the variable suffix of the permission node or -1 if none was found.
     */
    private static CompletableFuture<Integer> getHighestPermissionSuffix(
        final @NotNull Set<PermissionAttachmentInfo> permissions,
        final @NotNull String permissionNode)
    {
        return CompletableFuture.supplyAsync(
            () ->
            {
                int ret = -1;
                for (PermissionAttachmentInfo perms : permissions)
                    if (perms.getPermission().startsWith(permissionNode))
                        try
                        {
                            ret = Math.max(ret, Integer.parseInt(perms.getPermission().split(permissionNode)[1]));
                        }
                        catch (Exception unimportant)
                        {
                            // Ignore
                        }
                return ret;
            });
    }


    /**
     * Send a white message to a player.
     *
     * @param player Player to receive the message.
     * @param msg    The message.
     */
    public static void messagePlayer(final @NotNull Player player, final @NotNull String msg)
    {
        messagePlayer(player, ChatColor.WHITE, msg);
    }

    /**
     * Send a number message to a player.
     *
     * @param player The player that will receive the message
     * @param msg    The messages
     */
    public static void messagePlayer(final @NotNull Player player, final @NotNull String[] msg)
    {
        messagePlayer(player, Util.stringFromArray(msg));
    }

    /**
     * Send a number of messages to a player.
     *
     * @param player The player that will receive the message
     * @param color  The color of the message
     * @param msg    The messages
     */
    public static void messagePlayer(final @NotNull Player player, final @NotNull ChatColor color,
                                     final @NotNull String[] msg)
    {
        messagePlayer(player, color, Util.stringFromArray(msg));
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
     * Certain material types need to be refreshed when being placed down.
     *
     * @param mat Material to be checked.
     * @return True if it needs to be refreshed.
     *
     * @deprecated I'm pretty sure this is no longer needed.
     */
    @Deprecated
    public static boolean needsRefresh(final @NotNull Material mat)
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
