package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.tooluser.PowerBlockInspector;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command to inspect a location to check if there are any powerblocks registered there.
 */
@ToString(callSuper = true)
public class InspectPowerBlock extends BaseCommand
{
    @ToString.Exclude
    private final ToolUserManager toolUserManager;

    @ToString.Exclude
    private final PowerBlockInspector.IFactory inspectPowerBlockFactory;

    @AssistedInject
    InspectPowerBlock(
        @Assisted ICommandSender commandSender,
        IExecutor executor,
        ILocalizer localizer,
        ITextFactory textFactory,
        ToolUserManager toolUserManager,
        PowerBlockInspector.IFactory inspectPowerBlockFactory)
    {
        super(commandSender, executor, localizer, textFactory);
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
        toolUserManager.startToolUser(
            inspectPowerBlockFactory.create((IPlayer) getCommandSender(), permissions.hasAdminPermission()),
            Constants.STRUCTURE_CREATOR_TIME_LIMIT
        );
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
         *     They can only discover {@link Structure}s attached to specific locations if they both have access to the
         *     specific location and access to the specific structure(s).
         * @return See {@link BaseCommand#run()}.
         */
        InspectPowerBlock newInspectPowerBlock(ICommandSender commandSender);
    }
}
