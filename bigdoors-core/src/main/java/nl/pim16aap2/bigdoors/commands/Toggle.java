package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorToggleRequestBuilder;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import javax.inject.Named;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Represents a command that toggles a door.
 *
 * @author Pim
 */
@ToString
@Flogger
public class Toggle extends BaseCommand
{
    protected static final double DEFAULT_SPEED_MULTIPLIER = 0D;
    protected static final DoorActionType DEFAULT_DOOR_ACTION_TYPE = DoorActionType.TOGGLE;

    private final DoorToggleRequestBuilder doorToggleRequestBuilder;
    private final DoorRetriever[] doorRetrievers;
    private final IMessageable messageableServer;
    private final DoorActionType doorActionType;
    private final double time;

    @AssistedInject //
    Toggle(@Assisted ICommandSender commandSender, ILocalizer localizer, @Assisted DoorActionType doorActionType,
           @Assisted double time, DoorToggleRequestBuilder doorToggleRequestBuilder,
           @Named("MessageableServer") IMessageable messageableServer, @Assisted DoorRetriever... doorRetrievers)
    {
        super(commandSender, localizer);
        this.doorActionType = doorActionType;
        this.time = time;
        this.doorToggleRequestBuilder = doorToggleRequestBuilder;
        this.doorRetrievers = doorRetrievers;
        this.messageableServer = messageableServer;
    }

    @Override
    protected boolean validInput()
    {
        if (doorRetrievers.length > 0)
            return true;

        getCommandSender().sendMessage(localizer.getMessage("commands.toggle.error.not_enough_doors"));
        return false;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.TOGGLE;
    }

    /**
     * Checks if the provided {@link AbstractDoor} can be toggled with the action provided by {@link #doorActionType}.
     * <p>
     * For example, if the action is {@link DoorActionType#CLOSE} and the door is already closed, the action is not
     * possible.
     *
     * @param door
     *     The door for which to check whether it can be toggled.
     * @return True if the toggle action is possible, otherwise false.
     */
    protected final boolean canToggle(AbstractDoor door)
    {
        switch (doorActionType)
        {
            case TOGGLE:
                return true;
            case OPEN:
                return door.isCloseable();
            case CLOSE:
                return door.isOpenable();
            default:
                log.at(Level.SEVERE)
                   .withCause(new IllegalStateException("Reached unregistered case: " + doorActionType.name()))
                   .log();
                return false;
        }
    }

    private void toggleDoor(AbstractDoor door, DoorActionCause doorActionCause, boolean hasBypassPermission)
    {
        if (!hasAccessToAttribute(door, DoorAttribute.TOGGLE, hasBypassPermission))
        {
            getCommandSender().sendMessage(localizer.getMessage("commands.toggle.error.no_access",
                                                                door.getBasicInfo()));
            log.at(Level.FINE).log("%s has no access for command %s for door %s!", getCommandSender(), this, door);
            return;
        }
        if (!canToggle(door))
        {
            getCommandSender().sendMessage(localizer.getMessage("commands.toggle.error.cannot_toggle",
                                                                door.getBasicInfo()));
            log.at(Level.FINER).log("Blocked action for command %s for door %s by %s", this, door, getCommandSender());
            return;
        }

        final Optional<IPPlayer> playerOptional = getCommandSender().getPlayer();
        doorToggleRequestBuilder.builder()
                                .door(door)
                                .doorActionCause(doorActionCause)
                                .doorActionType(doorActionType)
                                .responsible(playerOptional.orElse(null))
                                .messageReceiver(playerOptional.isPresent() ? playerOptional.get() : messageableServer)
                                .time(time)
                                .build().execute();
    }

    @SneakyThrows
    private CompletableFuture<Void> handleDoorRequest(DoorRetriever doorRetriever,
                                                      DoorActionCause doorActionCause, boolean hasBypassPermission)
    {
        return getDoor(doorRetriever)
            .thenAccept(doorOpt -> doorOpt.ifPresent(door -> toggleDoor(door, doorActionCause, hasBypassPermission)));
    }

    @Override
    protected final CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        final DoorActionCause actionCause = getCommandSender().isPlayer() ?
                                            DoorActionCause.PLAYER : DoorActionCause.SERVER;
        final CompletableFuture<?>[] actions = new CompletableFuture[doorRetrievers.length];
        for (int idx = 0; idx < actions.length; ++idx)
            actions[idx] = handleDoorRequest(doorRetrievers[idx], actionCause, permissions.second);
        return CompletableFuture.allOf(actions).thenApply(ignored -> true);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Toggle} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} to hold responsible for the toggle action.
         * @param doorActionType
         *     The type of action to apply.
         *     <p>
         *     For example, when {@link DoorActionType#OPEN} is used, the door can only be toggled if it is possible to
         *     open it (in most cases that would mean that it is currently closed).
         *     <p>
         *     {@link DoorActionType#TOGGLE}, however, is possible regardless of its current open/close status.
         * @param speedMultiplier
         *     The speed multiplier to apply to the animation.
         * @param doorRetrievers
         *     The door(s) to toggle.
         * @return See {@link BaseCommand#run()}.
         */
        Toggle newToggle(ICommandSender commandSender, DoorActionType doorActionType, double speedMultiplier,
                         DoorRetriever... doorRetrievers);

        /**
         * See {@link #newToggle(ICommandSender, DoorActionType, double, DoorRetriever...)}.
         * <p>
         * Defaults to {@link Toggle#DEFAULT_SPEED_MULTIPLIER} for the speed multiplier.
         */
        default Toggle newToggle(ICommandSender commandSender, DoorActionType doorActionType,
                                 DoorRetriever... doorRetrievers)
        {
            return newToggle(commandSender, doorActionType, Toggle.DEFAULT_SPEED_MULTIPLIER, doorRetrievers);
        }

        /**
         * See {@link #newToggle(ICommandSender, DoorActionType, double, DoorRetriever...)}.
         * <p>
         * Defaults to {@link Toggle#DEFAULT_DOOR_ACTION_TYPE} for the door action type.
         */
        default Toggle newToggle(ICommandSender commandSender, double speedMultiplier,
                                 DoorRetriever... doorRetrievers)
        {
            return newToggle(commandSender, Toggle.DEFAULT_DOOR_ACTION_TYPE, speedMultiplier, doorRetrievers);
        }

        /**
         * See {@link #newToggle(ICommandSender, DoorActionType, double, DoorRetriever...)}.
         * <p>
         * Defaults to {@link Toggle#DEFAULT_SPEED_MULTIPLIER} for the speed multiplier and to {@link
         * Toggle#DEFAULT_DOOR_ACTION_TYPE} for the door action type.
         */
        default Toggle newToggle(ICommandSender commandSender, DoorRetriever... doorRetrievers)
        {
            return newToggle(commandSender, Toggle.DEFAULT_DOOR_ACTION_TYPE, Toggle.DEFAULT_SPEED_MULTIPLIER,
                             doorRetrievers);
        }
    }
}
