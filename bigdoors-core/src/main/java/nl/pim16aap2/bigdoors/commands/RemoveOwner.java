package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the remove owner command. This command is used to remove owners from a door.
 *
 * @author Pim
 */
@ToString
public class RemoveOwner extends DoorTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.REMOVE_OWNER;

    private final IPPlayer targetPlayer;
    private final DatabaseManager databaseManager;

    @AssistedInject //
    RemoveOwner(
        @Assisted ICommandSender commandSender, ILocalizer localizer, @Assisted DoorRetriever doorRetriever,
        @Assisted IPPlayer targetPlayer, DatabaseManager databaseManager)
    {
        super(commandSender, localizer, doorRetriever, DoorAttribute.REMOVE_OWNER);
        this.targetPlayer = targetPlayer;
        this.databaseManager = databaseManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractDoor door)
    {
        return databaseManager.removeOwner(door, targetPlayer, getCommandSender().getPlayer().orElse(null))
                              .thenApply(this::handleDatabaseActionResult);
    }

    @Override
    protected boolean isAllowed(AbstractDoor door, boolean hasBypassPermission)
    {
        final boolean bypassOwnership = !getCommandSender().isPlayer() || hasBypassPermission;

        final var doorOwner = getCommandSender().getPlayer().flatMap(door::getDoorOwner);
        if (doorOwner.isEmpty() && !bypassOwnership)
        {
            getCommandSender().sendMessage(localizer.getMessage("commands.remove_owner.error.not_an_owner"));
            return false;
        }

        // Assume a permission level of 0 in case the command sender is not an owner but DOES have bypass access.
        final int ownerPermission = doorOwner.map(DoorOwner::permission).orElse(0);
        if (ownerPermission > DoorAttribute.getPermissionLevel(DoorAttribute.REMOVE_OWNER))
        {
            getCommandSender().sendMessage(localizer.getMessage("commands.remove_owner.error.not_allowed"));
            return false;
        }

        final var targetDoorOwner = door.getDoorOwner(targetPlayer);
        if (targetDoorOwner.isEmpty())
        {
            getCommandSender()
                .sendMessage(localizer.getMessage("commands.remove_owner.error.target_not_an_owner",
                                                  targetPlayer.asString(), door.getBasicInfo()));
            return false;
        }

        if (targetDoorOwner.get().permission() <= ownerPermission)
        {
            getCommandSender()
                .sendMessage(localizer.getMessage("commands.remove_owner.error.cannot_remove_higher_permission"));
            return false;
        }
        return true;
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link RemoveOwner} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for removing a co-owner of the door.
         * @param doorRetriever
         *     A {@link DoorRetrieverFactory} representing the {@link DoorBase} for which a co-owner is requested to be
         *     removed.
         * @param targetPlayer
         *     The co-owner that is requested to be removed.
         * @return See {@link BaseCommand#run()}.
         */
        RemoveOwner newRemoveOwner(
            ICommandSender commandSender, DoorRetriever doorRetriever,
            IPPlayer targetPlayer);
    }
}
