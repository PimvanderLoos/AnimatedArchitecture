package nl.pim16aap2.bigDoors.util;

import org.bukkit.Location;

public class Selection
{
	Location min, max, engine;
	
	public Selection(Location min, Location max, Location engine)
	{
		this.min    = min;
		this.max    = max;
		this.engine = engine;
	}
}
