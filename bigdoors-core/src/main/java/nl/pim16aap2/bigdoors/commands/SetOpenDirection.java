package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.util.CompletableFutureHandler;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.RotateDirection;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that changes the opening direction of doors.
 *
 * @author Pim
 */
@ToString
public class SetOpenDirection extends DoorTargetCommand
{
    private static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_OPEN_DIR;

    private final RotateDirection rotateDirection;

    @AssistedInject //
    SetOpenDirection(@Assisted ICommandSender commandSender, IPLogger logger, ILocalizer localizer,
                     @Assisted DoorRetriever.AbstractRetriever doorRetriever,
                     @Assisted RotateDirection rotateDirection, CompletableFutureHandler handler)
    {
        super(commandSender, logger, localizer, doorRetriever, DoorAttribute.OPEN_DIRECTION, handler);
        this.rotateDirection = rotateDirection;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractDoor door)
    {
        if (!door.getDoorType().isValidOpenDirection(rotateDirection))
        {
            getCommandSender().sendMessage(
                localizer.getMessage("commands.set_open_direction.error.invalid_rotation",
                                     rotateDirection.name(), door.getBasicInfo()));

            return CompletableFuture.completedFuture(true);
        }

        door.setOpenDir(rotateDirection);
        return door.syncData().thenApply(x -> true);
    }

    //    /**
//     * Executes the {@link SetOpenDirection} command without a known {@link #rotateDirection}.
//     * <p>
//     * These missing values will be retrieved using a {@link DelayedCommandInputRequest}. The player will be asked to
//     * use the {@link SetOpenDirection} command (again, if needed) to supply the missing data.
//     * <p>
//     * These missing data can be supplied using {@link #provideDelayedInput(ICommandSender, IPLogger, ILocalizer,
//     * RotateDirection)}.
//     *
//     * @param commandSender
//     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
//     *     execution.
//     * @param doorRetriever
//     *     A {@link DoorRetriever} that references the target door.
//     * @return See {@link BaseCommand#run()}.
//     */
//    public static CompletableFuture<Boolean> runDelayed(ICommandSender commandSender, IPLogger logger,
//                                                        ILocalizer localizer,
//                                                        DoorRetriever.AbstractRetriever doorRetriever)
//    {
//        final int commandTimeout = Constants.COMMAND_WAITER_TIMEOUT;
//        return new DelayedCommandInputRequest<>(commandTimeout, commandSender, COMMAND_DEFINITION, logger, localizer,
//                                                delayedInput -> delayedInputExecutor(commandSender, logger, localizer,
//                                                                                     doorRetriever, delayedInput),
//                                                () -> SetOpenDirection.inputRequestMessage(doorRetriever, localizer),
//                                                RotateDirection.class).getCommandOutput();
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
//     * @param openDir
//     *     The new open direction for the door.
//     * @return See {@link BaseCommand#run()}.
//     */
//    public static CompletableFuture<Boolean> provideDelayedInput(ICommandSender commandSender, IPLogger logger,
//                                                                 ILocalizer localizer,
//                                                                 RotateDirection openDir)
//    {
//        return delayedCommandInputManager().getInputRequest(commandSender)
//                                           .map(request -> request.provide(openDir))
//                                           .orElse(CompletableFuture.completedFuture(false));
//    }
//
//    /**
//     * The method that is run once delayed input is received.
//     * <p>
//     * It processes the new input and executes the command using the previously-provided data (see {@link
//     * #runDelayed(ICommandSender, IPLogger, ILocalizer, DoorRetriever.AbstractRetriever)}).
//     *
//     * @param commandSender
//     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
//     *     execution.
//     * @param doorRetriever
//     *     A {@link DoorRetriever} that references the target door.
//     * @param openDir
//     *     The new open direction for the door.
//     * @return See {@link BaseCommand#run()}.
//     */
//    private static CompletableFuture<Boolean> delayedInputExecutor(ICommandSender commandSender, IPLogger logger,
//                                                                   ILocalizer localizer,
//                                                                   DoorRetriever.AbstractRetriever doorRetriever,
//                                                                   RotateDirection openDir)
//    {
//        return new SetOpenDirection(commandSender, logger, localizer, doorRetriever, openDir).run();
//    }
//
//    /**
//     * Retrieves the message that will be sent to the command sender after initialization of a delayed input request.
//     *
//     * @return The init message for the delayed input request.
//     */
//    private static String inputRequestMessage(DoorRetriever.AbstractRetriever doorRetriever, ILocalizer localizer)
//    {
//        if (!doorRetriever.isAvailable())
//            return localizer.getMessage("commands.set_open_direction.init");
//
//        final var sb = new StringBuilder(localizer.getMessage("commands.set_open_direction.delayed_init_header"))
//            .append('\n');
//
//        final var futureDoor = doorRetriever.getDoor();
//        if (!futureDoor.isDone())
//            throw new IllegalStateException("Door that should be available is not done!");
//        final var optionalDoor = futureDoor.join();
//        if (optionalDoor.isEmpty())
//            throw new IllegalStateException("Door that should be available is not present!");
//
//        final var directions = optionalDoor.get().getDoorType().getValidOpenDirections();
//        for (int idx = 0; idx < directions.size(); ++idx)
//        {
//            sb.append(localizer.getMessage(directions.get(idx).getLocalizationKey()));
//            if (idx < directions.size() - 1)
//                sb.append(", ");
//        }
//        return sb.toString();
//    }

    @AssistedFactory
    interface Factory
    {
        /**
         * Creates (but does not execute!) a new {@link SetOpenDirection} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing open direction of the door.
         * @param doorRetriever
         *     A {@link DoorRetriever} representing the {@link DoorBase} for which the open direction will be modified.
         * @param rotateDirection
         *     The new open direction.
         * @return See {@link BaseCommand#run()}.
         */
        SetOpenDirection newSetOpenDirection(ICommandSender commandSender,
                                             DoorRetriever.AbstractRetriever doorRetriever,
                                             RotateDirection rotateDirection);
    }
}
