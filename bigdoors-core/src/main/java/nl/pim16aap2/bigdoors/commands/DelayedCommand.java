package nl.pim16aap2.bigdoors.commands;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.delayedinput.DelayedInputRequest;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a delayed command.
 * <p>
 * A delayed command allows "starting" the command while only knowing the target movable. This is useful when the
 * movable is already known (e.g. after having been selected in a GUI), but the other specific data is not.
 * <p>
 * An example usage of this system could be changing the name of a movable via a GUI. Using the GUI, the user has to
 * select a movable to apply the change to, but we'll need to wait for user input to update the name. For a user, having
 * to use specify the movable they would need to change in a command or something would be rather awkward, so this way
 * we can remember that information and not require the user to input duplicate data.
 *
 * @author Pim
 */
@Flogger
public abstract class DelayedCommand<T>
{
    protected final DelayedCommandInputManager delayedCommandInputManager;
    protected final ILocalizer localizer;
    protected final ITextFactory textFactory;
    protected final Provider<CommandFactory> commandFactory;
    protected final DelayedCommandInputRequest.IFactory<T> inputRequestFactory;
    private final Class<T> delayedInputClz;

    protected DelayedCommand(
        Context context,
        DelayedCommandInputRequest.IFactory<T> inputRequestFactory,
        Class<T> delayedInputClz)
    {
        this.delayedCommandInputManager = context.delayedCommandInputManager;
        this.localizer = context.localizer;
        this.commandFactory = context.commandFactoryProvider;
        this.inputRequestFactory = inputRequestFactory;
        this.delayedInputClz = delayedInputClz;
        this.textFactory = context.textFactory;
    }

    /**
     * Starts the (new) {@link DelayedInputRequest} for this delayed command.
     * <p>
     * The {@link DelayedCommandInputRequest} will be used to retrieve the values that are required to execute the
     * command. The player will be asked to use the command (again, if needed) to supply the missing data.
     * <p>
     * These missing data can be supplied using {@link #provideDelayedInput(ICommandSender, T)}. Once the data are
     * supplied, the command will be executed.
     *
     * @param commandSender
     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
     *     execution.
     * @param creator
     *     A {@link Creator} that will wait for the input.
     */
    public CompletableFuture<Boolean> runDelayed(
        ICommandSender commandSender, Creator creator, Function<T, CompletableFuture<Boolean>> executor,
        @Nullable Supplier<String> initMessageSupplier)
    {
        log.atFinest()
           .log("Creating delayed command for command '%s' with command sender: '%s' for Creator: %s",
                getCommandDefinition(), commandSender, creator);

        final int commandTimeout = Constants.COMMAND_WAITER_TIMEOUT;
        return inputRequestFactory.create(commandTimeout, commandSender, getCommandDefinition(),
                                          wrapExecutor(commandSender, executor), initMessageSupplier, delayedInputClz)
                                  .getCommandOutput();
    }

    /**
     * Starts the (new) {@link DelayedInputRequest} for this delayed command.
     * <p>
     * The {@link DelayedCommandInputRequest} will be used to retrieve the values that are required to execute the
     * command. The player will be asked to use the command (again, if needed) to supply the missing data.
     * <p>
     * These missing data can be supplied using {@link #provideDelayedInput(ICommandSender, T)}. Once the data are
     * supplied, the command will be executed.
     *
     * @param commandSender
     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
     *     execution.
     * @param movableRetriever
     *     A {@link MovableRetrieverFactory} that references the target movable.
     * @return See {@link BaseCommand#run()}.
     */
    public CompletableFuture<Boolean> runDelayed(ICommandSender commandSender, MovableRetriever movableRetriever)
    {
        log.atFinest()
           .log("Creating delayed command for command '%s' with command sender: '%s' for MovableRetriever: %s",
                getCommandDefinition(), commandSender, movableRetriever);

        final int commandTimeout = Constants.COMMAND_WAITER_TIMEOUT;
        return inputRequestFactory.create(commandTimeout, commandSender, getCommandDefinition(),
                                          getExecutor(commandSender, movableRetriever),
                                          () -> inputRequestMessage(commandSender, movableRetriever), delayedInputClz)
                                  .getCommandOutput();
    }

    private Function<T, CompletableFuture<Boolean>> getExecutor(
        ICommandSender commandSender, MovableRetriever movableRetriever)
    {
        return delayedInput ->
        {
            try
            {
                return delayedInputExecutor(commandSender, movableRetriever, delayedInput);
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e)
                   .log("Failed to executed delayed command '%s' for command sender '%s' with input '%s'",
                        this, commandSender, delayedInput);
                e.printStackTrace();
                return CompletableFuture.completedFuture(false);
            }
        };
    }

    private Function<T, CompletableFuture<Boolean>> wrapExecutor(
        ICommandSender commandSender, Function<T, CompletableFuture<Boolean>> executor)
    {
        return delayedInput ->
        {
            try
            {
                return executor.apply(delayedInput);
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e)
                   .log("Delayed command '%s' failed to provide data for command sender '%s' with input '%s'",
                        this, commandSender, delayedInput);
                e.printStackTrace();
                return CompletableFuture.completedFuture(false);
            }
        };
    }

    /**
     * Provides the delayed input if there is currently an active {@link DelayedCommandInputRequest} for the
     * {@link ICommandSender}. After processing the input, the new command will be executed immediately.
     * <p>
     * If no active {@link DelayedCommandInputRequest} can be found for the command sender, the command sender will be
     * informed about it (e.g. "We are not waiting for a command!").
     *
     * @param commandSender
     *     The {@link ICommandSender} for which to look for an active {@link DelayedCommandInputRequest} that can be
     *     fulfilled.
     * @param data
     *     The data specified by the user.
     * @return See {@link Creator#handleInput(Object)}.
     */
    public CompletableFuture<Boolean> provideDelayedInput(ICommandSender commandSender, T data)
    {
        log.atFinest()
           .log("Providing delayed command data for command '%s' with command sender: '%s' and data: '%s'",
                getCommandDefinition(), commandSender, data);

        return delayedCommandInputManager
            .getInputRequest(commandSender)
            .map(request -> request.provide(data))
            .orElseGet(
                () ->
                {
                    log.atSevere()
                       .log("'%s' tried to issue delayed command input '%s' without active command waiter!",
                            commandSender, data);
                    commandSender.sendMessage(textFactory, TextType.ERROR,
                                              localizer.getMessage("commands.base.error.not_waiting"));
                    return CompletableFuture.completedFuture(false);
                });
    }

    protected abstract CommandDefinition getCommandDefinition();

    /**
     * The method that is run once delayed input is received.
     * <p>
     * It processes the new input and executes the command using the previously-provided data (see
     * {@link #runDelayed(ICommandSender, MovableRetriever)}).
     *
     * @param commandSender
     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
     *     execution.
     * @param movableRetriever
     *     A {@link MovableRetrieverFactory} that references the target movable.
     * @param delayedInput
     *     The delayed input that was retrieved.
     * @return See {@link BaseCommand#run()}.
     */
    protected abstract CompletableFuture<Boolean> delayedInputExecutor(
        ICommandSender commandSender, MovableRetriever movableRetriever, T delayedInput);

    /**
     * Retrieves the message that will be sent to the command sender after initialization of a delayed input request.
     *
     * @param commandSender
     *     The user responsible for the delayed command.
     * @param movableRetriever
     *     The movable retriever as currently specified.
     * @return The init message for the delayed input request.
     */
    protected abstract String inputRequestMessage(ICommandSender commandSender, MovableRetriever movableRetriever);

    @Singleton
    public static final class Context
    {
        private final DelayedCommandInputManager delayedCommandInputManager;
        private final ILocalizer localizer;
        private final ITextFactory textFactory;
        private final Provider<CommandFactory> commandFactoryProvider;

        @Inject
        public Context(
            DelayedCommandInputManager delayedCommandInputManager,
            ILocalizer localizer,
            ITextFactory textFactory,
            Provider<CommandFactory> commandFactoryProvider)
        {
            this.delayedCommandInputManager = delayedCommandInputManager;
            this.localizer = localizer;
            this.textFactory = textFactory;
            this.commandFactoryProvider = commandFactoryProvider;
        }
    }
}
