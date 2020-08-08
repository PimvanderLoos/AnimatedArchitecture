package nl.pim16aap2.bigdoors.spigot.waitforcommand;

import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandSetBlocksToMove;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a delayed command to change the blocks a {@link AbstractDoorBase} will attempt to move.
 *
 * @author Pim
 */
public class WaitForSetBlocksToMove extends WaitForCommand
{
    private final AbstractDoorBase door;
    private final SubCommandSetBlocksToMove subCommand;

    public WaitForSetBlocksToMove(final @NotNull BigDoorsSpigot plugin,
                                  final @NotNull SubCommandSetBlocksToMove subCommand,
                                  final @NotNull Player player, final @NotNull AbstractDoorBase door)
    {
        super(plugin, subCommand);
        this.subCommand = subCommand;
        this.player = player;
        this.door = door;
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString(Message.COMMAND_BLOCKSTOMOVE_INIT));
    }

    @Override
    public boolean executeCommand(final @NotNull String[] args)
        throws CommandActionNotAllowedException, IllegalArgumentException
    {
//        abortSilently();
        return subCommand.execute(player, door, args[1]);
    }
}
