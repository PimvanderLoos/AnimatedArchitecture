package nl.pim16aap2.bigdoors.toolusers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import org.bukkit.entity.Player;

public class FlagCreator extends BigDoorCreator
{
    public FlagCreator(BigDoors plugin, Player player, String name)
    {
        super(plugin, player, name, "FLAG");
        type = DoorType.FLAG;
    }
}
