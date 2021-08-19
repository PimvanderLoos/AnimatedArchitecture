package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.util.Constants;
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

    protected SetOpenDirection(ICommandSender commandSender, DoorRetriever doorRetriever,
                               RotateDirection rotateDirection)
    {
        super(commandSender, doorRetriever, DoorAttribute.OPEN_DIRECTION);
        this.rotateDirection = rotateDirection;
    }

    /**
     * Runs the {@link SetOpenDirection} command.
     *
     * @param commandSender   The {@link ICommandSender} responsible for changing open direction of the door.
     * @param doorRetriever   A {@link DoorRetriever} representing the {@link DoorBase} for which the open direction
     *                        will be modified.
     * @param rotateDirection The new open direction.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, DoorRetriever doorRetriever,
                                                 RotateDirection rotateDirection)
    {
        return new SetOpenDirection(commandSender, doorRetriever, rotateDirection).run();
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
                BigDoors.get().getLocalizer().getMessage("commands.set_open_direction.error.invalid_rotation",
                                                         rotateDirection.name(), door.getBasicInfo()));

            return CompletableFuture.completedFuture(true);
        }

        door.setOpenDir(rotateDirection);
        return door.syncData().thenApply(x -> true);
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
    public static CompletableFuture<Boolean> runDelayed(ICommandSender commandSender, DoorRetriever doorRetriever)
    {
        final int commandTimeout = Constants.COMMAND_WAITER_TIMEOUT;
        return new DelayedCommandInputRequest<>(commandTimeout, commandSender, COMMAND_DEFINITION,
                                                delayedInput -> delayedInputExecutor(commandSender,
                                                                                     doorRetriever,
                                                                                     delayedInput),
                                                () -> SetOpenDirection.inputRequestMessage(doorRetriever),
                                                RotateDirection.class).getCommandOutput();
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
    public static CompletableFuture<Boolean> provideDelayedInput(ICommandSender commandSender, RotateDirection openDir)
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
    private static CompletableFuture<Boolean> delayedInputExecutor(ICommandSender commandSender,
                                                                   DoorRetriever doorRetriever, RotateDirection openDir)
    {
        return new SetOpenDirection(commandSender, doorRetriever, openDir).run();
    }

    /**
     * Retrieves the message that will be sent to the command sender after initialization of a delayed input request.
     *
     * @return The init message for the delayed input request.
     */
    private static String inputRequestMessage(DoorRetriever doorRetriever)
    {
        val localizer = BigDoors.get().getLocalizer();
        if (!doorRetriever.isAvailable())
            return localizer.getMessage("commands.set_open_direction.init");

        val sb = new StringBuilder(localizer.getMessage("commands.set_open_direction.delayed_init_header"))
            .append("\n");

        val futureDoor = doorRetriever.getDoor();
        if (!futureDoor.isDone())
            throw new IllegalStateException("Door that should be available is not done!");
        val optionalDoor = futureDoor.join();
        if (optionalDoor.isEmpty())
            throw new IllegalStateException("Door that should be available is not present!");

        val directions = optionalDoor.get().getDoorType().getValidOpenDirections();
        for (int idx = 0; idx < directions.size(); ++idx)
        {
            sb.append(localizer.getMessage(directions.get(idx).getLocalizationKey()));
            if (idx < directions.size() - 1)
                sb.append(", ");
        }
        return sb.toString();
    }
}
