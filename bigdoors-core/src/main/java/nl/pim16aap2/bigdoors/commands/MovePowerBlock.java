package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
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
    protected MovePowerBlock(ICommandSender commandSender, CommandContext context, DoorRetriever doorRetriever)
    {
        super(commandSender, context, doorRetriever, DoorAttribute.RELOCATE_POWERBLOCK);
    }

    /**
     * Runs the {@link MovePowerBlock} command.
     *
     * @param commandSender
     *     The {@link ICommandSender} responsible for moving the powerblock for the door.
     * @param doorRetriever
     *     A {@link DoorRetriever} representing the {@link DoorBase} for which the powerblock will be moved.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, CommandContext context,
                                                 DoorRetriever doorRetriever)
    {
        return new MovePowerBlock(commandSender, context, doorRetriever).run();
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
        context.getToolUserManager()
               .startToolUser(new PowerBlockRelocator((IPPlayer) getCommandSender(), door),
                              Constants.DOOR_CREATOR_TIME_LIMIT);
        return CompletableFuture.completedFuture(true);

    }
}
