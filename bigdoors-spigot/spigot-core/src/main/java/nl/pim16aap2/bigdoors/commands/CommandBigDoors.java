package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.managers.CommandManager;

public class CommandBigDoors extends SuperCommand
{
    private static final CommandData command = CommandData.BIGDOORS;
    private static final int minArgCount = 0;

    public CommandBigDoors(final BigDoorsSpigot plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(minArgCount, command);
    }
}
