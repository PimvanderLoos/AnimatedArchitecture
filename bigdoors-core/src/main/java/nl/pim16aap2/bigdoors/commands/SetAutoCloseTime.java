package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;

import java.util.concurrent.CompletableFuture;

/**
 * Implements the command that changes the auto-close-timer for doors.
 *
 * @author Pim
 */
@ToString
public class SetAutoCloseTime extends DoorTargetCommand
{
    private static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_AUTO_CLOSE_TIME;

    private final int autoCloseTime;

    protected SetAutoCloseTime(ICommandSender commandSender, DoorRetriever doorRetriever, int autoCloseTime)
    {
        super(commandSender, doorRetriever, DoorAttribute.AUTO_CLOSE_TIMER);
        this.autoCloseTime = autoCloseTime;
    }

    /**
     * Runs the {@link SetAutoCloseTime} command.
     *
     * @param commandSender The {@link ICommandSender} responsible for changing the auto-close timer of the door.
     * @param doorRetriever A {@link DoorRetriever} representing the {@link DoorBase} for which the auto-close timer
     *                      will be modified.
     * @param autoCloseTime The new auto-close time value.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, DoorRetriever doorRetriever,
                                                 int autoCloseTime)
    {
        return new SetAutoCloseTime(commandSender, doorRetriever, autoCloseTime).run();
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractDoor door)
    {
        if (!(door instanceof ITimerToggleable))
        {
            getCommandSender().sendMessage(BigDoors.get().getLocalizer()
                                                   .getMessage("commands.set_auto_close_timer.error.invalid_door_type",
                                                               door.getBasicInfo()));
            return CompletableFuture.completedFuture(true);
        }

        ((ITimerToggleable) door).setAutoCloseTime(autoCloseTime);
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
    public static CompletableFuture<Boolean> runDelayed(ICommandSender commandSender, DoorRetriever doorRetriever)
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
    public static CompletableFuture<Boolean> provideDelayedInput(ICommandSender commandSender, int autoCloseTime)
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
    private static CompletableFuture<Boolean> delayedInputExecutor(ICommandSender commandSender,
                                                                   DoorRetriever doorRetriever, int autoCloseTime)
    {
        return new SetAutoCloseTime(commandSender, doorRetriever, autoCloseTime).run();
    }

    /**
     * Retrieves the message that will be sent to the command sender after initialization of a delayed input request.
     *
     * @return The init message for the delayed input request.
     */
    private static String inputRequestMessage()
    {
        return BigDoors.get().getLocalizer().getMessage("commands.set_auto_close_timer.init");
    }
}
