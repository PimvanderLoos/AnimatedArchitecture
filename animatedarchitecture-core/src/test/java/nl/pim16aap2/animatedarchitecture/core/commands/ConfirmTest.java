package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.animatedarchitecture.core.commands.CommandTestingUtil.initCommandSenderPermissions;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfirmTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock
    private ToolUser toolUser;

    @Mock
    private ToolUserManager toolUserManager;

    @Mock
    private IExecutor executor;

    private Confirm.IFactory factory;

    private UUID uuid;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        uuid = UUID.randomUUID();

        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        initCommandSenderPermissions(commandSender, true, true);
        when(commandSender.getUUID()).thenReturn(uuid);
        when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));
        when(toolUser.handleInput(true)).thenReturn(CompletableFuture.completedFuture(null));

        factory = new AssistedFactoryMocker<>(Confirm.class, Confirm.IFactory.class, Mockito.CALLS_REAL_METHODS)
            .setMock(ILocalizer.class, UnitTestUtil.initLocalizer())
            .setMock(ITextFactory.class, ITextFactory.getSimpleTextFactory())
            .setMock(ToolUserManager.class, toolUserManager)
            .setMock(IExecutor.class, executor)
            .getFactory();
    }

    @Test
    void testServer()
    {
        // Ensure the server running the method does not result in a ToolUser being started.
        Assertions.assertDoesNotThrow(
            () -> factory.newConfirm(Mockito.mock(IServer.class, Answers.CALLS_REAL_METHODS))
                .run()
                .get(1, TimeUnit.SECONDS)
        );
        Mockito.verify(toolUserManager, Mockito.never()).getToolUser(Mockito.any(UUID.class));
    }

    @Test
    void test()
    {
        Assertions.assertDoesNotThrow(() -> factory.newConfirm(commandSender).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).getToolUser(uuid);
        Mockito.verify(toolUser).handleInput(true);
        Mockito.verify(commandSender, Mockito.never()).sendMessage(Mockito.any(Text.class));

        when(toolUserManager.getToolUser(Mockito.any(UUID.class))).thenReturn(Optional.empty());
        Assertions.assertDoesNotThrow(() -> factory.newConfirm(commandSender).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager, Mockito.times(2)).getToolUser(uuid);
        Mockito.verify(toolUser).handleInput(true);
        Mockito.verify(commandSender).sendMessage(Mockito.any(Text.class));
    }
}
