package nl.pim16aap2.bigdoors.toolusers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PowerBlockRelocator extends ToolUser
{
    protected Location newLoc = null;

    public PowerBlockRelocator(BigDoors plugin, Player player, long doorUID)
    {
        super(plugin, player);
        this.doorUID = doorUID;
        triggerGiveTool();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void triggerGiveTool()
    {
        giveToolToPlayer(messages.getString(Message.CREATOR_PBRELOCATOR_STICKLORE).split("\n"),
                         messages.getString(Message.CREATOR_PBRELOCATOR_INIT).split("\n"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void triggerFinishUp()
    {
        if (newLoc != null)
        {
            plugin.getDatabaseManager().updatePowerBlockLoc(doorUID, newLoc);
            SpigotUtil.messagePlayer(player, messages.getString(Message.CREATOR_PBRELOCATOR_SUCCESS));
        }
        finishUp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selector(Location loc)
    {
        if (plugin.getDatabaseManager().isPowerBlockLocationValid(loc))
        {
            newLoc = loc;
            setIsDone(true);
        }
        else
            SpigotUtil.messagePlayer(player, messages.getString(Message.CREATOR_PBRELOCATOR_LOCATIONINUSE));
    }
}
