package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.PermissionLevel;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.inject.Provider;
import java.util.concurrent.CompletableFuture;

import static nl.pim16aap2.bigdoors.commands.DelayedCommandTest.initInputRequestFactory;
import static org.mockito.AdditionalAnswers.delegatesTo;

@Timeout(1)
@SuppressWarnings("unused")
class AddOwnerDelayedTest
{
    @Spy DelayedCommandInputManager delayedCommandInputManager =
        new DelayedCommandInputManager(Mockito.mock(DebuggableRegistry.class));
    @Mock ILocalizer localizer;
    @Mock DelayedCommandInputRequest.IFactory<AddOwnerDelayed.DelayedInput> inputRequestFactory;
    @InjectMocks DelayedCommand.Context context;

    @Mock CommandFactory commandFactory;
    @SuppressWarnings("unchecked") Provider<CommandFactory> commandFactoryProvider =
        Mockito.mock(Provider.class, delegatesTo((Provider<CommandFactory>) () -> commandFactory));

    @Mock ICommandSender commandSender;

    @Mock AbstractDoor door;
    DoorRetriever doorRetriever;
    @InjectMocks DoorRetrieverFactory doorRetrieverFactory;

    @Mock AddOwner addOwner;
    @Mock IPPlayer targetPlayer;

    AutoCloseable openMocks;

    @BeforeEach
    void init()
    {
        openMocks = MockitoAnnotations.openMocks(this);

        Mockito.when(localizer.getMessage(Mockito.anyString(), ArgumentMatchers.<String>any())).thenAnswer(
            invocation -> invocation.getArgument(0, String.class));
        initInputRequestFactory(inputRequestFactory, localizer, delayedCommandInputManager);

        doorRetriever = doorRetrieverFactory.of(door);

        Mockito.when(addOwner.run()).thenReturn(CompletableFuture.completedFuture(true));

        Mockito.when(commandFactory.newAddOwner(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
               .thenReturn(addOwner);
    }

    @AfterEach
    void cleanup()
        throws Exception
    {
        openMocks.close();
    }

    @Test
    @SneakyThrows
    void normal()
    {
        final AddOwnerDelayed addOwnerDelayed = new AddOwnerDelayed(context, inputRequestFactory);

        final CompletableFuture<Boolean> result0 = addOwnerDelayed.runDelayed(commandSender, doorRetriever);
        final AddOwnerDelayed.DelayedInput input = new AddOwnerDelayed.DelayedInput(targetPlayer);
        final CompletableFuture<Boolean> result1 = addOwnerDelayed.provideDelayedInput(commandSender, input);

        Assertions.assertTrue(result0.get());
        Assertions.assertTrue(result1.get());

        Mockito.verify(commandFactory, Mockito.times(1))
               .newAddOwner(commandSender, doorRetriever, input.getTargetPlayer(), PermissionLevel.USER);
    }
}
