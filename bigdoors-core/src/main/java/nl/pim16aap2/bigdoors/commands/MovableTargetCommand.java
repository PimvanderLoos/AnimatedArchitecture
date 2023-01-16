package nl.pim16aap2.bigdoors.commands;

import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableAttribute;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

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

    protected MovableTargetCommand(
        ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory, MovableRetriever movableRetriever,
        MovableAttribute movableAttribute)
    {
        super(commandSender, localizer, textFactory);
        this.movableRetriever = movableRetriever;
        this.movableAttribute = movableAttribute;
    }

    @Override
    protected final CompletableFuture<Boolean> executeCommand(PermissionsStatus permissions)
    {
        return getMovable(getMovableRetriever())
            .thenApplyAsync(movable -> processMovableResult(movable, permissions))
            .exceptionally(t -> Util.exceptionally(t, false));
    }

    /**
     * Handles the result of retrieving the movable.
     *
     * @param movable
     *     The result of trying to retrieve the movable.
     * @param permissions
     *     Whether the ICommandSender has user and/or admin permissions.
     * @return The result of running the command, see {@link BaseCommand#run()}.
     */
    private boolean processMovableResult(Optional<AbstractMovable> movable, PermissionsStatus permissions)
    {
        if (movable.isEmpty())
        {
            log.at(Level.FINE).log("Failed to find movable %s for command: %s", getMovableRetriever(), this);

            getCommandSender()
                .sendMessage(textFactory, TextType.ERROR,
                             localizer.getMessage("commands.movable_target_command.base.error.movable_not_found"));
            return false;
        }

        if (!isAllowed(movable.get(), permissions.hasAdminPermission()))
        {
            log.at(Level.FINE)
               .log("%s does not have access to movable %s for command %s", getCommandSender(), movable, this);

            getCommandSender()
                .sendMessage(textFactory, TextType.ERROR,
                             localizer.getMessage("commands.movable_target_command.base.error.no_permission_for_action",
                                                  localizer.getMovableType(movable.get())));
            return true;
        }

        try
        {
            return performAction(movable.get()).get(30, TimeUnit.MINUTES);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
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
     * @return True if everything was successful.
     */
    protected abstract CompletableFuture<Boolean> performAction(AbstractMovable movable);
}
