package nl.pim16aap2.bigdoors.toolusers;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.util.Util;

public class PortcullisCreator extends Creator
{
    private String typeString;

    public PortcullisCreator(BigDoors plugin, Player player, String name, String typeString)
    {
        super(plugin, player, name, DoorType.PORTCULLIS);
        this.typeString = typeString;
        Util.messagePlayer(player, messages.getString("CREATOR." + typeString + ".Init"));
        if (name == null)
            Util.messagePlayer(player, ChatColor.GREEN, messages.getString("CREATOR.GENERAL.GiveNameInstruc"));
        else
            triggerGiveTool();
    }

    public PortcullisCreator(BigDoors plugin, Player player, String name)
    {
        this(plugin, player, name, "PORTCULLIS");
    }

    @Override
    protected void triggerGiveTool()
    {
        giveToolToPlayer(messages.getString("CREATOR." + typeString + ".StickLore"    ).split("\n"),
                         messages.getString("CREATOR." + typeString + ".StickReceived").split("\n"));
    }

    @Override
    protected boolean isReadyToCreateDoor()
    {
        return one != null && two != null && engine != null;
    }

    @Override
    protected void triggerFinishUp()
    {
        finishUp(messages.getString("CREATOR." + typeString + ".Success"));
    }

    // Make sure the power point is in the middle.
    protected void setEngine()
    {
        int xMid = one.getBlockX() + (two.getBlockX() - one.getBlockX()) / 2;
        int zMid = one.getBlockZ() + (two.getBlockZ() - one.getBlockZ()) / 2;
        int yMin = one.getBlockY();
        engine = new Location(one.getWorld(), xMid, yMin, zMid);
    }

    // Check if the second position is valid (door is 1 deep).
    protected boolean isPosTwoValid(Location loc)
    {
        return true;
    }

    // Take care of the selection points.
    @Override
    public void selector(Location loc)
    {
        if (doorName == null)
        {
            Util.messagePlayer(player, messages.getString("CREATOR.GENERAL.GiveNameInstruc"));
            return;
        }
        String canBreakBlock = plugin.canBreakBlock(player.getUniqueId(), loc);
        if (canBreakBlock != null)
        {
            Util.messagePlayer(player, messages.getString("CREATOR.GENERAL.NoPermissionHere") + " " + canBreakBlock);
            return;
        }

        if (one == null)
        {
            one = loc;
            Util.messagePlayer(player, messages.getString("CREATOR." + typeString + ".Step1"));
        }
        else
        {
            if (isPosTwoValid(loc))
                two = loc;
            else
                Util.messagePlayer(player, messages.getString("CREATOR.GENERAL.InvalidPoint"));
        }

        if (one != null && two != null)
        {
            minMaxFix();
            setEngine();
            setIsDone(true);
        }
    }

    @Override
    protected void setOpenDirection()
    {
        // TODO Auto-generated method stub

    }
}
