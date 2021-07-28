package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Implements the command that changes the auto-close-timer for doors.
 *
 * @author Pim
 */
@ToString
public class SetAutoCloseTime extends DoorTargetCommand
{
    private static final @NotNull CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_AUTO_CLOSE_TIME;

    private final int autoCloseTime;

    protected SetAutoCloseTime(final @NotNull ICommandSender commandSender, final @NotNull DoorRetriever doorRetriever,
                               final int autoCloseTime)
    {
        super(commandSender, doorRetriever, DoorAttribute.AUTO_CLOSE_TIMER);
        this.autoCloseTime = autoCloseTime;
    }

    /**
     * Runs the {@link SetAutoCloseTime} command.
     *
     * @param commandSender The {@link ICommandSender} responsible for changing the auto-close timer of the door.
     * @param doorRetriever A {@link DoorRetriever} representing the {@link AbstractDoorBase} for which the auto-close
     *                      timer will be modified.
     * @param autoCloseTime The new auto-close time value.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender,
                                                          final @NotNull DoorRetriever doorRetriever,
                                                          final int autoCloseTime)
    {
        return new SetAutoCloseTime(commandSender, doorRetriever, autoCloseTime).run();
    }

    @Override
    public @NotNull CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected @NotNull CompletableFuture<Boolean> performAction(final @NotNull AbstractDoorBase door)
    {
        if (!(door instanceof ITimerToggleableArchetype))
        {
            // TODO: Localization
            getCommandSender().sendMessage("This door has no auto close timer property!");
            return CompletableFuture.completedFuture(true);
        }

        ((ITimerToggleableArchetype) door).setAutoCloseTime(autoCloseTime);
        return door.syncData().thenApply(x -> true);
    }

    /**
     * Executes the {@link SetAutoCloseTime} command without a known {@link #autoCloseTime}.
     * <p>
     * These missing values will be retrieved using a {@link DelayedCommandInputRequest}. The player will be asked to
     * use the {@link SetAutoCloseTime} command (again, if needed) to supply the missing data.
     * <p>
     * These missing data can be supplied using {@link #provideDelayedInput(ICommandSender, int)}.
     *
     * @param commandSender The entity that sent the command and is held responsible (i.e. permissions, communication)
     *                      for its execution.
     * @param doorRetriever A {@link DoorRetriever} that references the target door.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NotNull CompletableFuture<Boolean> runDelayed(final @NotNull ICommandSender commandSender,
                                                                 final @NotNull DoorRetriever doorRetriever)
    {
        final int commandTimeout = Constants.COMMAND_WAITER_TIMEOUT;
        return new DelayedCommandInputRequest<>(commandTimeout, commandSender, COMMAND_DEFINITION,
                                                delayedInput -> delayedInputExecutor(commandSender,
                                                                                     doorRetriever,
                                                                                     delayedInput),
                                                SetAutoCloseTime::inputRequestMessage, Integer.class)
            .getCommandOutput();
    }

    /**
     * Provides the delayed input if there is currently an active {@link DelayedCommandInputRequest} for the {@link
     * ICommandSender}.
     * <p>
     * If no active {@link DelayedCommandInputRequest} can be found for the command sender, the command sender will be
     * informed about it.
     *
     * @param commandSender The {@link ICommandSender} for which to look for an active {@link
     *                      DelayedCommandInputRequest} that can be fulfilled.
     * @param autoCloseTime The new auto-close timer. This is the amount of time (in seconds) after which a door is
     *                      opened to automatically close it again.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NotNull CompletableFuture<Boolean> provideDelayedInput(final @NotNull ICommandSender commandSender,
                                                                          final int autoCloseTime)
    {
        return BigDoors.get().getDelayedCommandInputManager().getInputRequest(commandSender)
                       .map(request -> request.provide(autoCloseTime))
                       .orElse(CompletableFuture.completedFuture(false));
    }

    /**
     * The method that is run once delayed input is received.
     * <p>
     * It processes the new input and executes the command using the previously-provided data (see {@link
     * #runDelayed(ICommandSender, DoorRetriever)}).
     *
     * @param commandSender The entity that sent the command and is held responsible (i.e. permissions, communication)
     *                      for its execution.
     * @param doorRetriever A {@link DoorRetriever} that references the target door.
     * @param autoCloseTime The new auto-close timer. This is the amount of time (in seconds) after which a door is
     *                      opened to automatically close it again.
     * @return See {@link BaseCommand#run()}.
     */
    private static @NotNull CompletableFuture<Boolean> delayedInputExecutor(final @NotNull ICommandSender commandSender,
                                                                            final @NotNull DoorRetriever doorRetriever,
                                                                            final int autoCloseTime)
    {
        return new SetAutoCloseTime(commandSender, doorRetriever, autoCloseTime).run();
    }

    /**
     * Retrieves the message that will be sent to the command sender after initialization of a delayed input request.
     *
     * @return The init message for the delayed input request.
     */
    private static @NotNull String inputRequestMessage()
    {
        return BigDoors.get().getPlatform().getMessages().getString(Message.COMMAND_SETTIME_INIT);
    }
}
