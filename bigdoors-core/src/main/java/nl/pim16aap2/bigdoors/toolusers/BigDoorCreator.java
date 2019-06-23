package nl.pim16aap2.bigdoors.toolusers;


import org.bukkit.Location;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;

/**
 * Represents a user creating a Big Door.
 *
 * @author Pim
 * @see Creator
 **/
public class BigDoorCreator extends Creator
{
    protected String typeString;

    public BigDoorCreator(BigDoors plugin, Player player, String name, String typeString)
    {
        super(plugin, player, name, DoorType.BIGDOOR);
        this.typeString = typeString;
        SpigotUtil.messagePlayer(player, messages.getString("CREATOR." + typeString + ".Init"));
        if (name == null)
            SpigotUtil.messagePlayer(player, messages.getString("CREATOR.GENERAL.GiveNameInstruc"));
        else
            triggerGiveTool();
    }

    public BigDoorCreator(BigDoors plugin, Player player, String name)
    {
        this(plugin, player, name, "DOOR");
    }

    @Override
    protected void triggerGiveTool()
    {
        giveToolToPlayer(messages.getString("CREATOR." + typeString + ".StickLore").split("\n"),
                         messages.getString("CREATOR." + typeString + ".StickReceived").split("\n"));
    }

    @Override
    protected void triggerFinishUp()
    {
        finishUp(messages.getString("CREATOR." + typeString + ".Success"));
    }

    @Override
    protected boolean isReadyToCreateDoor()
    {
        return one != null && two != null && engine != null;
    }

    // Check if the engine selection is valid.
    protected boolean isEngineValid(Location loc)
    {
        if (loc.getBlockY() < one.getBlockY() || loc.getBlockY() > two.getBlockY())
            return false;

        // For a regular door, the engine should be on one of the outer pillars of the
        // door.
        int xDepth = Math.abs(one.getBlockX() - two.getBlockX());
        int zDepth = Math.abs(one.getBlockZ() - two.getBlockZ());

        if (xDepth == 0 && loc.getBlockX() == one.getBlockX())
            return loc.getBlockZ() == one.getBlockZ() || loc.getBlockZ() == two.getBlockZ();
        else if (zDepth == 0 && loc.getBlockZ() == one.getBlockZ())
            return loc.getBlockX() == one.getBlockX() || loc.getBlockX() == two.getBlockX();
        return false;
    }

    // Check if the second position is valid (door is 1 deep).
    protected boolean isPosTwoValid(Location loc)
    {
        int xDepth = Math.abs(one.getBlockX() - loc.getBlockX());
        int yDepth = Math.abs(one.getBlockY() - loc.getBlockY());
        int zDepth = Math.abs(one.getBlockZ() - loc.getBlockZ());

        // If the door is only 1 block high, it's a drawbridge.
        if (yDepth == 0)
            return false;
        // Check if it's only 1 deep in exactly 1 direction (single moving pillar is
        // useless).
        return xDepth == 0 ^ zDepth == 0;
    }

    // The engine should be at the very bottom, so put it there once created.
    protected void updateEngineLoc()
    {
        engine.setY(one.getBlockY());
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
            SpigotUtil.messagePlayer(player, messages.getString("CREATOR.GENERAL.NoPermissionHere") + " " + canBreakBlock);
            return;
        }

        if (one == null)
        {
            one = loc;
            String[] message = messages.getString("CREATOR." + typeString + ".Step1").split("\n");
            SpigotUtil.messagePlayer(player, message);
        }
        else if (two == null)
        {
            if (isPosTwoValid(loc) && one != loc)
            {
                two = loc;
                // extending classes might want to have this be optional. This way they can just set engine
                // in isPosTwoValid() if possible, in which case the next print statement is skipped.
                if (engine == null)
                {
                    String[] message = messages.getString("CREATOR." + typeString + ".Step2").split("\n");
                    SpigotUtil.messagePlayer(player, message);
                }
                super.minMaxFix();
            }
            else
                SpigotUtil.messagePlayer(player, messages.getString("CREATOR.GENERAL.InvalidPoint"));

        }
        // If the engine position has not been determined yet
        else if (engine == null)
        {
            if (isEngineValid(loc))
            {
                engine = loc;
                updateEngineLoc();
                setIsDone(true);
            }
            else
                SpigotUtil.messagePlayer(player, messages.getString("CREATOR.GENERAL.InvalidRotation"));
        }
        else
            setIsDone(true);
    }
}
