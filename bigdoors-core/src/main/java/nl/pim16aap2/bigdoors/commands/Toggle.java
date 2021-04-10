package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.util.DoorRetriever;

public class Toggle extends BaseCommand
{
    private final @NonNull DoorRetriever[] doorRetrievers;
    private final double speedMultiplier;

    public Toggle(@NonNull ICommandSender commandSender)
    {
        super(commandSender);
        doorRetrievers = null;
        speedMultiplier = 1;
    }

    public Toggle(@NonNull ICommandSender commandSender, double speedMultiplier,
                  @NonNull DoorRetriever... doorRetrievers)
    {
        super(commandSender);
        this.speedMultiplier = speedMultiplier;
        this.doorRetrievers = doorRetrievers;
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.TOGGLE;
    }
}
