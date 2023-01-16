package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableAttribute;
import nl.pim16aap2.bigdoors.movable.MovableOwner;
import nl.pim16aap2.bigdoors.movable.PermissionLevel;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that adds co-owners to a given movable.
 *
 * @author Pim
 */
@ToString
public class AddOwner extends MovableTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.ADD_OWNER;

    /**
     * The default value to use for {@link #targetPermissionLevel} when none is specified.
     */
    protected static final PermissionLevel DEFAULT_PERMISSION_LEVEL = PermissionLevel.USER;

    /**
     * The target player that will be added to the {@link #movableRetriever} as co-owner.
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
        @Assisted MovableRetriever doorRetriever, @Assisted IPPlayer targetPlayer,
        @Assisted @Nullable PermissionLevel targetPermissionLevel, DatabaseManager databaseManager)
    {
        super(commandSender, localizer, textFactory, doorRetriever, MovableAttribute.ADD_OWNER);
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
    protected CompletableFuture<Boolean> performAction(AbstractMovable movable)
    {
        return databaseManager.addOwner(movable, targetPlayer, targetPermissionLevel,
                                        getCommandSender().getPlayer().orElse(null))
                              .thenApply(this::handleDatabaseActionResult);
    }

    @Override
    protected boolean isAllowed(AbstractMovable movable, boolean hasBypassPermission)
    {
        final PermissionLevel existingPermission = movable.getMovableOwner(targetPlayer).map(MovableOwner::permission)
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

        final var doorOwner = getCommandSender().getPlayer().flatMap(movable::getMovableOwner);
        if (doorOwner.isEmpty())
        {
            getCommandSender().sendError(textFactory, localizer.getMessage("commands.add_owner.error.not_an_owner",
                                                                           localizer.getMovableType(movable)));
            return false;
        }

        final PermissionLevel executorPermission = doorOwner.get().permission();
        if (!MovableAttribute.ADD_OWNER.canAccessWith(doorOwner.get().permission()))
        {
            getCommandSender().sendError(textFactory, localizer.getMessage("commands.add_owner.error.not_allowed",
                                                                           localizer.getMovableType(movable)));
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
                                                             targetPlayer.asString(),
                                                             localizer.getMovableType(movable)));
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
         *     A {@link MovableRetrieverFactory} that references the target movable.
         * @param targetPlayer
         *     The target player to add to this movable as co-owner.
         *     <p>
         *     If this player is already an owner of the target door, their permission will be overridden provided that
         *     the command sender is allowed to add/remove co-owners at both the old and the new target permission
         *     level.
         * @param targetPermissionLevel
         *     The permission level of the new owner's ownership.
         * @return See {@link BaseCommand#run()}.
         */
        AddOwner newAddOwner(
            ICommandSender commandSender, MovableRetriever doorRetriever, IPPlayer targetPlayer,
            @Nullable PermissionLevel targetPermissionLevel);

        /**
         * See {@link #newAddOwner(ICommandSender, MovableRetriever, IPPlayer, PermissionLevel)}.
         * <p>
         * The default permission node defined by {@link AddOwner#DEFAULT_PERMISSION_LEVEL} is used.
         */
        default AddOwner newAddOwner(
            ICommandSender commandSender, MovableRetriever doorRetriever, IPPlayer targetPlayer)
        {
            return newAddOwner(commandSender, doorRetriever, targetPlayer, null);
        }
    }
}
