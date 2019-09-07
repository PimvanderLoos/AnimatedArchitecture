package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SubCommandFill extends SubCommand
{
    protected static final String help = "Replaces all the blocks in this door by stone. Not particularly useful usually.";
    protected static final String argsHelp = "<doorUID>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.FILLDOOR;

    public SubCommandFill(final @NotNull BigDoorsSpigot plugin, final @NotNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    /**
     * Replaces all blocks between the minimum and maximum coordinates of a {@link DoorBase} with stone.
     *
     * @param door The {@link DoorBase}.
     */
    public boolean execute(@NotNull DoorBase door)
    {
        for (int i = door.getMinimum().getBlockX(); i <= door.getMaximum().getBlockX(); ++i)
            for (int j = door.getMinimum().getBlockY(); j <= door.getMaximum().getBlockY(); ++j)
                for (int k = door.getMinimum().getBlockZ(); k <= door.getMaximum().getBlockZ(); ++k)
                    door.getWorld().getBlockAt(i, j, k).setType(Material.STONE);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, IllegalArgumentException
    {
        plugin.getDatabaseManager().getDoor(CommandManager.getLongFromArg(args[1]))
              .whenComplete((optionalDoor, throwable) -> optionalDoor.ifPresent(this::execute));
        return true;
    }
}
