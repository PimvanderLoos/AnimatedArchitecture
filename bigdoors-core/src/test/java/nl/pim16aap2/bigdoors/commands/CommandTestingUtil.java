package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

class CommandTestingUtil
{
    public static final PPlayerData playerData = Mockito.mock(PPlayerData.class);
    public static final DoorOwner doorOwner0 = new DoorOwner(0, 0, playerData);
    public static final DoorOwner doorOwner1 = new DoorOwner(0, 1, playerData);
    public static final DoorOwner doorOwner2 = new DoorOwner(0, 2, playerData);
    public static final DoorOwner doorOwner3 = new DoorOwner(0, 3, playerData);

    /**
     * Sets up a {@link DoorRetriever} with a specific {@link DoorBase} such that requesting the door from the retriever
     * will return the specified door.
     *
     * @param doorRetriever The retriever to set up.
     * @param door          The door to be retrieved by the retriever.
     */
    public static void initDoorRetriever(final DoorRetriever doorRetriever, final AbstractDoor door)
    {
        Mockito.when(doorRetriever.getDoor()).thenReturn(CompletableFuture.completedFuture(Optional.of(door)));
        Mockito.when(doorRetriever.getDoor(Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(Optional.of(door)));
    }

    /**
     * Sets up the permissions for an {@link ICommandSender}.
     *
     * @param commandSender The command sender for which to set up the permissions.
     * @param userPerm      Whether or not user permissions are true/false.
     * @param adminPerm     Whether or not admin permissions are true/false.
     */
    public static void initCommandSenderPermissions(final ICommandSender commandSender,
                                                    final boolean userPerm, final boolean adminPerm)
    {
        Mockito.doReturn(CompletableFuture.completedFuture(userPerm))
               .when(commandSender).hasPermission(Mockito.anyString());

        Mockito.doReturn(CompletableFuture.completedFuture(new BooleanPair(userPerm, adminPerm)))
               .when(commandSender).hasPermission(Mockito.any(CommandDefinition.class));
    }
}
