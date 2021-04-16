package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;

import java.util.concurrent.CompletableFuture;

@ToString
public class SetAutoCloseTime extends DoorTargetCommand
{
    private final int autoCloseTime;

    public SetAutoCloseTime(final @NonNull ICommandSender commandSender, final @NonNull DoorRetriever doorRetriever,
                            final int autoCloseTime)
    {
        super(commandSender, doorRetriever);
        this.autoCloseTime = autoCloseTime;
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.SETAUTOCLOSETIME;
    }

    @Override
    protected boolean isAllowed(final @NonNull AbstractDoorBase door, final boolean bypassPermission)
    {
        return hasAccessToAttribute(door, DoorAttribute.AUTOCLOSETIMER, bypassPermission);
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
