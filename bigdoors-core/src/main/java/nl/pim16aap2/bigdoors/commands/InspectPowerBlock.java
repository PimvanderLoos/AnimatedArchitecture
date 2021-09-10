package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.PowerBlockInspector;
import nl.pim16aap2.bigdoors.util.CompletableFutureHandler;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command to inspect a location to check if there are any powerblocks registered there.
 *
 * @author Pim
 */
@ToString
public class InspectPowerBlock extends BaseCommand
{
    private final ToolUserManager toolUserManager;
    private final PowerBlockInspector.Factory inspectPowerBlockFactory;

    @AssistedInject //
    InspectPowerBlock(@Assisted ICommandSender commandSender, IPLogger logger, ILocalizer localizer,
                      ToolUserManager toolUserManager, PowerBlockInspector.Factory inspectPowerBlockFactory,
                      CompletableFutureHandler handler)
    {
        super(commandSender, logger, localizer, handler);
        this.toolUserManager = toolUserManager;
        this.inspectPowerBlockFactory = inspectPowerBlockFactory;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.INSPECT_POWERBLOCK;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        toolUserManager.startToolUser(inspectPowerBlockFactory.create((IPPlayer) getCommandSender(),
                                                                      permissions.second),
                                      Constants.DOOR_CREATOR_TIME_LIMIT);
        return CompletableFuture.completedFuture(true);
    }

    @AssistedFactory
    interface Factory
    {
        /**
         * Creates (but does not execute!) a new {@link InspectPowerBlock} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for inspecting the powerblocks.
         *     <p>
         *     They can only discover {@link DoorBase}s attached to specific locations if they both have access to the
         *     specific location and access to the specific door(s).
         * @return See {@link BaseCommand#run()}.
         */
        InspectPowerBlock newInspectPowerBlock(ICommandSender commandSender);
    }
}
