package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
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

    public SubCommandInfo(final @NotNull BigDoors plugin, final @NotNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    private void highlightBlock(final @NotNull Location loc, final @NotNull UUID playerUUID,
                                final @NotNull ChatColor color)
    {
        plugin.getGlowingBlockSpawner().spawnGlowinBlock(playerUUID, loc.getWorld().getName(), 15L,
                                                         loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), color);
    }

    public boolean execute(final @NotNull CommandSender sender, final @NotNull DoorBase door)
    {
        if (sender instanceof Player && door.getPermission() >= 0 &&
            door.getPermission() > DoorAttribute.getPermissionLevel(DoorAttribute.INFO))
            return true;
        plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, door.toString());
        if (sender instanceof Player)
        {
            try
            {
                UUID playerUUID = ((Player) sender).getUniqueId();
                highlightBlock(door.getPowerBlockLoc(), playerUUID, ChatColor.GOLD);
                highlightBlock(door.getEngine(), playerUUID, ChatColor.DARK_PURPLE);
                highlightBlock(door.getMinimum(), playerUUID, ChatColor.BLUE);
                highlightBlock(door.getMaximum(), playerUUID, ChatColor.RED);

                Location loc = new Location(door.getWorld(), 0, 0, 0);
                for (int x = door.getMinimum().getBlockX(); x <= door.getMaximum().getBlockX(); ++x)
                    for (int y = door.getMinimum().getBlockY(); y <= door.getMaximum().getBlockY(); ++y)
                        for (int z = door.getMinimum().getBlockZ(); z <= door.getMaximum().getBlockZ(); ++z)
                        {
                            loc.setX(x);
                            loc.setY(y);
                            loc.setZ(z);
                            if (loc.equals(door.getMinimum()) || loc.equals(door.getMaximum()) ||
                                loc.equals(door.getEngine()))
                                continue;
                            highlightBlock(loc, playerUUID, ChatColor.GREEN);
                        }
            }
            catch (Exception e)
            {
                plugin.getPLogger().logException(e, "Failed to spawn a glowing block!");
            }
        }
        return true;
    }

    public boolean execute(final @NotNull CommandSender sender, final @NotNull List<DoorBase> doors)
    {
        doors.forEach(door -> execute(sender, door));
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
    {
        commandManager.getDoorFromArg(sender, args[minArgCount - 1], cmd, args).whenComplete(
            (optionalDoor, throwable) -> optionalDoor.ifPresent(door -> execute(sender, door)));
        return true;
    }
}
