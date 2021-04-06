package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.Util;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the information command that provides the issuer with more information about the door.
 *
 * @author Pim
 */
@ToString
public class Info extends BaseCommand
{
    private final @NonNull DoorRetriever doorRetriever;

    public Info(@NonNull ICommandSender commandSender, @NonNull DoorRetriever doorRetriever)
    {
        super(commandSender);
        this.doorRetriever = doorRetriever;
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.INFO;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand()
    {
        return getDoor(doorRetriever).thenApplyAsync(
            door ->
            {
                if (door.isEmpty())
                    return false;
                commandSender.sendMessage(door.get().toString());
                highlightBlocks(door.get());
                return true;
            }).exceptionally(t -> Util.exceptionally(t, false));
    }

    private void highlightBlocks(@NonNull AbstractDoorBase doorBase)
    {
        if (!(commandSender instanceof IPPlayer))
            return;
        BigDoors.get().getPlatform().getGlowingBlockSpawner().map(
            spawner -> spawner.spawnGlowingBlocks(doorBase, (IPPlayer) commandSender));
    }
}
