package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.ICommandSender;

public class Close extends Toggle
{

    public Close(@NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }
}
