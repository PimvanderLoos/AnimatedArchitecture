package nl.pim16aap2.bigdoors.toolusers;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.DoorType;
import nl.pim16aap2.bigdoors.util.Util;

/**
 * This class represents players in the process of creating doors. Objects of
 * this class are instantiated when the createdoor command is used and they are
 * destroyed after The creation process has been completed successfully or the
 * timer ran out. In EventHandlers this class is used To check whether a user
 * that is left-clicking is a DoorCreator && tell this class a left-click
 * happened.
 **/
public class WindmillCreator extends DoorCreator
{
    protected String typeString;

    public WindmillCreator(BigDoors plugin, Player player, String name)
    {
        super(plugin, player, name, "WINDMILL");
        type = DoorType.WINDMILL;
    }

    // No updating of the engine location is required for this type.
    @Override
    protected void updateEngineLoc()
    {}

    // Check if the engine selection is valid. In the case of a windmill, all this
    // means is that the selected block should be withint x/y/z bounds.
    @Override
    protected boolean isEngineValid(Location loc)
    {
        if (loc.getBlockX() < one.getBlockX() || loc.getBlockX() > two.getBlockX() ||
            loc.getBlockY() < one.getBlockY() || loc.getBlockY() > two.getBlockY() ||
            loc.getBlockZ() < one.getBlockZ() || loc.getBlockZ() > two.getBlockZ())
            return false;
        return true;
    }

    // Check if the second position is valid (door is 1 deep).
    @Override
    protected boolean isPosTwoValid(Location loc)
    {
        int xDepth = Math.abs(one.getBlockX() - loc.getBlockX());
        int yDepth = Math.abs(one.getBlockY() - loc.getBlockY());
        int zDepth = Math.abs(one.getBlockZ() - loc.getBlockZ());

        // Check if it's only 1 deep in exactly 1 direction.
        Util.broadcastMessage("Windmill checking pos2 validity: xDepth: " + xDepth + ", yDepth: " + yDepth + ", zDepth: " + zDepth);
        Util.broadcastMessage("Check1 (xDepth == 0 ^ zDepth == 0 ^ yDepth == 0): " + (xDepth == 0 ^ zDepth == 0 ^ yDepth == 0));
//        Util.broadcastMessage("Check2 (xDepth > 1 || yDepth > 1 || zDepth > 1: " + (xDepth > 1 || yDepth > 1 || zDepth > 1));
//        Util.broadcastMessage("Result: " + ((xDepth == 0 ^ zDepth == 0) && (xDepth > 1 || yDepth > 1 || zDepth > 1)));
        return (xDepth == 0 ^ zDepth == 0 ^ yDepth == 0);
    }
}
