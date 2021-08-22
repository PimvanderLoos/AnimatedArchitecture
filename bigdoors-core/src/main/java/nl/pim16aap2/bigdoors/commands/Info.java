package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the information command that provides the issuer with more information about the door.
 *
 * @author Pim
 */
@ToString
public class Info extends DoorTargetCommand
{
    protected Info(ICommandSender commandSender, DoorRetriever doorRetriever)
    {
        super(commandSender, doorRetriever, DoorAttribute.INFO);
    }

    /**
     * Runs the {@link Info} command.
     *
     * @param commandSender
     *     The {@link ICommandSender} responsible for retrieving the door info and the receiver of the door's
     *     information.
     * @param doorRetriever
     *     A {@link DoorRetriever} representing the {@link DoorBase} for which the information will be retrieved.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, DoorRetriever doorRetriever)
    {
        return new Info(commandSender, doorRetriever).run();
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.INFO;
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractDoor door)
    {
        getCommandSender().sendMessage(door.toString());
        highlightBlocks(door);
        return CompletableFuture.completedFuture(true);
    }

    protected void highlightBlocks(AbstractDoor doorBase)
    {
        if (!(getCommandSender() instanceof IPPlayer))
            return;
        BigDoors.get().getPlatform().getGlowingBlockSpawner()
                .ifPresent(spawner -> spawner.spawnGlowingBlocks(doorBase, (IPPlayer) getCommandSender()));
    }
}
