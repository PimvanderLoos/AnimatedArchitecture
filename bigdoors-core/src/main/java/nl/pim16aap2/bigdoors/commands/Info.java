package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
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
    private final IGlowingBlockSpawner glowingBlockSpawner;

    @AssistedInject
    public Info(@Assisted ICommandSender commandSender, IPLogger logger, ILocalizer localizer,
                @Assisted DoorRetriever.AbstractRetriever doorRetriever, IGlowingBlockSpawner glowingBlockSpawner)
    {
        super(commandSender, logger, localizer, doorRetriever, DoorAttribute.INFO);
        this.glowingBlockSpawner = glowingBlockSpawner;
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
        glowingBlockSpawner.spawnGlowingBlocks(doorBase, (IPPlayer) getCommandSender());
    }

    @AssistedFactory
    interface Factory
    {
        /**
         * Creates (but does not execute!) a new {@link Info} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for retrieving the door info and the receiver of the door's
         *     information.
         * @param doorRetriever
         *     A {@link DoorRetriever} representing the {@link DoorBase} for which the information will be retrieved.
         * @return See {@link BaseCommand#run()}.
         */
        Info newInfo(ICommandSender commandSender, DoorRetriever.AbstractRetriever doorRetriever);
    }
}
