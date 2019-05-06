package nl.pim16aap2.bigdoors.toolusers;

import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.DoorType;

public class FlagCreator extends DoorCreator
{
    public FlagCreator(BigDoors plugin, Player player, String name)
    {
        super(plugin, player, name, "FLAG");
        type = DoorType.FLAG;
    }
}
