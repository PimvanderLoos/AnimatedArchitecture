package nl.pim16aap2.bigdoors.spigot.waitforcommand;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandRemoveOwner;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a delayed command to remove an owner of a {@link AbstractDoorBase}.
 *
 * @author Pim
 */
public class WaitForRemoveOwner extends WaitForCommand
{
    @NotNull
    private final AbstractDoorBase door;
    @NotNull
    private final SubCommandRemoveOwner subCommand;

    public WaitForRemoveOwner(final @NotNull BigDoorsSpigot plugin, final @NotNull SubCommandRemoveOwner subCommand,
                              final @NotNull Player player, final @NotNull AbstractDoorBase door)
    {
        super(plugin, subCommand);
        this.subCommand = subCommand;
        this.player = player;
        this.door = door;
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString(Message.COMMAND_REMOVEOWNER_INIT));
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString(Message.COMMAND_REMOVEOWNER_LIST));

        final @NotNull StringBuilder builder = new StringBuilder();
        door.getDoorOwners().forEach((owner) -> builder.append(owner.getPlayer().getName()).append(", "));
        SpigotUtil.messagePlayer(player, builder.toString());
    }

    @Override
    public boolean executeCommand(final @NotNull String[] args)
        throws CommandPlayerNotFoundException, CommandActionNotAllowedException, IllegalArgumentException
    {
        abortSilently();
        return subCommand.execute(player, door, args[2]);
    }
}
