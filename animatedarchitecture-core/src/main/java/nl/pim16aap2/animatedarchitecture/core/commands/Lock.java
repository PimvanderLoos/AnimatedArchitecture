package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IAnimatedArchitectureEventFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.events.IAnimatedArchitectureEventCaller;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetrieverFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to change whether a structure is locked.
 *
 * @author Pim
 */
@ToString
@Flogger
public class Lock extends StructureTargetCommand
{
    private final boolean lockedStatus;
    private final IAnimatedArchitectureEventCaller animatedArchitectureEventCaller;
    private final IAnimatedArchitectureEventFactory animatedArchitectureEventFactory;

    @AssistedInject //
    Lock(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted StructureRetriever structureRetriever, @Assisted boolean lockedStatus,
        IAnimatedArchitectureEventCaller animatedArchitectureEventCaller,
        IAnimatedArchitectureEventFactory animatedArchitectureEventFactory)
    {
        super(commandSender, localizer, textFactory, structureRetriever, StructureAttribute.LOCK);
        this.lockedStatus = lockedStatus;
        this.animatedArchitectureEventCaller = animatedArchitectureEventCaller;
        this.animatedArchitectureEventFactory = animatedArchitectureEventFactory;
    }

    @Override
    protected void handleDatabaseActionSuccess()
    {
        final String msg = lockedStatus ? "commands.lock.success.locked" : "commands.lock.success.unlocked";
        final var desc = getRetrievedStructureDescription();
        getCommandSender().sendSuccess(textFactory, localizer.getMessage(msg, desc.typeName(), desc.id()));
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.LOCK;
    }

    @Override
    protected CompletableFuture<?> performAction(AbstractStructure structure)
    {
        final var event = animatedArchitectureEventFactory
            .createStructurePrepareLockChangeEvent(structure, lockedStatus,
                                                   getCommandSender().getPlayer().orElse(null));

        animatedArchitectureEventCaller.callAnimatedArchitectureEvent(event);

        if (event.isCancelled())
        {
            log.atFinest().log("Event %s was cancelled!", event);
            return CompletableFuture.completedFuture(null);
        }

        structure.setLocked(lockedStatus);
        return structure.syncData().thenAccept(this::handleDatabaseActionResult);
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
         *     A {@link StructureRetrieverFactory} representing the {@link AbstractStructure} for which the locked
         *     status will be modified.
         * @param lock
         *     The new lock status.
         * @return See {@link BaseCommand#run()}.
         */
        Lock newLock(ICommandSender commandSender, StructureRetriever structureRetriever, boolean lock);
    }
}
