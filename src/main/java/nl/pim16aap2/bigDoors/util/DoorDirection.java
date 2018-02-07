package nl.pim16aap2.bigDoors.util;

public enum DoorDirection
{	
	// Direction		// Extra angle
	NORTH			(1.0 * Math.PI), 
	EAST				(0.5 * Math.PI), 
	SOUTH			(0.0 * Math.PI), 
	WEST				(1.5 * Math.PI);

	private double extraAngle;
	
    private DoorDirection(double extraAngle) 
    {
        this.extraAngle = extraAngle;
    }
    
    public static double getExtraAngle(DoorDirection dir) { return dir.extraAngle; }
}
