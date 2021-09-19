package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.IDiscreteMovement;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

class SetBlocksToMoveTest
{
    private AbstractDoor door;

    private DoorRetriever.AbstractRetriever doorRetriever;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SetBlocksToMove.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        door = Mockito.mock(AbstractDoor.class, Mockito.withSettings().extraInterfaces(IDiscreteMovement.class));
        Mockito.when(door.syncData()).thenReturn(CompletableFuture.completedFuture(true));

        initCommandSenderPermissions(commandSender, true, true);
        doorRetriever = DoorRetriever.ofDoor(door);

        final IPLogger logger = new BasicPLogger();
        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newSetBlocksToMove(Mockito.any(ICommandSender.class),
                                                Mockito.any(DoorRetriever.AbstractRetriever.class),
                                                Mockito.anyInt()))
               .thenAnswer(invoc -> new SetBlocksToMove(invoc.getArgument(0, ICommandSender.class), logger, localizer,
                                                        invoc.getArgument(1, DoorRetriever.AbstractRetriever.class),
                                                        invoc.getArgument(2, Integer.class)));
    }

    @Test
    @SneakyThrows
    void testDoorTypes()
    {
        final int blocksToMove = 42;

        final SetBlocksToMove command = factory.newSetBlocksToMove(commandSender, doorRetriever, blocksToMove);
        final AbstractDoor altDoor = Mockito.mock(AbstractDoor.class);

        Assertions.assertTrue(command.performAction(altDoor).get(1, TimeUnit.SECONDS));
        Mockito.verify(altDoor, Mockito.never()).syncData();

        Assertions.assertTrue(command.performAction(door).get(1, TimeUnit.SECONDS));
        Mockito.verify((IDiscreteMovement) door).setBlocksToMove(blocksToMove);
        Mockito.verify(door).syncData();
    }

    // TODO: Re-implement
//    @Test
//    @SneakyThrows
//    void testDelayedInput()
//    {
//        final int blocksToMove = 42;
//
//        final int first = SetBlocksToMove.runDelayed(commandSender, doorRetriever);
//        final int second = SetBlocksToMove.provideDelayedInput(commandSender, blocksToMove);
//
//        Assertions.assertTrue(first.get(1, TimeUnit.SECONDS));
//        Assertions.assertEquals(first, second);
//
//        Mockito.verify((IDiscreteMovement) door).setBlocksToMove(blocksToMove);
//        Mockito.verify(door).syncData();
//    }
}
