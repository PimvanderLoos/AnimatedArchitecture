package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IAnimatedArchitectureEventFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.events.IAnimatedArchitectureEventCaller;
import nl.pim16aap2.animatedarchitecture.core.events.IStructurePrepareLockChangeEvent;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.animatedarchitecture.core.commands.CommandTestingUtil.initCommandSenderPermissions;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LockTest
{
    private StructureRetriever doorRetriever;

    @Mock
    private Structure door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock
    private IStructurePrepareLockChangeEvent event;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Lock.IFactory factory;

    @BeforeEach
    void init()
    {
        initCommandSenderPermissions(commandSender, true, true);
        Mockito.when(door.isOwner(Mockito.any(UUID.class), Mockito.any())).thenReturn(true);
        Mockito.when(door.isOwner(Mockito.any(IPlayer.class), Mockito.any())).thenReturn(true);
        doorRetriever = StructureRetrieverFactory.ofStructure(door);

        Mockito.when(door.syncData())
            .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        final IAnimatedArchitectureEventFactory eventFactory = Mockito.mock(IAnimatedArchitectureEventFactory.class);
        Mockito.when(eventFactory.createStructurePrepareLockChangeEvent(
                Mockito.any(),
                Mockito.anyBoolean(),
                Mockito.any()))
            .thenReturn(event);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newLock(
                Mockito.any(ICommandSender.class),
                Mockito.any(StructureRetriever.class),
                Mockito.anyBoolean(),
                Mockito.anyBoolean()))
            .thenAnswer(invoc -> new Lock(
                invoc.getArgument(0, ICommandSender.class),
                invoc.getArgument(1, StructureRetriever.class),
                invoc.getArgument(2, Boolean.class),
                invoc.getArgument(3, Boolean.class),
                localizer,
                ITextFactory.getSimpleTextFactory(),
                Mockito.mock(CommandFactory.class),
                Mockito.mock(IAnimatedArchitectureEventCaller.class),
                eventFactory)
            );
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
