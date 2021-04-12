package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

@ToString
public class InspectPowerBlock extends BaseCommand
{
    public InspectPowerBlock(@NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.INSPECTPOWERBLOCK;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand(@NonNull BooleanPair permissions)
    {
        throw new UnsupportedOperationException("This command has not yet been implemented!");
    }
}
