package nl.pim16aap2.bigdoors.commands;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.delayedinput.DelayedInputRequest;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Represents the base BigDoors command.
 * <p>
 * This handles all the basics shared by all commands and contains some utility methods for common actions.
 *
 * @author Pim
 */
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
            // TODO: Localization
            getCommandSender().sendMessage("No permission!");
            return CompletableFuture.completedFuture(true);
        }
        if (!isPlayer && !availableForNonPlayers())
        {
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
            case SUCCESS:
                break;
            case FAIL:
                commandSender.sendMessage("An error occurred! Please contact a server administrator.");
        }
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

    /**
     * Represents a request for delayed additional input for a command.
     * <p>
     * Taking the {@link AddOwner} command as an example, it could be initialized using a GUI, in which case it is known
     * who the {@link ICommandSender} is and what the target {@link AbstractDoorBase} is. However, for some GUIs (e.g.
     * Spigot), it is not yet known who the target player is and what the desired permission level is. This class can
     * then be used to retrieve the additional data that is required to execute the command.
     *
     * @param <T> The type of data that is to be retrieved from the player.
     */
    public static final class DelayedCommandInputRequest<T> extends DelayedInputRequest<T>
    {
        /**
         * The function to execute after retrieving the delayed input from the command sender.
         */
        private final @NonNull Function<T, CompletableFuture<Boolean>> executor;

        /**
         * See {@link BaseCommand#commandSender}.
         */
        private final @NonNull ICommandSender commandSender;

        /**
         * The {@link CommandDefinition} for which the delayed input will be retrieved.
         */
        private final @NonNull CommandDefinition commandDefinition;

        /**
         * The supplier used to retrieve the message that will be sent to the command sender when this request is
         * initialized (after calling {@link #run()}).
         * <p>
         * If the resulting message is blank, nothing will be sent to the user.
         */
        private final @NonNull Supplier<String> initMessageSupplier;

        /**
         * Constructs a new delayed command input request.
         *
         * @param timeout             The amount of time (in ms)
         * @param commandSender       See {@link BaseCommand#commandSender}.
         * @param commandDefinition   The {@link CommandDefinition} for which the delayed input will be retrieved.
         * @param executor            The function to execute after retrieving the delayed input from the command
         *                            sender.
         * @param initMessageSupplier The supplier used to retrieve the message that will be sent to the commandsender
         *                            when this request is initialized (after calling {@link #run()}).
         *                            <p>
         *                            If the resulting message is blank, nothing will be sent to the user.
         */
        protected DelayedCommandInputRequest(final long timeout, final @NonNull ICommandSender commandSender,
                                             final @NonNull CommandDefinition commandDefinition,
                                             final @NonNull Function<T, CompletableFuture<Boolean>> executor,
                                             final @NonNull Supplier<String> initMessageSupplier)
        {
            super(timeout);
            this.commandSender = commandSender;
            this.commandDefinition = commandDefinition;
            this.executor = executor;
            this.initMessageSupplier = initMessageSupplier;
        }

        @Override
        protected void cleanup()
        {
            if (getStatus() == Status.TIMED_OUT)
                // TODO: Localization
                commandSender.sendMessage("Timed out waiting for input for command: " +
                                              commandDefinition.name().toLowerCase());
            if (getStatus() == Status.CANCELLED)
                // TODO: Localization
                commandSender.sendMessage("Cancelled waiting for command:  " +
                                              commandDefinition.name().toLowerCase());
        }

        /**
         * Wrapper for {@link DelayedInputRequest#waitForInput()} that rethrows any checked exceptions that may occur as
         * {@link RuntimeException}s (so, unchecked).
         *
         * @return The result of {@link DelayedInputRequest#waitForInput()}.
         */
        private @NonNull Optional<T> waitForInputUnchecked()
        {
            try
            {
                return super.waitForInput();
            }
            catch (Exception e)
            {
                throw new RuntimeException("Failed to retrieve data for command: " + commandDefinition, e);
            }
        }

        /**
         * Ensures the input request is logged.
         */
        private void log()
        {
            BigDoors.get().getPLogger()
                    .dumpStackTrace(Level.FINEST,
                                    "Started delayed input request for command: " + commandDefinition.name());
        }

        /**
         * Attempts to retrieve the delayed input from the {@link #commandSender} and applies the obtained output to the
         * {@link #executor} if retrieval was successful.
         *
         * @return The result of {@link #executor}.
         */
        protected final @NonNull CompletableFuture<Boolean> run()
        {
            log();
            return CompletableFuture
                .supplyAsync(this::waitForInputUnchecked)
                .thenCompose(input -> input.map(executor).orElse(CompletableFuture.completedFuture(Boolean.FALSE)))
                .exceptionally(ex -> Util.exceptionally(ex, Boolean.FALSE));
        }

        @Override
        protected void init()
        {
            BigDoors.get().getDelayedCommandInputManager().register(commandSender, this);
            val initMessage = initMessageSupplier.get();
            if (initMessage != null && !initMessage.isBlank())
                commandSender.sendMessage(initMessage);
        }
    }
}
