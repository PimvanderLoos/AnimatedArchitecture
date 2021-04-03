package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.ICommandSender;

public class SetName extends BaseCommand
{

    public SetName(@NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }
}
