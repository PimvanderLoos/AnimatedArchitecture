package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.ICommandSender;

public class RemoveOwner extends BaseCommand
{

    public RemoveOwner(@NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }
}
