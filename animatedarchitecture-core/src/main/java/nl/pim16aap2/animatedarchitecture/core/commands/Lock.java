package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IAnimatedArchitectureEventFactory;
import nl.pim16aap2.animatedarchitecture.core.events.IAnimatedArchitectureEventCaller;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to change whether a structure is locked.
 */
@ToString(callSuper = true)
@Flogger
public class Lock extends StructureTargetCommand
{
    private final boolean isLocked;

    @ToString.Exclude
    private final IAnimatedArchitectureEventCaller animatedArchitectureEventCaller;

    @ToString.Exclude
    private final IAnimatedArchitectureEventFactory animatedArchitectureEventFactory;

    @AssistedInject
    Lock(
        @Assisted ICommandSender commandSender,
        @Assisted StructureRetriever structureRetriever,
        @Assisted("isLocked") boolean isLocked,
        @Assisted("sendUpdatedInfo") boolean sendUpdatedInfo,
        IExecutor executor,
        CommandFactory commandFactory,
        IAnimatedArchitectureEventCaller animatedArchitectureEventCaller,
        IAnimatedArchitectureEventFactory animatedArchitectureEventFactory)
    {
        super(
            commandSender,
            executor,
            structureRetriever,
            StructureAttribute.LOCK,
            sendUpdatedInfo,
            commandFactory
        );

        this.isLocked = isLocked;
        this.animatedArchitectureEventCaller = animatedArchitectureEventCaller;
        this.animatedArchitectureEventFactory = animatedArchitectureEventFactory;
    }

    @Override
    protected void handleDatabaseActionSuccess(@Nullable Structure retrieverResult)
    {
        final String msg = isLocked ? "commands.lock.success.locked" : "commands.lock.success.unlocked";
        final var desc = getRetrievedStructureDescription(retrieverResult);
        getCommandSender().sendSuccess(
            msg,
            arg -> arg.highlight(desc.localizedTypeName()),
            arg -> arg.highlight(desc.id())
        );
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.LOCK;
    }

    @Override
    protected CompletableFuture<?> performAction(Structure structure)
    {
        final var event = animatedArchitectureEventFactory.createStructurePrepareLockChangeEvent(
            structure,
            isLocked,
            getCommandSender().getPlayer().orElse(null)
        );

        animatedArchitectureEventCaller.callAnimatedArchitectureEvent(event);

        if (event.isCancelled())
        {
            log.atFine().log("Event %s was cancelled!", event);
            return CompletableFuture.completedFuture(null);
        }

        structure.setLocked(isLocked);
        return structure
            .syncData()
            .thenAccept(result -> handleDatabaseActionResult(result, structure))
            .thenRunAsync(() -> sendUpdatedInfo(structure), executor.getVirtualExecutor());
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Lock} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing the locked status of the structure.
         * @param structureRetriever
         *     A {@link StructureRetrieverFactory} representing the {@link Structure} for which the locked status will
         *     be modified.
         * @param isLocked
         *     True if the structure should be locked, false if it should be unlocked.
         * @param sendUpdatedInfo
         *     True to send the updated info text to the user after the command has been executed.
         * @return See {@link BaseCommand#run()}.
         */
        Lock newLock(
            ICommandSender commandSender,
            StructureRetriever structureRetriever,
            @Assisted("isLocked") boolean isLocked,
            @Assisted("sendUpdatedInfo") boolean sendUpdatedInfo
        );

        /**
         * Creates (but does not execute!) a new {@link Lock} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing the locked status of the structure.
         * @param structureRetriever
         *     A {@link StructureRetrieverFactory} representing the {@link Structure} for which the locked status will
         *     be modified.
         * @param isLocked
         *     True if the structure should be locked, false if it should be unlocked.
         * @return See {@link BaseCommand#run()}.
         */
        default Lock newLock(ICommandSender commandSender, StructureRetriever structureRetriever, boolean isLocked)
        {
            return newLock(commandSender, structureRetriever, isLocked, false);
        }
    }
}
