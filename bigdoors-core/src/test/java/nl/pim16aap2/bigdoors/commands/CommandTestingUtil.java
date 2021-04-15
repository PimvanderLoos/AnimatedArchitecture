package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
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
     * Sets up a {@link DoorRetriever} with a specific {@link AbstractDoorBase} such that requesting the door from the
     * retriever will return the specified door.
     *
     * @param doorRetriever The retriever to set up.
     * @param door          The door to be retrieved by the retriever.
     */
    public static void initDoorRetriever(final DoorRetriever doorRetriever, final AbstractDoorBase door)
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
        Mockito.when(commandSender.hasPermission(Mockito.any(String.class)))
               .thenReturn(CompletableFuture.completedFuture(userPerm));
        Mockito.when(commandSender.hasPermission(Mockito.any(CommandDefinition.class)))
               .thenReturn(CompletableFuture.completedFuture(new BooleanPair(userPerm, adminPerm)));
    }

    /**
     * Initializes and registers a new {@link IBigDoorsPlatform}. A {@link BasicPLogger} is also set up.
     *
     * @return The new {@link IBigDoorsPlatform}.
     */
    public static IBigDoorsPlatform initPlatform()
    {
        IBigDoorsPlatform platform = Mockito.mock(IBigDoorsPlatform.class);
        BigDoors.get().setBigDoorsPlatform(platform);
        Mockito.when(platform.getPLogger()).thenReturn(new BasicPLogger());
        return platform;
    }
}
