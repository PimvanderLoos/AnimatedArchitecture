package nl.pim16aap2.bigdoors.toolusers;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.Util;

public class PowerBlockRelocator extends ToolUser
{
    public PowerBlockRelocator(BigDoors plugin, Player player, long doorUID)
    {
        super(plugin, player, null, null);
        this.doorUID = doorUID;
        Util.messagePlayer(player, messages.getString("CREATOR.PBRELOCATOR.Init"));
        triggerGiveTool();
    }

    @Override
    protected void triggerGiveTool()
    {
        giveToolToPlayer(messages.getString("CREATOR.PBRELOCATOR.StickLore"    ).split("\n"),
                         messages.getString("CREATOR.PBRELOCATOR.StickReceived").split("\n"));
    }

    @Override
    protected void triggerFinishUp()
    {
        if (one != null)
        {
            plugin.getDatabaseManager().updatePowerBlockLoc(doorUID, one);
            Util.messagePlayer(player, messages.getString("CREATOR.PBRELOCATOR.Success"));
        }
        finishUp(null);
    }

    // Take care of the selection points.
    @Override
    public void selector(Location loc)
    {
        if (plugin.getDatabaseManager().isPowerBlockLocationValid(loc))
        {
            done = true;
            one  = loc;
            setIsDone(true);
        }
        else
            Util.messagePlayer(player, messages.getString("CREATOR.PBRELOCATOR.LocationInUse"));
    }

    @Override
    protected boolean isReadyToCreateDoor()
    {
        return false;
    }
}
