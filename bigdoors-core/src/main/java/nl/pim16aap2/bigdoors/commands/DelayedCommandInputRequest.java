package nl.pim16aap2.bigdoors.commands;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.delayedinput.DelayedInputRequest;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Represents a request for delayed additional input for a command.
 * <p>
 * Taking the {@link AddOwner} command as an example, it could be initialized using a GUI, in which case it is known who
 * the {@link ICommandSender} is and what the target {@link DoorBase} is. However, for some GUIs (e.g. Spigot), it is
 * not yet known who the target player is and what the desired permission level is. This class can then be used to
 * retrieve the additional data that is required to execute the command.
 *
 * @param <T> The type of data that is to be retrieved from the player.
 * @author Pim
 */
@ToString
@EqualsAndHashCode(callSuper = false)
public final class DelayedCommandInputRequest<T> extends DelayedInputRequest<T>
{
    /**
     * See {@link BaseCommand#getCommandSender()}.
     */
    private final @NotNull ICommandSender commandSender;

    /**
     * The {@link CommandDefinition} for which the delayed input will be retrieved.
     */
    private final @NotNull CommandDefinition commandDefinition;

    /**
     * The supplier used to retrieve the message that will be sent to the command sender when this request is
     * initialized (after calling {@link BaseCommand#run()}).
     * <p>
     * If the resulting message is blank, nothing will be sent to the user.
     */
    @ToString.Exclude
    private final @NotNull Supplier<String> initMessageSupplier;

    /**
     * The class of the input object that is expected.
     */
    private final @NotNull Class<T> inputClass;

    /**
     * The output of the command. See {@link BaseCommand#run()}.
     */
    @ToString.Exclude
    @Getter(AccessLevel.PROTECTED)
    private final @NotNull CompletableFuture<Boolean> commandOutput;

    /**
     * Constructs a new delayed command input request.
     *
     * @param timeout             The amount of time (in ms).
     * @param commandSender       See {@link BaseCommand#getCommandSender()}.
     * @param commandDefinition   The {@link CommandDefinition} for which the delayed input will be retrieved.
     * @param executor            The function to execute after retrieving the delayed input from the command sender.
     * @param initMessageSupplier The supplier used to retrieve the message that will be sent to the command sender when
     *                            this request is initialized (after calling {@link BaseCommand#run()}).
     *                            <p>
     *                            If the resulting message is blank, nothing will be sent to the user.
     * @param inputClass          The class of the input object that is expected.
     */
    protected DelayedCommandInputRequest(final long timeout, final @NotNull ICommandSender commandSender,
                                         final @NotNull CommandDefinition commandDefinition,
                                         final @NotNull Function<T, CompletableFuture<Boolean>> executor,
                                         final @NotNull Supplier<String> initMessageSupplier,
                                         final @NotNull Class<T> inputClass)
    {
        super(timeout);
        this.commandSender = commandSender;
        this.commandDefinition = commandDefinition;
        this.initMessageSupplier = initMessageSupplier;
        this.inputClass = inputClass;
        log();
        commandOutput = constructOutput(executor);
        init();
    }

    private void init()
    {
        BigDoors.get().getDelayedCommandInputManager().register(commandSender, this);
        val initMessage = initMessageSupplier.get();
        if (initMessage != null && !initMessage.isBlank())
            commandSender.sendMessage(initMessage);
    }

    private @NotNull CompletableFuture<Boolean> constructOutput(
        final @NotNull Function<T, CompletableFuture<Boolean>> executor)
    {
        return getInputResult()
            .thenCompose(input -> input.map(executor).orElse(CompletableFuture.completedFuture(Boolean.FALSE)))
            .exceptionally(ex -> Util.exceptionally(ex, Boolean.FALSE));
    }

    /**
     * Provides the input object as input for this input request. See {@link DelayedInputRequest#set(Object)}.
     * <p>
     * If the provided input is not of the correct type as defined by {@link #inputClass}, a future containing a false
     * boolean is returned to indicate incorrect command usage.
     *
     * @param input The input object to provide.
     * @return When the input is of the correct type, {@link #commandOutput} is returned, otherwise false.
     */
    protected @NotNull CompletableFuture<Boolean> provide(final @NotNull Object input)
    {
        if (!inputClass.isInstance(input))
        {
            BigDoors.get().getPLogger().logMessage(Level.FINE,
                                                   "Trying to supply object of type " + input.getClass().getName()
                                                       + " for request: " + this);
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }

        //noinspection unchecked
        super.set((T) input);
        return commandOutput;
    }

    @Override
    protected void cleanup()
    {
        BigDoors.get().getDelayedCommandInputManager().deregister(commandSender, this);
        if (getStatus() == Status.TIMED_OUT)
            commandSender.sendMessage(BigDoors.get().getLocalizer().getMessage("commands.base.error.timed_out",
                                                                               commandDefinition.name().toLowerCase()));

        if (getStatus() == Status.CANCELLED)
            commandSender.sendMessage(BigDoors.get().getLocalizer().getMessage("commands.base.error.cancelled",
                                                                               commandDefinition.name().toLowerCase()));
    }

    /**
     * Ensures the input request is logged.
     */
    private void log()
    {
        BigDoors.get().getPLogger()
                .dumpStackTrace(Level.FINEST, "Started delayed input request for command: " + this);
    }
}
