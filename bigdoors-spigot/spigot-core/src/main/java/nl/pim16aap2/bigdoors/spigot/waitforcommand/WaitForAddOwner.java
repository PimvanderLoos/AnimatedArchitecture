package nl.pim16aap2.bigdoors.spigot.waitforcommand;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.entity.Player;

/**
 * Represents a delayed command to add another owner to a {@link AbstractDoorBase}.
 *
 * @author Pim
 */
public class WaitForAddOwner extends WaitForCommand
{
    private final @NonNull AbstractDoorBase door;
    private final @NonNull Object subCommand;

    public WaitForAddOwner(final @NonNull BigDoorsSpigot plugin, final @NonNull Object subCommand,
                           final @NonNull Player player, final @NonNull AbstractDoorBase door)
    {
        super(plugin, subCommand);
        this.player = player;
        this.subCommand = subCommand;
        this.door = door;
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString(Message.COMMAND_ADDOWNER_INIT));
    }

    @Override
    public boolean executeCommand(final @NonNull String[] args)
        throws CommandPlayerNotFoundException, CommandActionNotAllowedException
    {
        throw new UnsupportedOperationException("Deprecated!");
    }
}
