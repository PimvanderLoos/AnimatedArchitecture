package nl.pim16aap2.animatedarchitecture.core.commands;

import com.google.common.flogger.StackSize;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Locked;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.exceptions.CommandExecutionException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.NoAccessToStructureCommandException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.RequiredPropertiesMissingForCommandException;
import nl.pim16aap2.animatedarchitecture.core.localization.PersonalizedLocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Represents a command that relates to an existing structure.
 */
@Flogger
@ToString(callSuper = true)
public abstract class StructureTargetCommand extends BaseCommand
{
    private final boolean sendUpdatedInfo;

    @ToString.Exclude
    private final @Nullable CommandFactory commandFactory;

    @Getter
    @ToString.Exclude
    protected final StructureRetriever structureRetriever;

    @Getter
    @ToString.Exclude
    protected final StructureAttribute structureAttribute;

    /**
     * The result of the {@link #structureRetriever}.
     * <p>
     * This will not be available until after {@link #executeCommand(PermissionsStatus)} has started, but before
     * {@link #performAction(Structure)} is called.
     * <p>
     * Even after the result has been set, it may still be null in case no doors were found.
     */
    @Getter(onMethod_ = @Locked.Read)
    @Setter(value = AccessLevel.PRIVATE, onMethod_ = @Locked.Write)
    @GuardedBy("$lock")
    private @Nullable Structure retrieverResult;

    @Contract("_, _, _, _, true, null -> fail")
    protected StructureTargetCommand(
        ICommandSender commandSender,
        IExecutor executor,
        StructureRetriever structureRetriever,
        StructureAttribute structureAttribute,
        boolean sendUpdatedInfo,
        @Nullable CommandFactory commandFactory)
    {
        super(commandSender, executor);

        if (sendUpdatedInfo && commandFactory == null)
            throw new IllegalArgumentException("Command factory cannot be null if sendUpdatedInfo is true.");

        this.sendUpdatedInfo = sendUpdatedInfo;
        this.commandFactory = commandFactory;
        this.structureRetriever = structureRetriever;
        this.structureAttribute = structureAttribute;
    }

    protected StructureTargetCommand(
        ICommandSender commandSender,
        IExecutor executor,
        StructureRetriever structureRetriever,
        StructureAttribute structureAttribute)
    {
        this(commandSender, executor, structureRetriever, structureAttribute, false, null);
    }

    @Override
    protected final CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        return getStructure(getStructureRetriever(), getStructureAttribute().getPermissionLevel())
            .thenApply(structure ->
            {
                setRetrieverResult(structure);
                return structure;
            })
            .thenAcceptAsync(
                structure -> processStructureResult(structure, permissions),
                executor.getVirtualExecutor()
            );
    }

    /**
     * Sends the updated info of the structure to the {@link ICommandSender}.
     * <p>
     * Only applies if {@link #sendUpdatedInfo} is true.
     *
     * @param structure
     *     The structure to send the updated info of.
     */
    protected void sendUpdatedInfo(Structure structure)
    {
        if (sendUpdatedInfo)
            Util.requireNonNull(commandFactory, "CommandFactor")
                .newInfo(getCommandSender(), StructureRetrieverFactory.ofStructure(structure)).run().join();
    }

    /**
     * Runs {@link #isAllowed(Structure, boolean)}. If it fails, it will throw a {@link CommandExecutionException}.
     * <p>
     * Any other exceptions will be caught and rethrown as a {@link CommandExecutionException}.
     *
     * @param structure
     *     The structure to check.
     * @param permissions
     *     The permissions of the {@link ICommandSender}.
     * @throws CommandExecutionException
     *     If the {@link ICommandSender} is not allowed to perform the action or if an exception occurs.
     */
    private void checkIsAllowed(Structure structure, PermissionsStatus permissions)
        throws CommandExecutionException
    {
        try
        {
            isAllowed(structure, permissions.hasAdminPermission());
        }
        catch (CommandExecutionException e)
        {
            throw new CommandExecutionException(e.isUserInformed(), e);
        }
        catch (Exception exception)
        {
            getCommandSender().sendError(
                "commands.structure_target_command.base.error.no_permission_for_action",
                arg -> arg.localizedHighlight(structure)
            );

            throw new NoAccessToStructureCommandException(
                true,
                String.format(
                    "CommandSender %s does not have access to structure %s for command %s",
                    getCommandSender(),
                    structure,
                    this),
                exception
            );
        }
    }

    private void checkHasRequiredProperties(Structure structure)
    {
        if (!hasRequiredProperties(structure, getRequiredProperties()))
        {
            notifyMissingProperties(structure);

            throw new RequiredPropertiesMissingForCommandException(
                true,
                String.format(
                    "Structure %s does not have required properties '%s' for command %s",
                    structure,
                    getRequiredProperties(),
                    this)
            );
        }
    }

    /**
     * Handles the result of retrieving the structure.
     *
     * @param structure
     *     The result of trying to retrieve the structure.
     * @param permissions
     *     Whether the ICommandSender has user and/or admin permissions.
     */
    private void processStructureResult(Structure structure, PermissionsStatus permissions)
    {
        checkIsAllowed(structure, permissions);

        checkHasRequiredProperties(structure);

        try
        {
            performAction(structure).get(5, TimeUnit.MINUTES);
        }
        catch (Exception e)
        {
            throw new CommandExecutionException(
                false,
                "Failed to perform command " + this + " for structure " + structure,
                e
            );
        }
    }

    /**
     * Notifies the {@link ICommandSender} that the targeted {@link Structure} is missing required properties.
     *
     * @param structure
     *     The {@link Structure} that is missing the required properties.
     */
    protected void notifyMissingProperties(Structure structure)
    {
        getCommandSender().sendError(
            "commands.structure_target_command.base.error.missing_properties",
            arg -> arg.localizedHighlight(structure),
            arg -> arg.highlight(getRequiredProperties())
        );
    }

    /**
     * Checks if execution of this command is allowed for the given {@link Structure}.
     * <p>
     * If it is not allowed, an exception will be thrown.
     *
     * @param structure
     *     The {@link Structure} that is the target for this command.
     * @param bypassPermission
     *     Whether the {@link ICommandSender} has bypass access.
     * @throws CommandExecutionException
     *     If the {@link ICommandSender} is not allowed to perform the action on the {@link Structure}.
     */
    protected void isAllowed(Structure structure, boolean bypassPermission)
        throws CommandExecutionException
    {
        if (!hasAccessToAttribute(structure, structureAttribute, bypassPermission))
            throw new NoAccessToStructureCommandException(false);
    }

    /**
     * Checks if the {@link Structure} has all the required properties.
     * <p>
     * See {@link Structure#hasProperties(Collection)}.
     *
     * @param structure
     *     The {@link Structure} to check.
     * @param requiredProperties
     *     The required properties.
     * @return True if the {@link Structure} has all the required properties.
     */
    protected boolean hasRequiredProperties(Structure structure, List<Property<?>> requiredProperties)
    {
        return requiredProperties.isEmpty() || structure.hasProperties(getRequiredProperties());
    }

    /**
     * Performs the action of this command on the {@link Structure}.
     *
     * @param structure
     *     The {@link Structure} to perform the action on.
     * @return The future of the command execution.
     */
    protected abstract CompletableFuture<?> performAction(Structure structure);

    /**
     * @return The structure description of the {@link #retrieverResult}.
     */
    protected final StructureDescription getRetrievedStructureDescription(@Nullable Structure retrieverResult)
    {
        return StructureDescription.of(getCommandSender().getPersonalizedLocalizer(), retrieverResult);
    }

    /**
     * Gets all required properties for this command.
     * <p>
     * If targeted structure does not have all required properties, the command will not be executed.
     * <p>
     * By default, this returns an empty list. Subclasses that require specific properties should override this method.
     *
     * @return A list of all required properties.
     */
    protected List<Property<?>> getRequiredProperties()
    {
        return Collections.emptyList();
    }

    /**
     * Called by {@link #handleDatabaseActionResult(DatabaseManager.ActionResult, Structure)} when the database action
     * was cancelled.
     */
    protected void handleDatabaseActionCancelled(@Nullable Structure retrieverResult)
    {
        getCommandSender().sendError("commands.base.error.action_cancelled");
    }

    /**
     * Called by {@link #handleDatabaseActionResult(DatabaseManager.ActionResult, Structure)} when the database action
     * was successful.
     */
    protected void handleDatabaseActionSuccess(@Nullable Structure retrieverResult)
    {
    }

    /**
     * Called by {@link #handleDatabaseActionResult(DatabaseManager.ActionResult, Structure)} when the database action
     * failed.
     */
    protected void handleDatabaseActionFail(@Nullable Structure retrieverResult)
    {
        getCommandSender().sendError("constants.error.generic");
    }

    /**
     * Handles the results of a database action by informing the user of any non-success states.
     * <p>
     * To customize the handling, you can override {@link #handleDatabaseActionFail(Structure)},
     * {@link #handleDatabaseActionCancelled(Structure)}, or {@link #handleDatabaseActionSuccess(Structure)}.
     *
     * @param result
     *     The result obtained from the database.
     * @param structure
     *     The structure that was the target of the command.
     */
    protected final void handleDatabaseActionResult(DatabaseManager.ActionResult result, Structure structure)
    {
        log.atFine().log("Handling database action result: %s for command: %s", result.name(), this);
        switch (result)
        {
            case CANCELLED -> handleDatabaseActionCancelled(structure);
            case SUCCESS -> handleDatabaseActionSuccess(structure);
            case FAIL -> handleDatabaseActionFail(structure);
        }
    }

    /**
     * A simple description of a structure.
     *
     * @param localizedTypeName
     *     The localized name of the structure's type.
     * @param id
     *     The user-friendly identifier of the structure.
     */
    protected record StructureDescription(String localizedTypeName, String id)
    {
        private static final StructureDescription EMPTY_DESCRIPTION = new StructureDescription("Structure", "null");

        private static StructureDescription of(PersonalizedLocalizer localizer, @Nullable Structure structure)
        {
            if (structure != null)
                return new StructureDescription(
                    localizer.getMessage(structure.getType().getLocalizationKey()),
                    structure.getName() + " (" + structure.getUid() + ")"
                );

            log.atSevere().withStackTrace(StackSize.FULL).log("Structure not available after database action!");
            return EMPTY_DESCRIPTION;
        }
    }
}
