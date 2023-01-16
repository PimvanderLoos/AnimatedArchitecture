package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

class SetAutoCloseTimeTest
{
    private AbstractMovable movable;

    private MovableRetriever movableRetriever;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SetAutoCloseTime.IFactory factory;

    @Mock
    private MovableType movableType;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        movable = Mockito.mock(AbstractMovable.class,
                               Mockito.withSettings().extraInterfaces(ITimerToggleable.class));
        Mockito.when(movable.syncData()).thenReturn(CompletableFuture.completedFuture(true));
        Mockito.when(movable.isMovableOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(movable.isMovableOwner(Mockito.any(IPPlayer.class))).thenReturn(true);

        Mockito.when(movableType.getLocalizationKey()).thenReturn("MovableType");
        Mockito.when(movable.getMovableType()).thenReturn(movableType);

        initCommandSenderPermissions(commandSender, true, true);
        movableRetriever = MovableRetrieverFactory.ofMovable(movable);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newSetAutoCloseTime(Mockito.any(ICommandSender.class),
                                                 Mockito.any(MovableRetriever.class),
                                                 Mockito.anyInt()))
               .thenAnswer(invoc -> new SetAutoCloseTime(invoc.getArgument(0, ICommandSender.class), localizer,
                                                         ITextFactory.getSimpleTextFactory(),
                                                         invoc.getArgument(1, MovableRetriever.class),
                                                         invoc.getArgument(2, Integer.class)));
    }

    @Test
    void testMovableTypes()
        throws Exception
    {
        final int autoCloseValue = 42;

        final SetAutoCloseTime command = factory.newSetAutoCloseTime(commandSender, movableRetriever, autoCloseValue);
        final AbstractMovable altMovable = Mockito.mock(AbstractMovable.class);
        Mockito.when(altMovable.getMovableType()).thenReturn(movableType);

        Assertions.assertTrue(command.performAction(altMovable).get(1, TimeUnit.SECONDS));
        Mockito.verify(altMovable, Mockito.never()).syncData();

        Assertions.assertTrue(command.performAction(movable).get(1, TimeUnit.SECONDS));
        Mockito.verify((ITimerToggleable) movable).setAutoCloseTime(autoCloseValue);
        Mockito.verify(movable).syncData();
    }

    // TODO: Re-implement
//    @Test
//    @SneakyThrows
//    void testDelayedInput()
//    {
//        final int autoCloseValue = 42;
//
//        final int first = SetAutoCloseTime.runDelayed(commandSender, doorRetriever);
//        final int second = SetAutoCloseTime.provideDelayedInput(commandSender, autoCloseValue);
//
//        Assertions.assertTrue(first.get(1, TimeUnit.SECONDS));
//        Assertions.assertEquals(first, second);
//
//        Mockito.verify((ITimerToggleable) door).setAutoCloseTime(autoCloseValue);
//        Mockito.verify(door).syncData();
//    }
}
