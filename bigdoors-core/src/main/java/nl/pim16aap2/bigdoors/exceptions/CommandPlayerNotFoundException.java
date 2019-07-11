package nl.pim16aap2.bigdoors.exceptions;

public class CommandPlayerNotFoundException extends Exception
{
    private static final long serialVersionUID = 1L;
    private final String playerArg;

    public CommandPlayerNotFoundException(final String playerArg)
    {
        this.playerArg = playerArg;
    }

    public String getPlayerArg()
    {
        return playerArg;
    }

}
