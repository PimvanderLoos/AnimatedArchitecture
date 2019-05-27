package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.managers.CommandManager;

public class CommandBigDoors extends SuperCommand
{
    private static final CommandData command = CommandData.BIGDOORS;
    private static final int minArgCount = 0;

    public CommandBigDoors(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(minArgCount, command);
    }
}
