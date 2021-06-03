package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that changes the opening direction of doors.
 *
 * @author Pim
 */
@ToString
public class SetOpenDirection extends DoorTargetCommand
{
    private final @NotNull RotateDirection rotateDirection;

    private static final @NotNull CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_OPEN_DIR;

    protected SetOpenDirection(final @NotNull ICommandSender commandSender, final @NotNull DoorRetriever doorRetriever,
                               final @NotNull RotateDirection rotateDirection)
    {
        super(commandSender, doorRetriever, DoorAttribute.OPEN_DIRECTION);
        this.rotateDirection = rotateDirection;
    }

    /**
     * Runs the {@link SetOpenDirection} command.
     *
     * @param commandSender   The {@link ICommandSender} responsible for changing open direction of the door.
     * @param doorRetriever   A {@link DoorRetriever} representing the {@link AbstractDoorBase} for which the open
     *                        direction will be modified.
     * @param rotateDirection The new open direction.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender,
                                                          final @NotNull DoorRetriever doorRetriever,
                                                          final @NotNull RotateDirection rotateDirection)
    {
        return new SetOpenDirection(commandSender, doorRetriever, rotateDirection).run();
    }

    @Override
    public @NotNull CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected @NotNull CompletableFuture<Boolean> performAction(final @NotNull AbstractDoorBase door)
    {
        if (!door.getDoorType().isValidOpenDirection(rotateDirection))
        {
            // TODO: Localization
            getCommandSender().sendMessage(
                rotateDirection.name() + " is not a valid rotation direction for door " + door.getBasicInfo());
            return CompletableFuture.completedFuture(true);
        }

        return door.setOpenDir(rotateDirection).syncData().thenApply(x -> true);
    }

    /**
     * Executes the {@link SetOpenDirection} command without a known {@link #rotateDirection}.
     * <p>
     * These missing values will be retrieved using a {@link DelayedCommandInputRequest}. The player will be asked to
     * use the {@link SetOpenDirection} command (again, if needed) to supply the missing data.
     * <p>
     * These missing data can be supplied using {@link #provideDelayedInput(ICommandSender, RotateDirection)}.
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
                                                SetOpenDirection::inputRequestMessage, RotateDirection.class)
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
     * @param openDir       The new open direction for the door.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NotNull CompletableFuture<Boolean> provideDelayedInput(final @NotNull ICommandSender commandSender,
                                                                          final @NotNull RotateDirection openDir)
    {
        return BigDoors.get().getDelayedCommandInputManager().getInputRequest(commandSender)
                       .map(request -> request.provide(openDir))
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
     * @param openDir       The new open direction for the door.
     * @return See {@link BaseCommand#run()}.
     */
    private static @NotNull CompletableFuture<Boolean> delayedInputExecutor(final @NotNull ICommandSender commandSender,
                                                                            final @NotNull DoorRetriever doorRetriever,
                                                                            final @NotNull RotateDirection openDir)
    {
        return new SetOpenDirection(commandSender, doorRetriever, openDir).run();
    }

    /**
     * Retrieves the message that will be sent to the command sender after initialization of a delayed input request.
     *
     * @return The init message for the delayed input request.
     */
    private static @NotNull String inputRequestMessage()
    {
        return BigDoors.get().getPlatform().getMessages().getString(Message.COMMAND_SET_OPEN_DIR_DELAYED_INIT);
    }
}
