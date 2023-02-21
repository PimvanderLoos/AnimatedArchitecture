package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the remove owner command. This command is used to remove owners from a structure.
 *
 * @author Pim
 */
@ToString
@Flogger
public class RemoveOwner extends StructureTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.REMOVE_OWNER;

    private final IPlayer targetPlayer;
    private final DatabaseManager databaseManager;

    @AssistedInject //
    RemoveOwner(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted StructureRetriever structureRetriever, @Assisted IPlayer targetPlayer,
        DatabaseManager databaseManager)
    {
        super(commandSender, localizer, textFactory, structureRetriever, StructureAttribute.REMOVE_OWNER);
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
        final var description = getRetrievedStructureDescription();
        getCommandSender().sendSuccess(textFactory,
                                       localizer.getMessage("commands.remove_owner.success",
                                                            targetPlayer.getName(), description.typeName()));
        targetPlayer.sendInfo(textFactory,
                              localizer.getMessage("commands.remove_owner.removed_player_notification",
                                                   description.typeName(), description.id()));
    }

    @Override
    protected CompletableFuture<?> performAction(AbstractStructure structure)
    {
        return databaseManager.removeOwner(structure, targetPlayer, getCommandSender().getPlayer().orElse(null))
                              .thenAccept(this::handleDatabaseActionResult);
    }

    @Override
    protected boolean isAllowed(AbstractStructure structure, boolean hasBypassPermission)
    {
        final boolean bypassOwnership = !getCommandSender().isPlayer() || hasBypassPermission;

        final var structureOwner = getCommandSender().getPlayer().flatMap(structure::getOwner);
        if (structureOwner.isEmpty() && !bypassOwnership)
        {
            getCommandSender().sendMessage(textFactory, TextType.ERROR,
                                           localizer.getMessage("commands.remove_owner.error.not_an_owner",
                                                                localizer.getStructureType(structure)));
            return false;
        }

        // Assume a permission level of 0 in case the command sender is not an owner but DOES have bypass access.
        final PermissionLevel ownerPermission = structureOwner.map(StructureOwner::permission)
                                                              .orElse(PermissionLevel.CREATOR);
        if (!StructureAttribute.REMOVE_OWNER.canAccessWith(ownerPermission))
        {
            getCommandSender().sendMessage(textFactory, TextType.ERROR,
                                           localizer.getMessage("commands.remove_owner.error.not_allowed",
                                                                localizer.getStructureType(structure)));
            return false;
        }

        final var targetStructureOwner = structure.getOwner(targetPlayer);
        if (targetStructureOwner.isEmpty())
        {
            getCommandSender()
                .sendMessage(textFactory, TextType.ERROR,
                             localizer.getMessage("commands.remove_owner.error.target_not_an_owner",
                                                  targetPlayer.asString(), localizer.getStructureType(structure),
                                                  structure.getBasicInfo()));
            return false;
        }

        if (targetStructureOwner.get().permission().isLowerThanOrEquals(ownerPermission))
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
         *     The {@link ICommandSender} responsible for removing a co-owner of the structure.
         * @param structureRetriever
         *     A {@link StructureRetrieverFactory} representing the {@link AbstractStructure} for which a co-owner is
         *     requested to be removed.
         * @param targetPlayer
         *     The co-owner that is requested to be removed.
         * @return See {@link BaseCommand#run()}.
         */
        RemoveOwner newRemoveOwner(
            ICommandSender commandSender, StructureRetriever structureRetriever, IPlayer targetPlayer);
    }
}
