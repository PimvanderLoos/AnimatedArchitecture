package nl.pim16aap2.bigDoors.util;

import java.util.HashMap;
import java.util.Map;

public enum DoorType
{	
	// DoorType		// Value
	DOOR				(0), 
	DRAWBRIDGE		(1), 
	PORTCULLIS		(2);

	private int    val;
    private static Map<Integer, DoorType> map = new HashMap<Integer, DoorType>();
	
    private DoorType(int val) 
    {
        this.val = val;
    }
    
    public static int   	 	getValue	(DoorType type)	{	return type.val;			}
	public static DoorType 	valueOf	(int type) 		{	return map.get(type);	}
    
	static 
	{
		for (DoorType type : DoorType.values()) 
		{
			map.put(type.val, type);
		}
	}
}
