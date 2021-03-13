package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Represents the information command.
 *
 * @author Pim
 */
public class SubCommandInfo extends SubCommand
{
    protected static final String help = "Display info of a door.";
    protected static final String argsHelp = "<doorUID/Name>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.INFO;

    public SubCommandInfo(final @NotNull BigDoorsSpigot plugin, final @NotNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    private void highlightBlock(final @NotNull Vector3DiConst loc, final @NotNull IPWorld world,
                                final @NotNull IPPlayer player, final @NotNull PColor color)
    {
        plugin.getGlowingBlockSpawner().spawnGlowingBlock(player, world, 15, loc.getX(), loc.getY(), loc.getZ(), color);
    }

    private void sendDoorInfo(final @NotNull CommandSender sender, final @NotNull AbstractDoorBase door)
    {
        plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, door.toString());
    }

    public boolean execute(final @NotNull CommandSender sender, final @NotNull AbstractDoorBase door)
    {
        if (!(sender instanceof Player))
        {
            sendDoorInfo(sender, door);
            return true;
        }

        final @NotNull Optional<DoorOwner> doorOwner = door.getDoorOwner(((Player) sender).getUniqueId());
        if (doorOwner.isEmpty() ||
            doorOwner.get().getPermission() > DoorAttribute.getPermissionLevel(DoorAttribute.INFO))
        {
            sender.sendMessage(messages.getString(Message.ERROR_COMMAND_NOPERMISSION));
            return true;
        }

        sendDoorInfo(sender, door);
//        try
//        {
//            final IPPlayer player = doorOwner.get().getPlayer();
//            highlightBlock(door.getPowerBlock(), door.getWorld(), player, PColor.GOLD);
//            highlightBlock(door.getEngine(), door.getWorld(), player, PColor.DARK_PURPLE);
//            highlightBlock(door.getMinimum(), door.getWorld(), player, PColor.BLUE);
//            highlightBlock(door.getMaximum(), door.getWorld(), player, PColor.RED);
//
//            Vector3Di loc = new Vector3Di(0, 0, 0);
//            for (int x = door.getMinimum().getX(); x <= door.getMaximum().getX(); ++x)
//                for (int y = door.getMinimum().getY(); y <= door.getMaximum().getY(); ++y)
//                    for (int z = door.getMinimum().getZ(); z <= door.getMaximum().getZ(); ++z)
//                    {
//                        loc.setX(x);
//                        loc.setY(y);
//                        loc.setZ(z);
//                        if (loc.equals(door.getMinimum()) || loc.equals(door.getMaximum()) ||
//                            loc.equals(door.getEngine()))
//                            continue;
//                        highlightBlock(loc, door.getWorld(), player, PColor.GREEN);
//                    }
//        }
//        catch (Exception e)
//        {
//            plugin.getPLogger().logThrowable(e, "Failed to spawn a glowing block!");
//        }
        return true;
    }

    public boolean execute(final @NotNull CommandSender sender, final @NotNull List<AbstractDoorBase> doors)
    {
        doors.forEach(door -> execute(sender, door));
        return true;
    }

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
    {
        commandManager.getDoorFromArg(sender, args[minArgCount - 1], cmd, args).whenComplete(
            (optionalDoor, throwable) -> optionalDoor.ifPresent(door -> execute(sender, door)));
        return true;
    }
}
