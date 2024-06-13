package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.tooluser.PowerBlockRelocator;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command to initiate the process to move the powerblock of a structure to a different location.
 */
@ToString
public class MovePowerBlock extends StructureTargetCommand
{
    private final ToolUserManager toolUserManager;
    private final PowerBlockRelocator.IFactory powerBlockRelocatorFactory;

    @AssistedInject MovePowerBlock(
        @Assisted ICommandSender commandSender,
        ILocalizer localizer,
        ITextFactory textFactory,
        @Assisted StructureRetriever structureRetriever,
        ToolUserManager toolUserManager,
        PowerBlockRelocator.IFactory powerBlockRelocatorFactory)
    {
        super(commandSender, localizer, textFactory, structureRetriever, StructureAttribute.RELOCATE_POWERBLOCK);
        this.toolUserManager = toolUserManager;
        this.powerBlockRelocatorFactory = powerBlockRelocatorFactory;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.MOVE_POWER_BLOCK;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected CompletableFuture<?> performAction(AbstractStructure structure)
    {
        toolUserManager.startToolUser(
            powerBlockRelocatorFactory.create((IPlayer) getCommandSender(), structure),
            Constants.STRUCTURE_CREATOR_TIME_LIMIT
        );
        return CompletableFuture.completedFuture(null);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link MovePowerBlock} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for moving the powerblock for the structure.
         * @param structureRetriever
         *     A {@link StructureRetrieverFactory} representing the {@link AbstractStructure} for which the powerblock
         *     will be moved.
         * @return See {@link BaseCommand#run()}.
         */
        MovePowerBlock newMovePowerBlock(ICommandSender commandSender, StructureRetriever structureRetriever);
    }
}
