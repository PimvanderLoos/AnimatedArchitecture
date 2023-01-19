package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IDiscreteMovement;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

@Timeout(1)
class SetBlocksToMoveTest
{
    private AbstractMovable movable;

    private MovableRetriever movableRetriever;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SetBlocksToMove.IFactory factory;

    @Mock
    private MovableType movableType;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        movable = Mockito.mock(AbstractMovable.class, Mockito.withSettings().extraInterfaces(IDiscreteMovement.class));
        Mockito.when(movable.syncData())
               .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        Mockito.when(movableType.getLocalizationKey()).thenReturn("MovableType");
        Mockito.when(movable.getType()).thenReturn(movableType);

        initCommandSenderPermissions(commandSender, true, true);
        movableRetriever = MovableRetrieverFactory.ofMovable(movable);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newSetBlocksToMove(Mockito.any(ICommandSender.class),
                                                Mockito.any(MovableRetriever.class),
                                                Mockito.anyInt()))
               .thenAnswer(invoc -> new SetBlocksToMove(invoc.getArgument(0, ICommandSender.class), localizer,
                                                        ITextFactory.getSimpleTextFactory(),
                                                        invoc.getArgument(1, MovableRetriever.class),
                                                        invoc.getArgument(2, Integer.class)));
    }

    @Test
    void testMovableTypes()
    {
        final int blocksToMove = 42;

        final SetBlocksToMove command = factory.newSetBlocksToMove(commandSender, movableRetriever, blocksToMove);
        final AbstractMovable altMovable = Mockito.mock(AbstractMovable.class);
        Mockito.when(altMovable.getType()).thenReturn(movableType);

        Assertions.assertDoesNotThrow(() -> command.performAction(altMovable).get(1, TimeUnit.SECONDS));
        Mockito.verify(altMovable, Mockito.never()).syncData();

        Assertions.assertDoesNotThrow(() -> command.performAction(movable).get(1, TimeUnit.SECONDS));
        Mockito.verify((IDiscreteMovement) movable).setBlocksToMove(blocksToMove);
        Mockito.verify(movable).syncData();
    }
}
