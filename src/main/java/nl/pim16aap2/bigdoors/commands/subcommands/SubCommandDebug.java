package nl.pim16aap2.bigdoors.commands.subcommands;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandManager;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.util.Util;

/*
 * This class really does whatever I want to test at a given point.
 * I do not guarantee I will clean it up between releases and most
 * definitely do not guarantee that this command will have the same
 * or even remotely similar effects between commits, let alone release.
 * As such, this command should not be used by anyone, hence the weird
 * permission node (so it isn't accidentally included by bigdoors.*).
 * I would hardcode my own username, but I think that that'd be a bit much.
 */
public class SubCommandDebug extends SubCommand
{
    protected static final String help = "Do not use this unless you are me... What?";
    protected static final String argsHelp = null;
    protected static final int minArgCount = 1;
    protected static final CommandData command = CommandData.DEBUG;

    public SubCommandDebug(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(CommandSender sender)
    {
        if (sender instanceof Player)
        {
            World world  = ((Player) sender).getWorld();
            Location loc = new Location(world, 49, 77, 191);
            Block block = world.getBlockAt(loc);
            BlockData bd = block.getBlockData();

            if (bd instanceof Stairs)
            {
                Util.broadcastMessage("Shape: " + ((Stairs) bd).getShape().toString() + " Facing: " + ((Stairs) bd).getFacing() + " Half: " + ((Stairs) bd).getHalf());
            }

//            if (! (block.getBlockData() instanceof Directional))
//            {
//                Util.broadcastMessage("Not a Directional block! " + block.toString());
//                return true;
//            }
//            Set<BlockFace> allowedFaces = ((Directional) block.getBlockData()).getFaces();
//
//            Location newLoc = loc;
//            newLoc.add(0, -1, 0);
//
//            int tickRate = 20;
//
//            new BukkitRunnable()
//            {
//                int seconds = 0;
//
//                @Override
//                public void run()
//                {
//                    seconds += tickRate;
//                    if (seconds > 15 * 20)
//                        cancel();
//                    else
//                    {
//                        BlockData bd = block.getBlockData();
//                        Directional mf = (Directional) bd;
//
//                        Set<BlockFace> currentFaces = new HashSet<>();
//                        currentFaces.add(mf.getFacing());
//                        List<MyBlockFace> myFaces = new ArrayList<>(currentFaces.size());
//                        List<MyBlockFace> newFaces = new ArrayList<>(currentFaces.size());
//                        currentFaces.forEach((K) -> myFaces.add(MyBlockFace.getMyBlockFace(K)));
//
//
//                        Material mat = newLoc.getBlock().getType();
//                        Util.broadcastMessage("Material: " + mat.toString());
//
//                        switch(mat)
//                        {
//                        case IRON_BLOCK:
//                            myFaces.forEach((K) -> newFaces.add(MyBlockFace.rotateCounterClockwise(K)));
//                            break;
//                        case COAL_BLOCK:
//                            myFaces.forEach((K) -> newFaces.add(MyBlockFace.rotateVerticallyNorth(K)));
//                            break;
//                        case DIAMOND_BLOCK:
//                            myFaces.forEach((K) -> newFaces.add(MyBlockFace.rotateVerticallyEast(K)));
//                            break;
//                        case BONE_BLOCK:
//                            myFaces.forEach((K) -> newFaces.add(MyBlockFace.rotateVerticallySouth(K)));
//                            break;
//                        case QUARTZ_BLOCK:
//                            myFaces.forEach((K) -> newFaces.add(MyBlockFace.rotateVerticallyWest(K)));
//                            break;
//                        default:
//                            myFaces.forEach((K) -> newFaces.add(MyBlockFace.rotateClockwise(K)));
//                            break;
//                        }
//                        {
//                            StringBuilder builder = new StringBuilder();
//                            currentFaces.forEach((K) -> builder.append(" " + K.toString()));
//                            Util.broadcastMessage("Old faces:" + builder.toString());
//                        }
//                        {
//                            StringBuilder builder = new StringBuilder();
//                            newFaces.forEach((K) -> builder.append(" (" + K.toString() + "): " + " " + MyBlockFace.getBukkitFace(K).toString()));
//                            Util.broadcastMessage("New faces:" + builder.toString());
//                        }
//
////                        currentFaces.forEach((K) -> mf.setFace(K, false));
//                        newFaces.forEach((K) ->
//                        {
//                            if (allowedFaces.contains(MyBlockFace.getBukkitFace(K)))
//                                mf.setFacing(MyBlockFace.getBukkitFace(K));
//                            else
//                                Util.broadcastMessage("\"" + MyBlockFace.getBukkitFace(K).toString() + "\" is not an allowed face!");
//                        });
//                        block.setBlockData(mf);
//                    }
//                }
//            }.runTaskTimer(plugin, 20, tickRate);

        }



//        Door door = plugin.getCommander().getDoor(119);
//        int xMin = door.getMinimum().getBlockX();
//        int yMin = door.getMinimum().getBlockY();
//        int zMin = door.getMinimum().getBlockZ();
//
//        int xMax = door.getMaximum().getBlockX();
//        int yMax = door.getMaximum().getBlockY();
//        int zMax = door.getMaximum().getBlockZ();
//
//        Util.broadcastMessage("Min = " + door.getMinimum());
//        Util.broadcastMessage("Max = " + door.getMaximum());
//
//        StringBuilder builder = new StringBuilder();
//        builder.append("\n");
//        World world = door.getWorld();
//        for (int x = xMin; xMin <= xMax; x++)
//        {
//            if (x > xMax)
//                break;
//            for (int y = yMin; yMin <= yMax; y++)
//            {
//                if (y > yMax)
//                    break;
//                for (int z = zMin; zMin <= zMax; z++)
//                {
//                    if (z > zMax)
//                        break;
//                    Block block = world.getBlockAt(x, y, z);
//                    if (!block.getType().equals(Material.AIR))
//                        builder.append(String.format("%-24s: %5s\n", block.getType().toString(), String.valueOf(Util.isAllowedBlock(block))));
//                }
//            }
//        }
//        Util.broadcastMessage(builder.toString());
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        return execute(sender);
    }
}
