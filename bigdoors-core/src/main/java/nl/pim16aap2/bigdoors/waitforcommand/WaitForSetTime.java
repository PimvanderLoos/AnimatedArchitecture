package nl.pim16aap2.bigdoors.waitforcommand;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetAutoCloseTime;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import org.bukkit.entity.Player;

public class WaitForSetTime extends WaitForCommand
{
    private final DoorBase door;
    private final SubCommandSetAutoCloseTime subCommand;

    public WaitForSetTime(final BigDoors plugin, final SubCommandSetAutoCloseTime subCommand, final Player player,
                          final DoorBase door)
    {
        super(plugin, subCommand);
        this.player = player;
        this.subCommand = subCommand;
        this.door = door;
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString("COMMAND.SetTime.Init"));
    }

    @Override
    public boolean executeCommand(String[] args)
            throws CommandActionNotAllowedException, IllegalArgumentException
    {
        abortSilently();
        return subCommand.execute(player, door, args[1]);
    }
}
