package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.api.PlayerData;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;

class CommandTestingUtil
{
    public static final PlayerData playerData = Mockito.mock(PlayerData.class);
    public static final StructureOwner structureOwnerCreator = new StructureOwner(0, PermissionLevel.CREATOR,
                                                                                  playerData);
    public static final StructureOwner structureOwnerAdmin = new StructureOwner(0, PermissionLevel.ADMIN, playerData);
    public static final StructureOwner structureOwnerUser = new StructureOwner(0, PermissionLevel.USER, playerData);
    public static final StructureOwner structureOwnerNoPerm = new StructureOwner(0, PermissionLevel.NO_PERMISSION,
                                                                                 playerData);

    /**
     * Sets up the permissions for an {@link ICommandSender}.
     *
     * @param commandSender
     *     The command sender for which to set up the permissions.
     * @param userPerm
     *     Whether user permissions are true/false.
     * @param adminPerm
     *     Whether admin permissions are true/false.
     */
    public static void initCommandSenderPermissions(ICommandSender commandSender, boolean userPerm, boolean adminPerm)
    {
        Mockito.doReturn(CompletableFuture.completedFuture(userPerm))
               .when(commandSender).hasPermission(Mockito.anyString());

        Mockito.doReturn(CompletableFuture.completedFuture(new PermissionsStatus(userPerm, adminPerm)))
               .when(commandSender).hasPermission(Mockito.any(CommandDefinition.class));
    }
}
