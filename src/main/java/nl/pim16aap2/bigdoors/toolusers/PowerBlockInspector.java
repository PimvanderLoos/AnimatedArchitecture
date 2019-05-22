package nl.pim16aap2.bigdoors.toolusers;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandInfo;
import nl.pim16aap2.bigdoors.util.Util;

public class PowerBlockInspector extends ToolUser
{
    public PowerBlockInspector(BigDoors plugin, Player player, long doorUID)
    {
        super(plugin, player, null, null);
        this.doorUID = doorUID;
        Util.messagePlayer(player, messages.getString("CREATOR.PBINSPECTOR.Init"));
        triggerGiveTool();
    }

    @Override
    protected void triggerGiveTool()
    {
        giveToolToPlayer(messages.getString("CREATOR.PBINSPECTOR.StickLore"    ).split("\n"),
                         messages.getString("CREATOR.PBINSPECTOR.StickReceived").split("\n"));
    }

    @Override
    protected void triggerFinishUp()
    {
        finishUp(null);
    }

    // Take care of the selection points.
    @Override
    public void selector(Location loc)
    {
        done = true;
        Door door = plugin.getCommander().doorFromPowerBlockLoc(loc);
        if (door != null)
        {
            ((SubCommandInfo) plugin.getCommand(CommandData.INFO)).execute(player, door);
            setIsDone(true);
        }
    }

    @Override
    protected boolean isReadyToCreateDoor()
    {
        return false;
    }
}
