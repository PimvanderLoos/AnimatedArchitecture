package nl.pim16aap2.animatedarchitecture.core.commands;

import jakarta.inject.Provider;
import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class AddOwnerDelayedTest
{
    @Spy
    private final DelayedCommandInputManager delayedCommandInputManager =
        new DelayedCommandInputManager(Mockito.mock());

    private DelayedCommandInputRequest.IFactory<AddOwnerDelayed.DelayedInput> inputRequestFactory;

    @InjectMocks
    private DelayedCommand.Context context;

    @Mock
    private CommandFactory commandFactory;

    @SuppressWarnings({"unchecked", "unused"})
    private final Provider<CommandFactory> commandFactoryProvider =
        mock(Provider.class, delegatesTo((Provider<CommandFactory>) () -> commandFactory));

    @Mock
    private ICommandSender commandSender;

    @Mock
    private IExecutor executor;

    private StructureRetriever structureRetriever;

    @Mock
    private Structure structure;

    @InjectMocks
    private StructureRetrieverFactory structureRetrieverFactory;

    @Mock
    private AddOwner addOwner;

    @Mock
    private IPlayer targetPlayer;

    @BeforeEach
    void init()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        //noinspection unchecked
        inputRequestFactory = AssistedFactoryMocker
            .injectMocksFromTestClass(DelayedCommandInputRequest.IFactory.class, this)
            .injectParameter(delayedCommandInputManager)
            .getFactory();

        structureRetriever = structureRetrieverFactory.of(structure);

        when(addOwner.run()).thenReturn(CompletableFuture.completedFuture(null));
        when(commandFactory.newAddOwner(any(), any(), any(), any())).thenReturn(addOwner);
    }

    @Test
    void normal()
    {
        final AddOwnerDelayed addOwnerDelayed = new AddOwnerDelayed(context, inputRequestFactory);
        UnitTestUtil.initMessageable(commandSender);

        final CompletableFuture<?> result0 = addOwnerDelayed.runDelayed(commandSender, structureRetriever);
        final AddOwnerDelayed.DelayedInput input = new AddOwnerDelayed.DelayedInput(targetPlayer);
        final CompletableFuture<?> result1 = addOwnerDelayed.provideDelayedInput(commandSender, input);

        assertDoesNotThrow(() -> result0.get(1, TimeUnit.SECONDS));
        assertDoesNotThrow(() -> result1.get(1, TimeUnit.SECONDS));

        verify(commandFactory, times(1))
            .newAddOwner(commandSender, structureRetriever, input.targetPlayer(), PermissionLevel.USER);
    }
}
