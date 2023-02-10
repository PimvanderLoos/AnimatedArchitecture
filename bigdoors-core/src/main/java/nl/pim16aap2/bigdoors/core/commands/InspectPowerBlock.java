package nl.pim16aap2.bigdoors.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.tooluser.PowerBlockInspector;
import nl.pim16aap2.bigdoors.core.util.Constants;

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
    private final PowerBlockInspector.IFactory inspectPowerBlockFactory;

    @AssistedInject //
    InspectPowerBlock(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        ToolUserManager toolUserManager, PowerBlockInspector.IFactory inspectPowerBlockFactory)
    {
        super(commandSender, localizer, textFactory);
        this.toolUserManager = toolUserManager;
        this.inspectPowerBlockFactory = inspectPowerBlockFactory;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.INSPECT_POWER_BLOCK;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        toolUserManager.startToolUser(inspectPowerBlockFactory.create((IPlayer) getCommandSender(),
                                                                      permissions.hasAdminPermission()),
                                      Constants.STRUCTURE_CREATOR_TIME_LIMIT);
        return CompletableFuture.completedFuture(null);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link InspectPowerBlock} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for inspecting the powerblocks.
         *     <p>
         *     They can only discover {@link AbstractStructure}s attached to specific locations if they both have access
         *     to the specific location and access to the specific structure(s).
         * @return See {@link BaseCommand#run()}.
         */
        InspectPowerBlock newInspectPowerBlock(ICommandSender commandSender);
    }
}
