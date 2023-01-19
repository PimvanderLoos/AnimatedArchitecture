package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableAttribute;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.movable.MovableOwner;
import nl.pim16aap2.bigdoors.movable.PermissionLevel;
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the remove owner command. This command is used to remove owners from a movable.
 *
 * @author Pim
 */
@ToString
@Flogger
public class RemoveOwner extends MovableTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.REMOVE_OWNER;

    private final IPPlayer targetPlayer;
    private final DatabaseManager databaseManager;

    @AssistedInject //
    RemoveOwner(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted MovableRetriever movableRetriever, @Assisted IPPlayer targetPlayer, DatabaseManager databaseManager)
    {
        super(commandSender, localizer, textFactory, movableRetriever, MovableAttribute.REMOVE_OWNER);
        this.targetPlayer = targetPlayer;
        this.databaseManager = databaseManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected void handleDatabaseActionSuccess()
    {
        final var description = getRetrievedMovableDescription();
        getCommandSender().sendSuccess(textFactory,
                                       localizer.getMessage("commands.remove_owner.success",
                                                            targetPlayer.getName(), description.typeName()));
        targetPlayer.sendInfo(textFactory,
                              localizer.getMessage("commands.remove_owner.removed_player_notification",
                                                   description.typeName(), description.id()));
    }

    @Override
    protected CompletableFuture<?> performAction(AbstractMovable movable)
    {
        return databaseManager.removeOwner(movable, targetPlayer, getCommandSender().getPlayer().orElse(null))
                              .thenAccept(this::handleDatabaseActionResult);
    }

    @Override
    protected boolean isAllowed(AbstractMovable movable, boolean hasBypassPermission)
    {
        final boolean bypassOwnership = !getCommandSender().isPlayer() || hasBypassPermission;

        final var movableOwner = getCommandSender().getPlayer().flatMap(movable::getOwner);
        if (movableOwner.isEmpty() && !bypassOwnership)
        {
            getCommandSender().sendMessage(textFactory, TextType.ERROR,
                                           localizer.getMessage("commands.remove_owner.error.not_an_owner",
                                                                localizer.getMovableType(movable)));
            return false;
        }

        // Assume a permission level of 0 in case the command sender is not an owner but DOES have bypass access.
        final PermissionLevel ownerPermission = movableOwner.map(MovableOwner::permission)
                                                            .orElse(PermissionLevel.CREATOR);
        if (!MovableAttribute.REMOVE_OWNER.canAccessWith(ownerPermission))
        {
            getCommandSender().sendMessage(textFactory, TextType.ERROR,
                                           localizer.getMessage("commands.remove_owner.error.not_allowed",
                                                                localizer.getMovableType(movable)));
            return false;
        }

        final var targetMovableOwner = movable.getOwner(targetPlayer);
        if (targetMovableOwner.isEmpty())
        {
            getCommandSender()
                .sendMessage(textFactory, TextType.ERROR,
                             localizer.getMessage("commands.remove_owner.error.target_not_an_owner",
                                                  targetPlayer.asString(), localizer.getMovableType(movable),
                                                  movable.getBasicInfo()));
            return false;
        }

        if (targetMovableOwner.get().permission().isLowerThanOrEquals(ownerPermission))
        {
            getCommandSender()
                .sendMessage(textFactory, TextType.ERROR,
                             localizer.getMessage("commands.remove_owner.error.cannot_remove_lower_permission"));
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
         *     The {@link ICommandSender} responsible for removing a co-owner of the movable.
         * @param movableRetriever
         *     A {@link MovableRetrieverFactory} representing the {@link MovableBase} for which a co-owner is requested
         *     to be removed.
         * @param targetPlayer
         *     The co-owner that is requested to be removed.
         * @return See {@link BaseCommand#run()}.
         */
        RemoveOwner newRemoveOwner(
            ICommandSender commandSender, MovableRetriever movableRetriever, IPPlayer targetPlayer);
    }
}
