package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import lombok.val;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.DoorOpener;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.*;

class ToggleTest
{
    @Mock
    private DoorRetriever doorRetriever;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    private IBigDoorsPlatform platform;

    @Mock
    private AbstractDoorBase door;

    private DoorOpener doorOpener;

    @BeforeEach
    void init()
    {
        platform = initPlatform();
        MockitoAnnotations.openMocks(this);

        doorOpener = Mockito.mock(DoorOpener.class);
        Mockito.when(platform.getDoorOpener()).thenReturn(doorOpener);
        initCommandSenderPermissions(commandSender, true, true);
        initDoorRetriever(doorRetriever, door);
    }

    @Test
    void testValidInput()
    {
        Assertions.assertTrue(new Toggle(commandSender, Toggle.DEFAULT_DOOR_ACTION_TYPE,
                                         Toggle.DEFAULT_SPEED_MULTIPLIER, doorRetriever).validInput());

        //noinspection RedundantArrayCreation
        Assertions.assertFalse(new Toggle(commandSender, Toggle.DEFAULT_DOOR_ACTION_TYPE,
                                          Toggle.DEFAULT_SPEED_MULTIPLIER, new DoorRetriever[0]).validInput());
    }

    private static void verifyDoorOpenerCall(int times, DoorOpener doorOpener, AbstractDoorBase door,
                                             DoorActionCause doorActionCause,
                                             ICommandSender commandSender, double time, boolean skip,
                                             DoorActionType doorActionType)
    {
        Mockito.verify(doorOpener, Mockito.times(times))
               .animateDoorAsync(door, doorActionCause, (IPPlayer) commandSender, time, skip, doorActionType);
    }

    @Test
    @SneakyThrows
    void testSuccess()
    {
        DoorOpener doorOpener = Mockito.mock(DoorOpener.class);
        Mockito.when(platform.getDoorOpener()).thenReturn(doorOpener);

        Toggle toggle = new Toggle(commandSender, Toggle.DEFAULT_DOOR_ACTION_TYPE,
                                   Toggle.DEFAULT_SPEED_MULTIPLIER, doorRetriever);
        toggle.executeCommand(new BooleanPair(true, true)).get(1, TimeUnit.SECONDS);
        verifyDoorOpenerCall(1, doorOpener,
                             door, DoorActionCause.PLAYER, commandSender, 0.0D, false, DoorActionType.TOGGLE);

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
        toggle.executeCommand(new BooleanPair(true, false)).get(1, TimeUnit.SECONDS);
        verifyDoorOpenerCall(2, doorOpener,
                             door, DoorActionCause.PLAYER, commandSender, 0.0D, false, DoorActionType.TOGGLE);
    }

    @Test
    @SneakyThrows
    void testExecution()
    {
        // Ensure that supplying multiple door retrievers properly attempts toggling all of them.
        final int count = 10;
        val retrievers = new DoorRetriever[count];
        val doors = new AbstractDoorBase[count];
        for (int idx = 0; idx < count; ++idx)
        {
            doors[idx] = Mockito.mock(AbstractDoorBase.class);
            retrievers[idx] = Mockito.mock(DoorRetriever.class);
            initDoorRetriever(retrievers[idx], doors[idx]);
        }

        val toggle = new Toggle(commandSender, Toggle.DEFAULT_DOOR_ACTION_TYPE,
                                Toggle.DEFAULT_SPEED_MULTIPLIER, retrievers);
        toggle.executeCommand(new BooleanPair(true, true)).get(1, TimeUnit.SECONDS);

        val toggledDoors = Mockito.mockingDetails(doorOpener).getInvocations().stream()
                                  .<AbstractDoorBase>map(invocation -> invocation.getArgument(0))
                                  .collect(Collectors.toSet());

        Assertions.assertEquals(count, toggledDoors.size());
        for (int idx = 0; idx < count; ++idx)
            Assertions.assertTrue(toggledDoors.contains(doors[idx]));
    }

    @Test
    @SneakyThrows
    void testStaticRunners()
    {
        Mockito.when(door.isCloseable()).thenReturn(true);
        Mockito.when(door.isOpenable()).thenReturn(true);

        Assertions.assertTrue(Toggle.run(commandSender, doorRetriever).get(1, TimeUnit.SECONDS));
        verifyDoorOpenerCall(1, doorOpener, door, DoorActionCause.PLAYER, commandSender,
                             Toggle.DEFAULT_SPEED_MULTIPLIER, false, DoorActionType.TOGGLE);

        Assertions.assertTrue(Toggle.run(commandSender, 3.141592653589793D, doorRetriever).get(1, TimeUnit.SECONDS));
        verifyDoorOpenerCall(1, doorOpener, door, DoorActionCause.PLAYER, commandSender,
                             3.141592653589793D, false, DoorActionType.TOGGLE);

        Assertions.assertTrue(Toggle.run(commandSender, DoorActionType.CLOSE, doorRetriever).get(1, TimeUnit.SECONDS));
        verifyDoorOpenerCall(1, doorOpener, door, DoorActionCause.PLAYER, commandSender,
                             Toggle.DEFAULT_SPEED_MULTIPLIER, false, DoorActionType.CLOSE);

        Assertions.assertTrue(Toggle.run(commandSender, DoorActionType.OPEN, 42, doorRetriever)
                                    .get(1, TimeUnit.SECONDS));
        verifyDoorOpenerCall(1, doorOpener, door, DoorActionCause.PLAYER, commandSender,
                             42, false, DoorActionType.OPEN);
    }

    @Test
    @SneakyThrows
    void testServerCommandSender()
    {
        val serverCommandSender = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertTrue(Toggle.run(serverCommandSender, DoorActionType.TOGGLE, doorRetriever)
                                    .get(1, TimeUnit.SECONDS));
        verifyDoorOpenerCall(1, doorOpener, door, DoorActionCause.SERVER, null,
                             Toggle.DEFAULT_SPEED_MULTIPLIER, false, DoorActionType.TOGGLE);
    }

    @Test
    @SneakyThrows
    void testAbort()
    {
        Mockito.when(door.isCloseable()).thenReturn(false);

        Assertions.assertTrue(Toggle.run(commandSender, DoorActionType.CLOSE, doorRetriever).get(1, TimeUnit.SECONDS));

        verifyDoorOpenerCall(0, doorOpener, door, DoorActionCause.PLAYER, commandSender,
                             Toggle.DEFAULT_SPEED_MULTIPLIER, false, DoorActionType.CLOSE);


        Mockito.when(door.isCloseable()).thenReturn(true);
        Mockito.when(commandSender.hasPermission(Mockito.any(CommandDefinition.class)))
               .thenReturn(CompletableFuture.completedFuture(new BooleanPair(false, false)));
        Mockito.when(door.getDoorOwner(Mockito.any(IPPlayer.class))).thenReturn(Optional.empty());

        Assertions.assertTrue(Toggle.run(commandSender, DoorActionType.CLOSE, doorRetriever).get(1, TimeUnit.SECONDS));

        verifyDoorOpenerCall(0, doorOpener, door, DoorActionCause.PLAYER, commandSender,
                             Toggle.DEFAULT_SPEED_MULTIPLIER, false, DoorActionType.CLOSE);
    }
}
