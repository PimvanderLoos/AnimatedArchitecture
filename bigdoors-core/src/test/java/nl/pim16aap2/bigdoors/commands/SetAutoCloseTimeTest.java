package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

@Timeout(1)
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
        Mockito.when(movable.syncData())
               .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));
        Mockito.when(movable.isOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(movable.isOwner(Mockito.any(IPPlayer.class))).thenReturn(true);

        Mockito.when(movableType.getLocalizationKey()).thenReturn("MovableType");
        Mockito.when(movable.getType()).thenReturn(movableType);

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
    {
        final int autoCloseValue = 42;

        final SetAutoCloseTime command = factory.newSetAutoCloseTime(commandSender, movableRetriever, autoCloseValue);
        final AbstractMovable altMovable = Mockito.mock(AbstractMovable.class);
        Mockito.when(altMovable.getType()).thenReturn(movableType);

        Assertions.assertDoesNotThrow(() -> command.performAction(altMovable).get(1, TimeUnit.SECONDS));
        Mockito.verify(altMovable, Mockito.never()).syncData();

        Assertions.assertDoesNotThrow(() -> command.performAction(movable).get(1, TimeUnit.SECONDS));
        Mockito.verify((ITimerToggleable) movable).setAutoCloseTime(autoCloseValue);
        Mockito.verify(movable).syncData();
    }
}
