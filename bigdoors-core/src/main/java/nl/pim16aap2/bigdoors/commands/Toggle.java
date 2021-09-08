package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Represents a command that toggles a door.
 *
 * @author Pim
 */
@ToString
public class Toggle extends BaseCommand
{
    protected static final double DEFAULT_SPEED_MULTIPLIER = 0D;
    protected static final DoorActionType DEFAULT_DOOR_ACTION_TYPE = DoorActionType.TOGGLE;

    private final DoorRetriever[] doorRetrievers;
    private final DoorActionType doorActionType;
    private final double speedMultiplier;

    protected Toggle(ICommandSender commandSender, CommandContext context, DoorActionType doorActionType,
                     double speedMultiplier, DoorRetriever... doorRetrievers)
    {
        super(commandSender, context);
        this.doorActionType = doorActionType;
        this.speedMultiplier = speedMultiplier;
        this.doorRetrievers = doorRetrievers;
    }

    /**
     * Runs the {@link Toggle} command.
     *
     * @param commandSender
     *     The {@link ICommandSender} to hold responsible for the toggle action.
     * @param doorActionType
     *     The type of action to apply.
     *     <p>
     *     For example, when {@link DoorActionType#OPEN} is used, the door can only be toggled if it is possible to open
     *     it (in most cases that would mean that it is currently closed).
     *     <p>
     *     {@link DoorActionType#TOGGLE}, however, is possible regardless of its current open/close status.
     * @param speedMultiplier
     *     The speed multiplier to apply to the animation.
     * @param doorRetrievers
     *     The door(s) to toggle.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, CommandContext context,
                                                 DoorActionType doorActionType, double speedMultiplier,
                                                 DoorRetriever... doorRetrievers)
    {
        return new Toggle(commandSender, context, doorActionType, speedMultiplier, doorRetrievers).run();
    }

    /**
     * Runs the {@link Toggle} command with the {@link #DEFAULT_SPEED_MULTIPLIER}
     * <p>
     * See {@link #run(ICommandSender, CommandContext, DoorActionType, double, DoorRetriever...)}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, CommandContext context,
                                                 DoorActionType doorActionType, DoorRetriever... doorRetrievers)
    {
        return run(commandSender, context, doorActionType, DEFAULT_SPEED_MULTIPLIER, doorRetrievers);
    }

    /**
     * Runs the {@link Toggle} command using the {@link #DEFAULT_DOOR_ACTION_TYPE}.
     * <p>
     * See {@link #run(ICommandSender, CommandContext, DoorActionType, double, DoorRetriever...)}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, CommandContext context,
                                                 double speedMultiplier, DoorRetriever... doorRetrievers)
    {
        return run(commandSender, context, DEFAULT_DOOR_ACTION_TYPE, speedMultiplier, doorRetrievers);
    }

    /**
     * Runs the {@link Toggle} command using the {@link #DEFAULT_DOOR_ACTION_TYPE} and the {@link
     * #DEFAULT_SPEED_MULTIPLIER}.
     * <p>
     * See {@link #run(ICommandSender, CommandContext, DoorActionType, double, DoorRetriever...)}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, CommandContext context,
                                                 DoorRetriever... doorRetrievers)
    {
        return run(commandSender, context, DEFAULT_DOOR_ACTION_TYPE, DEFAULT_SPEED_MULTIPLIER, doorRetrievers);
    }

    @Override
    protected boolean validInput()
    {
        if (doorRetrievers.length > 0)
            return true;

        getCommandSender().sendMessage(localizer
                                           .getMessage("commands.toggle.error.not_enough_doors"));
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
                logger
                    .logThrowable(new IllegalStateException("Reached unregistered case: " + doorActionType.name()));
                return false;
        }
    }

    private void toggleDoor(AbstractDoor door, DoorActionCause doorActionCause, boolean hasBypassPermission)
    {
        if (!hasAccessToAttribute(door, DoorAttribute.TOGGLE, hasBypassPermission))
        {
            getCommandSender().sendMessage(localizer.getMessage("commands.toggle.error.no_access",
                                                                door.getBasicInfo()));
            logger
                .logMessage(Level.FINE, () -> "No access access for command " + this + " for door: " + door);
            return;
        }
        if (!canToggle(door))
        {
            getCommandSender().sendMessage(
                localizer.getMessage("commands.toggle.error.cannot_toggle", door.getBasicInfo()));
            logger
                .logMessage(Level.FINER, () -> "Blocked action for command " + this + " for door: " + door);
            return;
        }

        context.getPlatform().getDoorOpener()
               .animateDoorAsync(door, doorActionCause, getCommandSender().getPlayer().orElse(null),
                                 speedMultiplier, false, doorActionType);
    }

    private CompletableFuture<Void> handleDoorRequest(DoorRetriever doorRetriever, DoorActionCause doorActionCause,
                                                      boolean hasBypassPermission)
    {
        return getDoor(doorRetriever)
            .thenAccept(doorOpt -> doorOpt.ifPresent(door -> toggleDoor(door, doorActionCause, hasBypassPermission)));
    }

    @Override
    protected final CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        final var actionCause = getCommandSender().isPlayer() ? DoorActionCause.PLAYER : DoorActionCause.SERVER;
        final var actions = new CompletableFuture[doorRetrievers.length];
        for (int idx = 0; idx < actions.length; ++idx)
            actions[idx] = handleDoorRequest(doorRetrievers[idx], actionCause, permissions.second);
        return CompletableFuture.allOf(actions).thenApply(ignored -> true);
    }
}
