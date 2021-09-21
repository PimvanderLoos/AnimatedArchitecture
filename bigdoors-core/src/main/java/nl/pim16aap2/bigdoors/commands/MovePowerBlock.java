package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.PowerBlockRelocator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command to initiate the process to move the powerblock of a door to a different location.
 *
 * @author Pim
 */
@ToString
public class MovePowerBlock extends DoorTargetCommand
{
    private final ToolUserManager toolUserManager;
    private final PowerBlockRelocator.IFactory powerBlockRelocatorFactory;

    @AssistedInject //
    MovePowerBlock(@Assisted ICommandSender commandSender, ILocalizer localizer, @Assisted DoorRetriever doorRetriever,
                   ToolUserManager toolUserManager, PowerBlockRelocator.IFactory powerBlockRelocatorFactory)
    {
        super(commandSender, localizer, doorRetriever, DoorAttribute.RELOCATE_POWERBLOCK);
        this.toolUserManager = toolUserManager;
        this.powerBlockRelocatorFactory = powerBlockRelocatorFactory;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.MOVE_POWERBLOCK;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractDoor door)
    {
        toolUserManager.startToolUser(powerBlockRelocatorFactory.create((IPPlayer) getCommandSender(), door),
                                      Constants.DOOR_CREATOR_TIME_LIMIT);
        return CompletableFuture.completedFuture(true);

    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link MovePowerBlock} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for moving the powerblock for the door.
         * @param doorRetriever
         *     A {@link DoorRetrieverFactory} representing the {@link DoorBase} for which the powerblock will be moved.
         * @return See {@link BaseCommand#run()}.
         */
        MovePowerBlock newMovePowerBlock(ICommandSender commandSender,
                                         DoorRetriever doorRetriever);
    }
}
