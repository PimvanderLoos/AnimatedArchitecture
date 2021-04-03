package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.ICommandSender;

public class Confirm extends BaseCommand
{

    public Confirm(@NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }
}
