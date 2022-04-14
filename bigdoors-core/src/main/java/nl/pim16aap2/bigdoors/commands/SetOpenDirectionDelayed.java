package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents the delayed version of {@link SetOpenDirection}.
 * <p>
 * The delayed
 *
 * @author Pim
 */
@Singleton
public class SetOpenDirectionDelayed
{
    private final DelayedCommandInputManager delayedCommandInputManager;
    private final ILocalizer localizer;
    private final Provider<CommandFactory> commandFactory;
    private final DelayedCommandInputRequest.IFactory<RotateDirection> inputRequestFactory;

    @Inject SetOpenDirectionDelayed(
        DelayedCommandInputManager delayedCommandInputManager, ILocalizer localizer,
        Provider<CommandFactory> commandFactory,
        DelayedCommandInputRequest.IFactory<RotateDirection> inputRequestFactory)
    {
        this.delayedCommandInputManager = delayedCommandInputManager;
        this.localizer = localizer;
        this.commandFactory = commandFactory;
        this.inputRequestFactory = inputRequestFactory;
    }

    /**
     * Executes the {@link SetOpenDirection} command without a known rotateDirection.
     * <p>
     * These missing values will be retrieved using a {@link DelayedCommandInputRequest}. The player will be asked to
     * use the {@link SetOpenDirection} command (again, if needed) to supply the missing data.
     * <p>
     * These missing data can be supplied using {@link #provideDelayedInput(ICommandSender, RotateDirection)}.
     *
     * @param commandSender
     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
     *     execution.
     * @param doorRetriever
     *     A {@link DoorRetrieverFactory} that references the target door.
     * @return See {@link BaseCommand#run()}.
     */
    public CompletableFuture<Boolean> runDelayed(ICommandSender commandSender, DoorRetriever doorRetriever)
    {
        final Function<RotateDirection, CompletableFuture<Boolean>> executor =
            (delayedInput) -> delayedInputExecutor(commandSender, doorRetriever, delayedInput);

        final Supplier<String> initMessageSupplier = () -> inputRequestMessage(doorRetriever);

        final int commandTimeout = Constants.COMMAND_WAITER_TIMEOUT;
        return inputRequestFactory.create(commandTimeout, commandSender, SetOpenDirection.COMMAND_DEFINITION,
                                          executor, initMessageSupplier, RotateDirection.class)
                                  .getCommandOutput();
    }

    /**
     * Provides the delayed input if there is currently an active {@link DelayedCommandInputRequest} for the {@link
     * ICommandSender}.
     * <p>
     * If no active {@link DelayedCommandInputRequest} can be found for the command sender, the command sender will be
     * informed about it.
     *
     * @param commandSender
     *     The {@link ICommandSender} for which to look for an active {@link DelayedCommandInputRequest} that can be
     *     fulfilled.
     * @param openDir
     *     The new open direction for the door.
     * @return See {@link BaseCommand#run()}.
     */
    public CompletableFuture<Boolean> provideDelayedInput(ICommandSender commandSender, RotateDirection openDir)
    {
        return delayedCommandInputManager
            .getInputRequest(commandSender)
            .map(request -> request.provide(openDir))
            .orElseGet(
                () ->
                {
                    commandSender.sendMessage(localizer.getMessage("commands.set_open_direction.delayed.not_waiting"));
                    return CompletableFuture.completedFuture(false);
                });
    }

    /**
     * The method that is run once delayed input is received.
     * <p>
     * It processes the new input and executes the command using the previously-provided data (see {@link
     * #runDelayed(ICommandSender, DoorRetriever)}).
     *
     * @param commandSender
     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
     *     execution.
     * @param doorRetriever
     *     A {@link DoorRetrieverFactory} that references the target door.
     * @param openDir
     *     The new open direction for the door.
     * @return See {@link BaseCommand#run()}.
     */
    private CompletableFuture<Boolean> delayedInputExecutor(
        ICommandSender commandSender, DoorRetriever doorRetriever, RotateDirection openDir)
    {
        return commandFactory.get().newSetOpenDirection(commandSender, doorRetriever, openDir).run();
    }

    /**
     * Retrieves the message that will be sent to the command sender after initialization of a delayed input request.
     *
     * @return The init message for the delayed input request.
     */
    private String inputRequestMessage(DoorRetriever doorRetriever)
    {
        if (!doorRetriever.isAvailable())
            return localizer.getMessage("commands.set_open_direction.delayed.init");

        final var sb = new StringBuilder(localizer.getMessage("commands.set_open_direction.delayed.init_header"))
            .append('\n');

        final var futureDoor = doorRetriever.getDoor();
        if (!futureDoor.isDone())
            throw new IllegalStateException("Door that should be available is not done!");
        final var optionalDoor = futureDoor.join();
        if (optionalDoor.isEmpty())
            throw new IllegalStateException("Door that should be available is not present!");

        final var directions = optionalDoor.get().getDoorType().getValidOpenDirections();
        for (int idx = 0; idx < directions.size(); ++idx)
        {
            sb.append(localizer.getMessage(directions.get(idx).getLocalizationKey()));
            if (idx < directions.size() - 1)
                sb.append(", ");
        }
        return sb.toString();
    }
}
