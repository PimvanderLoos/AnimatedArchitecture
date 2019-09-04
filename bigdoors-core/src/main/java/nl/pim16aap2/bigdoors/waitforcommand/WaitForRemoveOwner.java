package nl.pim16aap2.bigdoors.waitforcommand;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandRemoveOwner;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a delayed command to remove an owner of a {@link DoorBase}.
 *
 * @author Pim
 */
public class WaitForRemoveOwner extends WaitForCommand
{
    private final DoorBase door;
    private final SubCommandRemoveOwner subCommand;

    public WaitForRemoveOwner(final @NotNull BigDoors plugin, final @NotNull SubCommandRemoveOwner subCommand,
                              final @NotNull Player player, final @NotNull DoorBase door)
    {
        super(plugin, subCommand);
        this.subCommand = subCommand;
        this.player = player;
        this.door = door;
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString(Message.COMMAND_REMOVEOWNER_INIT));
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString(Message.COMMAND_REMOVEOWNER_LIST));

        plugin.getDatabaseManager().getDoorOwners(door.getDoorUID()).whenComplete(
            (ownerList, throwable) ->
            {
                StringBuilder builder = new StringBuilder();
                for (DoorOwner owner : ownerList)
                    builder.append(owner.getPlayerName()).append(", ");
                SpigotUtil.messagePlayer(player, builder.toString());
            }
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean executeCommand(final @NotNull String[] args)
        throws CommandPlayerNotFoundException, CommandActionNotAllowedException, IllegalArgumentException
    {
        abortSilently();
        return subCommand.execute(player, door, args[2]);
    }
}
