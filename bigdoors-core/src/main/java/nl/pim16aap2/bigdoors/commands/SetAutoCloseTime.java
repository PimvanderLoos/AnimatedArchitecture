package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;

import java.util.concurrent.CompletableFuture;

/**
 * Implements the command that changes the auto-close-timer for doors.
 *
 * @author Pim
 */
@ToString
public class SetAutoCloseTime extends DoorTargetCommand
{
    private final int autoCloseTime;

    protected SetAutoCloseTime(final @NonNull ICommandSender commandSender, final @NonNull DoorRetriever doorRetriever,
                               final int autoCloseTime)
    {
        super(commandSender, doorRetriever);
        this.autoCloseTime = autoCloseTime;
    }

    /**
     * Runs the {@link SetAutoCloseTime} command.
     *
     * @param commandSender The {@link ICommandSender} responsible for changing the auto-close timer of the door.
     * @param doorRetriever A {@link DoorRetriever} representing the {@link AbstractDoorBase} for which the auto-close
     *                      timer will be modified.
     * @param autoCloseTime The new auto-close time value.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NonNull CompletableFuture<Boolean> run(final @NonNull ICommandSender commandSender,
                                                          final @NonNull DoorRetriever doorRetriever,
                                                          final int autoCloseTime)
    {
        return new SetAutoCloseTime(commandSender, doorRetriever, autoCloseTime).run();
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.SET_AUTO_CLOSE_TIME;
    }

    @Override
    protected boolean isAllowed(final @NonNull AbstractDoorBase door, final boolean bypassPermission)
    {
        return hasAccessToAttribute(door, DoorAttribute.AUTO_CLOSE_TIMER, bypassPermission);
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> performAction(final @NonNull AbstractDoorBase door)
    {
        if (!(door instanceof ITimerToggleableArchetype))
        {
            // TODO: Localization
            getCommandSender().sendMessage("This door has no auto close timer property!");
            return CompletableFuture.completedFuture(true);
        }

        ((ITimerToggleableArchetype) door).setAutoCloseTime(autoCloseTime);
        return door.syncData().thenApply(x -> true);
    }
}
