package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SubCommandMovePowerBlock extends SubCommand
{
    protected static final String help = "Change the location of the powerblock of a door.";
    protected static final String argsHelp = "<doorUID/Name>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.MOVEPOWERBLOCK;

    public SubCommandMovePowerBlock(final @NotNull BigDoorsSpigot plugin, final @NotNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(final @NotNull Player player, final @NotNull AbstractDoorBase door)
    {
        BigDoors.get().getDatabaseManager().hasPermissionForAction(SpigotAdapter.wrapPlayer(player), door.getDoorUID(),
                                                                   DoorAttribute.RELOCATEPOWERBLOCK)
                .whenComplete(
                    (isAllowed, throwable) ->
                    {
                        if (!isAllowed)
                            commandManager.handleException(new CommandActionNotAllowedException(), player, null, null);
                        else
                            plugin.getAbortableTaskManager().startPowerBlockRelocator(player, door);
                    });
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
        throws CommandSenderNotPlayerException, IllegalArgumentException
    {
        if (!(sender instanceof Player))
            throw new CommandSenderNotPlayerException();

        commandManager.getDoorFromArg(sender, args[getMinArgCount() - 1], cmd, args).whenComplete(
            (optionalDoorBase, throwable) -> optionalDoorBase.ifPresent(door -> execute((Player) sender, door)));
        return true;
    }
}
