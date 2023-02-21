package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IAnimatedArchitectureEventFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.events.IAnimatedArchitectureEventCaller;
import nl.pim16aap2.animatedarchitecture.core.events.IStructurePrepareLockChangeEvent;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetrieverFactory;
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

import static nl.pim16aap2.animatedarchitecture.core.commands.CommandTestingUtil.initCommandSenderPermissions;

@Timeout(1)
class LockTest
{
    private StructureRetriever doorRetriever;

    @Mock
    private AbstractStructure door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock
    private IStructurePrepareLockChangeEvent event;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Lock.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);
        Mockito.when(door.isOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(door.isOwner(Mockito.any(IPlayer.class))).thenReturn(true);
        doorRetriever = StructureRetrieverFactory.ofStructure(door);

        Mockito.when(door.syncData())
               .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        final IAnimatedArchitectureEventFactory eventFactory = Mockito.mock(IAnimatedArchitectureEventFactory.class);
        Mockito.when(
                   eventFactory.createStructurePrepareLockChangeEvent(Mockito.any(), Mockito.anyBoolean(), Mockito.any()))
               .thenReturn(event);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newLock(Mockito.any(ICommandSender.class),
                                     Mockito.any(StructureRetriever.class),
                                     Mockito.anyBoolean()))
               .thenAnswer(invoc -> new Lock(invoc.getArgument(0, ICommandSender.class), localizer,
                                             ITextFactory.getSimpleTextFactory(),
                                             invoc.getArgument(1, StructureRetriever.class),
                                             invoc.getArgument(2, Boolean.class),
                                             Mockito.mock(IAnimatedArchitectureEventCaller.class),
                                             eventFactory));
    }

    @Test
    void test()
        throws Exception
    {
        final boolean lock = true;
        Mockito.when(event.isCancelled()).thenReturn(true);

        Assertions.assertDoesNotThrow(
            () -> factory.newLock(commandSender, doorRetriever, lock).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(door, Mockito.never()).setLocked(lock);

        Mockito.when(event.isCancelled()).thenReturn(false);
        Assertions.assertDoesNotThrow(
            () -> factory.newLock(commandSender, doorRetriever, lock).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(door).setLocked(lock);
    }
}
