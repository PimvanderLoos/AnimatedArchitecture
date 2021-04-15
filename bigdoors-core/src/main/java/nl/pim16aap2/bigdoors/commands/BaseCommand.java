package nl.pim16aap2.bigdoors.commands;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@RequiredArgsConstructor
public abstract class BaseCommand
{
    @Getter
    private final @NonNull ICommandSender commandSender;

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
     * @return True if the command could be executed successfully or if the command execution failed through no fault of
     * the {@link ICommandSender}.
     */
    public final @NonNull CompletableFuture<Boolean> run()
    {
        log();
        if (!validInput())
        {
            BigDoors.get().getPLogger().logMessage(Level.FINE, () -> "Invalid input for command: " + this);
            return CompletableFuture.completedFuture(false);
        }
        // We return true in case of an exception, because it cannot (should not?) be possible
        // for an exception to be caused be the command sender.
        return startExecution().exceptionally(
            throwable ->
            {
                BigDoors.get().getPLogger().logThrowable(throwable, "Failed to execute command: " + this);
                if (getCommandSender().isPlayer())
                    // TODO: Localization
                    getCommandSender().sendMessage("Failed to execute command! Please contact an administrator!");
                return true;
            });
    }

    /**
     * Starts the execution of this command. It performs the permission check (See {@link #hasPermission()}) and runs
     * {@link #executeCommand(BooleanPair)} if the {@link ICommandSender} has access to either the user or the admin
     * permission.
     *
     * @return True if the command could be executed successfully or if the command execution failed through no fault of
     * the {@link ICommandSender}.
     */
    protected final CompletableFuture<Boolean> startExecution()
    {
        final CompletableFuture<Boolean> ret = new CompletableFuture<>();

        hasPermission()
            .thenApplyAsync(hasPermission ->
                            {
                                if (!hasPermission.first && !hasPermission.second)
                                {
                                    // TODO: Localization
                                    getCommandSender().sendMessage("No permission!");
                                    ret.complete(true);
                                }
                                return hasPermission;
                            })
            .thenCompose(this::executeCommand)
            .thenApply(ret::complete)
            .exceptionally(throwable -> Util.exceptionallyCompletion(throwable, true, ret));
        return ret;
    }

    /**
     * Executes the command. This method is only called if {@link #validInput()} and {@link #hasPermission()} are true.
     * <p>
     * Note that this method is called asynchronously.
     *
     * @param permissions Whether the {@link ICommandSender} has user and/or admin permissions respectively.
     * @return True if the method execution was successful.
     */
    protected abstract @NonNull CompletableFuture<Boolean> executeCommand(@NonNull BooleanPair permissions);

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
    protected @NonNull CompletableFuture<Optional<AbstractDoorBase>> getDoor(final @NonNull DoorRetriever doorRetriever)
    {
        return getCommandSender().getPlayer().map(doorRetriever::getDoorInteractive)
                                 .orElseGet(doorRetriever::getDoor).thenApplyAsync(
                door ->
                {
                    if (door.isPresent())
                        return door;
                    // TODO: Localization
                    getCommandSender().sendMessage("Could not find the provided door!");
                    return Optional.<AbstractDoorBase>empty();
                });
    }

    /**
     * Checks if the {@link ICommandSender} has access to this command.
     *
     * @return True if the commandsender has access to this command.
     */
    protected @NonNull CompletableFuture<BooleanPair> hasPermission()
    {
        return getCommandSender().hasPermission(getCommand());
    }
}
