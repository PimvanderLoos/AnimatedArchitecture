package nl.pim16aap2.animatedarchitecture.core.commands;

import com.google.common.flogger.StackSize;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.Getter;
import lombok.Locked;
import lombok.Setter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.FutureUtil;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Represents a command that relates to an existing structure.
 */
@Flogger
public abstract class StructureTargetCommand extends BaseCommand
{
    private final boolean sendUpdatedInfo;

    private final @Nullable CommandFactory commandFactory;

    @Getter
    protected final StructureRetriever structureRetriever;

    @Getter
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
    @Setter(onMethod_ = @Locked.Write)
    @GuardedBy("$lock")
    private @Nullable Structure retrieverResult;

    @Contract("_, _, _, _, _, true, null -> fail")
    protected StructureTargetCommand(
        ICommandSender commandSender,
        ILocalizer localizer,
        ITextFactory textFactory,
        StructureRetriever structureRetriever,
        StructureAttribute structureAttribute,
        boolean sendUpdatedInfo,
        @Nullable CommandFactory commandFactory)
    {
        super(commandSender, localizer, textFactory);

        if (sendUpdatedInfo && commandFactory == null)
            throw new IllegalArgumentException("Command factory cannot be null if sendUpdatedInfo is true.");

        this.sendUpdatedInfo = sendUpdatedInfo;
        this.commandFactory = commandFactory;
        this.structureRetriever = structureRetriever;
        this.structureAttribute = structureAttribute;
    }

    protected StructureTargetCommand(
        ICommandSender commandSender,
        ILocalizer localizer,
        ITextFactory textFactory,
        StructureRetriever structureRetriever,
        StructureAttribute structureAttribute)
    {
        this(commandSender, localizer, textFactory, structureRetriever, structureAttribute, false, null);
    }

    @Override
    protected final CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        return getStructure(getStructureRetriever(), getStructureAttribute().getPermissionLevel())
            .thenApply(structure ->
            {
                setRetrieverResult(structure.orElse(null));
                return structure;
            })
            .thenAcceptAsync(structure -> processStructureResult(structure, permissions))
            .exceptionally(FutureUtil::exceptionally);
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
     * Handles the result of retrieving the structure.
     *
     * @param structure
     *     The result of trying to retrieve the structure.
     * @param permissions
     *     Whether the ICommandSender has user and/or admin permissions.
     */
    private void processStructureResult(Optional<Structure> structure, PermissionsStatus permissions)
    {
        if (structure.isEmpty())
        {
            log.atFine().log("Failed to find structure %s for command: %s", getStructureRetriever(), this);

            getCommandSender().sendError(
                textFactory,
                localizer.getMessage("commands.structure_target_command.base.error.structure_not_found")
            );
            return;
        }

        if (!isAllowed(structure.get(), permissions.hasAdminPermission()))
        {
            log.atFine().log(
                "%s does not have access to structure %s for command %s", getCommandSender(), structure, this);

            getCommandSender().sendMessage(textFactory.newText().append(
                localizer.getMessage("commands.structure_target_command.base.error.no_permission_for_action"),
                TextType.ERROR,
                arg -> arg.highlight(localizer.getStructureType(structure.get())))
            );
            return;
        }

        if (!hasRequiredProperties(structure.get(), getRequiredProperties()))
        {
            log.atFine().log(
                "Structure %s does not have required properties '%s' for command %s",
                structure.get(),
                getRequiredProperties(),
                this
            );
            notifyMissingProperties(structure.get());
            return;
        }

        try
        {
            performAction(structure.get()).get(5, TimeUnit.MINUTES);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to perform command " + this + " for structure " + structure, e);
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

        getCommandSender().sendMessage(textFactory.newText().append(
                localizer.getMessage("commands.structure_target_command.base.error.missing_properties"),
                TextType.ERROR,
                arg -> arg.highlight(localizer.getStructureType(structure)),
                arg -> arg.highlight(getRequiredProperties())
            )
        );
    }

    /**
     * Checks if execution of this command is allowed for the given {@link Structure}.
     *
     * @param structure
     *     The {@link Structure} that is the target for this command.
     * @param bypassPermission
     *     Whether the {@link ICommandSender} has bypass access.
     * @return True if execution of this command is allowed.
     */
    protected boolean isAllowed(Structure structure, boolean bypassPermission)
    {
        return hasAccessToAttribute(structure, structureAttribute, bypassPermission);
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
        return (!requiredProperties.isEmpty()) && structure.hasProperties(getRequiredProperties());
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
    protected final StructureDescription getRetrievedStructureDescription()
    {
        return StructureDescription.of(localizer, getRetrieverResult());
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
     * Called by {@link #handleDatabaseActionResult(DatabaseManager.ActionResult)} when the database action was
     * cancelled.
     */
    protected void handleDatabaseActionCancelled()
    {
        getCommandSender().sendMessage(
            textFactory,
            TextType.ERROR,
            localizer.getMessage("commands.base.error.action_cancelled")
        );
    }

    /**
     * Called by {@link #handleDatabaseActionResult(DatabaseManager.ActionResult)} when the database action was
     * successful.
     */
    protected void handleDatabaseActionSuccess()
    {
    }

    /**
     * Called by {@link #handleDatabaseActionResult(DatabaseManager.ActionResult)} when the database action failed.
     */
    protected void handleDatabaseActionFail()
    {
        getCommandSender().sendMessage(textFactory, TextType.ERROR, localizer.getMessage("constants.error.generic"));
    }

    /**
     * Handles the results of a database action by informing the user of any non-success states.
     * <p>
     * To customize the handling, you can override {@link #handleDatabaseActionFail()},
     * {@link #handleDatabaseActionCancelled()}, or {@link #handleDatabaseActionSuccess()}.
     *
     * @param result
     *     The result obtained from the database.
     */
    protected final void handleDatabaseActionResult(DatabaseManager.ActionResult result)
    {
        log.atFine().log("Handling database action result: %s for command: %s", result.name(), this);
        switch (result)
        {
            case CANCELLED -> handleDatabaseActionCancelled();
            case SUCCESS -> handleDatabaseActionSuccess();
            case FAIL -> handleDatabaseActionFail();
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

        private static StructureDescription of(ILocalizer localizer, @Nullable Structure structure)
        {
            if (structure != null)
                return new StructureDescription(
                    localizer.getStructureType(structure), structure.getName() + " (" + structure.getUid() + ")");

            log.atSevere().withStackTrace(StackSize.FULL).log("Structure not available after database action!");
            return EMPTY_DESCRIPTION;
        }
    }
}
