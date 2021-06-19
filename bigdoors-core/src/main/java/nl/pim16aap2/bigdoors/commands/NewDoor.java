package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
import org.jetbrains.annotations.NotNull;
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
    private final @NotNull DoorType doorType;
    private final @Nullable String doorName;

    protected NewDoor(final @NotNull ICommandSender commandSender, final @NotNull DoorType doorType,
                      final @Nullable String doorName)
    {
        super(commandSender);
        this.doorType = doorType;
        this.doorName = doorName;
    }

    /**
     * Runs the {@link NewDoor} command.
     *
     * @param commandSender The {@link ICommandSender} responsible for creating a new door.
     * @param doorType      The type of door that will be created.
     * @param doorName      The name of the door, if it has been specified already.
     *                      <p>
     *                      When this is null, the creator will start at the first step (specifying the name). If it has
     *                      been specified, this step will be skipped.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender,
                                                          final @NotNull DoorType doorType,
                                                          final @Nullable String doorName)
    {
        return new NewDoor(commandSender, doorType, doorName).run();
    }

    /**
     * Runs the {@link NewDoor} command without a specified name for the door.
     * <p>
     * See {@link #run(ICommandSender, DoorType, String)}.
     *
     * @return See {@link BaseCommand#run()}.
     */
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender,
                                                          final @NotNull DoorType doorType)
    {
        return run(commandSender, doorType, null);
    }

    @Override
    protected boolean validInput()
    {
        if (doorName == null || Util.isValidDoorName(doorName))
            return true;

        // TODO: Localization
        getCommandSender().sendMessage("The name \"" + doorName + "\" is not valid! Please select a different name");
        return false;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    public @NotNull CommandDefinition getCommand()
    {
        return CommandDefinition.NEW_DOOR;
    }

    @Override
    protected @NotNull CompletableFuture<Boolean> executeCommand(final @NotNull BooleanPair permissions)
    {
        BigDoors.get().getToolUserManager().startToolUser(doorType.getCreator((IPPlayer) getCommandSender(), doorName),
                                                          Constants.DOOR_CREATOR_TIME_LIMIT);
        return CompletableFuture.completedFuture(true);
    }
}
