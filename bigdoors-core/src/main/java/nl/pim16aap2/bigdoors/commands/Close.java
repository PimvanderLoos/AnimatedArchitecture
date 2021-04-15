package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.DoorRetriever;

@ToString
public class Close extends Toggle
{
    public Close(final @NonNull ICommandSender commandSender, final double speedMultiplier,
                 final @NonNull DoorRetriever... doorRetrievers)
    {
        super(commandSender, speedMultiplier, doorRetrievers);
    }

    public Close(final @NonNull ICommandSender commandSender, final @NonNull DoorRetriever[] doorRetrievers)
    {
        super(commandSender, doorRetrievers);
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.CLOSE;
    }

    @Override
    public @NonNull DoorActionType getDoorActionType()
    {
        return DoorActionType.CLOSE;
    }
}
