package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.ICommandSender;

public class NewDoor extends BaseCommand
{

    public NewDoor(@NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }
}
