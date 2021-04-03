package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.ICommandSender;

public class Open extends Toggle
{

    public Open(@NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }
}
