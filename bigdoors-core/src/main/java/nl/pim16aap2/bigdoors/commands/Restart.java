package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.util.CompletableFutureHandler;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to restart BigDoors.
 *
 * @author Pim
 */
@ToString
public class Restart extends BaseCommand
{
    private final IBigDoorsPlatform bigDoorsPlatform;

    @AssistedInject //
    Restart(@Assisted ICommandSender commandSender, IPLogger logger, ILocalizer localizer,
            IBigDoorsPlatform bigDoorsPlatform, CompletableFutureHandler handler)
    {
        super(commandSender, logger, localizer, handler);
        this.bigDoorsPlatform = bigDoorsPlatform;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.RESTART;
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        bigDoorsPlatform.restartPlugin();
        return CompletableFuture.completedFuture(true);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Restart} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for restarting BigDoors.
         * @return See {@link BaseCommand#run()}.
         */
        Restart newRestart(ICommandSender commandSender);
    }
}
