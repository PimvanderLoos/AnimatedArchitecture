package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.exceptions.InvalidCommandInputException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.NoAccessToStructureCommandException;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the remove owner command. This command is used to remove owners from a structure.
 */
@ToString(callSuper = true)
@Flogger
public class RemoveOwner extends StructureTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.REMOVE_OWNER;

    private final IPlayer targetPlayer;

    @ToString.Exclude
    private final DatabaseManager databaseManager;

    @AssistedInject
    RemoveOwner(
        @Assisted ICommandSender commandSender,
        @Assisted StructureRetriever structureRetriever,
        @Assisted IPlayer targetPlayer,
        IExecutor executor,
        DatabaseManager databaseManager)
    {
        super(commandSender, executor, structureRetriever, StructureAttribute.REMOVE_OWNER);
        this.targetPlayer = targetPlayer;
        this.databaseManager = databaseManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected void handleDatabaseActionSuccess(@Nullable Structure retrieverResult)
    {
        final var descriptionForCommandSender = getRetrievedStructureDescription(retrieverResult);
        getCommandSender().sendSuccess(
            "commands.remove_owner.success",
            arg -> arg.highlight(targetPlayer.getName()),
            arg -> arg.highlight(descriptionForCommandSender.localizedTypeName())
        );

        final var descriptionForTargetPlayer = getRetrievedStructureDescription(retrieverResult, targetPlayer);
        targetPlayer.sendInfo(
            "commands.remove_owner.removed_player_notification",
            arg -> arg.highlight(descriptionForTargetPlayer.localizedTypeName()),
            arg -> arg.highlight(descriptionForTargetPlayer.id())
        );
    }

    @Override
    protected CompletableFuture<?> performAction(Structure structure)
    {
        return databaseManager
            .removeOwner(structure, targetPlayer, getCommandSender().getPlayer().orElse(null))
            .thenAccept(result -> handleDatabaseActionResult(result, structure));
    }

    @Override
    protected void isAllowed(Structure structure, boolean hasBypassPermission)
    {
        final boolean bypassOwnership = !getCommandSender().isPlayer() || hasBypassPermission;

        final var structureOwner = getCommandSender().getPlayer().flatMap(structure::getOwner);
        if (structureOwner.isEmpty() && !bypassOwnership)
        {
            getCommandSender().sendError(
                "commands.remove_owner.error.not_an_owner",
                arg -> arg.localizedHighlight(structure)
            );
            throw new InvalidCommandInputException(
                true,
                String.format("Player %s is not an owner of structure %s", getCommandSender(), structure.getBasicInfo())
            );
        }

        // Assume a permission level of CREATOR in case the command sender is not an owner but DOES have bypass access.
        final PermissionLevel ownerPermission =
            structureOwner
                .map(StructureOwner::permission)
                .orElse(PermissionLevel.CREATOR);

        if (!StructureAttribute.REMOVE_OWNER.canAccessWith(ownerPermission))
        {
            getCommandSender().sendError(
                "commands.remove_owner.error.not_allowed",
                arg -> arg.localizedHighlight(structure)
            );
            throw new NoAccessToStructureCommandException(true,
                String.format("Player %s does not have permission to remove an owner", getCommandSender()));
        }

        final var targetStructureOwner = structure.getOwner(targetPlayer);
        if (targetStructureOwner.isEmpty())
        {
            getCommandSender().sendError(
                "commands.remove_owner.error.target_not_an_owner",
                arg -> arg.highlight(targetPlayer.asString()),
                arg -> arg.localizedHighlight(structure),
                arg -> arg.highlight(structure.getBasicInfo())
            );
            throw new NoAccessToStructureCommandException(
                true,
                String.format(
                    "Player %s cannot remove non-owner %s structure %s",
                    getCommandSender(),
                    targetPlayer,
                    structure.getBasicInfo())
            );
        }

        if (targetStructureOwner.get().permission().isLowerThanOrEquals(ownerPermission))
        {
            getCommandSender().sendError("commands.remove_owner.error.cannot_remove_lower_permission");
            throw new NoAccessToStructureCommandException(
                true,
                "Player cannot remove an owner with equal or lower permission level"
            );
        }
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link RemoveOwner} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for removing a co-owner of the structure.
         * @param structureRetriever
         *     A {@link StructureRetrieverFactory} representing the {@link Structure} for which a co-owner is requested
         *     to be removed.
         * @param targetPlayer
         *     The co-owner that is requested to be removed.
         * @return See {@link BaseCommand#run()}.
         */
        RemoveOwner newRemoveOwner(
            ICommandSender commandSender,
            StructureRetriever structureRetriever,
            IPlayer targetPlayer
        );
    }
}
