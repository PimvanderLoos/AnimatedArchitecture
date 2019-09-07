package nl.pim16aap2.bigdoors.waitforcommand;

import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetBlocksToMove;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a delayed command to change the blocks a {@link DoorBase} will attempt to move.
 *
 * @author Pim
 */
public class WaitForSetBlocksToMove extends WaitForCommand
{
    private final DoorBase door;
    private final SubCommandSetBlocksToMove subCommand;

    public WaitForSetBlocksToMove(final @NotNull BigDoorsSpigot plugin,
                                  final @NotNull SubCommandSetBlocksToMove subCommand,
                                  final @NotNull Player player, final @NotNull DoorBase door)
    {
        super(plugin, subCommand);
        this.subCommand = subCommand;
        this.player = player;
        this.door = door;
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString(Message.COMMAND_BLOCKSTOMOVE_INIT));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean executeCommand(final @NotNull String[] args)
        throws CommandActionNotAllowedException, IllegalArgumentException
    {
//        abortSilently();
        return subCommand.execute(player, door, args[1]);
    }
}
