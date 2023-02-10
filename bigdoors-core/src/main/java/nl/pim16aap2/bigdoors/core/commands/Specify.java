package nl.pim16aap2.bigdoors.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.managers.StructureSpecificationManager;
import nl.pim16aap2.bigdoors.core.text.TextType;
import nl.pim16aap2.bigdoors.core.util.delayedinput.DelayedInputRequest;

import java.util.concurrent.CompletableFuture;

/**
 * Implements the command to specify a structure for the {@link StructureSpecificationManager}.
 *
 * @author Pim
 */
@ToString
public class Specify extends BaseCommand
{
    private final String input;
    private final StructureSpecificationManager structureSpecificationManager;

    @AssistedInject //
    Specify(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted String input, StructureSpecificationManager structureSpecificationManager)
    {
        super(commandSender, localizer, textFactory);
        this.input = input;
        this.structureSpecificationManager = structureSpecificationManager;
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
    protected CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        if (!structureSpecificationManager.handleInput((IPlayer) getCommandSender(), input))
            getCommandSender().sendMessage(
                textFactory, TextType.ERROR, localizer.getMessage("commands.base.error.no_pending_process"));
        return CompletableFuture.completedFuture(null);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Specify} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible specifying a structure.
         * @param name
         *     The name/index that specifies a structure based on the {@link DelayedInputRequest} for the command sender
         *     as registered by the {@link StructureSpecificationManager}.
         * @return See {@link BaseCommand#run()}.
         */
        Specify newSpecify(ICommandSender commandSender, String name);
    }
}
