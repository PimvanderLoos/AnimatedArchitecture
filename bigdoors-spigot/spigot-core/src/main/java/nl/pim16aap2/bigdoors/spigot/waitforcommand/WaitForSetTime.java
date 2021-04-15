package nl.pim16aap2.bigdoors.spigot.waitforcommand;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandSetAutoCloseTime;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.entity.Player;

/**
 * Represents a delayed command to change the auto close timer of a {@link AbstractDoorBase}.
 *
 * @author Pim
 */
public class WaitForSetTime extends WaitForCommand
{
private final @NonNull AbstractDoorBase door;
private final @NonNull SubCommandSetAutoCloseTime subCommand;

    public WaitForSetTime(final @NonNull BigDoorsSpigot plugin, final @NonNull SubCommandSetAutoCloseTime subCommand,
                          final @NonNull Player player, final @NonNull AbstractDoorBase door)
    {
        super(plugin, subCommand);
        this.player = player;
        this.subCommand = subCommand;
        this.door = door;
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString(Message.COMMAND_SETTIME_INIT));
    }

    @Override
    public boolean executeCommand(@NonNull String[] args)
        throws CommandActionNotAllowedException, IllegalArgumentException
    {
        abortSilently();
        return subCommand.execute(player, door, args[1]);
    }
}
