package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
import org.jetbrains.annotations.NotNull;

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
    protected static final @NotNull DoorActionType DEFAULT_DOOR_ACTION_TYPE = DoorActionType.TOGGLE;

    private final @NotNull DoorRetriever[] doorRetrievers;
    private final @NotNull DoorActionType doorActionType;
    private final double speedMultiplier;

    protected Toggle(final @NotNull ICommandSender commandSender, final @NotNull DoorActionType doorActionType,
                     final double speedMultiplier, final @NotNull DoorRetriever... doorRetrievers)
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
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender,
                                                          final @NotNull DoorActionType doorActionType,
                                                          final double speedMultiplier,
                                                          final @NotNull DoorRetriever... doorRetrievers)
    {
        return new Toggle(commandSender, doorActionType, speedMultiplier, doorRetrievers).run();
    }

    /**
     * Runs the {@link Toggle} command with the {@link #DEFAULT_SPEED_MULTIPLIER}
     * <p>
     * See {@link #run(ICommandSender, DoorActionType, double, DoorRetriever...)}.
     */
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender,
                                                          final @NotNull DoorActionType doorActionType,
                                                          final @NotNull DoorRetriever... doorRetrievers)
    {
        return run(commandSender, doorActionType, DEFAULT_SPEED_MULTIPLIER, doorRetrievers);
    }

    /**
     * Runs the {@link Toggle} command using the {@link #DEFAULT_DOOR_ACTION_TYPE}.
     * <p>
     * See {@link #run(ICommandSender, DoorActionType, double, DoorRetriever...)}.
     */
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender,
                                                          final double speedMultiplier,
                                                          final @NotNull DoorRetriever... doorRetrievers)
    {
        return run(commandSender, DEFAULT_DOOR_ACTION_TYPE, speedMultiplier, doorRetrievers);
    }

    /**
     * Runs the {@link Toggle} command using the {@link #DEFAULT_DOOR_ACTION_TYPE} and the {@link
     * #DEFAULT_SPEED_MULTIPLIER}.
     * <p>
     * See {@link #run(ICommandSender, DoorActionType, double, DoorRetriever...)}.
     */
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender,
                                                          final @NotNull DoorRetriever... doorRetrievers)
    {
        return run(commandSender, DEFAULT_DOOR_ACTION_TYPE, DEFAULT_SPEED_MULTIPLIER, doorRetrievers);
    }

    @Override
    protected boolean validInput()
    {
        if (doorRetrievers.length > 0)
            return true;

        // TODO: Localization
        getCommandSender().sendMessage("At least 1 door must be specified!");
        return false;
    }

    @Override
    public @NotNull CommandDefinition getCommand()
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
    protected final boolean canToggle(final @NotNull AbstractDoorBase door)
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
                BigDoors.get().getPLogger()
                        .logThrowable(new IllegalStateException("Reached unregistered case: " + doorActionType.name()));
                return false;
        }
    }

    private void toggleDoor(final @NotNull AbstractDoorBase door, final @NotNull DoorActionCause doorActionCause,
                            final boolean hasBypassPermission)
    {
        if (!hasAccessToAttribute(door, DoorAttribute.TOGGLE, hasBypassPermission))
        {
            // TODO: Localization
            getCommandSender().sendMessage("You do not have access to the command for this door!");
            BigDoors.get().getPLogger()
                    .logMessage(Level.FINE, () -> "No access access for command " + this + " for door: " + door);
            return;
        }
        if (!canToggle(door))
        {
            // TODO: Localization
            getCommandSender().sendMessage("Door can not be toggled because it is not openable/closeable.");
            BigDoors.get().getPLogger()
                    .logMessage(Level.FINER, () -> "Blocked action for command " + this + " for door: " + door);
            return;
        }

        BigDoors.get().getDoorOpener()
                .animateDoorAsync(door, doorActionCause, getCommandSender().getPlayer().orElse(null),
                                  speedMultiplier, false, doorActionType);
    }

    private @NotNull CompletableFuture<Void> handleDoorRequest(final @NotNull DoorRetriever doorRetriever,
                                                               final @NotNull DoorActionCause doorActionCause,
                                                               final boolean hasBypassPermission)
    {
        return getDoor(doorRetriever)
            .thenAccept(doorOpt -> doorOpt.ifPresent(door -> toggleDoor(door, doorActionCause, hasBypassPermission)));
    }

    @Override
    protected final @NotNull CompletableFuture<Boolean> executeCommand(final @NotNull BooleanPair permissions)
    {
        val actionCause = getCommandSender().isPlayer() ? DoorActionCause.PLAYER : DoorActionCause.SERVER;
        val actions = new CompletableFuture[doorRetrievers.length];
        for (int idx = 0; idx < actions.length; ++idx)
            actions[idx] = handleDoorRequest(doorRetrievers[idx], actionCause, permissions.second);
        return CompletableFuture.allOf(actions).thenApply(ignored -> true);
    }
}
