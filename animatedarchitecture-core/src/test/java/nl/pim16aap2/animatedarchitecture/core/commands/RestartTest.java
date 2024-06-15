package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatformProvider;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
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

import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

    @BeforeEach
    void beforeEach()
    {
        Mockito.when(platformProvider.getPlatform()).thenReturn(Optional.of(platform));

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newRestart(Mockito.any(ICommandSender.class)))
            .thenAnswer(invoc -> new Restart(
                invoc.getArgument(0, ICommandSender.class),
                localizer,
                ITextFactory.getSimpleTextFactory(),
                platformProvider)
            );
    }

    @Test
    void test()
    {
        Assertions.assertDoesNotThrow(() -> factory.newRestart(commandSender).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(platform).restartPlugin();
    }
}
