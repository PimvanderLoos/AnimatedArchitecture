package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorAttribute;
import nl.pim16aap2.bigdoors.doors.DoorOwner;
import nl.pim16aap2.bigdoors.doors.PermissionLevel;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
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
    protected static final PermissionLevel DEFAULT_PERMISSION_LEVEL = PermissionLevel.USER;

    /**
     * The target player that will be added to the {@link #doorRetriever} as co-owner.
     * <p>
     * If this player is already an owner of the target door, their permission will be overridden provided that the
     * command sender is allowed to add/remove co-owners at both the old and the new target permission level.
     */
    private final IPPlayer targetPlayer;

    /**
     * The permission level of the new owner's ownership.
     */
    private final PermissionLevel targetPermissionLevel;

    private final DatabaseManager databaseManager;

    @AssistedInject //
    AddOwner(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted DoorRetriever doorRetriever, @Assisted IPPlayer targetPlayer,
        @Assisted @Nullable PermissionLevel targetPermissionLevel, DatabaseManager databaseManager)
    {
        super(commandSender, localizer, textFactory, doorRetriever, DoorAttribute.ADD_OWNER);
        this.targetPlayer = targetPlayer;
        this.targetPermissionLevel = targetPermissionLevel == null ? DEFAULT_PERMISSION_LEVEL : targetPermissionLevel;
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
        if (targetPermissionLevel != PermissionLevel.CREATOR && targetPermissionLevel != PermissionLevel.NO_PERMISSION)
            return true;

        getCommandSender()
            .sendError(textFactory, localizer.getMessage("commands.add_owner.error.invalid_target_permission",
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
        final PermissionLevel existingPermission = door.getDoorOwner(targetPlayer).map(DoorOwner::permission)
                                                       .orElse(PermissionLevel.NO_PERMISSION);
        if (!getCommandSender().isPlayer() || hasBypassPermission)
        {
            if (existingPermission == PermissionLevel.CREATOR)
            {
                getCommandSender().sendError(textFactory,
                                             localizer.getMessage("commands.add_owner.error.targeting_prime_owner"));
                return false;
            }
            return true;
        }

        final var doorOwner = getCommandSender().getPlayer().flatMap(door::getDoorOwner);
        if (doorOwner.isEmpty())
        {
            getCommandSender().sendError(textFactory, localizer.getMessage("commands.add_owner.error.not_an_owner",
                                                                           localizer.getDoorType(door)));
            return false;
        }

        final PermissionLevel executorPermission = doorOwner.get().permission();
        if (!DoorAttribute.ADD_OWNER.canAccessWith(doorOwner.get().permission()))
        {
            getCommandSender().sendError(textFactory, localizer.getMessage("commands.add_owner.error.not_allowed",
                                                                           localizer.getDoorType(door)));
            return false;
        }

        if (targetPermissionLevel.isLowerThanOrEquals(executorPermission))
        {
            getCommandSender().sendError(textFactory,
                                         localizer.getMessage("commands.add_owner.error.cannot_assign_below_self"));
            return false;
        }

        if (existingPermission.isLowerThanOrEquals(executorPermission) || existingPermission == targetPermissionLevel)
        {
            getCommandSender()
                .sendError(textFactory, localizer.getMessage("commands.add_owner.error.target_already_owner",
                                                             targetPlayer.asString(), localizer.getDoorType(door)));
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
         *     The permission level of the new owner's ownership.
         * @return See {@link BaseCommand#run()}.
         */
        AddOwner newAddOwner(
            ICommandSender commandSender, DoorRetriever doorRetriever, IPPlayer targetPlayer,
            @Nullable PermissionLevel targetPermissionLevel);

        /**
         * See {@link #newAddOwner(ICommandSender, DoorRetriever, IPPlayer, PermissionLevel)}.
         * <p>
         * The default permission node defined by {@link AddOwner#DEFAULT_PERMISSION_LEVEL} is used.
         */
        default AddOwner newAddOwner(
            ICommandSender commandSender, DoorRetriever doorRetriever, IPPlayer targetPlayer)
        {
            return newAddOwner(commandSender, doorRetriever, targetPlayer, null);
        }
    }
}
