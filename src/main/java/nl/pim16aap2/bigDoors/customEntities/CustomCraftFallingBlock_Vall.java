package nl.pim16aap2.bigDoors.customEntities;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public interface CustomCraftFallingBlock_Vall
{
	boolean teleport(Location newPos);
	
	void remove();

	void setVelocity(Vector vector);

	Location getLocation();

	Vector getVelocity();
}

