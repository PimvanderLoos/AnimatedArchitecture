package nl.pim16aap2.bigdoors.commands;

public class CommandInvalidVariableException extends Exception
{
    private final String valueArg;
    private final String type;

    public CommandInvalidVariableException(final String valueArg, final String type)
    {
        this.valueArg = valueArg;
        this.type = type;
    }

    private static final long serialVersionUID = 1L;

    @Override
    public String getMessage()
    {
        return getValueArg() + " is not a valid " + getType();
    }

    public String getValueArg()
    {
        return valueArg;
    }

    public String getType()
    {
        return type;
    }

}
