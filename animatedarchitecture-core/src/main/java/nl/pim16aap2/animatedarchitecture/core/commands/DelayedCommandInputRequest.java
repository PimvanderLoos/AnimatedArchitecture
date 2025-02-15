package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.FutureUtil;
import nl.pim16aap2.animatedarchitecture.core.util.delayedinput.DelayedInputRequest;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a request for delayed additional input for a command.
 * <p>
 * Taking the {@link AddOwner} command as an example, it could be initialized using a GUI, in which case it is known who
 * the {@link ICommandSender} is and what the target {@link Structure} is. However, for some GUIs (e.g. Spigot),
 * it is not yet known who the target player is and what the desired permission level is. This class can then be used to
 * retrieve the additional data that is required to execute the command.
 *
 * @param <T>
 *     The type of data that is to be retrieved from the player.
 */
@ToString
@EqualsAndHashCode(callSuper = false)
@Flogger
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

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final ILocalizer localizer;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final ITextFactory textFactory;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final DelayedCommandInputManager delayedCommandInputManager;

    /**
     * The supplier used to retrieve the message that will be sent to the command sender when this request is
     * initialized (after calling {@link BaseCommand#run()}).
     * <p>
     * If the resulting message is blank, nothing will be sent to the user.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final @Nullable Supplier<String> initMessageSupplier;

    /**
     * The class of the input object that is expected.
     */
    private final Class<T> inputClass;

    /**
     * The output of the command. See {@link BaseCommand#run()}.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Getter(AccessLevel.PROTECTED)
    private final CompletableFuture<?> commandOutput;

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
    @AssistedInject
    DelayedCommandInputRequest(
        @Assisted long timeout,
        @Assisted ICommandSender commandSender,
        @Assisted CommandDefinition commandDefinition,
        @Assisted Function<T, CompletableFuture<?>> executor,
        @Assisted @Nullable Supplier<String> initMessageSupplier,
        @Assisted Class<T> inputClass,
        ILocalizer localizer,
        ITextFactory textFactory,
        DelayedCommandInputManager delayedCommandInputManager)
    {
        super(timeout, TimeUnit.MILLISECONDS);
        this.commandSender = commandSender;
        this.commandDefinition = commandDefinition;
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.delayedCommandInputManager = delayedCommandInputManager;
        this.initMessageSupplier = initMessageSupplier;
        this.inputClass = inputClass;
        log();
        commandOutput = constructOutput(executor);
        init();
    }

    private void init()
    {
        delayedCommandInputManager.register(commandSender, this);
        final @Nullable String initMessage = initMessageSupplier == null ? null : initMessageSupplier.get();
        if (initMessage != null && !initMessage.isBlank())
            commandSender.sendMessage(textFactory, TextType.INFO, initMessage);
    }

    private CompletableFuture<?> constructOutput(Function<T, CompletableFuture<?>> executor)
    {
        return getInputResult()
            .thenCompose(input -> input.map(executor).orElse(CompletableFuture.completedFuture(null)))
            .exceptionally(FutureUtil::exceptionally);
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
    public CompletableFuture<?> provide(Object input)
    {
        if (!inputClass.isInstance(input))
        {
            log.atFine().log("Trying to supply object of type %s for request: %s", input.getClass().getName(), this);
            return CompletableFuture.completedFuture(null);
        }

        //noinspection unchecked
        super.set((T) input);
        return commandOutput;
    }

    @Override
    protected void cleanup()
    {
        delayedCommandInputManager.deregister(commandSender, this);
        if (getStatus() == Status.TIMED_OUT)
            commandSender.sendMessage(textFactory.newText().append(
                localizer.getMessage("commands.base.error.timed_out"),
                TextType.ERROR,
                arg -> arg.highlight(commandDefinition.getName().toLowerCase(Locale.ROOT)))
            );

        if (getStatus() == Status.CANCELLED)
            commandSender.sendMessage(textFactory.newText().append(
                localizer.getMessage("commands.base.error.cancelled"),
                TextType.ERROR,
                arg -> arg.highlight(commandDefinition.getName().toLowerCase(Locale.ROOT)))
            );
    }

    /**
     * Ensures the input request is logged.
     */
    private void log()
    {
        log.atFinest().log("Started delayed input request for command: %s", this);
    }

    @AssistedFactory
    public interface IFactory<T>
    {
        DelayedCommandInputRequest<T> create(
            long timeout,
            ICommandSender commandSender,
            CommandDefinition commandDefinition,
            Function<T, CompletableFuture<?>> executor,
            @Nullable Supplier<String> initMessageSupplier,
            Class<T> inputClass
        );
    }
}
