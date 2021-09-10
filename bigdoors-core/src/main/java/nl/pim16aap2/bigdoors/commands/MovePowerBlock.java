package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.PowerBlockRelocator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;

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

    @AssistedInject //
    MovePowerBlock(@Assisted ICommandSender commandSender, IPLogger logger, ILocalizer localizer,
                   @Assisted DoorRetriever.AbstractRetriever doorRetriever, ToolUserManager toolUserManager)
    {
        super(commandSender, logger, localizer, doorRetriever, DoorAttribute.RELOCATE_POWERBLOCK);
        this.toolUserManager = toolUserManager;
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
        toolUserManager.startToolUser(new PowerBlockRelocator((IPPlayer) getCommandSender(), door),
                                      Constants.DOOR_CREATOR_TIME_LIMIT);
        return CompletableFuture.completedFuture(true);

    }

    @AssistedFactory
    interface Factory
    {
        /**
         * Creates (but does not execute!) a new {@link MovePowerBlock} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for moving the powerblock for the door.
         * @param doorRetriever
         *     A {@link DoorRetriever} representing the {@link DoorBase} for which the powerblock will be moved.
         * @return See {@link BaseCommand#run()}.
         */
        MovePowerBlock newMovePowerBlock(ICommandSender commandSender, DoorRetriever.AbstractRetriever doorRetriever);
    }
}
