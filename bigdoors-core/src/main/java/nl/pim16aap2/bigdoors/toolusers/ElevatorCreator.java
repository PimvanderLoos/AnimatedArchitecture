package nl.pim16aap2.bigdoors.toolusers;


import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ElevatorCreator extends Creator
{
    public ElevatorCreator(BigDoors plugin, Player player, String name)
    {
        super(plugin, player, name, DoorType.ELEVATOR);
        SpigotUtil.messagePlayer(player, messages.getString("CREATOR.ELEVATOR.Init"));
        if (name == null)
            SpigotUtil.messagePlayer(player, messages.getString("CREATOR.GENERAL.GiveNameInstruc"));
        else
            triggerGiveTool();
    }

    @Override
    protected void triggerGiveTool()
    {
        giveToolToPlayer(messages.getString("CREATOR.ELEVATOR.StickLore").split("\n"),
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
    protected void setEngine()
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
        if (doorName == null)
        {
            SpigotUtil.messagePlayer(player, messages.getString("CREATOR.GENERAL.GiveNameInstruc"));
            return;
        }
        String canBreakBlock = plugin.canBreakBlock(player.getUniqueId(), loc);
        if (canBreakBlock != null)
        {
            SpigotUtil.messagePlayer(player,
                                     messages.getString("CREATOR.GENERAL.NoPermissionHere") + " " + canBreakBlock);
            return;
        }

        if (one == null)
        {
            one = loc;
            SpigotUtil.messagePlayer(player, messages.getString("CREATOR.ELEVATOR.Step1"));
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
