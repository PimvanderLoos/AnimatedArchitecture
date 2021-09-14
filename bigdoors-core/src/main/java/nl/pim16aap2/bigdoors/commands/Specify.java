package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
import nl.pim16aap2.bigdoors.util.CompletableFutureHandler;
import nl.pim16aap2.bigdoors.util.delayedinput.DelayedInputRequest;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

/**
 * Implements the command to specify a door for the {@link DoorSpecificationManager}.
 *
 * @author Pim
 */
@ToString
public class Specify extends BaseCommand
{
    private final String input;
    private final DoorSpecificationManager doorSpecificationManager;

    @AssistedInject //
    Specify(@Assisted ICommandSender commandSender, IPLogger logger, ILocalizer localizer,
            @Assisted String input, DoorSpecificationManager doorSpecificationManager, CompletableFutureHandler handler)
    {
        super(commandSender, logger, localizer, handler);
        this.input = input;
        this.doorSpecificationManager = doorSpecificationManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.SPECIFY;
    }

    // TODO: Make it available for non-players as well.
    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        if (!doorSpecificationManager.handleInput((IPPlayer) getCommandSender(), input))
            getCommandSender().sendMessage(localizer
                                               .getMessage("commands.base.error.no_pending_process"));
        return CompletableFuture.completedFuture(true);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Specify} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible specifying a door.
         * @param name
         *     The name/index that specifies a door based on the {@link DelayedInputRequest} for the command sender as
         *     registered by the {@link DoorSpecificationManager}.
         * @return See {@link BaseCommand#run()}.
         */
        Specify newSpecify(ICommandSender commandSender, String name);
    }
}
