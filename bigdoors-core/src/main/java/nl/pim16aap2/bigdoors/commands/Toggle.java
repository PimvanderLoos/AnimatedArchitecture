package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.ICommandSender;

public class Toggle extends BaseCommand
{

    public Toggle(@NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }
}
