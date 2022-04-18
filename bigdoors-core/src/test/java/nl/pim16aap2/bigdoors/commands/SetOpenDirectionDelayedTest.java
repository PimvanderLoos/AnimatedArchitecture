package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
import nl.pim16aap2.testing.logging.LogInspector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.inject.Provider;
import java.util.concurrent.CompletableFuture;

@Timeout(1)
class SetOpenDirectionDelayedTest
{
    @Mock ILocalizer localizer;
    @Mock CommandFactory commandFactory;
    @Mock DelayedCommandInputRequest.IFactory<RotateDirection> inputRequestFactory;

    DelayedCommandInputManager delayedCommandInputManager;

    @Mock ICommandSender commandSender0;

    @Mock AbstractDoor door0;
    @Mock AbstractDoor door1;

    @Mock SetOpenDirection setOpenDirection;

    DoorRetriever doorRetriever0;
    DoorRetriever doorRetriever1;

    SetOpenDirectionDelayed setOpenDirectionDelayed;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);
        delayedCommandInputManager = new DelayedCommandInputManager(Mockito.mock(DebuggableRegistry.class));

        Mockito.when(localizer.getMessage(Mockito.anyString(), ArgumentMatchers.<String>any())).thenAnswer(
            invocation -> invocation.getArgument(0, String.class));

        Mockito.when(inputRequestFactory.create(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(),
                                                Mockito.any(), Mockito.any())).thenAnswer(
            invoc -> new DelayedCommandInputRequest<RotateDirection>(
                invoc.getArgument(0, Long.class),
                invoc.getArgument(1, ICommandSender.class),
                invoc.getArgument(2, CommandDefinition.class),
                invoc.getArgument(3),
                invoc.getArgument(4),
                invoc.getArgument(5),
                localizer,
                delayedCommandInputManager));

        final Provider<CommandFactory> commandFactoryProvider = (Provider<CommandFactory>) Mockito.mock(Provider.class);
        Mockito.when(commandFactoryProvider.get()).thenReturn(commandFactory);

        final DelayedCommand.Context context =
            new DelayedCommand.Context(delayedCommandInputManager, localizer, commandFactoryProvider);
        setOpenDirectionDelayed = new SetOpenDirectionDelayed(context, inputRequestFactory);

        final DoorRetrieverFactory doorRetrieverFactory = new DoorRetrieverFactory(
            Mockito.mock(DatabaseManager.class),
            Mockito.mock(IConfigLoader.class),
            Mockito.mock(DoorSpecificationManager.class)
        );

        doorRetriever0 = doorRetrieverFactory.of(door0);
        doorRetriever1 = doorRetrieverFactory.of(door1);

        Mockito.when(setOpenDirection.run()).thenReturn(CompletableFuture.completedFuture(true));

        Mockito.when(commandFactory.newSetOpenDirection(Mockito.any(), Mockito.any(), Mockito.any()))
               .thenReturn(setOpenDirection);

        LogInspector.get().clearHistory();
    }

    @Test
    @SneakyThrows
    void normal()
    {
        final CompletableFuture<Boolean> result0 = setOpenDirectionDelayed.runDelayed(commandSender0, doorRetriever0);
        final CompletableFuture<Boolean> result1 = setOpenDirectionDelayed.provideDelayedInput(commandSender0,
                                                                                               RotateDirection.UP);

        Assertions.assertTrue(result0.get());
        Assertions.assertTrue(result1.get());

        Mockito.verify(commandFactory, Mockito.times(1))
               .newSetOpenDirection(commandSender0, doorRetriever0, RotateDirection.UP);
    }

    @Test
    void notWaiting()
    {
        setOpenDirectionDelayed.provideDelayedInput(commandSender0, RotateDirection.UP);
        Mockito.verify(commandSender0, Mockito.times(1)).sendMessage("commands.base.error.not_waiting");
    }
}
