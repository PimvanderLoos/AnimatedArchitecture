package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@ToString
public class SetName extends BaseCommand
{
    private final @NonNull String name;

    public SetName(final @NonNull ICommandSender commandSender, final @NonNull String name)
    {
        super(commandSender);
        this.name = name;
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.SET_NAME;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        IPPlayer player = (IPPlayer) getCommandSender();
        Optional<ToolUser> tu = BigDoors.get().getToolUserManager().getToolUser(player.getUUID());
        if (tu.isPresent() && tu.get() instanceof Creator)
            tu.get().handleInput(name);
        return CompletableFuture.completedFuture(true);
    }
}
