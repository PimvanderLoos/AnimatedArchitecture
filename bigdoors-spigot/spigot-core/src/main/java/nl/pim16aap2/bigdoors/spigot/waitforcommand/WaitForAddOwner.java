package nl.pim16aap2.bigdoors.spigot.waitforcommand;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandAddOwner;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;

/**
 * Represents a delayed command to add another owner to a {@link AbstractDoorBase}.
 *
 * @author Pim
 */
public class WaitForAddOwner extends WaitForCommand
{
    @NotNull
    private final AbstractDoorBase door;
    @NotNull
    private final SubCommandAddOwner subCommand;

    public WaitForAddOwner(final @NotNull BigDoorsSpigot plugin, final @NotNull SubCommandAddOwner subCommand,
                           final @NotNull Player player, final @NotNull AbstractDoorBase door)
    {
        super(plugin, subCommand);
        this.player = player;
        this.subCommand = subCommand;
        this.door = door;
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString(Message.COMMAND_ADDOWNER_INIT));
    }

    @Override
    public boolean executeCommand(final @NotNull String[] args)
        throws CommandPlayerNotFoundException, CommandActionNotAllowedException
    {
        abortSilently();
        try
        {
            return subCommand.execute(player, door, args[1], subCommand.getPermissionFromArgs(player, args, 2));
        }
        catch (ExecutionException | InterruptedException e)
        {
            BigDoors.get().getPLogger().logThrowable(e);
        }
        return false;
    }
}
