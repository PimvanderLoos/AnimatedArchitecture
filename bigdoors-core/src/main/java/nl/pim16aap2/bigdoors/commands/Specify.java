package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.MovableSpecificationManager;
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.util.delayedinput.DelayedInputRequest;

import java.util.concurrent.CompletableFuture;

/**
 * Implements the command to specify a movable for the {@link MovableSpecificationManager}.
 *
 * @author Pim
 */
@ToString
public class Specify extends BaseCommand
{
    private final String input;
    private final MovableSpecificationManager movableSpecificationManager;

    @AssistedInject //
    Specify(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted String input, MovableSpecificationManager movableSpecificationManager)
    {
        super(commandSender, localizer, textFactory);
        this.input = input;
        this.movableSpecificationManager = movableSpecificationManager;
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
    protected CompletableFuture<Boolean> executeCommand(PermissionsStatus permissions)
    {
        if (!movableSpecificationManager.handleInput((IPPlayer) getCommandSender(), input))
            getCommandSender().sendMessage(textFactory, TextType.ERROR,
                                           localizer.getMessage("commands.base.error.no_pending_process"));
        return CompletableFuture.completedFuture(true);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Specify} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible specifying a movable.
         * @param name
         *     The name/index that specifies a movable based on the {@link DelayedInputRequest} for the command sender
         *     as registered by the {@link MovableSpecificationManager}.
         * @return See {@link BaseCommand#run()}.
         */
        Specify newSpecify(ICommandSender commandSender, String name);
    }
}
