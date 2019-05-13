package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.BigDoors;

public class CommandBigDoors extends SuperCommand
{
    private static final String name = "bigdoors";
    private static final String permission = "bigdoors.user";

    public CommandBigDoors(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager, name, permission);
    }

    @Override
    public int getMinArgCount()
    {
        return 1;
    }
}
