package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.DoorRetriever;

import java.util.concurrent.CompletableFuture;

@ToString
public class RemoveOwner extends DoorTargetCommand
{
    private final @NonNull IPPlayer targetPlayer;

    public RemoveOwner(@NonNull ICommandSender commandSender, @NonNull DoorRetriever doorRetriever,
                       @NonNull IPPlayer targetPlayer)
    {
        super(commandSender, doorRetriever);
        this.targetPlayer = targetPlayer;
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.REMOVEOWNER;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> performAction(@NonNull AbstractDoorBase door)
    {
        return BigDoors.get().getDatabaseManager().removeOwner(door, targetPlayer);
    }

    @Override
    protected boolean isAllowed(@NonNull AbstractDoorBase door, boolean hasBypassPermission)
    {
        final boolean bypassOwnership = !getCommandSender().isPlayer() || hasBypassPermission;

        val doorOwner = getCommandSender().getPlayer().flatMap(door::getDoorOwner);
        if (doorOwner.isEmpty() && !bypassOwnership)
        {
            // TODO: Localization
            getCommandSender().sendMessage("You are not an owner of this door!");
            return false;
        }

        // Assume a permission level of 0 in case the command sender is not an owner but DOES have bypass access.
        final int ownerPermission = doorOwner.map(DoorOwner::getPermission).orElse(0);
        if (ownerPermission > DoorAttribute.getPermissionLevel(DoorAttribute.REMOVEOWNER))
        {
            // TODO: Localization
            getCommandSender().sendMessage("Your are not allowed to remove co-owners from this door!");
            return false;
        }

        val targetDoorOwner = door.getDoorOwner(targetPlayer);
        if (targetDoorOwner.isEmpty())
        {
            // TODO: Localization
            getCommandSender()
                .sendMessage(targetPlayer.asString() + " is not a (co-)owner of door " + door.getBasicInfo());
            return false;
        }

        if (targetDoorOwner.get().getPermission() <= ownerPermission)
        {
            // TODO: Localization
            getCommandSender()
                .sendMessage("You can only remove (co)owners with a higher permission level than yourself! ");
            return false;
        }
        return true;
    }
}
