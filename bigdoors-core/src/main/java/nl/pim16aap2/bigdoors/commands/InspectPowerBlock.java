package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.tooluser.PowerBlockInspector;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command to inspect a location to check if there are any powerblocks registered there.
 *
 * @author Pim
 */
@ToString
public class InspectPowerBlock extends BaseCommand
{
    protected InspectPowerBlock(ICommandSender commandSender)
    {
        super(commandSender);
    }

    /**
     * Runs the {@link InspectPowerBlock} command.
     *
     * @param commandSender
     *     The {@link ICommandSender} responsible for inspecting the powerblocks.
     *     <p>
     *     They can only discover {@link DoorBase}s attached to specific locations if they both have access to the
     *     specific location and access to the specific door(s).
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender)
    {
        return new InspectPowerBlock(commandSender).run();
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.INSPECT_POWERBLOCK;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        BigDoors.get().getToolUserManager()
                .startToolUser(new PowerBlockInspector((IPPlayer) getCommandSender(), permissions.second),
                               Constants.DOOR_CREATOR_TIME_LIMIT);
        return CompletableFuture.completedFuture(true);
    }


}
