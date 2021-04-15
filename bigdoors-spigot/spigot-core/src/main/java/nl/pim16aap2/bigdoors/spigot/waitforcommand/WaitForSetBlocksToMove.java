package nl.pim16aap2.bigdoors.spigot.waitforcommand;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandSetBlocksToMove;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.entity.Player;

/**
 * Represents a delayed command to change the blocks a {@link AbstractDoorBase} will attempt to move.
 *
 * @author Pim
 */
public class WaitForSetBlocksToMove extends WaitForCommand
{
private final @NonNull AbstractDoorBase door;
private final @NonNull SubCommandSetBlocksToMove subCommand;

    public WaitForSetBlocksToMove(final @NonNull BigDoorsSpigot plugin,
                                  final @NonNull SubCommandSetBlocksToMove subCommand,
                                  final @NonNull Player player, final @NonNull AbstractDoorBase door)
    {
        super(plugin, subCommand);
        this.subCommand = subCommand;
        this.player = player;
        this.door = door;
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString(Message.COMMAND_BLOCKSTOMOVE_INIT));
    }

    @Override
    public boolean executeCommand(final @NonNull String[] args)
        throws CommandActionNotAllowedException, IllegalArgumentException
    {
//        abortSilently();
        return subCommand.execute(player, door, args[1]);
    }
}
