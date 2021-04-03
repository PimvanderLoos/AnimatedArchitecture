package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.ICommandSender;

public class ListPlayerDoors extends BaseCommand
{

    public ListPlayerDoors(@NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }
}
