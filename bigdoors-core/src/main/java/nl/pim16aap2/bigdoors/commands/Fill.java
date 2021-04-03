package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.ICommandSender;

public class Fill extends BaseCommand
{

    public Fill(@NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }
}
