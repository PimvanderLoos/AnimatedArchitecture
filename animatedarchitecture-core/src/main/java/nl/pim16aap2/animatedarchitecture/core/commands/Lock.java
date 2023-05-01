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
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;

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
    private final boolean isLocked;
    private final IAnimatedArchitectureEventCaller animatedArchitectureEventCaller;
    private final IAnimatedArchitectureEventFactory animatedArchitectureEventFactory;

    @AssistedInject //
    Lock(
        @Assisted ICommandSender commandSender,
        @Assisted StructureRetriever structureRetriever,
        @Assisted("isLocked") boolean isLocked,
        @Assisted("sendUpdatedInfo") boolean sendUpdatedInfo,
        ILocalizer localizer,
        ITextFactory textFactory,
        CommandFactory commandFactory,
        IAnimatedArchitectureEventCaller animatedArchitectureEventCaller,
        IAnimatedArchitectureEventFactory animatedArchitectureEventFactory)
    {
        super(commandSender, localizer, textFactory, structureRetriever, StructureAttribute.LOCK,
              sendUpdatedInfo, commandFactory);
        this.isLocked = isLocked;
        this.animatedArchitectureEventCaller = animatedArchitectureEventCaller;
        this.animatedArchitectureEventFactory = animatedArchitectureEventFactory;
    }

    @Override
    protected void handleDatabaseActionSuccess()
    {
        final String msg = isLocked ? "commands.lock.success.locked" : "commands.lock.success.unlocked";
        final var desc = getRetrievedStructureDescription();
        getCommandSender().sendMessage(textFactory.newText().append(
            localizer.getMessage(msg), TextType.SUCCESS,
            arg -> arg.highlight(desc.localizedTypeName()),
            arg -> arg.highlight(desc.id())));
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.LOCK;
    }

    @Override
    protected CompletableFuture<?> performAction(AbstractStructure structure)
    {
        final var event = animatedArchitectureEventFactory.createStructurePrepareLockChangeEvent(
            structure, isLocked, getCommandSender().getPlayer().orElse(null));

        animatedArchitectureEventCaller.callAnimatedArchitectureEvent(event);

        if (event.isCancelled())
        {
            log.atFinest().log("Event %s was cancelled!", event);
            return CompletableFuture.completedFuture(null);
        }

        structure.setLocked(isLocked);
        return structure.syncData()
                        .thenAccept(this::handleDatabaseActionResult)
                        .thenRunAsync(() -> sendUpdatedInfo(structure));
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
            @Assisted("sendUpdatedInfo") boolean sendUpdatedInfo);

        /**
         * Creates (but does not execute!) a new {@link Lock} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing the locked status of the structure.
         * @param structureRetriever
         *     A {@link StructureRetrieverFactory} representing the {@link AbstractStructure} for which the locked
         *     status will be modified.
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
