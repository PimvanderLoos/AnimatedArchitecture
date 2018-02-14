package nl.pim16aap2.bigDoors.util;

public enum Errors
{
	// Error			// Message
	NONE				("test"), 
	EAST				("test"), 
	SOUTH			("test"), 
	WEST				("test");

	private String message;
	
    private Errors(String message) 
    {
        this.message = message;
    }
    
    public static String getExtraAngle(Errors error) { return error.message; }
}
