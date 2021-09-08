package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
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

    protected NewDoor(ICommandSender commandSender, CommandContext context,
                      DoorType doorType, @Nullable String doorName)
    {
        super(commandSender, context);
        this.doorType = doorType;
        this.doorName = doorName;
    }

    /**
     * Runs the {@link NewDoor} command.
     *
     * @param commandSender
     *     The {@link ICommandSender} responsible for creating a new door.
     * @param doorType
     *     The type of door that will be created.
     * @param doorName
     *     The name of the door, if it has been specified already.
     *     <p>
     *     When this is null, the creator will start at the first step (specifying the name). If it has been specified,
     *     this step will be skipped.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, CommandContext context,
                                                 DoorType doorType, @Nullable String doorName)
    {
        return new NewDoor(commandSender, context, doorType, doorName).run();
    }

    /**
     * Runs the {@link NewDoor} command without a specified name for the door.
     * <p>
     * See {@link #run(ICommandSender, CommandContext, DoorType, String)}.
     *
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, CommandContext context,
                                                 DoorType doorType)
    {
        return run(commandSender, context, doorType, null);
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
        context.getToolUserManager().startToolUser(doorType.getCreator((IPPlayer) getCommandSender(), doorName),
                                                   Constants.DOOR_CREATOR_TIME_LIMIT);
        return CompletableFuture.completedFuture(true);
    }
}
