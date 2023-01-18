package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableAttribute;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.tooluser.PowerBlockRelocator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command to initiate the process to move the powerblock of a movable to a different location.
 *
 * @author Pim
 */
@ToString
public class MovePowerBlock extends MovableTargetCommand
{
    private final ToolUserManager toolUserManager;
    private final PowerBlockRelocator.IFactory powerBlockRelocatorFactory;

    @AssistedInject //
    MovePowerBlock(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted MovableRetriever movableRetriever, ToolUserManager toolUserManager,
        PowerBlockRelocator.IFactory powerBlockRelocatorFactory)
    {
        super(commandSender, localizer, textFactory, movableRetriever, MovableAttribute.RELOCATE_POWERBLOCK);
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
    protected CompletableFuture<Boolean> performAction(AbstractMovable movable)
    {
        toolUserManager.startToolUser(powerBlockRelocatorFactory.create((IPPlayer) getCommandSender(), movable),
                                      Constants.MOVABLE_CREATOR_TIME_LIMIT);
        return CompletableFuture.completedFuture(true);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link MovePowerBlock} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for moving the powerblock for the movable.
         * @param movableRetriever
         *     A {@link MovableRetrieverFactory} representing the {@link MovableBase} for which the powerblock will be
         *     moved.
         * @return See {@link BaseCommand#run()}.
         */
        MovePowerBlock newMovePowerBlock(ICommandSender commandSender, MovableRetriever movableRetriever);
    }
}
