package nl.pim16aap2.bigDoors.waitForCommand;

import org.bukkit.entity.Player;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.util.Util;

public class WaitForSetBlocksToMove extends WaitForCommand
{
    private long doorUID;

    public WaitForSetBlocksToMove(BigDoors plugin, Player player, String command, long doorUID)
    {
        super(plugin);
        this.player  = player;
        this.command = command;
        this.doorUID = doorUID;
        Util.messagePlayer(player, plugin.getMessages().getString("COMMAND.SetBlocksToMove.Init"));
        plugin.addCommandWaiter(this);
    }

    @Override
    public boolean executeCommand(String[] args)
    {
        if (args.length == 1)
        {
            try
            {
                int blocksToMove = Integer.parseInt(args[0]);
                plugin.getCommandHandler().setDoorBlocksToMove(player, doorUID, blocksToMove);
                plugin.removeCommandWaiter(this);
                if (blocksToMove > 0)
                    Util.messagePlayer(player, plugin.getMessages().getString("COMMAND.SetBlocksToMove.Success") + blocksToMove);
                else
                    Util.messagePlayer(player, plugin.getMessages().getString("COMMAND.SetBlocksToMove.Disabled"));
                isFinished = true;
                abort();
                return true;
            }
            catch (Exception e)
            {
                Util.messagePlayer(player, plugin.getMessages().getString("GENERAL.InvalidInput.Integer"));
            }
        }
        abort();
        return false;
    }
}
