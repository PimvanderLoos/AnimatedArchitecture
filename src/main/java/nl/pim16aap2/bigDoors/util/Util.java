package nl.pim16aap2.bigDoors.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.security.SecureRandom;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import com.google.common.hash.Hashing;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;

public final class Util
{
    private static final Set<Material> WHITELIST = EnumSet.noneOf(Material.class);
    private static final Set<Material> BLACKLIST = EnumSet.noneOf(Material.class);
    private static final Map<DoorDirection, RotateDirection> doorDirectionMapper = new EnumMap<>(DoorDirection.class);
    private static final Map<RotateDirection, DoorDirection> rotateDirectionMapper = new EnumMap<>(RotateDirection.class);
    static
    {
        doorDirectionMapper.put(DoorDirection.NORTH, RotateDirection.NORTH);
        doorDirectionMapper.put(DoorDirection.EAST, RotateDirection.EAST);
        doorDirectionMapper.put(DoorDirection.SOUTH, RotateDirection.SOUTH);
        doorDirectionMapper.put(DoorDirection.WEST, RotateDirection.WEST);

        rotateDirectionMapper.put(RotateDirection.NORTH, DoorDirection.NORTH);
        rotateDirectionMapper.put(RotateDirection.EAST, DoorDirection.EAST);
        rotateDirectionMapper.put(RotateDirection.SOUTH, DoorDirection.SOUTH);
        rotateDirectionMapper.put(RotateDirection.WEST, DoorDirection.WEST);
    }

    private Util()
    {
        // STAY OUT!
        throw new IllegalAccessError();
    }

    public static void processConfig(ConfigLoader configLoader)
    {
        WHITELIST.clear();
        BLACKLIST.clear();

        for (Material mat : configLoader.getWhitelist())
            WHITELIST.add(mat);

        for (Material mat : configLoader.getBlacklist())
            BLACKLIST.add(mat);

        for (Material mat : Material.values())
        {
            if (WHITELIST.contains(mat))
                continue;
            if (!Util.isAllowedBlockBackDoor(mat))
                BLACKLIST.add(mat);
        }
    }

    public static Optional<DoorDirection> getDoorDirection(RotateDirection rot)
    {
        return Optional.ofNullable(rot == null ? null : rotateDirectionMapper.get(rot));
    }

    public static Optional<RotateDirection> getRotateDirection(DoorDirection dir)
    {
        return Optional.ofNullable(dir == null ? null : doorDirectionMapper.get(dir));
    }

    // Send a message to a player in a specific color.
    public static void messagePlayer(Player player, ChatColor color, String s)
    {
        player.sendMessage(color + s);
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

    public static String readSHA256FromURL(final URL url)
    {
        try (Scanner scanner = new Scanner(url.openStream()))
        {
            String hash = scanner.nextLine();
            return hash.length() == 64 ? hash : "";
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return "";
    }

    private static final Pattern VERSION_CLEANUP = Pattern.compile("\\d+(\\.\\d+)+");

    /**
     * Gets the 'cleaned' version number of the current version. E.g. "Alpha
     * 0.1.8.22 (b620)" would return "0.1.8.22".
     *
     * @return The 'cleaned' version number of this version.
     */
    public static String getCleanedVersionString()
    {
        return getCleanedVersionString(BigDoors.get().getDescription().getVersion());
    }

    /**
     * Gets the 'cleaned' version number of the specified version. E.g. "Alpha
     * 0.1.8.22 (b620)" would return "0.1.8.22".
     *
     * @return The 'cleaned' version number of this version.
     */
    public static String getCleanedVersionString(String version)
    {
        Matcher matcher = VERSION_CLEANUP.matcher(version);
        if (!matcher.find())
            return "";
        return matcher.group(0);
    }

    public static String getSHA256(final File file) throws IOException
    {
        return com.google.common.io.Files.hash(file, Hashing.sha256()).toString();
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
            // Not doing anything with catch because I really couldn't care less if it
            // didn't work.
            catch (Exception e)
            {
            }
        if (player != null)
            return player.getName().equals(input) ? player.getUniqueId() : null;

        OfflinePlayer offPlayer = null;
        try
        {
            offPlayer = Bukkit.getOfflinePlayer(UUID.fromString(input));
        }
        // Not doing anything with catch because I really couldn't care less if it
        // didn't work.
        catch (Exception e)
        {
        }
        if (offPlayer != null)
            return offPlayer.getName().equals(input) ? offPlayer.getUniqueId() : null;
        return null;
    }

    public static String getBasicDoorInfo(Door door)
    {
        return String.format("%5d (%d): %s", door.getDoorUID(), door.getPermission(), door.getName());
    }

    public static String getFullDoorInfo(Door door)
    {

        return door == null ? "Door not found!" : door.getDoorUID() + ": " + door.getName().toString() + ", Min("
            + door.getMinimum().getBlockX() + ";" + door.getMinimum().getBlockY() + ";" + door.getMinimum().getBlockZ()
            + ")" + ", Max(" + door.getMaximum().getBlockX() + ";" + door.getMaximum().getBlockY() + ";"
            + door.getMaximum().getBlockZ() + ")" + ", Engine(" + door.getEngine().getBlockX() + ";"
            + door.getEngine().getBlockY() + ";" + door.getEngine().getBlockZ() + ")" + ", "
            + (door.isLocked() ? "" : "NOT ") + "locked" + "; Type=" + door.getType()
            + (door.getEngSide() == null ? "" :
                ("; EngineSide = " + door.getEngSide().toString() + "; doorLen = " + door.getLength()))
            + ", PowerBlockPos = (" + door.getPowerBlockLoc().getBlockX() + ";" + door.getPowerBlockLoc().getBlockY()
            + ";" + door.getPowerBlockLoc().getBlockZ() + ") = (" + door.getPowerBlockChunkHash() + ")" + ". It is "
            + (door.isOpen() ? "OPEN." : "CLOSED.") + " OpenDir = " + door.getOpenDir().toString() + ", Looking "
            + door.getLookingDir().toString() + ". It " + (door.getAutoClose() == -1 ? "does not auto close." :
                ("auto closes after " + door.getAutoClose() + " seconds."));
    }

    private static void playSoundSync(Location loc, String sound, float volume, float pitch)
    {
        final int range = BigDoors.get().getConfigLoader().getSoundRange();
        if (range < 1)
            return;
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, range, range, range))
            if (ent instanceof Player)
                ((Player) ent).playSound(loc, sound, volume, pitch);
    }

    // Play sound at a location.
    public static void playSound(Location loc, String sound, float volume, float pitch)
    {

        Bukkit.getScheduler().callSyncMethod(BigDoors.get(), () ->
        {
            playSoundSync(loc, sound, volume, pitch);
            return null;
        });
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

    /**
     * Gets the lowest positive integer out of 2.
     *
     * @param a The first int.
     * @param b The second int.
     * @return The lowest positive integer.
     */
    public static int getLowestPositiveNumber(final int a, final int b)
    {
        if (a < 0)
            return b;
        if (b < 0)
            return a;
        return Math.min(a, b);
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

        double distanceMultiplier = speed > 4 ? 1.01 : speed > 3.918 ? 1.08 : speed > 3.916 ? 1.10 :
            speed > 2.812 ? 1.12 : speed > 2.537 ? 1.19 : speed > 2.2 ? 1.22 : speed > 2.0 ? 1.23 :
            speed > 1.770 ? 1.25 : speed > 1.570 ? 1.28 : 1.30;
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
        switch (mode)
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
        return XMaterial
            .matchXMaterial(mat.toString()).map(xmat -> xmat.equals(XMaterial.AIR) || xmat.equals(XMaterial.CAVE_AIR) ||
                                                        xmat.equals(XMaterial.WATER) || xmat.equals(XMaterial.LAVA))
            .orElse(false);
    }

    // Logs, stairs and glass panes can rotate, but they don't rotate in exactly the
    // same way.
    public static int canRotate(Material mat)
    {
        XMaterial xmat = XMaterial.matchXMaterial(mat.toString()).orElse(null);
        if (xmat == null)
            return 0;

        if (xmat.equals(XMaterial.ACACIA_LOG) || xmat.equals(XMaterial.BIRCH_LOG) ||
            xmat.equals(XMaterial.DARK_OAK_LOG) || xmat.equals(XMaterial.JUNGLE_LOG) ||
            xmat.equals(XMaterial.OAK_LOG) || xmat.equals(XMaterial.SPRUCE_LOG))
            return 1;
        if (mat.toString().endsWith("STAIRS"))
            return 2;
        // Panes only have to rotate on 1.13+.
        // On versions before, rotating it only changes its color...
        if ((BigDoors.get().is1_13()) &&
            (xmat.equals(XMaterial.WHITE_STAINED_GLASS_PANE) || xmat.equals(XMaterial.YELLOW_STAINED_GLASS_PANE) ||
             xmat.equals(XMaterial.PURPLE_STAINED_GLASS_PANE) || xmat.equals(XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE) ||
             xmat.equals(XMaterial.GRAY_STAINED_GLASS_PANE) || xmat.equals(XMaterial.GREEN_STAINED_GLASS_PANE) ||
             xmat.equals(XMaterial.BLACK_STAINED_GLASS_PANE) || xmat.equals(XMaterial.LIME_STAINED_GLASS_PANE) ||
             xmat.equals(XMaterial.BLUE_STAINED_GLASS_PANE) || xmat.equals(XMaterial.BROWN_STAINED_GLASS_PANE) ||
             xmat.equals(XMaterial.CYAN_STAINED_GLASS_PANE) || xmat.equals(XMaterial.RED_STAINED_GLASS_PANE) ||
             xmat.equals(XMaterial.MAGENTA_STAINED_GLASS_PANE)))
            return 3;
        if (xmat.equals(XMaterial.ANVIL))
            return 4;
        if (xmat.equals(XMaterial.COBBLESTONE_WALL))
            return 5;
        if (xmat.equals(XMaterial.STRIPPED_ACACIA_LOG) || xmat.equals(XMaterial.STRIPPED_BIRCH_LOG) ||
            xmat.equals(XMaterial.STRIPPED_SPRUCE_LOG) || xmat.equals(XMaterial.STRIPPED_DARK_OAK_LOG) ||
            xmat.equals(XMaterial.STRIPPED_JUNGLE_LOG) || xmat.equals(XMaterial.STRIPPED_OAK_LOG) || 
            xmat.equals(XMaterial.CHAIN))
            return 6;
        if (xmat.equals(XMaterial.END_ROD) || xmat.equals(XMaterial.LIGHTNING_ROD))
            return 7;
        return 0;
    }

    public static boolean isAllowedBlock(Material mat)
    {
        return WHITELIST.contains(mat) || !BLACKLIST.contains(mat);
    }

    // Certain blocks don't work in doors, so don't allow their usage.
    public static boolean isAllowedBlockBackDoor(Material mat)
    {
        if (BigDoors.get().getConfigLoader().getBlacklist().contains(mat))
            return false;

        String name = mat.toString();

        if (name.endsWith("SLAB") || name.endsWith("STAIRS") || name.endsWith("WALL"))
            return true;

        if (name.contains("POLISHED") || name.contains("SMOOTH") || name.contains("BRICKS") || name.contains("DEEPSLATE"))
            return true;

        if (name.endsWith("TULIP"))
            return false;

        // TODO: Test if they truly don't work on 1.17. Depends on if they can or cannot float.
        if (name.endsWith("CANDLE"))
            return false;

        XMaterial xmat = XMaterial.matchXMaterial(name).orElse(null);
        if (xmat == null)
        {
//            BigDoors.get().getMyLogger().warn("Could not determine material of mat: " + name);
            return false;
        }

        switch (xmat)
        {
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
        case DRAGON_WALL_HEAD:
        case PISTON_HEAD:
        case PLAYER_HEAD:
        case PLAYER_WALL_HEAD:
        case ZOMBIE_HEAD:
        case ZOMBIE_WALL_HEAD:
        case SKELETON_SKULL:
        case SKELETON_WALL_SKULL:
        case WITHER_SKELETON_SKULL:
        case WITHER_SKELETON_WALL_SKULL:

        case RAIL:
        case DETECTOR_RAIL:
        case ACTIVATOR_RAIL:
        case POWERED_RAIL:
        case LEVER:

        case REDSTONE:
        case REDSTONE_WIRE:
        case TRAPPED_CHEST:
        case TRIPWIRE:
        case TRIPWIRE_HOOK:
        case REDSTONE_TORCH:
        case REDSTONE_WALL_TORCH:
        case TORCH:
        case WALL_TORCH:

        case BROWN_MUSHROOM:
        case RED_MUSHROOM:
        case DEAD_BUSH:
        case FERN:
        case LARGE_FERN:
        case CONDUIT:

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

        case REPEATER:
        case COMMAND_BLOCK:
        case COMMAND_BLOCK_MINECART:
        case COMPARATOR:
        case SEA_PICKLE:

        case POPPY:
        case BLUE_ORCHID:
        case ALLIUM:
        case AZURE_BLUET:
        case OXEYE_DAISY:
        case LILAC:
        case PEONY:
        case GRASS:
        case TALL_GRASS:
        case SEAGRASS:
        case TALL_SEAGRASS:

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

        case LADDER:
        case STONE_PRESSURE_PLATE:
        case HEAVY_WEIGHTED_PRESSURE_PLATE:
        case LIGHT_WEIGHTED_PRESSURE_PLATE:
        case OAK_PRESSURE_PLATE:
        case SPRUCE_PRESSURE_PLATE:
        case BIRCH_PRESSURE_PLATE:
        case JUNGLE_PRESSURE_PLATE:
        case ACACIA_PRESSURE_PLATE:
        case DARK_OAK_PRESSURE_PLATE:

        case STRUCTURE_BLOCK:
        case STRUCTURE_VOID:

            /* 1.14 start */

        case DANDELION:
        case CORNFLOWER:
        case LILY_OF_THE_VALLEY:
        case WITHER_ROSE:
        case BARREL:
        case ACACIA_SIGN:
        case BIRCH_SIGN:
        case DARK_OAK_SIGN:
        case JUNGLE_SIGN:
        case OAK_SIGN:
        case SPRUCE_SIGN:

        case ACACIA_WALL_SIGN:
        case BIRCH_WALL_SIGN:
        case DARK_OAK_WALL_SIGN:
        case JUNGLE_WALL_SIGN:
        case OAK_WALL_SIGN:
        case SPRUCE_WALL_SIGN:
        case BLAST_FURNACE:
        case CARTOGRAPHY_TABLE:
        case COMPOSTER:
        case FLETCHING_TABLE:

        case GRINDSTONE:
        case JIGSAW:
        case LECTERN:
        case LOOM:
        case SMITHING_TABLE:
//        case SMOKER:
        case STONECUTTER:
        case SWEET_BERRY_BUSH:
        case LANTERN:
        case BELL:
            /* 1.14 end */

            /* 1.15 start */
        case BEEHIVE:
        case BEE_NEST:
            /* 1.15 end */

            /* 1.16 start */
        case BAMBOO_SAPLING:
            /* 1.16 end */

            /* 1.17 start */
        case CAVE_VINES:
        case CAVE_VINES_PLANT:
        case GLOW_ITEM_FRAME:
        case GLOW_LICHEN:
        case HANGING_ROOTS:
        case MOSS_CARPET:
        case POINTED_DRIPSTONE:
        case SCULK_SENSOR:
            /* 1.17 Unsure. These will need to be tested but are currently disabled */
        case AMETHYST_BUD:
        case AMETHYST_CLUSTER:
        case AZALEA:
        case BIG_DRIPLEAF:
        case BIG_DRIPLEAF_STEM:
        case LIGHT:
        case POWDER_SNOW:
        case SPORE_BLOSSOM:
            /* 1.17 end */

            return false;
        default:
            return true;
        }
    }

    public static Optional<UUID> parseUUID(final String str)
    {
        try
        {
            return Optional.of(UUID.fromString(str));
        }
        catch (IllegalArgumentException e)
        {
            return Optional.empty();
        }
    }

    public static boolean between(int value, int start, int end)
    {
        return value <= end && value >= start;
    }
}
