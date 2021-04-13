package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

@ToString
public class Toggle extends BaseCommand
{
    private final @NonNull DoorRetriever[] doorRetrievers;
    private final double speedMultiplier;

    public Toggle(@NonNull ICommandSender commandSender, double speedMultiplier,
                  @NonNull DoorRetriever... doorRetrievers)
    {
        super(commandSender);
        this.speedMultiplier = speedMultiplier;
        this.doorRetrievers = doorRetrievers;
    }

    public Toggle(@NonNull ICommandSender commandSender, @NonNull DoorRetriever... doorRetrievers)
    {
        this(commandSender, 0D, doorRetrievers);
    }

    /**
     * Gets the {@link DoorActionType} for this command.
     */
    public @NonNull DoorActionType getDoorActionType()
    {
        return DoorActionType.TOGGLE;
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.TOGGLE;
    }

    @Override
    protected final @NonNull CompletableFuture<Boolean> executeCommand(@NonNull BooleanPair permissions)
    {
        throw new UnsupportedOperationException("This command has not yet been implemented!");
    }
}
