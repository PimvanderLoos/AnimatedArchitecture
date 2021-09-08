package nl.pim16aap2.bigdoors.commands;

import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
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
public abstract class BaseCommand
{
    /**
     * The entity (e.g. player, server, or command block) that initiated the command.
     * <p>
     * This is the entity that is held responsible for the command (i.e. their permissions are checked and they will
     * receive error/success/information messages when applicable).
     */
    @Getter
    private final ICommandSender commandSender;

    protected final CommandContext context;
    protected final IPLogger logger;
    protected final ILocalizer localizer;

    public BaseCommand(ICommandSender commandSender, CommandContext context)
    {
        this.commandSender = commandSender;
        this.context = context;
        logger = context.getLogger();
        localizer = context.getLocalizer();
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
        if (hasBypassPermission || !getCommandSender().isPlayer())
            return true;

        return getCommandSender().getPlayer()
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
    protected final CompletableFuture<Boolean> run()
    {
        log();
        if (!validInput())
        {
            logger.logMessage(Level.FINE, () -> "Invalid input for command: " + this);
            return CompletableFuture.completedFuture(false);
        }

        final boolean isPlayer = getCommandSender() instanceof IPPlayer;
        if (isPlayer && !availableForPlayers())
        {
            logger.logMessage(Level.FINE, () -> "Command not allowed for players: " + this);
            getCommandSender().sendMessage(localizer.getMessage("commands.base.error.no_permission_for_command"));
            return CompletableFuture.completedFuture(true);
        }
        if (!isPlayer && !availableForNonPlayers())
        {
            logger.logMessage(Level.FINE, () -> "Command not allowed for non-players: " + this);
            getCommandSender().sendMessage(localizer.getMessage("commands.base.error.only_available_for_players"));
            return CompletableFuture.completedFuture(true);
        }

        // We return true in case of an exception, because it cannot (should not?) be possible
        // for an exception to be caused be the command sender.
        return startExecution().exceptionally(
            throwable ->
            {
                logger.logThrowable(throwable, "Failed to execute command: " + this);
                if (getCommandSender().isPlayer())
                    getCommandSender().sendMessage(localizer.getMessage("commands.base.error.generic"));
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
        logger.logMessage(Level.FINE,
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
    protected final CompletableFuture<Boolean> startExecution()
    {
        return hasPermission().thenApplyAsync(this::handlePermissionResult);
    }

    private boolean handlePermissionResult(BooleanPair permissionResult)
    {
        if (!permissionResult.first && !permissionResult.second)
        {
            logger.logMessage(Level.FINE,
                              () -> "Permission for command: " + this + ": " + permissionResult);
            getCommandSender().sendMessage(localizer
                                               .getMessage("commands.base.error.no_permission_for_command"));
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
     *     Whether the {@link ICommandSender} has user and/or admin permissions respectively.
     * @return True if the method execution was successful.
     */
    protected abstract CompletableFuture<Boolean> executeCommand(BooleanPair permissions);

    /**
     * Ensures the command is logged.
     */
    private void log()
    {
        logger
            .dumpStackTrace(Level.FINEST, "Running command " + getCommand().name() + ": " + this);
    }

    /**
     * Attempts to get an {@link DoorBase} based on the provided {@link DoorRetriever} and the current {@link
     * ICommandSender}.
     * <p>
     * If no door is found, the {@link ICommandSender} will be informed.
     *
     * @param doorRetriever
     *     The {@link DoorRetriever} to use
     * @return The {@link DoorBase} if one could be retrieved.
     */
    protected CompletableFuture<Optional<AbstractDoor>> getDoor(DoorRetriever doorRetriever)
    {
        return getCommandSender().getPlayer().map(doorRetriever::getDoorInteractive)
                                 .orElseGet(doorRetriever::getDoor).thenApplyAsync(
                door ->
                {
                    logger.logMessage(Level.FINE,
                                      () -> "Retrieved door " + door + " for command: " + this);
                    if (door.isPresent())
                        return door;
                    getCommandSender().sendMessage(
                        localizer.getMessage("commands.base.error.cannot_find_target_door"));
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
    protected CompletableFuture<BooleanPair> hasPermission()
    {
        return getCommandSender().hasPermission(getCommand());
    }
}
