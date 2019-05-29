package nl.pim16aap2.bigdoors.waitforcommand;

import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.commands.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.commands.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandAddOwner;
import nl.pim16aap2.bigdoors.util.Util;

public class WaitForAddOwner extends WaitForCommand
{
    private final Door door;
    private final SubCommandAddOwner subCommand;

    public WaitForAddOwner(final BigDoors plugin, final SubCommandAddOwner subCommand, final Player player, final Door door)
    {
        super(plugin, subCommand);
        this.player = player;
        this.subCommand = subCommand;
        this.door = door;
        Util.messagePlayer(player, plugin.getMessages().getString("COMMAND.AddOwner.Init"));
    }

    @Override
    public boolean executeCommand(String[] args) throws CommandPlayerNotFoundException, CommandActionNotAllowedException
    {
        abortSilently();
        return subCommand.execute(player, door, args[1], subCommand.getPermissionFromArgs(player, args, 2));
    }
}
