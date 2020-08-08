package nl.pim16aap2.bigdoors.spigot.waitforcommand;

import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandSetAutoCloseTime;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a delayed command to change the auto close timer of a {@link AbstractDoorBase}.
 *
 * @author Pim
 */
public class WaitForSetTime extends WaitForCommand
{
    private final AbstractDoorBase door;
    private final SubCommandSetAutoCloseTime subCommand;

    public WaitForSetTime(final @NotNull BigDoorsSpigot plugin, final @NotNull SubCommandSetAutoCloseTime subCommand,
                          final @NotNull Player player, final @NotNull AbstractDoorBase door)
    {
        super(plugin, subCommand);
        this.player = player;
        this.subCommand = subCommand;
        this.door = door;
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString(Message.COMMAND_SETTIME_INIT));
    }

    @Override
    public boolean executeCommand(@NotNull String[] args)
        throws CommandActionNotAllowedException, IllegalArgumentException
    {
        abortSilently();
        return subCommand.execute(player, door, args[1]);
    }
}
