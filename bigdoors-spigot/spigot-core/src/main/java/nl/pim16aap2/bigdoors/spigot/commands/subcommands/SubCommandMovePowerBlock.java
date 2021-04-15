package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SubCommandMovePowerBlock extends SubCommand
{
    protected static final String help = "Change the location of the powerblock of a door.";
    protected static final String argsHelp = "<doorUID/Name>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.MOVEPOWERBLOCK;

    public SubCommandMovePowerBlock(final @NonNull BigDoorsSpigot plugin, final @NonNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(final @NonNull Player player, final @NonNull AbstractDoorBase door)
    {
//        BigDoors.get().getDatabaseManager().hasPermissionForAction(SpigotAdapter.wrapPlayer(player), door.getDoorUID(),
//                                                                   DoorAttribute.RELOCATEPOWERBLOCK)
//                .whenComplete(
//                    (isAllowed, throwable) ->
//                    {
//                        if (!isAllowed)
//                            commandManager.handleException(new CommandActionNotAllowedException(), player, null, null);
//                        else
//                            plugin.getAbortableTaskManager().startPowerBlockRelocator(player, door);
//                    });
        return true;
    }

    @Override
    public boolean onCommand(final @NonNull CommandSender sender, final @NonNull Command cmd,
                             final @NonNull String label, final @NonNull String[] args)
        throws CommandSenderNotPlayerException, IllegalArgumentException
    {
        if (!(sender instanceof Player))
            throw new CommandSenderNotPlayerException();

        commandManager.getDoorFromArg(sender, args[getMinArgCount() - 1], cmd, args).whenComplete(
            (optionalDoorBase, throwable) -> optionalDoorBase.ifPresent(door -> execute((Player) sender, door)));
        return true;
    }
}
