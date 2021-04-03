package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.ICommandSender;

public class StopDoors extends BaseCommand
{

    public StopDoors(@NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }
}
