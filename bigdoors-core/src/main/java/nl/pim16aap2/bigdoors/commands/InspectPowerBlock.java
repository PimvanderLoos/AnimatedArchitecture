package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.tooluser.PowerBlockInspector;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

@ToString
public class InspectPowerBlock extends BaseCommand
{
    public InspectPowerBlock(final @NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.INSPECT_POWERBLOCK;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        new PowerBlockInspector((IPPlayer) getCommandSender(), permissions.second);
        return CompletableFuture.completedFuture(true);
    }


}
