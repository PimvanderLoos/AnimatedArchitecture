package nl.pim16aap2.bigDoors.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import nl.pim16aap2.bigDoors.Door;

public final class Util
{
    // Send a message to a player in a specific color.
    public static void messagePlayer(Player player, ChatColor color, String s)
    {
        player.sendMessage(color + s);
    }

    public static String exceptionToString(Exception e)
    {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static void broadcastMessage(String message)
    {
        if (ConfigLoader.DEBUG)
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

    public static long locationHash(Location loc)
    {
        return loc.hashCode();
    }

    public static long locationHash(int x, int y, int z, UUID worldUUID)
    {
        return locationHash(new Location(Bukkit.getWorld(worldUUID), x, y, z));
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

    public static String playerUUIDFromString(String input)
    {
        Player player = null;
        player = Bukkit.getPlayer(input);
        if (player == null)
            try { player = Bukkit.getPlayer(UUID.fromString(input)); }
            // Not doing anything with catch because I really couldn't care less if it didn't work.
            catch (Exception e)    {}
        if (player != null)
            return player.getUniqueId().toString();

        OfflinePlayer offPlayer = null;
        try { offPlayer = Bukkit.getOfflinePlayer(UUID.fromString(input)); }
        // Not doing anything with catch because I really couldn't care less if it didn't work.
        catch (Exception e)    {}
        if (offPlayer != null)
            return offPlayer.getUniqueId().toString();
        return null;
    }

    public static String getBasicDoorInfo(Door door)
    {
        return door.getDoorUID() + " (" + door.getPermission() + ")" + ": " + door.getName().toString();
    }

    public static String getFullDoorInfo(Door door)
    {

        return     door == null ? "Door not found!" :
                door.getDoorUID() + ": " + door.getName().toString()  +
                ", Min("     + door.getMinimum().getBlockX() + ";"    + door.getMinimum().getBlockY() + ";"   + door.getMinimum().getBlockZ()   + ")" +
                ", Max("     + door.getMaximum().getBlockX() + ";"    + door.getMaximum().getBlockY() + ";"   + door.getMaximum().getBlockZ()   + ")" +
                ", Engine("  + door.getEngine().getBlockX()  + ";"    + door.getEngine().getBlockY()  + ";"   + door.getEngine().getBlockZ()    + ")" +
                ", " + (door.isLocked() ? "" : "NOT ") + "locked"     + "; Type=" + door.getType()    +
                (door.getEngSide() == null ? "" : ("; EngineSide = "  + door.getEngSide().toString()  + "; doorLen = " +
                 door.getLength())) + ", PowerBlockPos = (" + door.getPowerBlockLoc().getBlockX()     + ";"   +
                 door.getPowerBlockLoc().getBlockY() + ";"  + door.getPowerBlockLoc().getBlockZ()     + ") = (" + door.getPowerBlockChunkHash() + ")" +
                ". It is "   + (door.isOpen() ? "OPEN." : "CLOSED.")  + " OpenDir = " + door.getOpenDir().toString() +
                ", Looking " + door.getLookingDir().toString()        + ". It " +
                (door.getAutoClose() == -1 ? "does not auto close."   : ("auto closes after " + door.getAutoClose() + " seconds."));
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
        String permissionNode = "bigdoors.own.";
        int ret = -1;
        for (PermissionAttachmentInfo perms : player.getEffectivePermissions())
            if (perms.getPermission().startsWith(permissionNode))
                try
                {
                    ret = Math.max(ret, Integer.valueOf(perms.getPermission().split(permissionNode)[1]));
                }
                catch (Exception e) {}
        return ret;
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
        double ret[]    = new double[3];
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

    // Send an array of messages to a player.
    public static void messagePlayer(Player player, String[] s)
    {
        String message = "";
        for (String str : s)
            message += str + "\n";
        messagePlayer(player, message);
    }

    // Send an array of messages to a player.
    public static void messagePlayer(Player player, ChatColor color, String[] s)
    {
        String message = "";
        for (String str : s)
            message += str + "\n";
        messagePlayer(player, color, message);
    }

    // Swap min and max values for type mode (0/1/2 -> X/Y/Z) for a specified door.
    public static void swap(Door door, int mode)
    {
        Location newMin = door.getMinimum();
        Location newMax = door.getMaximum();
        double temp;
        switch(mode)
        {
        case 0:
            temp = door.getMaximum().getX();
            newMax.setX(newMin.getX());
            newMin.setX(temp);
            break;
        case 1:
            temp = door.getMaximum().getY();
            newMax.setY(newMin.getY());
            newMin.setY(temp);
            break;
        case 2:
            temp = door.getMaximum().getZ();
            newMax.setZ(newMin.getZ());
            newMin.setZ(temp);
            break;
        }
    }

    public static boolean isAirOrWater(Material mat)
    {
        XMaterial xmat = XMaterial.fromString(mat.toString());
        if (xmat == null)
            return false;
        return xmat.equals(XMaterial.AIR) || xmat.equals(XMaterial.WATER) || xmat.equals(XMaterial.LAVA);
    }

    // Logs, stairs and glass panes can rotate, but they don't rotate in exactly the same way.
    public static int canRotate(Material mat)
    {
        XMaterial xmat = XMaterial.fromString(mat.toString());
        if (xmat.equals(XMaterial.ACACIA_LOG)             || xmat.equals(XMaterial.BIRCH_LOG)           || xmat.equals(XMaterial.DARK_OAK_LOG)       ||
            xmat.equals(XMaterial.JUNGLE_LOG)             || xmat.equals(XMaterial.OAK_LOG)             || xmat.equals(XMaterial.SPRUCE_LOG))
            return 1;
        if (xmat.equals(XMaterial.ACACIA_STAIRS)          || xmat.equals(XMaterial.BIRCH_STAIRS)        || xmat.equals(XMaterial.BRICK_STAIRS)       ||
            xmat.equals(XMaterial.COBBLESTONE_STAIRS)     || xmat.equals(XMaterial.DARK_OAK_STAIRS)     || xmat.equals(XMaterial.JUNGLE_STAIRS)      ||
            xmat.equals(XMaterial.NETHER_BRICK_STAIRS)    || xmat.equals(XMaterial.PURPUR_STAIRS)       || xmat.equals(XMaterial.QUARTZ_STAIRS)      ||
            xmat.equals(XMaterial.RED_SANDSTONE_STAIRS)   || xmat.equals(XMaterial.SANDSTONE_STAIRS)    || xmat.equals(XMaterial.PRISMARINE_STAIRS)  ||
            xmat.equals(XMaterial.DARK_PRISMARINE_STAIRS) || xmat.equals(XMaterial.SPRUCE_STAIRS)       || xmat.equals(XMaterial.OAK_STAIRS)          ||
            xmat.equals(XMaterial.PRISMARINE_BRICK_STAIRS)|| xmat.equals(XMaterial.RED_SANDSTONE_STAIRS)|| xmat.equals(XMaterial.STONE_BRICK_STAIRS))
            return 2;
        if (xmat.equals(XMaterial.WHITE_STAINED_GLASS)       || xmat.equals(XMaterial.YELLOW_STAINED_GLASS)          ||
            xmat.equals(XMaterial.PURPLE_STAINED_GLASS)      || xmat.equals(XMaterial.LIGHT_BLUE_STAINED_GLASS)      ||
            xmat.equals(XMaterial.GRAY_STAINED_GLASS)        || xmat.equals(XMaterial.GREEN_STAINED_GLASS)           ||
            xmat.equals(XMaterial.BLACK_STAINED_GLASS)       || xmat.equals(XMaterial.LIME_STAINED_GLASS)            ||
            xmat.equals(XMaterial.BLUE_STAINED_GLASS)        || xmat.equals(XMaterial.BROWN_STAINED_GLASS)           ||
            xmat.equals(XMaterial.CYAN_STAINED_GLASS)        || xmat.equals(XMaterial.RED_STAINED_GLASS)             ||
            xmat.equals(XMaterial.MAGENTA_STAINED_GLASS)     ||
            xmat.equals(XMaterial.WHITE_STAINED_GLASS_PANE)  || xmat.equals(XMaterial.YELLOW_STAINED_GLASS_PANE)     ||
            xmat.equals(XMaterial.PURPLE_STAINED_GLASS_PANE) || xmat.equals(XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE) ||
            xmat.equals(XMaterial.GRAY_STAINED_GLASS_PANE)   || xmat.equals(XMaterial.GREEN_STAINED_GLASS_PANE)      ||
            xmat.equals(XMaterial.BLACK_STAINED_GLASS_PANE)  || xmat.equals(XMaterial.LIME_STAINED_GLASS_PANE)       ||
            xmat.equals(XMaterial.BLUE_STAINED_GLASS_PANE)   || xmat.equals(XMaterial.BROWN_STAINED_GLASS_PANE)      ||
            xmat.equals(XMaterial.CYAN_STAINED_GLASS_PANE)   || xmat.equals(XMaterial.RED_STAINED_GLASS_PANE)        ||
            xmat.equals(XMaterial.MAGENTA_STAINED_GLASS_PANE))
            return 3;
        if (xmat.equals(XMaterial.ANVIL))
            return 4;
        if (xmat.equals(XMaterial.COBBLESTONE_WALL))
            return 5;
        if (    xmat.equals(XMaterial.STRIPPED_ACACIA_LOG)    || xmat.equals(XMaterial.STRIPPED_BIRCH_LOG)  || xmat.equals(XMaterial.STRIPPED_SPRUCE_LOG)||
            xmat.equals(XMaterial.STRIPPED_DARK_OAK_LOG)  || xmat.equals(XMaterial.STRIPPED_JUNGLE_LOG) || xmat.equals(XMaterial.STRIPPED_OAK_LOG))
            return 6;
        if (xmat.equals(XMaterial.END_ROD))
            return 7;
        return 0;
    }

    public static boolean needsRefresh(XMaterial xmat)
    {
        switch(xmat)
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
    public static boolean isAllowedBlock(Material mat)
    {
        XMaterial xmat = XMaterial.fromString(mat.toString());
        if (xmat == null)
        {
            Util.broadcastMessage("Could not determine material of mat: " + mat.toString());
            return false;
        }

        switch(xmat)
        {
//        case COBBLESTONE_WALL:

        case AIR:
        case WATER:
        case LAVA:

        case ARMOR_STAND:
        case BREWING_STAND:
        case CAULDRON:
        case CHEST:
        case DROPPER:
        case DRAGON_EGG:
        case ENDER_CHEST:
        case HOPPER:
        case JUKEBOX:
        case PAINTING:
        case SIGN:
        case WALL_SIGN:
        case SPAWNER:
        case FURNACE:
        case FURNACE_MINECART:
        case CAKE:

        case WHITE_SHULKER_BOX:
        case YELLOW_SHULKER_BOX:
        case PURPLE_SHULKER_BOX:
        case LIGHT_BLUE_SHULKER_BOX:
        case GRAY_SHULKER_BOX:
        case GREEN_SHULKER_BOX:
        case BLACK_SHULKER_BOX:
        case LIME_SHULKER_BOX:
        case BLUE_SHULKER_BOX:
        case BROWN_SHULKER_BOX:
        case CYAN_SHULKER_BOX:
        case RED_SHULKER_BOX:

        case ACACIA_TRAPDOOR:
        case BIRCH_TRAPDOOR:
        case DARK_OAK_TRAPDOOR:
        case IRON_TRAPDOOR:
        case JUNGLE_TRAPDOOR:
        case OAK_TRAPDOOR:
        case SPRUCE_TRAPDOOR:
        case ACACIA_DOOR:
        case BIRCH_DOOR:
        case IRON_DOOR:
        case JUNGLE_DOOR:
        case OAK_DOOR:
        case SPRUCE_DOOR:
        case DARK_OAK_DOOR:

        case CREEPER_HEAD:
        case CREEPER_WALL_HEAD:
        case DRAGON_HEAD:
        case PISTON_HEAD:
        case PLAYER_HEAD:
        case PLAYER_WALL_HEAD:
        case ZOMBIE_HEAD:
        case ZOMBIE_WALL_HEAD:

        case RAIL:
        case DETECTOR_RAIL:
        case ACTIVATOR_RAIL:
        case POWERED_RAIL:

        case REDSTONE:
        case REDSTONE_WIRE:
        case TRAPPED_CHEST:
        case TRIPWIRE:
        case TRIPWIRE_HOOK:
        case REDSTONE_TORCH:
        case REDSTONE_WALL_TORCH:
        case TORCH:

        case BLACK_CARPET:
        case BLUE_CARPET:
        case BROWN_CARPET:
        case CYAN_CARPET:
        case GRAY_CARPET:
        case GREEN_CARPET:
        case LIGHT_BLUE_CARPET:
        case LIGHT_GRAY_CARPET:
        case LIME_CARPET:
        case MAGENTA_CARPET:
        case ORANGE_CARPET:
        case PINK_CARPET:
        case PURPLE_CARPET:
        case RED_CARPET:
        case WHITE_CARPET:
        case YELLOW_CARPET:

        case ACACIA_BUTTON:
        case BIRCH_BUTTON:
        case DARK_OAK_BUTTON:
        case JUNGLE_BUTTON:
        case OAK_BUTTON:
        case SPRUCE_BUTTON:
        case STONE_BUTTON:

        case ROSE_BUSH:
        case ATTACHED_MELON_STEM:
        case ATTACHED_PUMPKIN_STEM:
        case WHITE_TULIP:
        case DANDELION_YELLOW:
        case LILY_PAD:
        case SUGAR_CANE:
        case PUMPKIN_STEM:
        case NETHER_WART:
        case NETHER_WART_BLOCK:
        case VINE:
        case CHORUS_FLOWER:
        case CHORUS_FRUIT:
        case CHORUS_PLANT:
        case SUNFLOWER:

        case ACACIA_SAPLING:
        case BIRCH_SAPLING:
        case DARK_OAK_SAPLING:
        case JUNGLE_SAPLING:
        case OAK_SAPLING:
        case SPRUCE_SAPLING:
        case SHULKER_BOX:
        case LIGHT_GRAY_SHULKER_BOX:
        case MAGENTA_SHULKER_BOX:
        case ORANGE_SHULKER_BOX:
        case PINK_SHULKER_BOX:

        case BLACK_BED:
        case BLUE_BED:
        case BROWN_BED:
        case CYAN_BED:
        case GRAY_BED:
        case GREEN_BED:
        case LIME_BED:
        case MAGENTA_BED:
        case ORANGE_BED:
        case PINK_BED:
        case RED_BED:
        case WHITE_BED:
        case YELLOW_BED:
        case LIGHT_BLUE_BED:
        case LIGHT_GRAY_BED:

        case BLACK_BANNER:
        case BLACK_WALL_BANNER:
        case BLUE_BANNER:
        case BLUE_WALL_BANNER:
        case BROWN_BANNER:
        case BROWN_WALL_BANNER:
        case CYAN_BANNER:
        case CYAN_WALL_BANNER:
        case GRAY_BANNER:
        case GRAY_WALL_BANNER:
        case GREEN_BANNER:
        case GREEN_WALL_BANNER:
        case LIME_BANNER:
        case LIME_WALL_BANNER:
        case MAGENTA_BANNER:
        case MAGENTA_WALL_BANNER:
        case ORANGE_BANNER:
        case ORANGE_WALL_BANNER:
        case PINK_BANNER:
        case PINK_WALL_BANNER:
        case RED_BANNER:
        case RED_WALL_BANNER:
        case WHITE_BANNER:
        case WHITE_WALL_BANNER:
        case YELLOW_BANNER:
        case YELLOW_WALL_BANNER:
        case LIGHT_BLUE_BANNER:
        case LIGHT_BLUE_WALL_BANNER:
        case LIGHT_GRAY_BANNER:
        case LIGHT_GRAY_WALL_BANNER:
            return false;
        default:
            return true;
        }
    }

    public static boolean between(int value, int start, int end)
    {
        return value <= end && value >= start;
    }
}
