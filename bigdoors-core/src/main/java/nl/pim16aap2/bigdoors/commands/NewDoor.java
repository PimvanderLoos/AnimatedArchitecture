package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to create new doors.
 *
 * @author Pim
 */
@ToString
public class NewDoor extends BaseCommand
{
    private final DoorType doorType;
    private final @Nullable String doorName;
    private final ToolUserManager toolUserManager;

    @AssistedInject
    public NewDoor(@Assisted ICommandSender commandSender, IPLogger logger, ILocalizer localizer,
                   @Assisted DoorType doorType, @Assisted @Nullable String doorName, ToolUserManager toolUserManager)
    {
        super(commandSender, logger, localizer);
        this.doorType = doorType;
        this.doorName = doorName;
        this.toolUserManager = toolUserManager;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.NEW_DOOR;
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        toolUserManager.startToolUser(doorType.getCreator((IPPlayer) getCommandSender(), doorName),
                                      Constants.DOOR_CREATOR_TIME_LIMIT);
        return CompletableFuture.completedFuture(true);
    }

    @AssistedFactory
    interface Factory
    {
        /**
         * Creates (but does not execute!) a new {@link NewDoor} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for creating a new door.
         * @param doorType
         *     The type of door that will be created.
         * @param doorName
         *     The name of the door, if it has been specified already.
         *     <p>
         *     When this is null, the creator will start at the first step (specifying the name). If it has been
         *     specified, this step will be skipped.
         * @return See {@link BaseCommand#run()}.
         */
        NewDoor newNewDoor(ICommandSender commandSender, DoorType doorType, @Nullable String doorName);

        /**
         * See {@link #newNewDoor(ICommandSender, DoorType, String)}.
         */
        default NewDoor newNewDoor(ICommandSender commandSender, DoorType doorType)
        {
            return newNewDoor(commandSender, doorType, null);
        }
    }
}
