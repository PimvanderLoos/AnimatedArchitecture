package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
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
    protected Info(final @NonNull ICommandSender commandSender, final @NonNull DoorRetriever doorRetriever)
    {
        super(commandSender, doorRetriever, DoorAttribute.INFO);
    }

    /**
     * Runs the {@link Info} command.
     *
     * @param commandSender The {@link ICommandSender} responsible for retrieving the door info and the receiver of the
     *                      door's information.
     * @param doorRetriever A {@link DoorRetriever} representing the {@link AbstractDoorBase} for which the information
     *                      will be retrieved.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NonNull CompletableFuture<Boolean> run(final @NonNull ICommandSender commandSender,
                                                          final @NonNull DoorRetriever doorRetriever)
    {
        return new Info(commandSender, doorRetriever).run();
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.INFO;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> performAction(final @NonNull AbstractDoorBase door)
    {
        getCommandSender().sendMessage(door.toString());
        highlightBlocks(door);
        return CompletableFuture.completedFuture(true);
    }

    protected void highlightBlocks(final @NonNull AbstractDoorBase doorBase)
    {
        if (!(getCommandSender() instanceof IPPlayer))
            return;
        BigDoors.get().getPlatform().getGlowingBlockSpawner()
                .map(spawner -> spawner.spawnGlowingBlocks(doorBase, (IPPlayer) getCommandSender()));
    }
}
