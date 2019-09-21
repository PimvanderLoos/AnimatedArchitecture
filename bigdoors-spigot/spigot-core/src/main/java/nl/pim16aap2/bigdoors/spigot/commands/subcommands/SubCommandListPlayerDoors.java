package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SubCommandListPlayerDoors extends SubCommand
{
    protected static final String help = "Returns a list of all doors owned by a given player";
    protected static final String argsHelp = "<player> [doorName]";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.LISTPLAYERDOORS;

    public SubCommandListPlayerDoors(final @NotNull BigDoorsSpigot plugin, final @NotNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(final @NotNull CommandSender sender, final @NotNull List<AbstractDoorBase> doors)
    {
        if (doors.size() == 0)
        {
            plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, messages.getString(Message.ERROR_NODOORSFOUND));
            return true;
        }
        StringBuilder builder = new StringBuilder();
        for (AbstractDoorBase door : doors)
            builder.append(door.getBasicInfo());
        plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, builder.toString());
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
        throws CommandPlayerNotFoundException
    {
        UUID playerUUID = CommandManager.getPlayerFromArg(args[1]);
        String name = args.length > 2 ? args[2] : null;
        BigDoors.get().getDatabaseManager().getDoors(playerUUID, name).whenComplete(
            (doorList, throwable) -> execute(sender, doorList.orElse(new ArrayList<>())));
        return true;
    }
}
