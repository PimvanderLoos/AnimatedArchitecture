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
 * Represents a delayed command to remove an owner of a {@link AbstractDoorBase}.
 *
 * @author Pim
 */
public class WaitForRemoveOwner extends WaitForCommand
{
    private final @NonNull AbstractDoorBase door;
    private final @NonNull Object subCommand;

    public WaitForRemoveOwner(final @NonNull BigDoorsSpigot plugin, final @NonNull Object subCommand,
                              final @NonNull Player player, final @NonNull AbstractDoorBase door)
    {
        super(plugin, subCommand);
        this.subCommand = subCommand;
        this.player = player;
        this.door = door;
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString(Message.COMMAND_REMOVEOWNER_INIT));
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString(Message.COMMAND_REMOVEOWNER_LIST));

        final @NonNull StringBuilder builder = new StringBuilder();
        door.getDoorOwners().forEach((owner) -> builder.append(owner.getPPlayerData().getName()).append(", "));
        SpigotUtil.messagePlayer(player, builder.toString());
    }

    @Override
    public boolean executeCommand(final @NonNull String[] args)
        throws CommandPlayerNotFoundException, CommandActionNotAllowedException, IllegalArgumentException
    {
        abortSilently();
//        return subCommand.execute(player, door, args[2]);
        throw new UnsupportedOperationException("Deprecated!");
    }
}
