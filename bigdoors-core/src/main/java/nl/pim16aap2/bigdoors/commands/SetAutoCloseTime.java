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
import nl.pim16aap2.bigdoors.movable.movablearchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Implements the command that changes the auto-close-timer for movables.
 *
 * @author Pim
 */
@ToString
public class SetAutoCloseTime extends MovableTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_AUTO_CLOSE_TIME;

    private final int autoCloseTime;

    @AssistedInject //
    SetAutoCloseTime(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted MovableRetriever movableRetriever, @Assisted int autoCloseTime)
    {
        super(commandSender, localizer, textFactory, movableRetriever, MovableAttribute.AUTO_CLOSE_TIMER);
        this.autoCloseTime = autoCloseTime;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    private void onSuccess(AbstractMovable movable)
    {
        getCommandSender().sendSuccess(
            textFactory,
            localizer.getMessage("commands.set_auto_close_time.success", localizer.getMovableType(movable)));
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractMovable movable)
    {
        if (!(movable instanceof ITimerToggleable))
        {
            getCommandSender().sendMessage(
                textFactory, TextType.ERROR,
                localizer.getMessage("commands.set_auto_close_time.error.invalid_movable_type",
                                     localizer.getMovableType(movable), movable.getBasicInfo()));
            return CompletableFuture.completedFuture(true);
        }

        ((ITimerToggleable) movable).setAutoCloseTime(autoCloseTime);
        return movable.syncData().thenRun(() -> onSuccess(movable)).thenApply(x -> true);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link SetAutoCloseTime} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing the auto-close timer of the movable.
         * @param movableRetriever
         *     A {@link MovableRetrieverFactory} representing the {@link MovableBase} for which the auto-close timer
         *     will be modified.
         * @param autoCloseTime
         *     The new auto-close time value.
         * @return See {@link BaseCommand#run()}.
         */
        SetAutoCloseTime newSetAutoCloseTime(
            ICommandSender commandSender, MovableRetriever movableRetriever, int autoCloseTime);
    }
}
