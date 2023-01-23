package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableAttribute;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that changes the opening status of movables.
 *
 * @author Pim
 */
@ToString
public class SetOpenStatus extends MovableTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_OPEN_STATUS;

    private final boolean isOpen;

    @AssistedInject //
    SetOpenStatus(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted MovableRetriever movableRetriever, @Assisted boolean isOpen)
    {
        super(commandSender, localizer, textFactory, movableRetriever, MovableAttribute.OPEN_STATUS);
        this.isOpen = isOpen;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected void handleDatabaseActionSuccess()
    {
        final var desc = getRetrievedMovableDescription();
        getCommandSender().sendSuccess(textFactory, localizer.getMessage("commands.set_open_status.success",
                                                                         desc.typeName(), desc.id()));
    }

    @Override
    protected CompletableFuture<?> performAction(AbstractMovable movable)
    {
        if (movable.isOpen() == isOpen)
        {
            getCommandSender().sendError(
                textFactory,
                localizer.getMessage("commands.set_open_status.error.status_not_changed",
                                     localizer.getMovableType(movable), movable.getNameAndUid(),
                                     localizer.getMessage(isOpen ?
                                                          localizer.getMessage("constants.open_status.open") :
                                                          localizer.getMessage("constants.open_status.closed")
                                     )));
            return CompletableFuture.completedFuture(null);
        }

        movable.setOpen(isOpen);
        return movable.syncData().thenAccept(this::handleDatabaseActionResult);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link SetOpenStatus} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing open status of the movable.
         * @param movableRetriever
         *     A {@link MovableRetrieverFactory} representing the {@link AbstractMovable} for which the open status will
         *     be modified.
         * @param isOpen
         *     The new open status of the movable.
         * @return See {@link BaseCommand#run()}.
         */
        SetOpenStatus newSetOpenStatus(
            ICommandSender commandSender, MovableRetriever movableRetriever, boolean isOpen);
    }
}
