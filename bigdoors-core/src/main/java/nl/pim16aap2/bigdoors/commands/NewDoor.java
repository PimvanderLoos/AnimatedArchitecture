package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.Util;
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
    final @NonNull DoorType doorType;
    final @Nullable String doorName;

    public NewDoor(final @NonNull ICommandSender commandSender, final @NonNull DoorType doorType)
    {
        this(commandSender, doorType, null);
    }

    public NewDoor(final @NonNull ICommandSender commandSender, final @NonNull DoorType doorType,
                   final @Nullable String doorName)
    {
        super(commandSender);
        this.doorType = doorType;
        this.doorName = verifyDoorName(doorName);
    }

    /**
     * Ensures that the provided {@link #doorName} is valid (see {@link Util#isValidDoorName(String)}).
     * <p>
     * If the provided input is not a valid door name, null is returned and the user is informed. However, in the
     * special situation where the provided name is simply null, the user will not be informed, as it is assumed that
     * this is on purpose.
     *
     * @param doorName The input name to check. May be null.
     * @return The input name if it is a valid name.
     */
    private @Nullable String verifyDoorName(final @Nullable String doorName)
    {
        if (doorName == null || Util.isValidDoorName(doorName))
            return doorName;

        getCommandSender().sendMessage("The name \"" + doorName + "\" is not valid! Please select a different name");
        return null;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.NEW_DOOR;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        BigDoors.get().getToolUserManager().startToolUser(doorType.getCreator((IPPlayer) getCommandSender(), doorName),
                                                          Constants.DOOR_CREATOR_TIME_LIMIT);
        return CompletableFuture.completedFuture(true);
    }
}
