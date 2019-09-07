package nl.pim16aap2.bigdoors.waitforcommand;

import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetAutoCloseTime;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a delayed command to change the auto close timer of a {@link DoorBase}.
 *
 * @author Pim
 */
public class WaitForSetTime extends WaitForCommand
{
    private final DoorBase door;
    private final SubCommandSetAutoCloseTime subCommand;

    public WaitForSetTime(final @NotNull BigDoorsSpigot plugin, final @NotNull SubCommandSetAutoCloseTime subCommand,
                          final @NotNull Player player, final @NotNull DoorBase door)
    {
        super(plugin, subCommand);
        this.player = player;
        this.subCommand = subCommand;
        this.door = door;
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString(Message.COMMAND_SETTIME_INIT));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean executeCommand(@NotNull String[] args)
        throws CommandActionNotAllowedException, IllegalArgumentException
    {
        abortSilently();
        return subCommand.execute(player, door, args[1]);
    }
}
