package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.util.MovementDirection;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

@Timeout(1)
class SetOpenDirectionTest
{
    @Mock
    private AbstractMovable movable;

    @Mock
    private MovableType movableType;

    private MovableRetriever movableRetriever;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SetOpenDirection.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        Mockito.when(movable.syncData())
               .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));
        Mockito.when(movable.getType()).thenReturn(movableType);

        initCommandSenderPermissions(commandSender, true, true);
        movableRetriever = MovableRetrieverFactory.ofMovable(movable);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newSetOpenDirection(Mockito.any(ICommandSender.class),
                                                 Mockito.any(MovableRetriever.class),
                                                 Mockito.any(MovementDirection.class)))
               .thenAnswer(invoc -> new SetOpenDirection(invoc.getArgument(0, ICommandSender.class), localizer,
                                                         ITextFactory.getSimpleTextFactory(),
                                                         invoc.getArgument(1, MovableRetriever.class),
                                                         invoc.getArgument(2, MovementDirection.class)));
    }

    @Test
    void testOpenDirValidity()
    {
        final MovementDirection movementDirection = MovementDirection.CLOCKWISE;

        Mockito.when(movableType.isValidOpenDirection(Mockito.any())).thenReturn(false);
        final SetOpenDirection command =
            factory.newSetOpenDirection(commandSender, movableRetriever, movementDirection);

        Assertions.assertDoesNotThrow(() -> command.performAction(movable).get(1, TimeUnit.SECONDS));
        Mockito.verify(movable, Mockito.never()).syncData();
        Mockito.verify(movable, Mockito.never()).setOpenDir(movementDirection);


        Mockito.when(movableType.isValidOpenDirection(movementDirection)).thenReturn(true);
        Assertions.assertDoesNotThrow(() -> command.performAction(movable).get(1, TimeUnit.SECONDS));
        Mockito.verify(movable).setOpenDir(movementDirection);
        Mockito.verify(movable).syncData();
    }
}
