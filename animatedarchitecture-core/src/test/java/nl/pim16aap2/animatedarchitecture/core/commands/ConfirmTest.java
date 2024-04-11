package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
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
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.animatedarchitecture.core.commands.CommandTestingUtil.initCommandSenderPermissions;

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

    private Confirm.IFactory factory;

    private UUID uuid;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        uuid = UUID.randomUUID();

        initCommandSenderPermissions(commandSender, true, true);
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);
        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));
        Mockito.when(toolUser.handleInput(true)).thenReturn(CompletableFuture.completedFuture(null));

        factory = new AssistedFactoryMocker<>(Confirm.class, Confirm.IFactory.class, Mockito.CALLS_REAL_METHODS)
            .setMock(ILocalizer.class, UnitTestUtil.initLocalizer())
            .setMock(ToolUserManager.class, toolUserManager)
            .setMock(ITextFactory.class, ITextFactory.getSimpleTextFactory())
            .getFactory();
    }

    @Test
    void testServer()
    {
        // Ensure the server running the method does not result in a ToolUser being started.
        Assertions.assertDoesNotThrow(
            () -> factory.newConfirm(Mockito.mock(IServer.class, Answers.CALLS_REAL_METHODS)).run()
                         .get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager, Mockito.never()).getToolUser(Mockito.any(UUID.class));
    }

    @Test
    void test()
    {
        Assertions.assertDoesNotThrow(() -> factory.newConfirm(commandSender).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).getToolUser(uuid);
        Mockito.verify(toolUser).handleInput(true);
        Mockito.verify(commandSender, Mockito.never()).sendMessage(Mockito.any(Text.class));

        Mockito.when(toolUserManager.getToolUser(Mockito.any(UUID.class))).thenReturn(Optional.empty());
        Assertions.assertDoesNotThrow(() -> factory.newConfirm(commandSender).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager, Mockito.times(2)).getToolUser(uuid);
        Mockito.verify(toolUser).handleInput(true);
        Mockito.verify(commandSender).sendMessage(Mockito.any(Text.class));
    }
}
