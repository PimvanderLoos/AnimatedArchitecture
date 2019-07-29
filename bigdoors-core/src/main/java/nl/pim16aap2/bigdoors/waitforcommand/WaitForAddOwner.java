package nl.pim16aap2.bigdoors.waitforcommand;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandAddOwner;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a delayed command to add another owner to a {@link DoorBase}.
 *
 * @author Pim
 */
public class WaitForAddOwner extends WaitForCommand
{
    private final DoorBase door;
    private final SubCommandAddOwner subCommand;

    public WaitForAddOwner(final @NotNull BigDoors plugin, final @NotNull SubCommandAddOwner subCommand,
                           final @NotNull Player player, final @NotNull DoorBase door)
    {
        super(plugin, subCommand);
        this.player = player;
        this.subCommand = subCommand;
        this.door = door;
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString(Message.COMMAND_ADDOWNER_INIT));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean executeCommand(final @NotNull String[] args)
        throws CommandPlayerNotFoundException, CommandActionNotAllowedException
    {
        abortSilently();
        return subCommand.execute(player, door, args[1], subCommand.getPermissionFromArgs(player, args, 2));
    }
}
