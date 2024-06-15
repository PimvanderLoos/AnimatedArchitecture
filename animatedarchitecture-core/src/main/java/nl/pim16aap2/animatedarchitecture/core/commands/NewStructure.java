package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IPermissionsManager;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.inject.Provider;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to create new structures.
 */
@ToString
public class NewStructure extends BaseCommand
{
    private final StructureType structureType;
    private final @Nullable String structureName;
    private final IPermissionsManager permissionsManager;
    private final ToolUserManager toolUserManager;
    private final Provider<ToolUser.Context> creatorContextProvider;

    @AssistedInject
    NewStructure(
        @Assisted ICommandSender commandSender,
        @Assisted StructureType structureType,
        @Assisted @Nullable String structureName,
        ILocalizer localizer,
        ITextFactory textFactory,
        IPermissionsManager permissionsManager,
        ToolUserManager toolUserManager,
        Provider<ToolUser.Context> creatorContextProvider)
    {
        super(commandSender, localizer, textFactory);
        this.structureType = structureType;
        this.structureName = structureName;
        this.permissionsManager = permissionsManager;
        this.toolUserManager = toolUserManager;
        this.creatorContextProvider = creatorContextProvider;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.NEW_STRUCTURE;
    }

    @Override
    protected CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        toolUserManager.startToolUser(
            structureType.getCreator(creatorContextProvider.get(), (IPlayer) getCommandSender(), structureName),
            Constants.STRUCTURE_CREATOR_TIME_LIMIT
        );
        return CompletableFuture.completedFuture(null);
    }

    private PermissionsStatus hasPermission(PermissionsStatus basePermissions)
    {
        if (!basePermissions.hasAnyPermission())
            return basePermissions;

        if (!basePermissions.hasUserPermission() && basePermissions.hasAdminPermission())
            return basePermissions;

        final boolean permissionForType =
            permissionsManager.hasPermissionToCreateStructure(getCommandSender(), structureType);

        return new PermissionsStatus(
            basePermissions.hasUserPermission() && permissionForType,
            basePermissions.hasAdminPermission()
        );
    }

    @Override
    protected CompletableFuture<PermissionsStatus> hasPermission()
    {
        return super.hasPermission()
            .thenApply(this::hasPermission)
            .exceptionally(e -> Util.exceptionally(e, new PermissionsStatus(false, false)));
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link NewStructure} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for creating a new structure.
         * @param structureType
         *     The type of structure that will be created.
         * @param structureName
         *     The name of the structure, if it has been specified already.
         *     <p>
         *     When this is null, the creator will start at the first step (specifying the name). If it has been
         *     specified, this step will be skipped.
         * @return See {@link BaseCommand#run()}.
         */
        NewStructure newNewStructure(
            ICommandSender commandSender,
            StructureType structureType,
            @Nullable String structureName
        );

        /**
         * See {@link #newNewStructure(ICommandSender, StructureType, String)}.
         */
        default NewStructure newNewStructure(ICommandSender commandSender, StructureType structureType)
        {
            return newNewStructure(commandSender, structureType, null);
        }
    }
}
