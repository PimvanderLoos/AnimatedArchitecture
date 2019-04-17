package nl.pim16aap2.bigDoors.waitForCommand;

import org.bukkit.entity.Player;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.util.DoorAttribute;
import nl.pim16aap2.bigDoors.util.Util;

public class WaitForSetTime extends WaitForCommand
{
    private final long doorUID;

    public WaitForSetTime(BigDoors plugin, Player player, String command, long doorUID)
    {
        super(plugin);
        this.player  = player;
        this.command = command;
        this.doorUID = doorUID;
        Util.messagePlayer(player, plugin.getMessages().getString("COMMAND.SetTime.Init"));
        plugin.addCommandWaiter(this);
    }

    @Override
    public boolean executeCommand(String[] args)
    {
        if (!plugin.getCommander().hasPermissionForAction(player, doorUID, DoorAttribute.CHANGETIMER))
            return true;

        if (args.length == 1)
            try
            {
                int time = Integer.parseInt(args[0]);
                plugin.getCommandHandler().setDoorOpenTime(player, doorUID, time);
                plugin.removeCommandWaiter(this);
                if (time != -1)
                    Util.messagePlayer(player, plugin.getMessages().getString("COMMAND.SetTime.Success") + time + "s.");
                else
                    Util.messagePlayer(player, plugin.getMessages().getString("COMMAND.SetTime.Disabled"));
                isFinished = true;
                abort();
                return true;
            }
            catch (Exception e)
            {
                Util.messagePlayer(player, plugin.getMessages().getString("GENERAL.InvalidInput.Integer"));
            }
        abort();
        return false;
    }
}
