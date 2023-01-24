package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableAttribute;
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that changes the opening direction of movables.
 *
 * @author Pim
 */
@ToString
public class SetOpenDirection extends MovableTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_OPEN_DIRECTION;

    private final MovementDirection movementDirection;

    @AssistedInject //
    SetOpenDirection(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted MovableRetriever movableRetriever, @Assisted MovementDirection movementDirection)
    {
        super(commandSender, localizer, textFactory, movableRetriever, MovableAttribute.OPEN_DIRECTION);
        this.movementDirection = movementDirection;
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
        getCommandSender().sendSuccess(textFactory, localizer.getMessage("commands.set_open_direction.success",
                                                                         desc.typeName(), desc.id()));
    }

    @Override
    protected CompletableFuture<?> performAction(AbstractMovable movable)
    {
        if (!movable.getType().isValidOpenDirection(movementDirection))
        {
            getCommandSender().sendMessage(
                textFactory, TextType.ERROR,
                localizer.getMessage("commands.set_open_direction.error.invalid_rotation",
                                     localizer.getMessage(movementDirection.getLocalizationKey()),
                                     localizer.getMovableType(movable), movable.getBasicInfo()));

            return CompletableFuture.completedFuture(null);
        }

        movable.setOpenDir(movementDirection);
        return movable.syncData().thenAccept(this::handleDatabaseActionResult);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link SetOpenDirection} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing open direction of the movable.
         * @param movableRetriever
         *     A {@link MovableRetrieverFactory} representing the {@link AbstractMovable} for which the open direction
         *     will be modified.
         * @param movementDirection
         *     The new movement direction.
         * @return See {@link BaseCommand#run()}.
         */
        SetOpenDirection newSetOpenDirection(
            ICommandSender commandSender, MovableRetriever movableRetriever, MovementDirection movementDirection);
    }
}
