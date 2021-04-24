package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
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
    protected MovePowerBlock(final @NonNull ICommandSender commandSender, final @NonNull DoorRetriever doorRetriever)
    {
        super(commandSender, doorRetriever, DoorAttribute.RELOCATE_POWERBLOCK);
    }

    /**
     * Runs the {@link MovePowerBlock} command.
     *
     * @param commandSender The {@link ICommandSender} responsible for moving the powerblock for the door.
     * @param doorRetriever A {@link DoorRetriever} representing the {@link AbstractDoorBase} for which the powerblock
     *                      will be moved.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NonNull CompletableFuture<Boolean> run(final @NonNull ICommandSender commandSender,
                                                          final @NonNull DoorRetriever doorRetriever)
    {
        return new MovePowerBlock(commandSender, doorRetriever).run();
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.MOVE_POWERBLOCK;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> performAction(final @NonNull AbstractDoorBase door)
    {
        BigDoors.get().getToolUserManager()
                .startToolUser(new PowerBlockRelocator((IPPlayer) getCommandSender(), door),
                               Constants.DOOR_CREATOR_TIME_LIMIT);
        return CompletableFuture.completedFuture(true);

    }
}
