package nl.pim16aap2.bigdoors.commands.subcommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.commands.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandInvalidVariableException;
import nl.pim16aap2.bigdoors.commands.CommandManager;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.RotateDirection;

public class SubCommandSetRotation implements ISubCommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;

    private static final String help = "Change the rotation direction of a door";
    private static final String argsHelp = "<doorUID/Name> <CLOCK || COUNTER || ANY>";
    private static final int minArgCount = 3;
    private static final CommandData command = CommandData.SETROTATION;

    public SubCommandSetRotation(final BigDoors plugin, final CommandManager commandManager)
    {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    public void execute(CommandSender sender, Door door, RotateDirection openDir)
    {
        plugin.getCommander().updateDoorOpenDirection(door.getDoorUID(), openDir);

    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, CommandInvalidVariableException,
        CommandActionNotAllowedException
    {
        Door door = plugin.getCommander().getDoor(args[1], sender instanceof Player ? (Player) sender : null);
        if (door == null)
            throw new CommandInvalidVariableException(args[1], "door");

        if (sender instanceof Player && !plugin.getCommander()
            .hasPermissionForAction(((Player) sender), door.getDoorUID(),
                                    DoorAttribute.DIRECTION_STRAIGHT))
            throw new CommandActionNotAllowedException();

        RotateDirection openDir = RotateDirection.valueOf(args[2].toUpperCase());
        if (openDir != RotateDirection.NONE &&
            openDir != RotateDirection.CLOCKWISE &&
            openDir != RotateDirection.COUNTERCLOCKWISE)
            return false;

        return true;
    }

    @Override
    public String getHelp(CommandSender sender)
    {
        return help;
    }

    @Override
    public String getHelpArguments()
    {
        return argsHelp;
    }

    @Override
    public int getMinArgCount()
    {
        return minArgCount;
    }

    @Override
    public CommandData getCommandData()
    {
        return command;
    }

    @Override
    public String getPermission()
    {
        return CommandData.getPermission(command);
    }

    @Override
    public String getName()
    {
        return CommandData.getCommandName(command);
    }
}
