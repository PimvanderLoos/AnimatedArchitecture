package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the information command that provides the issuer with more information about the door.
 *
 * @author Pim
 */
@ToString
public class Info extends DoorTargetCommand
{
    protected Info(final @NotNull ICommandSender commandSender, final @NotNull DoorRetriever doorRetriever)
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
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender,
                                                          final @NotNull DoorRetriever doorRetriever)
    {
        return new Info(commandSender, doorRetriever).run();
    }

    @Override
    public @NotNull CommandDefinition getCommand()
    {
        return CommandDefinition.INFO;
    }

    @Override
    protected @NotNull CompletableFuture<Boolean> performAction(final @NotNull AbstractDoorBase door)
    {
        getCommandSender().sendMessage(door.toString());
        highlightBlocks(door);
        return CompletableFuture.completedFuture(true);
    }

    protected void highlightBlocks(final @NotNull AbstractDoorBase doorBase)
    {
        if (!(getCommandSender() instanceof IPPlayer))
            return;
        BigDoors.get().getPlatform().getGlowingBlockSpawner()
                .ifPresent(spawner -> spawner.spawnGlowingBlocks(doorBase, (IPPlayer) getCommandSender()));
    }
}
