package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.animation.StructureActivityManager;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command used to stop all active structures.
 */
@ToString(callSuper = true)
public class StopStructures extends BaseCommand
{
    @ToString.Exclude
    private final StructureActivityManager structureActivityManager;

    @AssistedInject
    StopStructures(
        @Assisted ICommandSender commandSender,
        IExecutor executor,
        ILocalizer localizer,
        ITextFactory textFactory,
        StructureActivityManager structureActivityManager)
    {
        super(commandSender, executor, localizer, textFactory);
        this.structureActivityManager = structureActivityManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.STOP_STRUCTURES;
    }

    @Override
    protected CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        structureActivityManager.shutDown();
        structureActivityManager.initialize();

        getCommandSender().sendSuccess(textFactory, localizer.getMessage("commands.stop_structures.success"));
        return CompletableFuture.completedFuture(null);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link StopStructures} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for stopping all active structures.
         * @return See {@link BaseCommand#run()}.
         */
        StopStructures newStopStructures(ICommandSender commandSender);
    }
}
