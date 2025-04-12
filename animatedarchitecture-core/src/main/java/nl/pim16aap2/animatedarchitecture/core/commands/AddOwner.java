package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.AccessLevel;
import lombok.Getter;
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
 * Represents the command that adds co-owners to a given structure.
 */
@ToString(callSuper = true)
@Flogger
public final class AddOwner extends StructureTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.ADD_OWNER;

    /**
     * The default value to use for {@link #targetPermissionLevel} when none is specified.
     */
    static final PermissionLevel DEFAULT_PERMISSION_LEVEL = PermissionLevel.USER;

    /**
     * The target player that will be added to the {@link #structureRetriever} as co-owner.
     * <p>
     * If this player is already an owner of the target door, their permission will be overridden provided that the
     * command sender is allowed to add/remove co-owners at both the old and the new target permission level.
     */
    private final IPlayer targetPlayer;

    /**
     * The permission level of the new owner's ownership.
     */
    @Getter(AccessLevel.PACKAGE)
    private final PermissionLevel targetPermissionLevel;

    @ToString.Exclude
    private final DatabaseManager databaseManager;

    @AssistedInject
    AddOwner(
        @Assisted ICommandSender commandSender,
        @Assisted StructureRetriever structureRetriever,
        @Assisted IPlayer targetPlayer,
        @Assisted @Nullable PermissionLevel targetPermissionLevel,
        IExecutor executor,
        DatabaseManager databaseManager)
    {
        super(commandSender, executor, structureRetriever, StructureAttribute.ADD_OWNER);
        this.targetPlayer = targetPlayer;
        this.targetPermissionLevel = targetPermissionLevel == null ? DEFAULT_PERMISSION_LEVEL : targetPermissionLevel;
        this.databaseManager = databaseManager;
    }

    @Override
    protected void handleDatabaseActionSuccess(@Nullable Structure retrieverResult)
    {
        getCommandSender().sendSuccess(
            "commands.add_owner.success",
            arg -> arg.highlight(targetPlayer.getName()),
            arg -> arg.localizedHighlight(targetPermissionLevel.getTranslationKey()),
            arg -> arg.localizedHighlight(retrieverResult)
        );

        final var descriptionForTargetPlayer = getRetrievedStructureDescription(retrieverResult, targetPlayer);
        targetPlayer.sendInfo(
            "commands.add_owner.added_player_notification",
            arg -> arg.localizedHighlight(targetPermissionLevel.getTranslationKey()),
            arg -> arg.highlight(descriptionForTargetPlayer.localizedTypeName()),
            arg -> arg.highlight(descriptionForTargetPlayer.id())
        );
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected void validateInput()
    {
        if (targetPermissionLevel != PermissionLevel.CREATOR &&
            targetPermissionLevel != PermissionLevel.NO_PERMISSION)
            return;

        getCommandSender().sendError(
            "commands.add_owner.error.invalid_target_permission",
            arg -> arg.highlight(targetPermissionLevel)
        );

        throw new InvalidCommandInputException(
            true,
            String.format(
                "The target permission level '%s' is invalid for the AddOwner command.",
                targetPermissionLevel
            ));
    }

    @Override
    protected CompletableFuture<?> performAction(Structure structure)
    {
        return databaseManager
            .addOwner(structure, targetPlayer, targetPermissionLevel, getCommandSender().getPlayer().orElse(null))
            .thenAccept(result -> handleDatabaseActionResult(result, structure));
    }

    PermissionLevel getExistingPermissionLevel(Structure structure)
    {
        return structure
            .getOwner(targetPlayer)
            .map(StructureOwner::permission)
            .orElse(PermissionLevel.NO_PERMISSION);
    }

    @Override
    protected void isAllowed(Structure structure, boolean hasBypassPermission)
    {
        final PermissionLevel existingPermission = getExistingPermissionLevel(structure);

        if (!getCommandSender().isPlayer() || hasBypassPermission)
        {
            if (existingPermission == PermissionLevel.CREATOR)
            {
                getCommandSender().sendError("commands.add_owner.error.targeting_prime_owner");
                throw new NoAccessToStructureCommandException(true, "Cannot target the prime owner of a structure.");
            }
            return;
        }

        final var doorOwner = getCommandSender().getPlayer().flatMap(structure::getOwner);
        if (doorOwner.isEmpty())
        {
            getCommandSender().sendError(
                "commands.add_owner.error.not_an_owner",
                arg -> arg.localizedHighlight(structure)
            );
            throw new NoAccessToStructureCommandException(true, "The command sender is not an owner of the structure.");
        }

        final PermissionLevel executorPermission = doorOwner.get().permission();
        if (!StructureAttribute.ADD_OWNER.canAccessWith(doorOwner.get().permission()))
        {
            getCommandSender().sendError(
                "commands.add_owner.error.not_allowed",
                arg -> arg.localizedHighlight(structure)
            );

            throw new NoAccessToStructureCommandException(true, "The command sender is not allowed to add owners.");
        }

        if (targetPermissionLevel.isLowerThanOrEquals(executorPermission))
        {
            getCommandSender().sendError("commands.add_owner.error.cannot_assign_below_self");
            throw new NoAccessToStructureCommandException(
                true,
                "Cannot assign a permission level below the command sender."
            );
        }

        if (existingPermission.isLowerThanOrEquals(targetPermissionLevel))
        {
            getCommandSender().sendError(
                "commands.add_owner.error.target_already_owner",
                arg -> arg.highlight(targetPlayer.asString()),
                arg -> arg.localizedHighlight(structure)
            );
            throw new NoAccessToStructureCommandException(
                true,
                "The target player is already an owner of the structure."
            );
        }
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
         * @param structureRetriever
         *     A {@link StructureRetrieverFactory} that references the target structure.
         * @param targetPlayer
         *     The target player to add to this structure as co-owner.
         *     <p>
         *     If this player is already an owner of the target door, their permission will be overridden provided that
         *     the command sender is allowed to add/remove co-owners at both the old and the new target permission
         *     level.
         * @param targetPermissionLevel
         *     The permission level of the new owner's ownership.
         * @return See {@link BaseCommand#run()}.
         */
        AddOwner newAddOwner(
            ICommandSender commandSender,
            StructureRetriever structureRetriever,
            IPlayer targetPlayer,
            @Nullable PermissionLevel targetPermissionLevel);

        /**
         * See {@link #newAddOwner(ICommandSender, StructureRetriever, IPlayer, PermissionLevel)}.
         * <p>
         * The default permission node defined by {@link AddOwner#DEFAULT_PERMISSION_LEVEL} is used.
         */
        default AddOwner newAddOwner(
            ICommandSender commandSender,
            StructureRetriever structureRetriever,
            IPlayer targetPlayer)
        {
            return newAddOwner(commandSender, structureRetriever, targetPlayer, null);
        }
    }
}
