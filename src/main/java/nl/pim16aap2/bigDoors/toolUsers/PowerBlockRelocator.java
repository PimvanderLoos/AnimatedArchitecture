package nl.pim16aap2.bigDoors.toolUsers;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.Util;

public class PowerBlockRelocator extends ToolUser
{
    private final Door door;
    private boolean powerBlockLocationChanged = false;

    public PowerBlockRelocator(BigDoors plugin, Player player, Door door)
    {
        super(plugin, player, null, null);
        this.door = door;
        doorUID = door.getDoorUID();
        Util.messagePlayer(player, messages.getString("CREATOR.PBRELOCATOR.Init"));
        triggerGiveTool();
    }

    @Override
    protected void triggerGiveTool()
    {
        giveToolToPlayer(messages.getString("CREATOR.PBRELOCATOR.StickLore").split("\n"),
                         messages.getString("CREATOR.PBRELOCATOR.StickReceived").split("\n"));
    }

    @Override
    protected void triggerFinishUp()
    {
        if (one != null)
        {
            // Only update the powerblock location if it actually changed.
            if (powerBlockLocationChanged)
                plugin.getCommander().updatePowerBlockLoc(doorUID, one);
            Util.messagePlayer(player, messages.getString("CREATOR.PBRELOCATOR.Success"));
        }
        finishUp(null);
    }

    private boolean verifyDistance(Location loc)
    {
        if (plugin.getConfigLoader().maxPowerBlockDistance() < 0)
            return true;

        double distance = door.getEngine().toVector().distance(loc.toVector());
        return distance <= plugin.getConfigLoader().maxPowerBlockDistance();
    }

    // Take care of the selection points.
    @Override
    public void selector(Location loc)
    {
        if (!verifyDistance(loc))
        {
            Util.messagePlayer(player, messages.getString("CREATOR.PBRELOCATOR.LocationTooFar"));
            return;
        }

        if (loc.equals(door.getPowerBlockLoc()))
        {
            powerBlockLocationChanged = false;
            applySelected(loc);
        }
        else if (plugin.getCommander().isPowerBlockLocationValid(loc))
        {
            powerBlockLocationChanged = true;
            applySelected(loc);
        }
        else
            Util.messagePlayer(player, messages.getString("CREATOR.PBRELOCATOR.LocationInUse"));
    }

    /**
     * Applies the selected location and continues the process.
     *
     * @param loc The Location to apply. It is assumed that the location is already valid.
     */
    private void applySelected(Location loc)
    {
        done = true;
        one = loc;
        setIsDone(true);
    }

    @Override
    protected boolean isReadyToCreateDoor()
    {
        return false;
    }
}
