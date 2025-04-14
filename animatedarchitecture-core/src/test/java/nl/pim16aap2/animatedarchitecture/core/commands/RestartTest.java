package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatformProvider;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class RestartTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IServer commandSender;

    @Mock
    private IAnimatedArchitecturePlatformProvider platformProvider;

    @Mock
    private IAnimatedArchitecturePlatform platform;

    @Mock
    private IExecutor executor;

    private Restart.IFactory factory;

    @BeforeEach
    void beforeEach()
        throws NoSuchMethodException
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());
        when(platformProvider.getPlatform()).thenReturn(Optional.of(platform));

        factory = new AssistedFactoryMocker<>(Restart.class, Restart.IFactory.class)
            .injectParameters(executor, platformProvider)
            .getFactory();
    }

    @Test
    void test()
    {
        UnitTestUtil.initMessageable(commandSender);
        assertDoesNotThrow(() -> factory.newRestart(commandSender).run().get(1, TimeUnit.SECONDS));
        verify(platform).restartPlugin();
    }
}
