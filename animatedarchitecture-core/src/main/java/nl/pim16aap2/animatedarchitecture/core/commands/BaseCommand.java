package nl.pim16aap2.animatedarchitecture.core.commands;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.exceptions.CommandExecutionException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.NoStructuresForCommandException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.NonPlayerExecutingPlayerCommandException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.PlayerExecutingNonPlayerCommandException;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Represents the base AnimatedArchitecture command.
 * <p>
 * This handles all the basics shared by all commands and contains some utility methods for common actions.
 */
@ToString
@Flogger
@ExtensionMethod(CompletableFutureExtensions.class)
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

    @ToString.Exclude
    protected final ILocalizer localizer;

    @ToString.Exclude
    protected final ITextFactory textFactory;

    @ToString.Exclude
    protected final IExecutor executor;

    protected BaseCommand(
        ICommandSender commandSender,
        IExecutor executor,
        ILocalizer localizer,
        ITextFactory textFactory)
    {
        this.commandSender = commandSender;
        this.executor = executor;
        this.localizer = localizer;
        this.textFactory = textFactory;
    }

    /**
     * Gets the {@link CommandDefinition} that contains the definition of this {@link BaseCommand}.
     *
     * @return The {@link CommandDefinition} that contains the definition of this {@link BaseCommand}.
     */
    public abstract CommandDefinition getCommand();

    /**
     * Checks if the input is valid before starting any potentially expensive tasks.
     */
    protected void validateInput()
    {
    }

    /**
     * Checks if the {@link #commandSender} has access to a given {@link StructureAttribute} for a given structure.
     *
     * @param structure
     *     The structure to check.
     * @param structureAttribute
     *     The {@link StructureAttribute} to check.
     * @param hasBypassPermission
     *     Whether the {@link #commandSender} has bypass permission or not.
     * @return True if the command sender has access to the provided attribute for the given structure.
     */
    protected boolean hasAccessToAttribute(
        Structure structure,
        StructureAttribute structureAttribute,
        boolean hasBypassPermission)
    {
        if (hasBypassPermission || !commandSender.isPlayer())
            return true;

        return commandSender.getPlayer()
            .flatMap(structure::getOwner)
            .map(doorOwner -> structureAttribute.canAccessWith(doorOwner.permission()))
            .orElse(false);
    }

    /**
     * Checks if this {@link BaseCommand} is available for {@link IPlayer}s.
     *
     * @return True if an {@link IPlayer} can execute this command.
     */
    protected boolean availableForPlayers()
    {
        return true;
    }

    /**
     * Checks if this {@link BaseCommand} is available for non-{@link IPlayer}s (e.g. the server).
     *
     * @return True if a non-{@link IPlayer} can execute this command.
     */
    protected boolean availableForNonPlayers()
    {
        return true;
    }

    /**
     * Handles an exception that occurred while running {@link #runWithRawResult()}.
     *
     * @param throwable
     *     The exception that occurred.
     * @param <T>
     *     The type of the return value.
     * @return null
     */
    @VisibleForTesting
    final @Nullable <T> T handleRunException(Throwable throwable)
    {
        final boolean shouldInformUser = shouldInformUser(throwable);

        // If the user has not been informed about the issue, it's a severe issue.
        final Level level;
        if (shouldInformUser)
            level = Level.SEVERE;
        else
            level = Level.FINE;

        log.at(level).withCause(throwable).log("Failed to execute command: %s", this);
        if (shouldInformUser && commandSender.isPlayer())
            sendGenericErrorMessage();

        return null;
    }

    /**
     * Executes the command.
     *
     * @return The result of the execution.
     */
    public final CompletableFuture<?> run()
    {
        return runWithRawResult().exceptionally(this::handleRunException);
    }

    /**
     * Checks if the user should be informed about the exception.
     *
     * @param throwable
     *     The exception to check.
     * @return True if the user should be informed about the exception.
     */
    static boolean shouldInformUser(Throwable throwable)
    {
        final Throwable rootCause = Util.getRootCause(throwable);
        if (rootCause instanceof CommandExecutionException commandExecutionException)
        {
            return !commandExecutionException.isUserInformed();
        }

        return true;
    }

    /**
     * Executes the command with a timeout.
     * <p>
     * This method does not handle exceptions. This is useful if you want to handle exceptions yourself.
     *
     * @param timeout
     *     The timeout.
     * @param timeUnit
     *     The time unit of the timeout.
     * @return The result of the execution.
     */
    public final CompletableFuture<?> runWithRawResult(int timeout, TimeUnit timeUnit)
    {
        return runWithRawResult().orTimeout(timeout, timeUnit);
    }

    /**
     * Executes the command.
     * <p>
     * This method is the same as {@link #run()}, but it does not handle exceptions. This is useful if you want to
     * handle exceptions yourself.
     *
     * @return The result of the execution.
     */
    @SuppressWarnings("FutureReturnValueIgnored") // It's safe to ignore the result of the last whenComplete here.
    public final CompletableFuture<?> runWithRawResult()
    {
        final CompletableFuture<?> ret = new CompletableFuture<>();

        CompletableFuture
            .completedFuture(null)
            .thenComposeAsync(__ -> this.runWithRawResult0(), executor.getVirtualExecutor())
            .whenComplete((val, throwable) ->
            {
                if (throwable == null)
                {
                    ret.complete(null);
                    return;
                }

                log.atFiner().withCause(throwable).log("Failed to execute command: %s", this);
                ret.completeExceptionally(new RuntimeException(throwable));
            });

        return ret;
    }

    private CompletableFuture<?> runWithRawResult0()
    {
        log();

        validateInput();

        final Supplier<String> exceptionContext = () -> String.format("Execute command: %s", this);

        final boolean isPlayer = commandSender instanceof IPlayer;
        if (isPlayer && !availableForPlayers())
        {
            log.atFine().log("Command not allowed for players: %s", this);
            commandSender.sendMessage(
                textFactory,
                TextType.ERROR,
                localizer.getMessage("commands.base.error.no_permission_for_command")
            );
            return CompletableFuture
                .failedFuture(new PlayerExecutingNonPlayerCommandException(true))
                .withExceptionContext(exceptionContext);
        }
        if (!isPlayer && !availableForNonPlayers())
        {
            log.atFine().log("Command not allowed for non-players: %s", this);
            commandSender.sendError(
                textFactory,
                localizer.getMessage("commands.base.error.only_available_for_players")
            );
            return CompletableFuture
                .failedFuture(new NonPlayerExecutingPlayerCommandException(true))
                .withExceptionContext(exceptionContext);
        }

        return startExecution()
            .withExceptionContext(exceptionContext);
    }

    protected final void sendGenericErrorMessage()
    {
        commandSender.sendError(
            textFactory,
            localizer.getMessage("commands.base.error.generic")
        );
    }

    /**
     * Starts the execution of this command. It performs the permission check (See {@link #hasPermission()}) and runs
     * {@link #executeCommand(PermissionsStatus)} if the {@link ICommandSender} has access to either the user or the
     * admin permission.
     */
    protected final CompletableFuture<?> startExecution()
    {
        return hasPermission().thenAcceptAsync(this::handlePermissionResult, executor.getVirtualExecutor());
    }

    @VisibleForTesting
    final void handlePermissionResult(PermissionsStatus permissionResult)
    {
        if (!permissionResult.hasAnyPermission())
        {
            log.atFine().log("Permission for command: %s: %s", this, permissionResult);
            commandSender.sendError(
                textFactory,
                localizer.getMessage("commands.base.error.no_permission_for_command")
            );
            throw new CommandExecutionException(
                true,
                String.format("CommandSender %s does not have permission to run command: %s", commandSender, this)
            );
        }
        try
        {
            executeCommand(permissionResult).get(30, java.util.concurrent.TimeUnit.MINUTES);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Encountered issue running command: " + this, e);
        }
    }

    /**
     * Executes the command. This method is only called if {@link #validateInput()} and {@link #hasPermission()} are
     * true.
     * <p>
     * Note that this method is called asynchronously.
     *
     * @param permissions
     *     Whether the {@link ICommandSender} has user and/or admin permissions.
     * @return The future of the command execution.
     */
    protected abstract CompletableFuture<?> executeCommand(PermissionsStatus permissions);

    /**
     * Ensures the command is logged.
     */
    private void log()
    {
        log.atFinest().log("Running command %s: %s", getCommand().getName(), this);
    }

    /**
     * Attempts to get an {@link Structure} based on the provided {@link StructureRetrieverFactory} and the current
     * {@link ICommandSender}.
     * <p>
     * If no structure is found, the {@link ICommandSender} will be informed.
     *
     * @param doorRetriever
     *     The {@link StructureRetrieverFactory} to use
     * @param permissionLevel
     *     The minimum {@link PermissionLevel} required to retrieve the structure.
     * @return The {@link Structure} if one could be retrieved.
     */
    protected CompletableFuture<Structure> getStructure(
        StructureRetriever doorRetriever,
        PermissionLevel permissionLevel)
    {
        return commandSender
            .getPlayer()
            .map(player -> doorRetriever.getStructureInteractive(player, permissionLevel))
            .orElseGet(doorRetriever::getStructure)
            .withExceptionContext(() -> String.format(
                "Get structure from retriever '%s' with permission level '%s' for command: %s",
                doorRetriever,
                permissionLevel,
                this
            ))
            .thenApply(structure ->
            {
                log.atFine().log("Retrieved structure %s for command: %s", structure, this);
                if (structure.isPresent())
                    return structure.get();

                commandSender.sendError(
                    textFactory,
                    localizer.getMessage("commands.base.error.cannot_find_target_structure")
                );
                throw new NoStructuresForCommandException(true);
            });
    }

    /**
     * Checks if the {@link ICommandSender} has the required permissions to use this command. See
     * {@link CommandDefinition#getUserPermission()} and {@link CommandDefinition#getAdminPermission()}.
     *
     * @return A pair of booleans that indicates whether the user has access to the user and admin permission nodes
     * respectively. For both, true indicates that they do have access to the node and false that they do not.
     */
    protected CompletableFuture<PermissionsStatus> hasPermission()
    {
        return commandSender.hasPermission(getCommand());
    }
}
