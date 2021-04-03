package nl.pim16aap2.bigdoors.commands;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import nl.pim16aap2.bigdoors.api.ICommandSender;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public abstract class BaseCommand
{
    @Getter
    protected final @NonNull ICommandSender commandSender;

    /**
     * Gets the {@link CommandDefinition} that contains the definition of this {@link BaseCommand}.
     *
     * @return The {@link CommandDefinition} that contains the definition of this {@link BaseCommand}.
     */
    // TODO: This should obviously be abstract.
    public @NonNull CommandDefinition getCommand()
    {
        return null;
    }

    /**
     * Executes this {@link BaseCommand}.
     *
     * @return True if the command was executed successfully.
     */
    // TODO: This should obviously be abstract.
    public CompletableFuture<Boolean> run()
    {
        return CompletableFuture.completedFuture(false);
    }

    protected @NonNull CompletableFuture<Boolean> hasPermission()
    {
        return getCommandSender().hasPermission(getCommand());
    }
}
