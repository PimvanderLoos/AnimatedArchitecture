package nl.pim16aap2.bigDoors.toolUsers;

import org.bukkit.entity.Player;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.util.DoorType;

public class FlagCreator extends DoorCreator
{
    public FlagCreator(BigDoors plugin, Player player, String name)
    {
        super(plugin, player, name, "FLAG");
        type = DoorType.FLAG;
    }
}
