package nl.pim16aap2.bigdoors.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public final class Util
{
    // Send a message to a player in a specific color.
    public static void messagePlayer(Player player, ChatColor color, String s)
    {
        player.sendMessage(color + s);
    }

    public static String helpFormat(String command, String explanation)
    {
        return String.format(ChatColor.GREEN + "/%s: " + ChatColor.BLUE + "%s\n", command, explanation);
    }

    public static String errorToString(Error e)
    {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static String exceptionToString(Exception e)
    {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static <T> T[] concatArrays(T[] first, T[] second)
    {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static<T> T[] doubleArraySize(T[] arr)
    {
        return Arrays.copyOf(arr, arr.length * 2);
    }

    public static<T> T[] truncateArray(T[] arr, int newLength)
    {
        return Arrays.copyOf(arr, newLength);
    }

    public static void broadcastMessage(String message)
    {
//        if (ConfigLoader.DEBUG)
        Bukkit.broadcastMessage(message);
    }

    public static String locIntToString(Location loc)
    {
        return String.format("(%d;%d;%d)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public static String locDoubleToString(Location loc)
    {
        return String.format("(%.2f;%.2f;%.2f)", loc.getX(), loc.getY(), loc.getZ());
    }

    public static long chunkHashFromLocation(Location loc)
    {
        return chunkHashFromLocation(loc.getBlockX(), loc.getBlockZ(), loc.getWorld().getUID());
    }

    public static long chunkHashFromLocation(int x, int z, UUID worldUUID)
    {
        int chunk_X = x >> 4;
        int chunk_Z = z >> 4;
        long hash = 3;
        hash = 19 * hash + worldUUID.hashCode();
        hash = 19 * hash + (int) (Double.doubleToLongBits(chunk_X) ^ (Double.doubleToLongBits(chunk_X) >>> 32));
        hash = 19 * hash + (int) (Double.doubleToLongBits(chunk_Z) ^ (Double.doubleToLongBits(chunk_Z) >>> 32));
        return hash;
    }

    // Doors aren't allowed to have numerical names, to differentiate doorNames from
    // doorUIDs.
    public static boolean isValidDoorName(String name)
    {
        try
        {
            Integer.parseInt(name);
            return false;
        }
        catch (NumberFormatException e)
        {
            return true;
        }
    }

    public static long locationHash(Location loc)
    {
        return loc.hashCode();
    }

    public static long locationHash(int x, int y, int z, UUID worldUUID)
    {
        return locationHash(new Location(Bukkit.getWorld(worldUUID), x, y, z));
    }

    static final String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom srnd = new SecureRandom();
    static Random rnd = new Random();

    public static String randomString(int length)
    {
        StringBuilder sb = new StringBuilder(length);
        for (int idx = 0; idx != length; ++idx)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    public static String secureRandomString(int length)
    {
        StringBuilder sb = new StringBuilder(length);
        for (int idx = 0; idx != length; ++idx)
            sb.append(chars.charAt(srnd.nextInt(chars.length())));
        return sb.toString();
    }

    public static String nameFromUUID(UUID playerUUID)
    {
        if (playerUUID == null)
            return null;
        String output = null;
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null)
            output = player.getName();
        else
            output = Bukkit.getOfflinePlayer(playerUUID).getName();
        return output;
    }

    public static String playerUUIDStrFromString(String input)
    {
        UUID playerUUID = playerUUIDFromString(input);
        return playerUUID == null ? null : playerUUID.toString();
    }

    public static UUID playerUUIDFromString(String input)
    {
        Player player = null;
        player = Bukkit.getPlayer(input);
        if (player == null)
            try
            {
                player = Bukkit.getPlayer(UUID.fromString(input));
            }
            catch (Exception dontcare)
            {
            }
        if (player != null)
            return player.getName().equals(input) ? player.getUniqueId() : null;

        OfflinePlayer offPlayer = null;
        try
        {
            offPlayer = Bukkit.getOfflinePlayer(UUID.fromString(input));
        }
        catch (Exception dontcare)
        {
        }
        if (offPlayer != null)
            return offPlayer.getName().equals(input) ? offPlayer.getUniqueId() : null;
        return null;
    }

    // Play sound at a location.
    public static void playSound(Location loc, String sound, float volume, float pitch)
    {
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, 15, 15, 15))
            if (ent instanceof Player)
                ((Player) ent).playSound(loc, sound, volume, pitch);
    }

    public static int getMaxDoorsForPlayer(Player player)
    {
        if (player.isOp())
            return -1;
        return getHighestPermissionSuffix(player, "bigdoors.own.");
    }

    public static int getMaxDoorSizeForPlayer(Player player)
    {
        if (player.isOp())
            return -1;
        return getHighestPermissionSuffix(player, "bigdoors.maxsize.");
    }

    private static int getHighestPermissionSuffix(Player player, String permissionNode)
    {
        int ret = -1;
        for (PermissionAttachmentInfo perms : player.getEffectivePermissions())
            if (perms.getPermission().startsWith(permissionNode))
                try
                {
                    ret = Math.max(ret, Integer.valueOf(perms.getPermission().split(permissionNode)[1]));
                }
                catch (Exception e)
                {
                }
        return ret;
    }

    public static double doubleFromString(String input, double defaultVal)
    {
        try
        {
            return input == null ? defaultVal : Double.parseDouble(input);
        }
        catch (NumberFormatException e)
        {
            return defaultVal;
        }
    }

    public static long longFromString(String input, long defaultVal)
    {
        try
        {
            return input == null ? defaultVal : Long.parseLong(input);
        }
        catch (NumberFormatException e)
        {
            return defaultVal;
        }
    }

    // Send a message to a player.
    public static void messagePlayer(Player player, String s)
    {
        messagePlayer(player, ChatColor.WHITE, s);
    }

    public static String stringFromArray(String[] strings)
    {
        StringBuilder builder = new StringBuilder();
        for (String str : strings)
            builder.append(str);
        return builder.toString();
    }

    // Send an array of messages to a player.
    public static void messagePlayer(Player player, String[] str)
    {
        messagePlayer(player, stringFromArray(str));
    }

    // Send an array of messages to a player.
    public static void messagePlayer(Player player, ChatColor color, String[] str)
    {
        messagePlayer(player, color, stringFromArray(str));
    }

    public static boolean isAirOrWater(Block block)
    {
        // Empty means it's air.
        return block.isLiquid() || block.isEmpty();
    }

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

    // Certain blocks don't work in doors, so don't allow their usage.
    public static boolean isAllowedBlock(Block block)
    {
        if (block == null || isAirOrWater(block))
            return false;

        Material mat = block.getType();
        if (mat == null)
            return false;

        BlockData blockData  = block.getBlockData();
        BlockState blockState = block.getState();

        if (
               blockState instanceof org.bukkit.inventory.InventoryHolder
            || blockState instanceof org.bukkit.block.CreatureSpawner
            )
            return false;

        if (
               blockData instanceof org.bukkit.block.data.type.Stairs
            || blockData instanceof org.bukkit.block.data.type.Gate

            )
            return true;

        if (
            // DaylightDetector, RedstoneWire
               blockData instanceof org.bukkit.block.data.AnaloguePowerable
            // Bamboo, Cocoa, Fire
            || blockData instanceof org.bukkit.block.data.Ageable
            // Door, Stairs, TrapDoor, sunflower, tall grass, tall seagrass, large fern, peony, rose bush, lilac,
            //
            || blockData instanceof org.bukkit.block.data.Bisected
            // Campfire, Furnace, RedstoneWallTorch
            || blockData instanceof org.bukkit.block.data.Lightable
            // Comparator, Door, Gate, Lectern, NoteBlock, Observer, RedstoneRail, Repeater,
            // Switch, TrapDoor, Tripwire, TripwireHook
            || blockData instanceof org.bukkit.block.data.Powerable
            || blockData instanceof org.bukkit.block.data.Rail

            || blockData instanceof org.bukkit.block.data.type.Bed
            || blockData instanceof org.bukkit.block.data.type.BrewingStand
            || blockData instanceof org.bukkit.block.data.type.Cake
            || blockData instanceof org.bukkit.block.data.type.CommandBlock
            || blockData instanceof org.bukkit.block.data.type.EnderChest
            || blockData instanceof org.bukkit.block.data.type.EndPortalFrame
            // Cauldron, Composter, Water, Lava
            || blockData instanceof org.bukkit.block.data.Levelled
            || blockData instanceof org.bukkit.block.data.type.Jukebox
            || blockData instanceof org.bukkit.block.data.type.Ladder
            || blockData instanceof org.bukkit.block.data.type.Piston
            || blockData instanceof org.bukkit.block.data.type.Sapling
            || blockData instanceof org.bukkit.block.data.type.Sign
            || blockData instanceof org.bukkit.block.data.type.TechnicalPiston
            || blockData instanceof org.bukkit.block.data.type.WallSign
            )
            return false;

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
        if (
               matName.endsWith("TULIP")
            || matName.endsWith("BANNER")
            || matName.endsWith("CARPET")
            || matName.endsWith("HEAD")
            )
            return false;
        return true;
    }

    public static boolean between(int value, int start, int end)
    {
        return value <= end && value >= start;
    }

    public static int tickRateFromSpeed(double speed)
    {
        int tickRate;
        if (speed > 9)
            tickRate = 1;
        else if (speed > 7)
            tickRate = 2;
        else if (speed > 6)
            tickRate = 3;
        else
            tickRate = 4;
        return tickRate;
    }

    // Return {time, tickRate, distanceMultiplier} for a given door size.
    public static double[] calculateTimeAndTickRate(int doorSize, double time, double speedMultiplier, double baseSpeed)
    {
        double ret[] = new double[3];
        double distance = Math.PI * doorSize / 2;
        if (time == 0.0)
            time = baseSpeed + doorSize / 3.5;
        double speed = distance / time;
        if (speedMultiplier != 1.0 && speedMultiplier != 0.0)
        {
            speed *= speedMultiplier;
            time = distance / speed;
        }

        // Too fast or too slow!
        double maxSpeed = 11;
        if (speed > maxSpeed || speed <= 0)
            time = distance / maxSpeed;

        double distanceMultiplier = speed > 4     ? 1.01 :
                                    speed > 3.918 ? 1.08 :
                                    speed > 3.916 ? 1.10 :
                                    speed > 2.812 ? 1.12 :
                                    speed > 2.537 ? 1.19 :
                                    speed > 2.2   ? 1.22 :
                                    speed > 2.0   ? 1.23 :
                                    speed > 1.770 ? 1.25 :
                                    speed > 1.570 ? 1.28 : 1.30;
        ret[0] = time;
        ret[1] = tickRateFromSpeed(speed);
        ret[2] = distanceMultiplier;
        return ret;
    }
}
