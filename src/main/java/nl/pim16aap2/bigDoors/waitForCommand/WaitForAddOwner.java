package nl.pim16aap2.bigDoors.waitForCommand;

import java.util.UUID;

import org.bukkit.entity.Player;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.Util;

public class WaitForAddOwner extends WaitForCommand
{
    private long doorUID;

    public WaitForAddOwner(BigDoors plugin, Player player, String command, long doorUID)
    {
        super(plugin);
        this.player  = player;
        this.command = command;
        this.doorUID = doorUID;
        Util.messagePlayer(player, plugin.getMessages().getString("COMMAND.AddOwner.Init"));
        plugin.addCommandWaiter(this);
    }

    @Override
    public boolean executeCommand(String[] args)
    {
        // Example: /BigDoors addOwner pim16aap2 1
        if (args.length == 3)
        {
            UUID playerUUID = Util.playerUUIDFromString(args[1]);
            Door door = plugin.getCommander().getDoor(player.getUniqueId(), doorUID);
            int permission = 1;
            try
            {
                if (args.length == 3)
                    permission = Integer.parseInt(args[2]);
            }
            catch (Exception uncaught)
            {
                Util.messagePlayer(player, plugin.getMessages().getString("GENERAL.InvalidInput.Integer"));
            }
            if (playerUUID != null)
            {
                if (plugin.getCommander().addOwner(playerUUID, door, permission))
                {
                    Util.messagePlayer(player, plugin.getMessages().getString("COMMAND.AddOwner.Init"));
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
