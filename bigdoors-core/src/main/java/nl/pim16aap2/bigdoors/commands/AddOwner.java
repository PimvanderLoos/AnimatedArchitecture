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

/**
 * Represents the command that adds co-owners to a given door.
 *
 * @author Pim
 */
@ToString
public class AddOwner extends DoorTargetCommand
{
    private final @NonNull IPPlayer targetPlayer;
    private final int targetPermissionLevel;

    public AddOwner(final @NonNull ICommandSender commandSender, final @NonNull DoorRetriever doorRetriever,
                    final @NonNull IPPlayer targetPlayer, final int targetPermissionLevel)
    {
        super(commandSender, doorRetriever);
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
    protected @NonNull CompletableFuture<Boolean> performAction(final @NonNull AbstractDoorBase door)
    {
        return BigDoors.get().getDatabaseManager().addOwner(door, targetPlayer, targetPermissionLevel);
    }

    @Override
    protected boolean isAllowed(final @NonNull AbstractDoorBase door, final boolean hasBypassPermission)
    {
        final int existingPermission = door.getDoorOwner(targetPlayer).map(DoorOwner::getPermission)
                                           .orElse(Integer.MAX_VALUE);

        if (!getCommandSender().isPlayer() || hasBypassPermission)
        {
            if (existingPermission == 0)
            {
                // TODO: Localization
                getCommandSender().sendMessage("You cannot change the permission level of this player!");
                return false;
            }
            return true;
        }

        val doorOwner = getCommandSender().getPlayer().flatMap(door::getDoorOwner);
        if (doorOwner.isEmpty())
        {
            // TODO: Localization
            getCommandSender().sendMessage("You are not an owner of this door!");
            return false;
        }

        final int ownerPermission = doorOwner.get().getPermission();
        if (ownerPermission > DoorAttribute.getPermissionLevel(DoorAttribute.ADDOWNER))
        {
            // TODO: Localization
            getCommandSender().sendMessage("Your are not allowed to add co-owners to this door!");
            return false;
        }

        if (ownerPermission >= targetPermissionLevel)
        {
            // TODO: Localization
            getCommandSender().sendMessage("You cannot only add co-owners with a higher permission level!");
            return false;
        }

        if (existingPermission <= ownerPermission)
        {
            // TODO: Localization
            getCommandSender().sendMessage(
                targetPlayer.asString() + " is already a (co-)owner of this door with a lower permission level!");
            return false;
        }
        if (existingPermission == targetPermissionLevel)
        {
            // TODO: Localization
            getCommandSender().sendMessage(
                targetPlayer.asString() + " is already a (co-)owner of this door the same permission level!");
            return false;
        }

        return true;
    }
}
