package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that adds co-owners to a given structure.
 */
@ToString
@Flogger
public class AddOwner extends StructureTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.ADD_OWNER;

    /**
     * The default value to use for {@link #targetPermissionLevel} when none is specified.
     */
    protected static final PermissionLevel DEFAULT_PERMISSION_LEVEL = PermissionLevel.USER;

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
    private final PermissionLevel targetPermissionLevel;

    private final DatabaseManager databaseManager;

    @AssistedInject
    AddOwner(
        @Assisted ICommandSender commandSender,
        ILocalizer localizer,
        ITextFactory textFactory,
        @Assisted StructureRetriever doorRetriever,
        @Assisted IPlayer targetPlayer,
        @Assisted @Nullable PermissionLevel targetPermissionLevel,
        DatabaseManager databaseManager)
    {
        super(commandSender, localizer, textFactory, doorRetriever, StructureAttribute.ADD_OWNER);
        this.targetPlayer = targetPlayer;
        this.targetPermissionLevel = targetPermissionLevel == null ? DEFAULT_PERMISSION_LEVEL : targetPermissionLevel;
        this.databaseManager = databaseManager;
    }

    @Override
    protected void handleDatabaseActionSuccess()
    {
        final var description = getRetrievedStructureDescription();
        final String localizedRank = localizer.getMessage(targetPermissionLevel.getTranslationKey());

        getCommandSender().sendMessage(textFactory.newText().append(
            localizer.getMessage("commands.add_owner.success"),
            TextType.SUCCESS,
            arg -> arg.highlight(targetPlayer.getName()),
            arg -> arg.highlight(localizedRank),
            arg -> arg.highlight(description.localizedTypeName()))
        );

        targetPlayer.sendMessage(textFactory.newText().append(
            localizer.getMessage("commands.add_owner.added_player_notification"),
            TextType.INFO,
            arg -> arg.highlight(localizedRank),
            arg -> arg.highlight(description.localizedTypeName()),
            arg -> arg.highlight(description.id()))
        );
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected boolean validInput()
    {
        if (targetPermissionLevel != PermissionLevel.CREATOR &&
            targetPermissionLevel != PermissionLevel.NO_PERMISSION)
            return true;

        getCommandSender().sendMessage(textFactory.newText().append(
            localizer.getMessage("commands.add_owner.error.invalid_target_permission"),
            TextType.ERROR,
            arg -> arg.highlight(targetPermissionLevel))
        );

        return false;
    }

    @Override
    protected CompletableFuture<?> performAction(Structure structure)
    {
        return databaseManager
            .addOwner(structure, targetPlayer, targetPermissionLevel, getCommandSender().getPlayer().orElse(null))
            .thenAccept(this::handleDatabaseActionResult);
    }

    @Override
    protected boolean isAllowed(Structure structure, boolean hasBypassPermission)
    {
        final PermissionLevel existingPermission = structure
            .getOwner(targetPlayer)
            .map(StructureOwner::permission)
            .orElse(PermissionLevel.NO_PERMISSION);

        if (!getCommandSender().isPlayer() || hasBypassPermission)
        {
            if (existingPermission == PermissionLevel.CREATOR)
            {
                getCommandSender().sendError(
                    textFactory,
                    localizer.getMessage("commands.add_owner.error.targeting_prime_owner")
                );
                return false;
            }
            return true;
        }

        final var doorOwner = getCommandSender().getPlayer().flatMap(structure::getOwner);
        if (doorOwner.isEmpty())
        {
            getCommandSender().sendMessage(textFactory.newText().append(
                localizer.getMessage("commands.add_owner.error.not_an_owner"),
                TextType.ERROR,
                arg -> arg.highlight(localizer.getStructureType(structure)))
            );
            return false;
        }

        final PermissionLevel executorPermission = doorOwner.get().permission();
        if (!StructureAttribute.ADD_OWNER.canAccessWith(doorOwner.get().permission()))
        {
            getCommandSender().sendMessage(textFactory.newText().append(
                localizer.getMessage("commands.add_owner.error.not_allowed"),
                TextType.ERROR,
                arg -> arg.highlight(localizer.getStructureType(structure)))
            );
            return false;
        }

        if (targetPermissionLevel.isLowerThanOrEquals(executorPermission))
        {
            getCommandSender().sendError(
                textFactory,
                localizer.getMessage("commands.add_owner.error.cannot_assign_below_self")
            );
            return false;
        }

        if (existingPermission.isLowerThanOrEquals(executorPermission) || existingPermission == targetPermissionLevel)
        {
            getCommandSender().sendMessage(textFactory.newText().append(
                localizer.getMessage("commands.add_owner.error.target_already_owner"),
                TextType.ERROR,
                arg -> arg.highlight(targetPlayer.asString()),
                arg -> arg.highlight(localizer.getStructureType(structure)))
            );
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
            StructureRetriever doorRetriever,
            IPlayer targetPlayer,
            @Nullable PermissionLevel targetPermissionLevel);

        /**
         * See {@link #newAddOwner(ICommandSender, StructureRetriever, IPlayer, PermissionLevel)}.
         * <p>
         * The default permission node defined by {@link AddOwner#DEFAULT_PERMISSION_LEVEL} is used.
         */
        default AddOwner newAddOwner(
            ICommandSender commandSender,
            StructureRetriever doorRetriever,
            IPlayer targetPlayer)
        {
            return newAddOwner(commandSender, doorRetriever, targetPlayer, null);
        }
    }
}
