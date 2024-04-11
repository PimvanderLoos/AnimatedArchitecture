package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
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
class SetNameTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock
    private ToolUserManager toolUserManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SetName.IFactory factory;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        initCommandSenderPermissions(commandSender, true, true);

        factory = new AssistedFactoryMocker<>(SetName.class, SetName.IFactory.class, Mockito.CALLS_REAL_METHODS)
            .setMock(ILocalizer.class, UnitTestUtil.initLocalizer())
            .setMock(ToolUserManager.class, toolUserManager)
            .setMock(ITextFactory.class, ITextFactory.getSimpleTextFactory())
            .getFactory();
    }

    @Test
    void testExecution()
    {
        final UUID uuid = UUID.randomUUID();
        final String name = "newDoor";

        final Creator toolUser = Mockito.mock(Creator.class);
        Mockito.when(toolUser.handleInput(name)).thenReturn(CompletableFuture.completedFuture(true));
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);
        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));

        Assertions.assertDoesNotThrow(() -> factory.newSetName(commandSender, name).run().get(1, TimeUnit.SECONDS));

        Mockito.verify(toolUser).handleInput(name);
    }

    @Test
    void testIncorrectToolUser()
    {
        final UUID uuid = UUID.randomUUID();
        final String name = "newDoor";

        final ToolUser toolUser = Mockito.mock(ToolUser.class);
        Mockito.when(toolUser.handleInput(true)).thenReturn(CompletableFuture.completedFuture(null));
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);
        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));

        Assertions.assertDoesNotThrow(() -> factory.newSetName(commandSender, name).run().get(1, TimeUnit.SECONDS));

        Mockito.verify(toolUser, Mockito.never()).handleInput(name);
    }

    @Test
    void testServer()
    {
        Assertions.assertDoesNotThrow(
            () -> factory.newSetName(Mockito.mock(IServer.class, Answers.CALLS_REAL_METHODS), "newDoor")
                         .run().get(1, TimeUnit.SECONDS));
    }
}
