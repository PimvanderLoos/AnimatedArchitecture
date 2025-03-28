package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatformProvider;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RestartTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IServer commandSender;

    @Mock
    private IAnimatedArchitecturePlatformProvider platformProvider;

    @Mock
    private IAnimatedArchitecturePlatform platform;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Restart.IFactory factory;

    @Mock
    private IExecutor executor;

    @BeforeEach
    void beforeEach()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());
        when(platformProvider.getPlatform()).thenReturn(Optional.of(platform));

        when(factory
            .newRestart(any(ICommandSender.class)))
            .thenAnswer(invoc -> new Restart(
                invoc.getArgument(0, ICommandSender.class),
                executor,
                platformProvider)
            );
    }

    @Test
    void test()
    {
        assertDoesNotThrow(() -> factory.newRestart(commandSender).run().get(1, TimeUnit.SECONDS));
        verify(platform).restartPlugin();
    }
}
