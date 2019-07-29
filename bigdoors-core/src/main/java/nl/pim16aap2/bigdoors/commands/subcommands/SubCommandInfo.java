package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Level;

public class SubCommandInfo extends SubCommand
{
    protected static final String help = "Display info of a door.";
    protected static final String argsHelp = "<doorUID/Name>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.INFO;

    public SubCommandInfo(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(CommandSender sender, DoorBase door)
    {
        if (sender instanceof Player && door.getPermission() >= 0 &&
            door.getPermission() > DoorAttribute.getPermissionLevel(DoorAttribute.INFO))
            return true;
        plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, door.toString());
        if (sender instanceof Player)
        {
            try
            {
                plugin.getGlowingBlockSpawner().spawnGlowinBlock(((Player) sender).getUniqueId(),
                                                                 door.getWorld().getName(), 30L,
                                                                 door.getPowerBlockLoc().getBlockX(),
                                                                 door.getPowerBlockLoc().getBlockY(),
                                                                 door.getPowerBlockLoc().getBlockZ());
            }
            catch (Exception e)
            {
                plugin.getPLogger().logException(e, "Failed to spawn a glowing block!");
            }
        }
        return true;
    }

    public boolean execute(CommandSender sender, List<DoorBase> doors)
    {
        doors.forEach(door -> execute(sender, door));
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
                             @NotNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, IllegalArgumentException
    {
        return execute(sender, commandManager.getDoorFromArg(sender, args[minArgCount - 1]));
    }
}
