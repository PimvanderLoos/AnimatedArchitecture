package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
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
    protected static final @NonNull DoorActionType DEFAULT_DOOR_ACTION_TYPE = DoorActionType.TOGGLE;

    private final @NonNull DoorRetriever[] doorRetrievers;
    private final @NonNull DoorActionType doorActionType;
    private final double speedMultiplier;

    protected Toggle(final @NonNull ICommandSender commandSender, final @NonNull DoorActionType doorActionType,
                     final double speedMultiplier, final @NonNull DoorRetriever... doorRetrievers)
    {
        super(commandSender);
        this.doorActionType = doorActionType;
        this.speedMultiplier = speedMultiplier;
        this.doorRetrievers = doorRetrievers;
    }

    /**
     * Runs the {@link Toggle} command.
     *
     * @param commandSender   The {@link ICommandSender} to hold responsible for the toggle action.
     * @param doorActionType  The type of action to apply.
     *                        <p>
     *                        For example, when {@link DoorActionType#OPEN} is used, the door can only be toggled if it
     *                        is possible to open it (in most cases that would mean that it is currently closed).
     *                        <p>
     *                        {@link DoorActionType#TOGGLE}, however, is possible regardless of its current open/close
     *                        status.
     * @param speedMultiplier The speed multiplier to apply to the animation.
     * @param doorRetrievers  The door(s) to toggle.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NonNull CompletableFuture<Boolean> run(final @NonNull ICommandSender commandSender,
                                                          final @NonNull DoorActionType doorActionType,
                                                          final double speedMultiplier,
                                                          final @NonNull DoorRetriever... doorRetrievers)
    {
        return new Toggle(commandSender, doorActionType, speedMultiplier, doorRetrievers).run();
    }

    /**
     * Runs the {@link Toggle} command with the {@link #DEFAULT_SPEED_MULTIPLIER}
     * <p>
     * See {@link #run(ICommandSender, DoorActionType, double, DoorRetriever...)}.
     */
    public static @NonNull CompletableFuture<Boolean> run(final @NonNull ICommandSender commandSender,
                                                          final @NonNull DoorActionType doorActionType,
                                                          final @NonNull DoorRetriever... doorRetrievers)
    {
        return run(commandSender, doorActionType, DEFAULT_SPEED_MULTIPLIER, doorRetrievers);
    }

    /**
     * Runs the {@link Toggle} command using the {@link #DEFAULT_DOOR_ACTION_TYPE}.
     * <p>
     * See {@link #run(ICommandSender, DoorActionType, double, DoorRetriever...)}.
     */
    public static @NonNull CompletableFuture<Boolean> run(final @NonNull ICommandSender commandSender,
                                                          final double speedMultiplier,
                                                          final @NonNull DoorRetriever... doorRetrievers)
    {
        return run(commandSender, DEFAULT_DOOR_ACTION_TYPE, speedMultiplier, doorRetrievers);
    }

    /**
     * Runs the {@link Toggle} command using the {@link #DEFAULT_DOOR_ACTION_TYPE} and the {@link
     * #DEFAULT_SPEED_MULTIPLIER}.
     * <p>
     * See {@link #run(ICommandSender, DoorActionType, double, DoorRetriever...)}.
     */
    public static @NonNull CompletableFuture<Boolean> run(final @NonNull ICommandSender commandSender,
                                                          final @NonNull DoorRetriever... doorRetrievers)
    {
        return run(commandSender, DEFAULT_DOOR_ACTION_TYPE, DEFAULT_SPEED_MULTIPLIER, doorRetrievers);
    }

    @Override
    protected boolean validInput()
    {
        return doorRetrievers.length > 0;
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.TOGGLE;
    }

    /**
     * Checks if the provided {@link AbstractDoorBase} can be toggled with the action provided by {@link
     * #doorActionType}.
     * <p>
     * For example, if the action is {@link DoorActionType#CLOSE} and the door is already closed, the action is not
     * possible.
     *
     * @param door The door for which to check whether it can be toggled.
     * @return True if the toggle action is possible, otherwise false.
     */
    protected final boolean canToggle(final @NonNull AbstractDoorBase door)
    {
        switch (doorActionType)
        {
            case TOGGLE:
                return true;
            case OPEN:
                return door.isCloseable();
            case CLOSE:
                return door.isOpenable();
        }
        return false;
    }

    private void toggleDoor(final @NonNull AbstractDoorBase door, final @NonNull DoorActionCause doorActionCause,
                            final boolean hasBypassPermission)
    {
        if (!hasAccessToAttribute(door, DoorAttribute.TOGGLE, hasBypassPermission))
        {
            BigDoors.get().getPLogger()
                    .logMessage(Level.FINE, () -> "No access access for command " + this + " for door: " + door);
            return;
        }
        if (!canToggle(door))
        {
            BigDoors.get().getPLogger()
                    .logMessage(Level.FINER, () -> "Blocked action for command " + this + " for door: " + door);
            return;
        }

        BigDoors.get().getDoorOpener()
                .animateDoorAsync(door, doorActionCause, getCommandSender().getPlayer().orElse(null),
                                  speedMultiplier, false, doorActionType);
    }

    private @NonNull CompletableFuture<Void> handleDoorRequest(final @NonNull DoorRetriever doorRetriever,
                                                               final @NonNull DoorActionCause doorActionCause,
                                                               final boolean hasBypassPermission)
    {
        return getDoor(doorRetriever)
            .thenAccept(doorOpt -> doorOpt.ifPresent(door -> toggleDoor(door, doorActionCause, hasBypassPermission)));
    }

    @Override
    protected final @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        val actionCause = getCommandSender().isPlayer() ? DoorActionCause.PLAYER : DoorActionCause.SERVER;
        val actions = new CompletableFuture[doorRetrievers.length];
        for (int idx = 0; idx < actions.length; ++idx)
            actions[idx] = handleDoorRequest(doorRetrievers[idx], actionCause, permissions.second);
        return CompletableFuture.allOf(actions).thenApply(ignored -> true);
    }
}
