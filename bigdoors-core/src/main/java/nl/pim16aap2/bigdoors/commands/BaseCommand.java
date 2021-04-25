package nl.pim16aap2.bigdoors.commands;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Represents the base BigDoors command.
 * <p>
 * This handles all the basics shared by all commands and contains some utility methods for common actions.
 *
 * @author Pim
 */
@ToString
@RequiredArgsConstructor
public abstract class BaseCommand
{
    /**
     * The entity (e.g. player, server, or command block) that initiated the command.
     * <p>
     * This is the entity that is held responsible for the command (i.e. their permissions are checked and they will
     * receive error/success/information messages when applicable).
     */
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
     * Checks if the {@link #commandSender} has access to a given {@link DoorAttribute} for a given door.
     *
     * @param door                The door to check.
     * @param doorAttribute       The {@link DoorAttribute} to check.
     * @param hasBypassPermission Whether the {@link #commandSender} has bypass permission or not.
     * @return True if the command sender has access to the provided attribute for the given door.
     */
    protected boolean hasAccessToAttribute(final @NonNull AbstractDoorBase door,
                                           final @NonNull DoorAttribute doorAttribute,
                                           final boolean hasBypassPermission)
    {
        if (hasBypassPermission || !getCommandSender().isPlayer())
            return true;

        return getCommandSender().getPlayer()
                                 .flatMap(door::getDoorOwner)
                                 .map(doorOwner -> doorOwner.getPermission() <= doorAttribute.getPermissionLevel())
                                 .orElse(false);
    }

    /**
     * Checks if this {@link BaseCommand} is available for {@link IPPlayer}s.
     *
     * @return True if an {@link IPPlayer} can execute this command.
     */
    protected boolean availableForPlayers()
    {
        return true;
    }

    /**
     * Checks if this {@link BaseCommand} is available for non-{@link IPPlayer}s (e.g. the server).
     *
     * @return True if an non-{@link IPPlayer} can execute this command.
     */
    protected boolean availableForNonPlayers()
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
    protected final @NonNull CompletableFuture<Boolean> run()
    {
        log();
        if (!validInput())
        {
            BigDoors.get().getPLogger().logMessage(Level.FINE, () -> "Invalid input for command: " + this);
            return CompletableFuture.completedFuture(false);
        }

        final boolean isPlayer = getCommandSender() instanceof IPPlayer;
        if (isPlayer && !availableForPlayers())
        {
            BigDoors.get().getPLogger().logMessage(Level.FINE, () -> "Command not allowed for players: " + this);
            // TODO: Localization
            getCommandSender().sendMessage("No permission!");
            return CompletableFuture.completedFuture(true);
        }
        if (!isPlayer && !availableForNonPlayers())
        {
            BigDoors.get().getPLogger().logMessage(Level.FINE, () -> "Command not allowed for non-players: " + this);
            getCommandSender().sendMessage("Only players can use this command!");
            return CompletableFuture.completedFuture(true);
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
     * Handles the results of a database action by informing the user of any non-success states.
     *
     * @param result The result obtained from the database.
     * @return True in all cases, as it is assumed that this is not user error.
     */
    protected @NonNull Boolean handleDatabaseActionResult(final @NonNull DatabaseManager.ActionResult result)
    {
        // TODO: Localization
        switch (result)
        {
            case CANCELLED:
                commandSender.sendMessage("Action was cancelled!");
                break;
            case SUCCESS:
                break;
            case FAIL:
                commandSender.sendMessage("An error occurred! Please contact a server administrator.");
                break;
        }
        BigDoors.get().getPLogger().logMessage(Level.FINE,
                                               () -> "Handling database action result: " + result.name() +
                                                   " for command: " + this);
        return true;
    }

    /**
     * Starts the execution of this command. It performs the permission check (See {@link #hasPermission()}) and runs
     * {@link #executeCommand(BooleanPair)} if the {@link ICommandSender} has access to either the user or the admin
     * permission.
     *
     * @return True if the command could be executed successfully or if the command execution failed through no fault of
     * the {@link ICommandSender}.
     */
    protected final @NonNull CompletableFuture<Boolean> startExecution()
    {
        return hasPermission().thenApplyAsync(this::handlePermissionResult);
    }

    private boolean handlePermissionResult(final @NonNull BooleanPair permissionResult)
    {
        if (!permissionResult.first && !permissionResult.second)
        {
            BigDoors.get().getPLogger().logMessage(Level.FINE,
                                                   () -> "Permission for command: " + this + ": " + permissionResult);
            // TODO: Localization
            getCommandSender().sendMessage("No permission!");
            return true;
        }
        try
        {
            return executeCommand(permissionResult).get(30, TimeUnit.MINUTES);
        }
        catch (Throwable t)
        {
            throw new RuntimeException("Encountered issue running command: " + this, t);
        }
    }

    /**
     * Executes the command. This method is only called if {@link #validInput()} and {@link #hasPermission()} are true.
     * <p>
     * Note that this method is called asynchronously.
     *
     * @param permissions Whether the {@link ICommandSender} has user and/or admin permissions respectively.
     * @return True if the method execution was successful.
     */
    protected abstract @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions);

    /**
     * Ensures the command is logged.
     */
    private void log()
    {
        BigDoors.get().getPLogger()
                .dumpStackTrace(Level.FINEST, "Running command " + getCommand().name() + ": " + this);
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
                    BigDoors.get().getPLogger().logMessage(Level.FINE,
                                                           () -> "Retrieved door " + door + " for command: " + this);
                    if (door.isPresent())
                        return door;
                    // TODO: Localization
                    getCommandSender().sendMessage("Could not find the provided door!");
                    return Optional.empty();
                });
    }

    /**
     * Checks if the {@link ICommandSender} has the required permissions to use this command. See {@link
     * CommandDefinition#getUserPermission()} and {@link CommandDefinition#getAdminPermission()}.
     *
     * @return A pair of booleans that indicates whether the user has access to the user and admin permission nodes
     * respectively. For both, true indicates that they do have access to the node and false that they do not.
     */
    protected @NonNull CompletableFuture<BooleanPair> hasPermission()
    {
        return getCommandSender().hasPermission(getCommand());
    }
}
