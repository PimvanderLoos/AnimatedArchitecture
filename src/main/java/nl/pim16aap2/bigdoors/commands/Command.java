package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.BigDoors;

public class Command implements ICommand
{
    protected final BigDoors plugin;
    protected final String name;

    protected Command(final BigDoors plugin, final String name)
    {
        this.plugin = plugin;
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }
}
