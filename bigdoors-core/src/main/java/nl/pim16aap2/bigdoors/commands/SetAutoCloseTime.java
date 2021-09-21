package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;

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

    @AssistedInject //
    SetAutoCloseTime(@Assisted ICommandSender commandSender, ILocalizer localizer,
                     @Assisted DoorRetriever doorRetriever, @Assisted int autoCloseTime)
    {
        super(commandSender, localizer, doorRetriever, DoorAttribute.AUTO_CLOSE_TIMER);
        this.autoCloseTime = autoCloseTime;
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
            getCommandSender().sendMessage(localizer.getMessage("commands.set_auto_close_timer.error.invalid_door_type",
                                                                door.getBasicInfo()));
            return CompletableFuture.completedFuture(true);
        }

        ((ITimerToggleable) door).setAutoCloseTime(autoCloseTime);
        return door.syncData().thenApply(x -> true);
    }

    //    /**
//     * Executes the {@link SetAutoCloseTime} command without a known {@link #autoCloseTime}.
//     * <p>
//     * These missing values will be retrieved using a {@link DelayedCommandInputRequest}. The player will be asked to
//     * use the {@link SetAutoCloseTime} command (again, if needed) to supply the missing data.
//     * <p>
//     * These missing data can be supplied using {@link #provideDelayedInput(ICommandSender, IPLogger, ILocalizer,
//     * int)}.
//     *
//     * @param commandSender
//     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
//     *     execution.
//     * @param doorRetriever
//     *     A {@link DoorRetrieverFactory} that references the target door.
//     * @return See {@link BaseCommand#run()}.
//     */
//    public static CompletableFuture<Boolean> runDelayed(ICommandSender commandSender, IPLogger logger,
//                                                        ILocalizer localizer,
//                                                        DoorRetriever doorRetriever)
//    {
//        final int commandTimeout = Constants.COMMAND_WAITER_TIMEOUT;
//        return new DelayedCommandInputRequest<>(commandTimeout, commandSender, COMMAND_DEFINITION, logger, localizer,
//                                                delayedInput -> delayedInputExecutor(commandSender, logger, localizer,
//                                                                                     doorRetriever, delayedInput),
//                                                () -> SetAutoCloseTime.inputRequestMessage(localizer), Integer.class)
//            .getCommandOutput();
//    }
//
//    /**
//     * Provides the delayed input if there is currently an active {@link DelayedCommandInputRequest} for the {@link
//     * ICommandSender}.
//     * <p>
//     * If no active {@link DelayedCommandInputRequest} can be found for the command sender, the command sender will be
//     * informed about it.
//     *
//     * @param commandSender
//     *     The {@link ICommandSender} for which to look for an active {@link DelayedCommandInputRequest} that can be
//     *     fulfilled.
//     * @param autoCloseTime
//     *     The new auto-close timer. This is the amount of time (in seconds) after which a door is opened to
//     *     automatically close it again.
//     * @return See {@link BaseCommand#run()}.
//     */
//    public static CompletableFuture<Boolean> provideDelayedInput(ICommandSender commandSender, IPLogger logger,
//                                                                 ILocalizer localizer,
//                                                                 int autoCloseTime)
//    {
//        return delayedCommandInputManager().getInputRequest(commandSender)
//                                           .map(request -> request.provide(autoCloseTime))
//                                           .orElse(CompletableFuture.completedFuture(false));
//    }
//
//    /**
//     * The method that is run once delayed input is received.
//     * <p>
//     * It processes the new input and executes the command using the previously-provided data (see {@link
//     * #runDelayed(ICommandSender, IPLogger, ILocalizer, DoorRetriever)}).
//     *
//     * @param commandSender
//     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
//     *     execution.
//     * @param doorRetriever
//     *     A {@link DoorRetrieverFactory} that references the target door.
//     * @param autoCloseTime
//     *     The new auto-close timer. This is the amount of time (in seconds) after which a door is opened to
//     *     automatically close it again.
//     * @return See {@link BaseCommand#run()}.
//     */
//    private static CompletableFuture<Boolean> delayedInputExecutor(ICommandSender commandSender, IPLogger logger,
//                                                                   ILocalizer localizer,
//                                                                   DoorRetriever doorRetriever,
//                                                                   int autoCloseTime)
//    {
//        return new SetAutoCloseTime(commandSender, logger, localizer, doorRetriever, autoCloseTime).run();
//    }
//
//    /**
//     * Retrieves the message that will be sent to the command sender after initialization of a delayed input request.
//     *
//     * @return The init message for the delayed input request.
//     */
//    private static String inputRequestMessage(ILocalizer localizer)
//    {
//        return localizer.getMessage("commands.set_auto_close_timer.init");
//    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link SetAutoCloseTime} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing the auto-close timer of the door.
         * @param doorRetriever
         *     A {@link DoorRetrieverFactory} representing the {@link DoorBase} for which the auto-close timer will be
         *     modified.
         * @param autoCloseTime
         *     The new auto-close time value.
         * @return See {@link BaseCommand#run()}.
         */
        SetAutoCloseTime newSetAutoCloseTime(ICommandSender commandSender,
                                             DoorRetriever doorRetriever, int autoCloseTime);
    }
}
