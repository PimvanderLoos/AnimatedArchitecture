package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableAttribute;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.util.RotateDirection;
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

    private final RotateDirection rotateDirection;

    @AssistedInject //
    SetOpenDirection(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted MovableRetriever movableRetriever, @Assisted RotateDirection rotateDirection)
    {
        super(commandSender, localizer, textFactory, movableRetriever, MovableAttribute.OPEN_DIRECTION);
        this.rotateDirection = rotateDirection;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractMovable movable)
    {
        if (!movable.getType().isValidOpenDirection(rotateDirection))
        {
            getCommandSender().sendMessage(
                textFactory, TextType.ERROR,
                localizer.getMessage("commands.set_open_direction.error.invalid_rotation",
                                     localizer.getMessage(rotateDirection.getLocalizationKey()),
                                     localizer.getMovableType(movable), movable.getBasicInfo()));

            return CompletableFuture.completedFuture(true);
        }

        movable.setOpenDir(rotateDirection);
        return movable.syncData().thenApply(x -> true);
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
         *     A {@link MovableRetrieverFactory} representing the {@link MovableBase} for which the open direction will
         *     be modified.
         * @param rotateDirection
         *     The new open direction.
         * @return See {@link BaseCommand#run()}.
         */
        SetOpenDirection newSetOpenDirection(
            ICommandSender commandSender, MovableRetriever movableRetriever, RotateDirection rotateDirection);
    }
}
