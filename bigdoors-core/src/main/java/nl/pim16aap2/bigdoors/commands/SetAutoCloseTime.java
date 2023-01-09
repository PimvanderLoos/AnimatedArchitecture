package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorAttribute;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Implements the command that changes the auto-close-timer for doors.
 *
 * @author Pim
 */
@ToString
public class SetAutoCloseTime extends DoorTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_AUTO_CLOSE_TIME;

    private final int autoCloseTime;

    @AssistedInject //
    SetAutoCloseTime(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted DoorRetriever doorRetriever, @Assisted int autoCloseTime)
    {
        super(commandSender, localizer, textFactory, doorRetriever, DoorAttribute.AUTO_CLOSE_TIMER);
        this.autoCloseTime = autoCloseTime;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractDoor door)
    {
        if (!(door instanceof ITimerToggleable))
        {
            getCommandSender().sendMessage(textFactory, TextType.ERROR,
                                           localizer.getMessage("commands.set_auto_close_time.error.invalid_door_type",
                                                                localizer.getDoorType(door), door.getBasicInfo()));
            return CompletableFuture.completedFuture(true);
        }

        ((ITimerToggleable) door).setAutoCloseTime(autoCloseTime);
        return door.syncData().thenApply(x -> true);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link SetAutoCloseTime} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing the auto-close timer of the door.
         * @param doorRetriever
         *     A {@link DoorRetrieverFactory} representing the {@link DoorBase} for which the auto-close timer will be
         *     modified.
         * @param autoCloseTime
         *     The new auto-close time value.
         * @return See {@link BaseCommand#run()}.
         */
        SetAutoCloseTime newSetAutoCloseTime(
            ICommandSender commandSender, DoorRetriever doorRetriever, int autoCloseTime);
    }
}
