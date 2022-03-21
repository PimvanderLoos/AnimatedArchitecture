package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that adds co-owners to a given door.
 *
 * @author Pim
 */
@ToString
public class AddOwner extends DoorTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.ADD_OWNER;

    /**
     * The default value to use for {@link #targetPermissionLevel} when none is specified.
     */
    protected static final int DEFAULT_PERMISSION_LEVEL = 2;

    /**
     * The target player that will be added to the {@link #doorRetriever} as co-owner.
     * <p>
     * If this player is already an owner of the target door, their permission will be overridden provided that the
     * command sender is allowed to add/remove co-owners at both the old and the new target permission level.
     */
    private final IPPlayer targetPlayer;

    /**
     * The permission level of the new owner's ownership. 1 = admin, 2 = user.
     */
    private final int targetPermissionLevel;

    private final DatabaseManager databaseManager;

    @AssistedInject //
    AddOwner(
        @Assisted ICommandSender commandSender, ILocalizer localizer, @Assisted DoorRetriever doorRetriever,
        @Assisted IPPlayer targetPlayer, @Assisted int targetPermissionLevel, DatabaseManager databaseManager)
    {
        super(commandSender, localizer, doorRetriever, DoorAttribute.ADD_OWNER);
        this.targetPlayer = targetPlayer;
        this.targetPermissionLevel = targetPermissionLevel;
        this.databaseManager = databaseManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected boolean validInput()
    {
        if (targetPermissionLevel == 1 || targetPermissionLevel == 2)
            return true;

        getCommandSender().sendMessage(localizer.getMessage("commands.add_owner.error.invalid_target_permission",
                                                            targetPermissionLevel));
        return false;
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractDoor door)
    {
        return databaseManager.addOwner(door, targetPlayer, targetPermissionLevel,
                                        getCommandSender().getPlayer().orElse(null))
                              .thenApply(this::handleDatabaseActionResult);
    }

    @Override
    protected boolean isAllowed(AbstractDoor door, boolean hasBypassPermission)
    {
        final int existingPermission = door.getDoorOwner(targetPlayer).map(DoorOwner::permission)
                                           .orElse(Integer.MAX_VALUE);

        if (!getCommandSender().isPlayer() || hasBypassPermission)
        {
            if (existingPermission == 0)
            {
                getCommandSender().sendMessage(localizer.getMessage("commands.add_owner.error.targeting_prime_owner"));
                return false;
            }
            return true;
        }

        final var doorOwner = getCommandSender().getPlayer().flatMap(door::getDoorOwner);
        if (doorOwner.isEmpty())
        {
            getCommandSender().sendMessage(localizer.getMessage("commands.add_owner.error.not_an_owner"));
            return false;
        }

        final int ownerPermission = doorOwner.get().permission();
        if (ownerPermission > DoorAttribute.getPermissionLevel(DoorAttribute.ADD_OWNER))
        {
            getCommandSender().sendMessage(localizer.getMessage("commands.add_owner.error.not_allowed"));
            return false;
        }

        if (ownerPermission >= targetPermissionLevel)
        {
            getCommandSender().sendMessage(localizer.getMessage("commands.add_owner.error.cannot_assign_below_self"));
            return false;
        }

        if (existingPermission <= ownerPermission || existingPermission == targetPermissionLevel)
        {
            getCommandSender().sendMessage(localizer.getMessage("commands.add_owner.error.target_already_owner",
                                                                targetPlayer.asString()));
            return false;
        }

        return true;
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link AddOwner} command.
         *
         * @param commandSender
         *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
         *     execution.
         * @param doorRetriever
         *     A {@link DoorRetrieverFactory} that references the target door.
         * @param targetPlayer
         *     The target player to add to this door as co-owner.
         *     <p>
         *     If this player is already an owner of the target door, their permission will be overridden provided that
         *     the command sender is allowed to add/remove co-owners at both the old and the new target permission
         *     level.
         * @param targetPermissionLevel
         *     The permission level of the new owner's ownership. 1 = admin, 2 = user.
         * @return See {@link BaseCommand#run()}.
         */
        AddOwner newAddOwner(
            ICommandSender commandSender, DoorRetriever doorRetriever, IPPlayer targetPlayer,
            int targetPermissionLevel);

        /**
         * See {@link #newAddOwner(ICommandSender, DoorRetriever, IPPlayer, int)}.
         * <p>
         * The default permission node defined by {@link AddOwner#DEFAULT_PERMISSION_LEVEL} is used.
         */
        default AddOwner newAddOwner(
            ICommandSender commandSender, DoorRetriever doorRetriever, IPPlayer targetPlayer)
        {
            return newAddOwner(commandSender, doorRetriever, targetPlayer, AddOwner.DEFAULT_PERMISSION_LEVEL);
        }

        /**
         * See {@link #newAddOwner(ICommandSender, DoorRetriever, IPPlayer, int)}.
         * <p>
         * The default permission node defined by {@link AddOwner#DEFAULT_PERMISSION_LEVEL} is used.
         */
        default AddOwner newAddOwner(ICommandSender commandSender, DoorRetriever doorRetriever,
                                     IPPlayer targetPlayer, @Nullable Integer permissionLevel)
        {
            return newAddOwner(commandSender, doorRetriever, targetPlayer,
                               permissionLevel == null ? AddOwner.DEFAULT_PERMISSION_LEVEL : permissionLevel);
        }
    }
}
