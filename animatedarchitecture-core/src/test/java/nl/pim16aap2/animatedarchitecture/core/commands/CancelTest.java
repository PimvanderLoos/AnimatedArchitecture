package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureSpecificationManager;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.animatedarchitecture.core.commands.CommandTestingUtil.initCommandSenderPermissions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CancelTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock
    private StructureSpecificationManager doorSpecificationManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Cancel.IFactory factory;

    @Mock
    private ToolUserManager toolUserManager;

    @Mock
    private IExecutor executor;

    @BeforeEach
    void init()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final UUID uuid = UUID.randomUUID();

        initCommandSenderPermissions(commandSender, true, true);
        when(commandSender.getUUID()).thenReturn(uuid);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        when(factory
            .newCancel(Mockito.any(ICommandSender.class)))
            .thenAnswer(invoc -> new Cancel(
                invoc.getArgument(0, ICommandSender.class),
                executor,
                localizer,
                ITextFactory.getSimpleTextFactory(),
                toolUserManager,
                doorSpecificationManager)
            );
    }

    @Test
    void test()
    {
        Assertions.assertDoesNotThrow(() -> factory.newCancel(commandSender).run().get(1, TimeUnit.SECONDS));

        verify(toolUserManager).cancelToolUser(commandSender);
        verify(doorSpecificationManager).cancelRequest(commandSender);
    }
}
