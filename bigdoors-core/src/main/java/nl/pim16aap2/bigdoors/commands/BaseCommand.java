package nl.pim16aap2.bigdoors.commands;

import com.google.common.flogger.StackSize;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;

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
@Flogger
public abstract class BaseCommand
{
    /**
     * The entity (e.g. player, server, or command block) that initiated the command.
     * <p>
     * This is the entity that is held responsible for the command (i.e. their permissions are checked, and they will
     * receive error/success/information messages when applicable).
     */
    @Getter
    private final ICommandSender commandSender;

    protected final ILocalizer localizer;

    public BaseCommand(ICommandSender commandSender, ILocalizer localizer)
    {
        this.commandSender = commandSender;
        this.localizer = localizer;
    }

    /**
     * Gets the {@link CommandDefinition} that contains the definition of this {@link BaseCommand}.
     *
     * @return The {@link CommandDefinition} that contains the definition of this {@link BaseCommand}.
     */
    public abstract CommandDefinition getCommand();

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
     * @param door
     *     The door to check.
     * @param doorAttribute
     *     The {@link DoorAttribute} to check.
     * @param hasBypassPermission
     *     Whether the {@link #commandSender} has bypass permission or not.
     * @return True if the command sender has access to the provided attribute for the given door.
     */
    protected boolean hasAccessToAttribute(AbstractDoor door, DoorAttribute doorAttribute, boolean hasBypassPermission)
    {
        if (hasBypassPermission || !commandSender.isPlayer())
            return true;

        return commandSender.getPlayer()
                            .flatMap(door::getDoorOwner)
                            .map(doorOwner -> doorOwner.permission() <= doorAttribute.getPermissionLevel())
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
     * @return True if a non-{@link IPPlayer} can execute this command.
     */
    protected boolean availableForNonPlayers()
    {
        return true;
    }

    /**
     * Creates (but does not execute!) a new command if certain criteria are met (i.e. the {@link ICommandSender} has
     * access and {@link #validInput()} returns true).
     *
     * @return True if the command could be executed successfully or if the command execution failed through no fault of
     * the {@link ICommandSender}.
     */
    protected final CompletableFuture<Boolean> run()
    {
        log();
        if (!validInput())
        {
            log.at(Level.FINE).log("Invalid input for command: %s", this);
            return CompletableFuture.completedFuture(false);
        }

        final boolean isPlayer = commandSender instanceof IPPlayer;
        if (isPlayer && !availableForPlayers())
        {
            log.at(Level.FINE).log("Command not allowed for players: %s", this);
            commandSender.sendMessage(localizer.getMessage("commands.base.error.no_permission_for_command"));
            return CompletableFuture.completedFuture(true);
        }
        if (!isPlayer && !availableForNonPlayers())
        {
            log.at(Level.FINE).log("Command not allowed for non-players: %s", this);
            commandSender.sendMessage(localizer.getMessage("commands.base.error.only_available_for_players"));
            return CompletableFuture.completedFuture(true);
        }

        // We return true in case of an exception, because it cannot (should not?) be possible
        // for an exception to be caused be the command sender.
        return startExecution().exceptionally(
            throwable ->
            {
                log.at(Level.SEVERE).withCause(throwable).log("Failed to execute command: %s", this);
                if (commandSender.isPlayer())
                    commandSender.sendMessage(localizer.getMessage("commands.base.error.generic"));
                return true;
            });
    }

    /**
     * Handles the results of a database action by informing the user of any non-success states.
     *
     * @param result
     *     The result obtained from the database.
     * @return True in all cases, as it is assumed that this is not user error.
     */
    protected Boolean handleDatabaseActionResult(DatabaseManager.ActionResult result)
    {
        switch (result)
        {
            case CANCELLED:
                commandSender.sendMessage(localizer.getMessage("commands.base.error.action_cancelled"));
                break;
            case SUCCESS:
                break;
            case FAIL:
                commandSender.sendMessage(localizer.getMessage("constants.error.generic"));
                break;
        }
        log.at(Level.FINE).log("Handling database action result: %s for command: %s", result.name(), this);
        return true;
    }

    /**
     * Starts the execution of this command. It performs the permission check (See {@link #hasPermission()}) and runs
     * {@link #executeCommand(PermissionsStatus)} if the {@link ICommandSender} has access to either the user or the
     * admin permission.
     *
     * @return True if the command could be executed successfully or if the command execution failed through no fault of
     * the {@link ICommandSender}.
     */
    protected final CompletableFuture<Boolean> startExecution()
    {
        return hasPermission().thenApplyAsync(this::handlePermissionResult);
    }

    private boolean handlePermissionResult(PermissionsStatus permissionResult)
    {
        if (!permissionResult.hasAnyPermission())
        {
            log.at(Level.FINE).log("Permission for command: %s: %s", this, permissionResult);
            commandSender.sendMessage(localizer.getMessage("commands.base.error.no_permission_for_command"));
            return true;
        }
        try
        {
            return executeCommand(permissionResult).get(30, TimeUnit.MINUTES);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Encountered issue running command: " + this, e);
        }
    }

    /**
     * Executes the command. This method is only called if {@link #validInput()} and {@link #hasPermission()} are true.
     * <p>
     * Note that this method is called asynchronously.
     *
     * @param permissions
     *     Whether the {@link ICommandSender} has user and/or admin permissions.
     * @return True if the method execution was successful.
     */
    protected abstract CompletableFuture<Boolean> executeCommand(PermissionsStatus permissions);

    /**
     * Ensures the command is logged.
     */
    private void log()
    {
        log.at(Level.FINEST).withStackTrace(StackSize.FULL).log("Running command %s: %s", getCommand().getName(), this);
    }

    /**
     * Attempts to get an {@link DoorBase} based on the provided {@link DoorRetrieverFactory} and the current {@link
     * ICommandSender}.
     * <p>
     * If no door is found, the {@link ICommandSender} will be informed.
     *
     * @param doorRetriever
     *     The {@link DoorRetrieverFactory} to use
     * @return The {@link DoorBase} if one could be retrieved.
     */
    protected CompletableFuture<Optional<AbstractDoor>> getDoor(DoorRetriever doorRetriever)
    {
        return commandSender.getPlayer().map(doorRetriever::getDoorInteractive)
                            .orElseGet(doorRetriever::getDoor).thenApplyAsync(
                door ->
                {
                    log.at(Level.FINE).log("Retrieved door " + door + " for command: %s", this);
                    if (door.isPresent())
                        return door;
                    commandSender.sendMessage(localizer.getMessage("commands.base.error.cannot_find_target_door"));
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
    protected CompletableFuture<PermissionsStatus> hasPermission()
    {
        return commandSender.hasPermission(getCommand());
    }
}
