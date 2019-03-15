package nl.pim16aap2.bigDoors.waitForCommand;

import java.util.UUID;

import org.bukkit.entity.Player;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.Util;

public class WaitForRemoveOwner extends WaitForCommand
{
    private long doorUID;

    public WaitForRemoveOwner(BigDoors plugin, Player player, String command, long doorUID)
    {
        super(plugin);
        this.player  = player;
        this.command = command;
        this.doorUID = doorUID;
        Util.messagePlayer(player, plugin.getMessages().getString("COMMAND.RemoveOwner.Init"));
        plugin.addCommandWaiter(this);
    }

    @Override
    public boolean executeCommand(String[] args)
    {
        // example: /BigDoors removeOwner pim16aap2
        if (args.length == 2)
        {
            UUID playerUUID = Util.playerUUIDFromString(args[1]);
            Door door = plugin.getCommander().getDoor(player.getUniqueId(), doorUID);

            if (playerUUID != null)
            {
                if (plugin.getCommander().removeOwner(playerUUID, door))
                {
                    Util.messagePlayer(player, plugin.getMessages().getString("COMMAND.RemoveOwner.Success"));
                    return true;
                }
                return false;
            }
            else
                return false;
        }
        return false;
    }
}
