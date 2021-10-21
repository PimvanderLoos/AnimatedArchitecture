package nl.pim16aap2.bigDoors.compatibility;

import org.bukkit.entity.Player;

public interface IPermissionsManager
{
    boolean hasPermission(Player player, String permission);
}
