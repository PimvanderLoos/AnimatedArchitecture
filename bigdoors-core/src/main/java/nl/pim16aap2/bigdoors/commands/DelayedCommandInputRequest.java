package nl.pim16aap2.bigdoors.commands;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.delayedinput.DelayedInputRequest;

import java.util.Locale;
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
 * @param <T>
 *     The type of data that is to be retrieved from the player.
 * @author Pim
 */
@ToString
@EqualsAndHashCode(callSuper = false)
public final class DelayedCommandInputRequest<T> extends DelayedInputRequest<T>
{
    /**
     * See {@link BaseCommand#getCommandSender()}.
     */
    private final ICommandSender commandSender;

    /**
     * The {@link CommandDefinition} for which the delayed input will be retrieved.
     */
    private final CommandDefinition commandDefinition;

    private final IPLogger logger;

    private final ILocalizer localizer;

    private final CommandContext context;

    /**
     * The supplier used to retrieve the message that will be sent to the command sender when this request is
     * initialized (after calling {@link BaseCommand#run()}).
     * <p>
     * If the resulting message is blank, nothing will be sent to the user.
     */
    @ToString.Exclude
    private final Supplier<String> initMessageSupplier;

    /**
     * The class of the input object that is expected.
     */
    private final Class<T> inputClass;

    /**
     * The output of the command. See {@link BaseCommand#run()}.
     */
    @ToString.Exclude
    @Getter(AccessLevel.PROTECTED)
    private final CompletableFuture<Boolean> commandOutput;

    /**
     * Constructs a new delayed command input request.
     *
     * @param timeout
     *     The amount of time (in ms).
     * @param commandSender
     *     See {@link BaseCommand#getCommandSender()}.
     * @param commandDefinition
     *     The {@link CommandDefinition} for which the delayed input will be retrieved.
     * @param executor
     *     The function to execute after retrieving the delayed input from the command sender.
     * @param initMessageSupplier
     *     The supplier used to retrieve the message that will be sent to the command sender when this request is
     *     initialized (after calling {@link BaseCommand#run()}).
     *     <p>
     *     If the resulting message is blank, nothing will be sent to the user.
     * @param inputClass
     *     The class of the input object that is expected.
     */
    DelayedCommandInputRequest(long timeout, ICommandSender commandSender, CommandDefinition commandDefinition,
                               CommandContext context, Function<T, CompletableFuture<Boolean>> executor,
                               Supplier<String> initMessageSupplier, Class<T> inputClass)
    {
        super(timeout);
        this.commandSender = commandSender;
        this.commandDefinition = commandDefinition;
        this.context = context;
        logger = this.context.getLogger();
        localizer = this.context.getLocalizer();
        this.initMessageSupplier = initMessageSupplier;
        this.inputClass = inputClass;
        log();
        commandOutput = constructOutput(executor);
        init();
    }

    private void init()
    {
        context.getDelayedCommandInputManager().register(commandSender, this);
        final var initMessage = initMessageSupplier.get();
        //noinspection ConstantConditions
        if (initMessage != null && !initMessage.isBlank())
            commandSender.sendMessage(initMessage);
    }

    private CompletableFuture<Boolean> constructOutput(Function<T, CompletableFuture<Boolean>> executor)
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
     * @param input
     *     The input object to provide.
     * @return When the input is of the correct type, {@link #commandOutput} is returned, otherwise false.
     */
    CompletableFuture<Boolean> provide(Object input)
    {
        if (!inputClass.isInstance(input))
        {
            logger.logMessage(Level.FINE,
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
        context.getDelayedCommandInputManager().deregister(commandSender, this);
        if (getStatus() == Status.TIMED_OUT)
            commandSender.sendMessage(localizer.getMessage("commands.base.error.timed_out",
                                                           commandDefinition.name().toLowerCase(Locale.ENGLISH)));

        if (getStatus() == Status.CANCELLED)
            commandSender.sendMessage(localizer.getMessage("commands.base.error.cancelled",
                                                           commandDefinition.name().toLowerCase(Locale.ENGLISH)));
    }

    /**
     * Ensures the input request is logged.
     */
    private void log()
    {
        logger.dumpStackTrace(Level.FINEST, "Started delayed input request for command: " + this);
    }
}
