package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.animation.StructureActivityManager;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.testing.AssistedFactoryMocker;
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

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StopStructuresTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IServer commandSender;

    @Mock
    private StructureActivityManager structureActivityManager;

    private StopStructures.IFactory factory;

    @Mock
    private IExecutor executor;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);

        factory = new AssistedFactoryMocker<>(StopStructures.class, StopStructures.IFactory.class)
            .injectParameters(structureActivityManager, executor)
            .getFactory();
    }

    @Test
    void test()
    {
        Assertions.assertDoesNotThrow(() -> factory.newStopStructures(commandSender).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(structureActivityManager).shutDown();
    }
}
