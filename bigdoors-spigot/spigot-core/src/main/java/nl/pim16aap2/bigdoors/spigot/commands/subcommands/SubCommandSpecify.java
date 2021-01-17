package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SubCommandSpecify extends SubCommandToggle
{
    protected final String help = "Specifies a door";
    protected static final String argsHelp = null;
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.IDENTIFY;

    public SubCommandSpecify(final @NotNull BigDoorsSpigot plugin, final @NotNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    @Override @SneakyThrows
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
    {
        if (!(sender instanceof Player))
            throw new CommandSenderNotPlayerException();
        Player player = (Player) sender;

        final String specified = args[args.length - 1];

        if (!DoorSpecificationManager.get().handleInput(SpigotAdapter.wrapPlayer(player), specified))
        {
            // TODO: Localization
            player.sendMessage("We are not currently waiting for your input!");
        }
        return true;
    }
}
