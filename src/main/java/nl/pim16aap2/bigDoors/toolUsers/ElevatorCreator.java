package nl.pim16aap2.bigDoors.toolUsers;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;

public class ElevatorCreator extends ToolUser
{
    public ElevatorCreator(BigDoors plugin, Player player, String name)
    {
        super(plugin, player, name, DoorType.ELEVATOR);
        Util.messagePlayer(player, messages.getString("CREATOR.ELEVATOR.Init"));
        if (name == null)
            Util.messagePlayer(player, messages.getString("CREATOR.GENERAL.GiveNameInstruc"));
        else
            triggerGiveTool();
        openDir = RotateDirection.UP;
    }

    @Override
    protected void triggerGiveTool()
    {
        giveToolToPlayer(messages.getString("CREATOR.ELEVATOR.StickLore"    ).split("\n"),
                         messages.getString("CREATOR.ELEVATOR.StickReceived").split("\n"));
    }

    @Override
    protected boolean isReadyToCreateDoor()
    {
        return one != null && two != null && engine != null;
    }

    @Override
    protected void triggerFinishUp()
    {
        finishUp(messages.getString("CREATOR.ELEVATOR.Success"));
    }

    // Make sure the power point is in the middle.
    private void setEngine()
    {
        int xMid = one.getBlockX() + (two.getBlockX() - one.getBlockX()) / 2;
        int zMid = one.getBlockZ() + (two.getBlockZ() - one.getBlockZ()) / 2;
        int yMin = one.getBlockY();
        engine = new Location(one.getWorld(), xMid, yMin, zMid);
    }

    // Take care of the selection points.
    @Override
    public void selector(Location loc)
    {
        if (name == null)
            return;

        if (one == null)
        {
            one = loc;
            Util.messagePlayer(player, messages.getString("CREATOR.ELEVATOR.Step1"));
        }
        else
            two = loc;

        if (one != null && two != null)
        {
            minMaxFix();
            setEngine();
            setIsDone(true);
        }
    }
}
