package nl.pim16aap2.bigdoors.commands;

import com.google.common.flogger.StackSize;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableAttribute;
import nl.pim16aap2.bigdoors.movable.MovableToggleRequestBuilder;
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import org.jetbrains.annotations.Nullable;

import javax.inject.Named;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a command that toggles a movable.
 *
 * @author Pim
 */
@ToString
@Flogger
public class Toggle extends BaseCommand
{
    protected static final MovableActionType DEFAULT_MOVABLE_ACTION_TYPE = MovableActionType.TOGGLE;

    private final MovableToggleRequestBuilder movableToggleRequestBuilder;
    private final MovableRetriever[] movableRetrievers;
    private final IMessageable messageableServer;
    private final MovableActionType movableActionType;
    private final @Nullable Double time;

    @AssistedInject //
    Toggle(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted MovableActionType movableActionType, @Assisted @Nullable Double time,
        MovableToggleRequestBuilder movableToggleRequestBuilder,
        @Named("MessageableServer") IMessageable messageableServer, @Assisted MovableRetriever... movableRetrievers)
    {
        super(commandSender, localizer, textFactory);
        this.movableActionType = movableActionType;
        this.time = time;
        this.movableToggleRequestBuilder = movableToggleRequestBuilder;
        this.movableRetrievers = movableRetrievers;
        this.messageableServer = messageableServer;
    }

    @Override
    protected boolean validInput()
    {
        if (movableRetrievers.length > 0)
            return true;

        getCommandSender().sendMessage(textFactory, TextType.ERROR,
                                       localizer.getMessage("commands.toggle.error.not_enough_movables"));
        return false;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.TOGGLE;
    }

    /**
     * Checks if the provided {@link AbstractMovable} can be toggled with the action provided by
     * {@link #movableActionType}.
     * <p>
     * For example, if the action is {@link MovableActionType#CLOSE} and the movable is already closed, the action is
     * not possible.
     *
     * @param movable
     *     The movable for which to check whether it can be toggled.
     * @return True if the toggle action is possible, otherwise false.
     */
    protected final boolean canToggle(AbstractMovable movable)
    {
        switch (movableActionType)
        {
            case TOGGLE:
                return true;
            case OPEN:
                return movable.isCloseable();
            case CLOSE:
                return movable.isOpenable();
            default:
                log.atSevere()
                   .withStackTrace(StackSize.FULL).log("Reached unregistered case: '%s'!", movableActionType.name());
                return false;
        }
    }

    private void toggleMovable(AbstractMovable movable, MovableActionCause cause, boolean hasBypassPermission)
    {
        if (!hasAccessToAttribute(movable, MovableAttribute.TOGGLE, hasBypassPermission))
        {
            getCommandSender()
                .sendMessage(textFactory, TextType.ERROR,
                             localizer.getMessage("commands.toggle.error.no_access",
                                                  localizer.getMovableType(movable), movable.getBasicInfo()));
            log.atFine()
               .log("%s has no access for command %s for movable %s!", getCommandSender(), this, movable);
            return;
        }
        if (!canToggle(movable))
        {
            getCommandSender()
                .sendMessage(textFactory, TextType.ERROR,
                             localizer.getMessage("commands.toggle.error.cannot_toggle",
                                                  localizer.getMovableType(movable), movable.getBasicInfo()));
            log.atFiner()
               .log("Blocked action for command %s for movable %s by %s", this, movable, getCommandSender());
            return;
        }

        final Optional<IPPlayer> playerOpt = getCommandSender().getPlayer();
        movableToggleRequestBuilder.builder()
                                   .movable(movable)
                                   .movableActionCause(cause)
                                   .movableActionType(movableActionType)
                                   .responsible(playerOpt.orElse(null))
                                   .messageReceiver(playerOpt.isPresent() ? playerOpt.get() : messageableServer)
                                   .time(time)
                                   .build().execute();
    }

    private CompletableFuture<Void> handleMovableRequest(
        MovableRetriever movableRetriever, MovableActionCause cause, boolean hasBypassPermission)
    {
        return getMovable(movableRetriever)
            .thenAccept(movableOpt ->
                            movableOpt.ifPresent(movable -> toggleMovable(movable, cause, hasBypassPermission)));
    }

    @Override
    protected final CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        final MovableActionCause actionCause =
            getCommandSender().isPlayer() ? MovableActionCause.PLAYER : MovableActionCause.SERVER;

        final CompletableFuture<?>[] actions = new CompletableFuture[movableRetrievers.length];
        for (int idx = 0; idx < actions.length; ++idx)
            actions[idx] = handleMovableRequest(movableRetrievers[idx], actionCause, permissions.hasAdminPermission());

        return CompletableFuture.allOf(actions);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Toggle} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} to hold responsible for the toggle action.
         * @param actionType
         *     The type of action to apply.
         *     <p>
         *     For example, when {@link MovableActionType#OPEN} is used, the movable can only be toggled if it is
         *     possible to open it (in most cases that would mean that it is currently closed).
         *     <p>
         *     {@link MovableActionType#TOGGLE}, however, is possible regardless of its current open/close status.
         * @param speedMultiplier
         *     The speed multiplier to apply to the animation.
         * @param movableRetrievers
         *     The movable(s) to toggle.
         * @return See {@link BaseCommand#run()}.
         */
        Toggle newToggle(
            ICommandSender commandSender, MovableActionType actionType, @Nullable Double speedMultiplier,
            MovableRetriever... movableRetrievers);

        /**
         * See {@link #newToggle(ICommandSender, MovableActionType, Double, MovableRetriever...)}.
         * <p>
         * Defaults to null for the time multiplier.
         */
        default Toggle newToggle(
            ICommandSender commandSender, MovableActionType actionType, MovableRetriever... movableRetrievers)
        {
            return newToggle(commandSender, actionType, null, movableRetrievers);
        }

        /**
         * See {@link #newToggle(ICommandSender, MovableActionType, Double, MovableRetriever...)}.
         * <p>
         * Defaults to {@link Toggle#DEFAULT_MOVABLE_ACTION_TYPE} for the movable action type.
         */
        default Toggle newToggle(
            ICommandSender commandSender, @Nullable Double speedMultiplier, MovableRetriever... movableRetrievers)
        {
            return newToggle(commandSender, Toggle.DEFAULT_MOVABLE_ACTION_TYPE, speedMultiplier, movableRetrievers);
        }

        /**
         * See {@link #newToggle(ICommandSender, MovableActionType, Double, MovableRetriever...)}.
         * <p>
         * Defaults to null for the speed multiplier and to {@link Toggle#DEFAULT_MOVABLE_ACTION_TYPE} for the movable
         * action type.
         */
        default Toggle newToggle(ICommandSender commandSender, MovableRetriever... movableRetrievers)
        {
            return newToggle(commandSender, Toggle.DEFAULT_MOVABLE_ACTION_TYPE, null, movableRetrievers);
        }
    }
}
