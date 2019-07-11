package nl.pim16aap2.bigdoors.toolusers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * This class represents players in the process of creating doors. Objects of this class are instantiated when the
 * createdoor command is used and they are destroyed after The creation process has been completed successfully or the
 * timer ran out. In EventHandlers this class is used To check whether a user that is left-clicking is a DoorCreator &&
 * tell this class a left-click happened.
 **/
public class RevolvingDoorCreator extends BigDoorCreator
{
    protected String typeString;

    public RevolvingDoorCreator(BigDoors plugin, Player player, String name)
    {
        super(plugin, player, name, "REVOLVINGDOOR");
        type = DoorType.REVOLVINGDOOR;
    }

    // Check if the engine selection is valid. In the case of a windmill, all this
    // means is that the selected block should be within x/y/z bounds.
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
        int zDepth = Math.abs(one.getBlockZ() - loc.getBlockZ());

        // Check if it's not just one block deep in horizontal axis.
        return xDepth > 0 && zDepth > 0;
    }
}
