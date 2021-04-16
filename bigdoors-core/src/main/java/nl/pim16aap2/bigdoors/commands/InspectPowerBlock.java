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
        return CommandDefinition.INSPECTPOWERBLOCK;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        if (!(getCommandSender() instanceof IPPlayer))
        {
            getCommandSender().sendMessage("Only players can use this command!");
            return CompletableFuture.completedFuture(true);
        }

        new PowerBlockInspector((IPPlayer) getCommandSender(), permissions.second);
        return CompletableFuture.completedFuture(true);
    }


}
