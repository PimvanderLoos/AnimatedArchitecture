package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.Util;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that adds co-owners to a given door.
 *
 * @author Pim
 */
@ToString
public class AddOwner extends BaseCommand
{
    private final @NonNull DoorRetriever doorRetriever;
    private final @NonNull IPPlayer targetPlayer;
    private final int targetPermissionLevel;

    public AddOwner(@NonNull ICommandSender commandSender, @NonNull DoorRetriever doorRetriever,
                    @NonNull IPPlayer targetPlayer, int targetPermissionLevel)
    {
        super(commandSender);
        this.doorRetriever = doorRetriever;
        this.targetPlayer = targetPlayer;
        this.targetPermissionLevel = targetPermissionLevel;
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.ADD_OWNER;
    }

    @Override
    protected boolean validInput()
    {
        return targetPermissionLevel == 1 || targetPermissionLevel == 2;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand()
    {
        return getDoor(doorRetriever).thenApply(
            door ->
            {
                if (door.isEmpty() || !isAllowed(door.get()))
                    return false;

                return BigDoors.get().getDatabaseManager()
                               .addOwner(door.get(), targetPlayer, targetPermissionLevel).join();
            }).exceptionally(t -> Util.exceptionally(t, false));
    }

    private boolean isAllowed(@NonNull AbstractDoorBase door)
    {
        if (!commandSender.isPlayer())
            return true;

        val doorOwner = commandSender.getPlayer().flatMap(door::getDoorOwner);
        if (doorOwner.isEmpty())
        {
            // TODO: Localization
            commandSender.sendMessage("You are not an owner of this door!");
            return false;
        }

        if (doorOwner.get().getPermission() > DoorAttribute.getPermissionLevel(DoorAttribute.ADDOWNER))
        {
            // TODO: Localization
            commandSender.sendMessage("Your are not allowed to add co-owners to this door!");
            return false;
        }

        if (doorOwner.get().getPermission() < targetPermissionLevel)
        {
            // TODO: Localization
            commandSender.sendMessage("You cannot add co-owners with a higher permission level!");
            return false;
        }

        return true;
    }
}
