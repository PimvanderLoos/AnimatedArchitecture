package nl.pim16aap2.bigdoors.commands;

import com.google.common.flogger.StackSize;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableAttribute;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Represents a command that relates to an existing movable.
 *
 * @author Pim
 */
@Flogger
public abstract class MovableTargetCommand extends BaseCommand
{
    @Getter
    protected final MovableRetriever movableRetriever;

    private final MovableAttribute movableAttribute;

    /**
     * The result of the {@link #movableRetriever}.
     * <p>
     * This will not be available until after {@link #executeCommand(PermissionsStatus)} has started, but before
     * {@link #performAction(AbstractMovable)} is called.
     * <p>
     * Even after the result has been set, it may still be null in case no doors were found.
     */
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    private @Nullable AbstractMovable retrieverResult;

    protected MovableTargetCommand(
        ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory, MovableRetriever movableRetriever,
        MovableAttribute movableAttribute)
    {
        super(commandSender, localizer, textFactory);
        this.movableRetriever = movableRetriever;
        this.movableAttribute = movableAttribute;
    }

    @Override
    protected final CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        return getMovable(getMovableRetriever())
            .thenApply(movable ->
                       {
                           setRetrieverResult(movable.orElse(null));
                           return movable;
                       })
            .thenAcceptAsync(movable -> processMovableResult(movable, permissions))
            .exceptionally(Util::exceptionally);
    }

    /**
     * Handles the result of retrieving the movable.
     *
     * @param movable
     *     The result of trying to retrieve the movable.
     * @param permissions
     *     Whether the ICommandSender has user and/or admin permissions.
     */
    private void processMovableResult(Optional<AbstractMovable> movable, PermissionsStatus permissions)
    {
        if (movable.isEmpty())
        {
            log.atFine().log("Failed to find movable %s for command: %s", getMovableRetriever(), this);

            getCommandSender()
                .sendMessage(textFactory, TextType.ERROR,
                             localizer.getMessage("commands.movable_target_command.base.error.movable_not_found"));
            return;
        }

        if (!isAllowed(movable.get(), permissions.hasAdminPermission()))
        {
            log.atFine()
               .log("%s does not have access to movable %s for command %s", getCommandSender(), movable, this);

            getCommandSender()
                .sendMessage(textFactory, TextType.ERROR,
                             localizer.getMessage("commands.movable_target_command.base.error.no_permission_for_action",
                                                  localizer.getMovableType(movable.get())));
            return;
        }

        try
        {
            performAction(movable.get()).get(30, TimeUnit.MINUTES);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to perform command " + this + " for movable " + movable, e);
        }
    }

    /**
     * Checks if execution of this command is allowed for the given {@link AbstractMovable}.
     *
     * @param movable
     *     The {@link AbstractMovable} that is the target for this command.
     * @param bypassPermission
     *     Whether the {@link ICommandSender} has bypass access.
     * @return True if execution of this command is allowed.
     */
    protected boolean isAllowed(AbstractMovable movable, boolean bypassPermission)
    {
        return hasAccessToAttribute(movable, movableAttribute, bypassPermission);
    }

    /**
     * Performs the action of this command on the {@link AbstractMovable}.
     *
     * @param movable
     *     The {@link MovableBase} to perform the action on.
     * @return The future of the command execution.
     */
    protected abstract CompletableFuture<?> performAction(AbstractMovable movable);

    /**
     * @return The movable description of the {@link #retrieverResult}.
     */
    protected final MovableDescription getRetrievedMovableDescription()
    {
        return MovableDescription.of(localizer, getRetrieverResult());
    }


    /**
     * Called by {@link #handleDatabaseActionResult(DatabaseManager.ActionResult)} when the database action was
     * cancelled.
     */
    protected void handleDatabaseActionCancelled()
    {
        getCommandSender().sendMessage(textFactory, TextType.ERROR,
                                       localizer.getMessage("commands.base.error.action_cancelled"));
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
     * @return True in all cases, as it is assumed that this is not user error.
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
     * A simple description of a movable.
     *
     * @param typeName
     *     The localized name of the movable's type.
     * @param id
     *     The user-friendly identifier of the movable.
     */
    protected record MovableDescription(String typeName, String id)
    {
        private static final MovableDescription EMPTY_DESCRIPTION = new MovableDescription("Movable", "null");

        private static MovableDescription of(ILocalizer localizer, @Nullable AbstractMovable movable)
        {
            if (movable != null)
                return new MovableDescription(
                    localizer.getMovableType(movable), movable.getName() + " (" + movable.getUid() + ")");

            log.atSevere().withStackTrace(StackSize.FULL).log("Movable not available after database action!");
            return EMPTY_DESCRIPTION;
        }
    }
}
