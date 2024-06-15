package nl.pim16aap2.animatedarchitecture.core.commands;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Represents the base AnimatedArchitecture command.
 * <p>
 * This handles all the basics shared by all commands and contains some utility methods for common actions.
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
    protected final ITextFactory textFactory;

    public BaseCommand(ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory)
    {
        this.commandSender = commandSender;
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
     *
     * @return True if the input is valid.
     */
    protected boolean validInput()
    {
        return true;
    }

    /**
     * Checks if the {@link #commandSender} has access to a given {@link StructureAttribute} for a given structure.
     *
     * @param door
     *     The structure to check.
     * @param doorAttribute
     *     The {@link StructureAttribute} to check.
     * @param hasBypassPermission
     *     Whether the {@link #commandSender} has bypass permission or not.
     * @return True if the command sender has access to the provided attribute for the given structure.
     */
    protected boolean hasAccessToAttribute(
        AbstractStructure door,
        StructureAttribute doorAttribute,
        boolean hasBypassPermission)
    {
        if (hasBypassPermission || !commandSender.isPlayer())
            return true;

        return commandSender.getPlayer()
            .flatMap(door::getOwner)
            .map(doorOwner -> doorAttribute.canAccessWith(doorOwner.permission()))
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
     * Executes the command.
     *
     * @return The result of the execution.
     */
    public final CompletableFuture<?> run()
    {
        log();
        if (!validInput())
        {
            log.atFine().log("Invalid input for command: %s", this);
            return CompletableFuture.completedFuture(null);
        }

        final boolean isPlayer = commandSender instanceof IPlayer;
        if (isPlayer && !availableForPlayers())
        {
            log.atFine().log("Command not allowed for players: %s", this);
            commandSender.sendMessage(
                textFactory,
                TextType.ERROR,
                localizer.getMessage("commands.base.error.no_permission_for_command")
            );
            return CompletableFuture.completedFuture(null);
        }
        if (!isPlayer && !availableForNonPlayers())
        {
            log.atFine().log("Command not allowed for non-players: %s", this);
            commandSender.sendMessage(
                textFactory,
                TextType.ERROR,
                localizer.getMessage("commands.base.error.only_available_for_players")
            );
            return CompletableFuture.completedFuture(null);
        }

        return startExecution().exceptionally(throwable ->
        {
            log.atSevere().withCause(throwable).log("Failed to execute command: %s", this);
            if (commandSender.isPlayer())
                commandSender.sendMessage(
                    textFactory,
                    TextType.ERROR,
                    localizer.getMessage("commands.base.error.generic")
                );
            return null;
        });
    }

    /**
     * Starts the execution of this command. It performs the permission check (See {@link #hasPermission()}) and runs
     * {@link #executeCommand(PermissionsStatus)} if the {@link ICommandSender} has access to either the user or the
     * admin permission.
     */
    protected final CompletableFuture<?> startExecution()
    {
        return hasPermission().thenAcceptAsync(this::handlePermissionResult);
    }

    private void handlePermissionResult(PermissionsStatus permissionResult)
    {
        if (!permissionResult.hasAnyPermission())
        {
            log.atFine().log("Permission for command: %s: %s", this, permissionResult);
            commandSender.sendMessage(
                textFactory,
                TextType.ERROR,
                localizer.getMessage("commands.base.error.no_permission_for_command")
            );
            return;
        }
        try
        {
            executeCommand(permissionResult).get(30, TimeUnit.MINUTES);
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
     * Attempts to get an {@link AbstractStructure} based on the provided {@link StructureRetrieverFactory} and the
     * current {@link ICommandSender}.
     * <p>
     * If no structure is found, the {@link ICommandSender} will be informed.
     *
     * @param doorRetriever
     *     The {@link StructureRetrieverFactory} to use
     * @param permissionLevel
     *     The minimum {@link PermissionLevel} required to retrieve the structure.
     * @return The {@link AbstractStructure} if one could be retrieved.
     */
    protected CompletableFuture<Optional<AbstractStructure>> getStructure(
        StructureRetriever doorRetriever,
        PermissionLevel permissionLevel)
    {
        return commandSender
            .getPlayer()
            .map(player -> doorRetriever.getStructureInteractive(player, permissionLevel))
            .orElseGet(doorRetriever::getStructure)
            .thenApplyAsync(structure ->
            {
                log.atFine().log("Retrieved structure %s for command: %s", structure, this);
                if (structure.isPresent())
                    return structure;

                commandSender.sendMessage(
                    textFactory,
                    TextType.ERROR,
                    localizer.getMessage("commands.base.error.cannot_find_target_structure")
                );
                return Optional.empty();
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
