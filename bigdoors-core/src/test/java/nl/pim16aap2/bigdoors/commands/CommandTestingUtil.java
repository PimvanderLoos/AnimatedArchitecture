package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;

class CommandTestingUtil
{
    public static final PPlayerData playerData = Mockito.mock(PPlayerData.class);
    public static final DoorOwner doorOwner0 = new DoorOwner(0, 0, playerData);
    public static final DoorOwner doorOwner1 = new DoorOwner(0, 1, playerData);
    public static final DoorOwner doorOwner2 = new DoorOwner(0, 2, playerData);
    public static final DoorOwner doorOwner3 = new DoorOwner(0, 3, playerData);

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

        Mockito.doReturn(CompletableFuture.completedFuture(new BooleanPair(userPerm, adminPerm)))
               .when(commandSender).hasPermission(Mockito.any(CommandDefinition.class));
    }
}
