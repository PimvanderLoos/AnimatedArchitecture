package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

@ToString
public class Restart extends BaseCommand
{
    public Restart(@NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.RESTART;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand(@NonNull BooleanPair permissions)
    {
        throw new UnsupportedOperationException("This command has not yet been implemented!");
    }
}
