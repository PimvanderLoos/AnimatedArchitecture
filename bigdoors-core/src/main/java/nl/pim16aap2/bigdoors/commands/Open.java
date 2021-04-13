package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.DoorRetriever;

@ToString
public class Open extends Toggle
{
    public Open(@NonNull ICommandSender commandSender, double speedMultiplier,
                @NonNull DoorRetriever... doorRetrievers)
    {
        super(commandSender, speedMultiplier, doorRetrievers);
    }

    public Open(@NonNull ICommandSender commandSender, @NonNull DoorRetriever[] doorRetrievers)
    {
        super(commandSender, doorRetrievers);
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.OPEN;
    }

    @Override
    public @NonNull DoorActionType getDoorActionType()
    {
        return DoorActionType.OPEN;
    }
}
