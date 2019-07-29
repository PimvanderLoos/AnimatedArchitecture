package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SubCommandFill extends SubCommand
{
    protected static final String help = "Replaces all the blocks in this door by stone. Not particularly useful usually.";
    protected static final String argsHelp = "<doorUID>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.FILLDOOR;

    public SubCommandFill(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(@NotNull DoorBase door)
    {
        plugin.getDatabaseManager().fillDoor(door);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
                             @NotNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, IllegalArgumentException
    {
        return plugin.getDatabaseManager().getDoor(CommandManager.getLongFromArg(args[1])).filter(this::execute)
                     .isPresent();
    }
}
