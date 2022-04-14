package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class SetOpenDirectionDelayed extends DelayedCommand<RotateDirection>
{
    @Inject SetOpenDirectionDelayed(
        Context context, DelayedCommandInputRequest.IFactory<RotateDirection> inputRequestFactory)
    {
        super(context, inputRequestFactory, RotateDirection.class);
    }

    @Override
    protected CommandDefinition getCommandDefinition()
    {
        return SetOpenDirection.COMMAND_DEFINITION;
    }

    @Override
    protected String getNotWaitingMessage()
    {
        return localizer.getMessage("commands.set_open_direction.delayed.not_waiting");
    }

    @Override
    protected CompletableFuture<Boolean> delayedInputExecutor(
        ICommandSender commandSender, DoorRetriever doorRetriever, RotateDirection openDir)
    {
        return commandFactory.get().newSetOpenDirection(commandSender, doorRetriever, openDir).run();
    }

    @Override
    protected String inputRequestMessage(ICommandSender commandSender, DoorRetriever doorRetriever)
    {
        if (!doorRetriever.isAvailable())
            return localizer.getMessage("commands.set_open_direction.delayed.init");

        final var sb = new StringBuilder(localizer.getMessage("commands.set_open_direction.delayed.init_header"))
            .append('\n');

        final var futureDoor = doorRetriever.getDoor();
        if (!futureDoor.isDone())
            throw new IllegalStateException("Door that should be available is not done!");
        final var optionalDoor = futureDoor.join();
        if (optionalDoor.isEmpty())
            throw new IllegalStateException("Door that should be available is not present!");

        final var directions = optionalDoor.get().getDoorType().getValidOpenDirections();
        for (int idx = 0; idx < directions.size(); ++idx)
        {
            sb.append(localizer.getMessage(directions.get(idx).getLocalizationKey()));
            if (idx < directions.size() - 1)
                sb.append(", ");
        }
        return sb.toString();
    }
}
