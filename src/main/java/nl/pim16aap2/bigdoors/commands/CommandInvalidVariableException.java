package nl.pim16aap2.bigdoors.commands;

public class CommandInvalidVariableException extends Exception
{
    private final String doorArg;
    private final String type;

    public CommandInvalidVariableException(final String doorArg, final String type)
    {
        this.doorArg = doorArg;
        this.type = type;
    }

    private static final long serialVersionUID = 1L;

    @Override
    public String getMessage()
    {
        return getDoorArg() + " is not a valid " + getType();
    }

    public String getDoorArg()
    {
        return doorArg;
    }

    public String getType()
    {
        return type;
    }

}
