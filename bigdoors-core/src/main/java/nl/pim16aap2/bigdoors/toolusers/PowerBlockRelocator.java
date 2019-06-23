package nl.pim16aap2.bigdoors.toolusers;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;

public class PowerBlockRelocator extends ToolUser
{
    protected Location newLoc = null;

    public PowerBlockRelocator(BigDoors plugin, Player player, long doorUID)
    {
        super(plugin, player);
        this.doorUID = doorUID;
        SpigotUtil.messagePlayer(player, messages.getString("CREATOR.PBRELOCATOR.Init"));
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
        if (newLoc != null)
        {
            plugin.getDatabaseManager().updatePowerBlockLoc(doorUID, newLoc);
            SpigotUtil.messagePlayer(player, messages.getString("CREATOR.PBRELOCATOR.Success"));
        }
        finishUp();
    }

    // Take care of the selection points.
    @Override
    public void selector(Location loc)
    {
        if (plugin.getDatabaseManager().isPowerBlockLocationValid(loc))
        {
            newLoc = loc;
            setIsDone(true);
        }
        else
            SpigotUtil.messagePlayer(player, messages.getString("CREATOR.PBRELOCATOR.LocationInUse"));
    }
}
