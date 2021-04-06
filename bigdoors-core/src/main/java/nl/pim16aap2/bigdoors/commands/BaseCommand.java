package nl.pim16aap2.bigdoors.commands;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.Util;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

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
    public abstract @NonNull CommandDefinition getCommand();

    /**
     * Checks if the input is valid before starting any potentially expensive tasks.
     *
     * @return True if the input is valid.
     */
    protected boolean validInput()
    {
        return true;
    }

    /**
     * Runs the command if certain criteria are met (i.e. the {@link ICommandSender} has access and {@link
     * #validInput()} returns true).
     *
     * @return True if the command was executed successfully.
     */
    public final @NonNull CompletableFuture<Boolean> run()
    {
        log();
        if (!validInput())
        {
            BigDoors.get().getPLogger().logMessage(Level.FINE, () -> "Invalid input for command: " + toString());
            return CompletableFuture.completedFuture(false);
        }

        return hasPermission().thenApplyAsync(
            hasPermission ->
            {
                if (!hasPermission)
                    return false;
                return executeCommand().join();
            }).exceptionally(t -> Util.exceptionally(t, false));
    }

    /**
     * Executes the command. This method is only called if {@link #validInput()} and {@link #hasPermission()} are true.
     * <p>
     * Note that this method is called asynchronously.
     *
     * @return True if the method execution was successful.
     */
    // TODO: This should obviously be abstract.
    protected @NonNull CompletableFuture<Boolean> executeCommand()
    {
        return CompletableFuture.completedFuture(false);
    }

    /**
     * Ensures the command is logged.
     */
    protected final void log()
    {
        BigDoors.get().getPLogger().dumpStackTrace(Level.FINEST,
                                                   "Running command " + getCommand().name() + ": " + toString());
    }

    /**
     * Attempts to get an {@link AbstractDoorBase} based on the provided {@link DoorRetriever} and the current {@link
     * ICommandSender}.
     * <p>
     * If no door is found, the {@link ICommandSender} will be informed.
     *
     * @param doorRetriever The {@link DoorRetriever} to use
     * @return The {@link AbstractDoorBase} if one could be retrieved.
     */
    protected @NonNull CompletableFuture<Optional<AbstractDoorBase>> getDoor(@NonNull DoorRetriever doorRetriever)
    {
        return commandSender.getPlayer().map(doorRetriever::getDoorInteractive)
                            .orElseGet(doorRetriever::getDoor).thenApplyAsync(
                door ->
                {
                    if (door.isPresent())
                        return door;
                    // TODO: Localization
                    commandSender.sendMessage("Could not find the provided door!");
                    return Optional.<AbstractDoorBase>empty();
                }).exceptionally(Util::exceptionallyOptional);
    }

    /**
     * Checks if the {@link ICommandSender} has access to this command.
     *
     * @return True if the commandsender has access to this command.
     */
    protected @NonNull CompletableFuture<Boolean> hasPermission()
    {
        return getCommandSender().hasPermission(getCommand());
    }
}
