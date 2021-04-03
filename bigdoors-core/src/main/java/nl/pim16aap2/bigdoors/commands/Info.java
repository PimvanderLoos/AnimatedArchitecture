package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.ICommandSender;

/**
 * Represents the information command.
 *
 * @author Pim
 */
public class Info extends BaseCommand
{

    public Info(@NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }
}
