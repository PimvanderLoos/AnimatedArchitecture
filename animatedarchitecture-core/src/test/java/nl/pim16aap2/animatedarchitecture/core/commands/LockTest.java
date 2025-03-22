package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IAnimatedArchitectureEventFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.events.IStructurePrepareLockChangeEvent;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.animatedarchitecture.core.commands.CommandTestingUtil.initCommandSenderPermissions;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LockTest
{
    private final ITextFactory textFactory = ITextFactory.getSimpleTextFactory();

    private StructureRetriever doorRetriever;

    @Mock
    private Structure door;

    @Mock
    private IPlayer commandSender;

    @Mock
    private IStructurePrepareLockChangeEvent event;

    private AssistedFactoryMocker<Lock, Lock.IFactory> assistedFactoryMocker;

    private Lock.IFactory factory;

    @Mock
    private IExecutor executor;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        initCommandSenderPermissions(commandSender, true, true);
        when(door.isOwner(any(UUID.class), any())).thenReturn(true);
        when(door.isOwner(any(IPlayer.class), any())).thenReturn(true);
        doorRetriever = StructureRetrieverFactory.ofStructure(door);

        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());
        when(door.syncData()).thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        final IAnimatedArchitectureEventFactory eventFactory = mock(IAnimatedArchitectureEventFactory.class);
        when(eventFactory.createStructurePrepareLockChangeEvent(any(), anyBoolean(), any())).thenReturn(event);

        assistedFactoryMocker = new AssistedFactoryMocker<>(Lock.class, Lock.IFactory.class)
            .setMock(ITextFactory.class, textFactory)
            .setMock(ILocalizer.class, UnitTestUtil.initLocalizer())
            .setMock(IExecutor.class, executor)
            .setMock(IAnimatedArchitectureEventFactory.class, eventFactory);

        factory = assistedFactoryMocker.getFactory();
    }

    @Test
    void test()
        throws Exception
    {
        final boolean lock = true;
        when(event.isCancelled()).thenReturn(true);

        UnitTestUtil.setStructureLocalization(door);

        assertDoesNotThrow(() -> factory.newLock(commandSender, doorRetriever, lock).run().get(1, TimeUnit.SECONDS));
        verify(door, never()).setLocked(lock);

        when(event.isCancelled()).thenReturn(false);
        assertDoesNotThrow(() -> factory.newLock(commandSender, doorRetriever, lock).run().get(1, TimeUnit.SECONDS));
        verify(door).setLocked(lock);
    }
}
