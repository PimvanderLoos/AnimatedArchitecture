package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.ICommandSender;

public class InspectPowerBlock extends BaseCommand
{

    public InspectPowerBlock(@NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }
}
