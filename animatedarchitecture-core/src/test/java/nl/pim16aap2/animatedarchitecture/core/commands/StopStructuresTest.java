package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.animation.StructureActivityManager;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
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

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StopStructuresTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IServer commandSender;

    @Mock
    private StructureActivityManager structureActivityManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private StopStructures.IFactory factory;

    @Mock
    private IExecutor executor;

    @BeforeEach
    void init()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        when(factory
            .newStopStructures(any(ICommandSender.class)))
            .thenAnswer(invoc -> new StopStructures(
                invoc.getArgument(0, ICommandSender.class),
                executor,
                localizer,
                ITextFactory.getSimpleTextFactory(),
                structureActivityManager
            ));
    }

    @Test
    void test()
    {
        Assertions.assertDoesNotThrow(() -> factory.newStopStructures(commandSender).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(structureActivityManager).shutDown();
    }
}
